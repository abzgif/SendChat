package com.bytegif.sendchat.viewmodel

import com.bytegif.sendchat.domain.model.Device
import com.bytegif.sendchat.domain.repository.DeviceRepository
import com.bytegif.sendchat.network.TransferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeviceListViewModel(
    private val deviceRepository: DeviceRepository,
    private val transferManager: TransferManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    val searchQuery = MutableStateFlow("")
    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val devices: StateFlow<List<Device>> = combine(
        deviceRepository.getAllKnownDevices(),
        searchQuery
    ) { allDevices, query ->
        if (query.isBlank()) {
            allDevices
        } else {
            allDevices.filter { it.name.contains(query, ignoreCase = true) || it.ip.contains(query) }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDevice(device: Device?) {
        _selectedDevice.value = device
    }

    fun selectDeviceById(deviceId: String) {
        scope.launch {
            val found = devices.value.firstOrNull { it.id == deviceId }
                ?: deviceRepository.getPairedDeviceById(deviceId)?.toDevice()
            if (found != null) {
                _selectedDevice.value = found
            }
        }
    }

    fun refreshDiscovery() {
        scope.launch {
            _isRefreshing.value = true
            transferManager.discoveryManager.refresh()
            _isRefreshing.value = false
        }
    }

    fun forgetDevice(deviceId: String) {
        scope.launch {
            deviceRepository.removePairedDevice(deviceId)
            if (_selectedDevice.value?.id == deviceId) {
                _selectedDevice.value = null
            }
        }
    }

    fun renameDevice(deviceId: String, newName: String) {
        scope.launch {
            deviceRepository.updateDeviceName(deviceId, newName)
        }
    }

    fun toggleDeviceMute(deviceId: String, isMuted: Boolean) {
        scope.launch {
            deviceRepository.updateDeviceMute(deviceId, isMuted)
            if (_selectedDevice.value?.id == deviceId) {
                _selectedDevice.value = _selectedDevice.value?.copy(isMuted = isMuted)
            }
        }
    }
}
