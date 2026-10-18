package com.bytegif.sendchat.viewmodel

import com.bytegif.sendchat.domain.model.Device
import com.bytegif.sendchat.domain.repository.SettingsRepository
import com.bytegif.sendchat.network.TransferManager
import com.bytegif.sendchat.network.protocol.PairResponse
import com.bytegif.sendchat.network.server.IncomingPairPrompt
import com.bytegif.sendchat.platform.PlatformInfo
import com.bytegif.sendchat.security.CryptoEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PairingUiState {
    data object Idle : PairingUiState()
    data class PairingWith(val device: Device) : PairingUiState()
    data object Connecting : PairingUiState()
    data class Success(val message: String) : PairingUiState()
    data class Error(val error: String) : PairingUiState()
}

class PairingViewModel(
    private val settingsRepository: SettingsRepository,
    private val transferManager: TransferManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _pairingState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val pairingState: StateFlow<PairingUiState> = _pairingState.asStateFlow()

    private val _incomingPrompt = MutableStateFlow<IncomingPairPrompt?>(null)
    val incomingPrompt: StateFlow<IncomingPairPrompt?> = _incomingPrompt.asStateFlow()

    val manualIpInput = MutableStateFlow("")
    val manualPortInput = MutableStateFlow("42831")

    init {
        // Collect incoming pair requests from TransferServer
        scope.launch {
            transferManager.incomingPairRequests.collect { prompt ->
                _incomingPrompt.value = prompt
            }
        }
    }

    fun requestPairing(targetDevice: Device) {
        scope.launch {
            _pairingState.value = PairingUiState.PairingWith(targetDevice)
            val response = transferManager.pairWithDevice(targetDevice)
            if (response.accepted) {
                _pairingState.value = PairingUiState.Success("Successfully paired with ${targetDevice.name}!")
            } else {
                _pairingState.value = PairingUiState.Error(response.error ?: "Pairing declined by ${targetDevice.name}")
            }
        }
    }

    fun requestPairingByIp(ip: String, port: Int = 42831) {
        if (ip.isBlank()) {
            _pairingState.value = PairingUiState.Error("Please enter a valid IP address")
            return
        }
        scope.launch {
            _pairingState.value = PairingUiState.Connecting
            val response = transferManager.pairWithIp(ip, port)
            if (response.accepted) {
                _pairingState.value = PairingUiState.Success("Successfully paired with device at $ip!")
            } else {
                _pairingState.value = PairingUiState.Error(response.error ?: "Failed to connect to $ip:$port")
            }
        }
    }

    fun respondToIncomingPair(accepted: Boolean) {
        val prompt = _incomingPrompt.value ?: return
        _incomingPrompt.value = null
        scope.launch {
            val settings = settingsRepository.getCurrentSettings()
            val identity = CryptoEngine.getOrCreateDeviceIdentity(settings.deviceId, settings.deviceName)

            prompt.deferredResponse.complete(
                PairResponse(
                    accepted = accepted,
                    deviceId = settings.deviceId,
                    deviceName = settings.deviceName,
                    platform = PlatformInfo.platform.name,
                    publicKeyFingerprint = identity.publicKeyFingerprint,
                    certificatePem = identity.certificatePem,
                    error = if (!accepted) "Pairing declined by user" else null
                )
            )
        }
    }

    fun resetState() {
        _pairingState.value = PairingUiState.Idle
        manualIpInput.value = ""
        manualPortInput.value = "42831"
    }
}
