package com.bytegif.sendchat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.bytegif.sendchat.data.db.Paired_device_entity
import com.bytegif.sendchat.data.db.SendChatDatabase
import com.bytegif.sendchat.domain.model.Device
import com.bytegif.sendchat.domain.model.DevicePlatform
import com.bytegif.sendchat.domain.model.PairedDevice
import com.bytegif.sendchat.domain.repository.DeviceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DeviceRepositoryImpl(
    private val database: SendChatDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : DeviceRepository {

    private val discoveredDevicesState = MutableStateFlow<Map<String, Device>>(emptyMap())

    override fun getPairedDevices(): Flow<List<PairedDevice>> {
        return database.sendChatDatabaseQueries
            .selectAllPairedDevices()
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getDiscoveredDevices(): Flow<List<Device>> {
        return discoveredDevicesState.map { it.values.toList() }
    }

    override fun getAllKnownDevices(): Flow<List<Device>> {
        return combine(getPairedDevices(), getDiscoveredDevices()) { pairedList, discoveredList ->
            val pairedMap = pairedList.associateBy { it.id }
            val discoveredMap = discoveredList.associateBy { it.id }

            val allIds = (pairedMap.keys + discoveredMap.keys).toSet()

            allIds.map { id ->
                val paired = pairedMap[id]
                val discovered = discoveredMap[id]

                if (paired != null && discovered != null) {
                    Device(
                        id = paired.id,
                        name = paired.name,
                        platform = paired.platform,
                        ip = discovered.ip,
                        port = discovered.port,
                        publicKeyFingerprint = paired.publicKeyFingerprint,
                        isPaired = true,
                        isOnline = true,
                        lastSeen = discovered.lastSeen,
                        autoSyncClipboard = paired.autoSyncClipboard,
                        isMuted = paired.isMuted
                    )
                } else if (paired != null) {
                    Device(
                        id = paired.id,
                        name = paired.name,
                        platform = paired.platform,
                        ip = paired.lastKnownIp,
                        port = paired.lastKnownPort,
                        publicKeyFingerprint = paired.publicKeyFingerprint,
                        isPaired = true,
                        isOnline = false,
                        lastSeen = paired.pairedAt,
                        autoSyncClipboard = paired.autoSyncClipboard,
                        isMuted = paired.isMuted
                    )
                } else {
                    val disc = discovered!!
                    Device(
                        id = disc.id,
                        name = disc.name,
                        platform = disc.platform,
                        ip = disc.ip,
                        port = disc.port,
                        publicKeyFingerprint = disc.publicKeyFingerprint,
                        isPaired = false,
                        isOnline = true,
                        lastSeen = disc.lastSeen,
                        autoSyncClipboard = false,
                        isMuted = false
                    )
                }
            }.sortedWith(
                compareByDescending<Device> { it.isOnline }
                    .thenByDescending { it.isPaired }
                    .thenBy { it.name.lowercase() }
            )
        }
    }

    override suspend fun savePairedDevice(device: PairedDevice) = withContext(dispatcher) {
        database.sendChatDatabaseQueries.insertOrReplacePairedDevice(
            id = device.id,
            name = device.name,
            platform = device.platform.name,
            publicKeyFingerprint = device.publicKeyFingerprint,
            lastKnownIp = device.lastKnownIp,
            lastKnownPort = device.lastKnownPort.toLong(),
            pairedAt = device.pairedAt,
            autoSyncClipboard = if (device.autoSyncClipboard) 1L else 0L,
            isMuted = if (device.isMuted) 1L else 0L
        )
    }

    override suspend fun removePairedDevice(deviceId: String) = withContext(dispatcher) {
        database.sendChatDatabaseQueries.deletePairedDevice(deviceId)
    }

    override suspend fun updateDeviceName(deviceId: String, newName: String) = withContext(dispatcher) {
        database.sendChatDatabaseQueries.updatePairedDeviceName(newName, deviceId)
    }

    override suspend fun updateDeviceClipboardSync(deviceId: String, enabled: Boolean) = withContext(dispatcher) {
        val existing = database.sendChatDatabaseQueries.selectPairedDeviceById(deviceId).executeAsOneOrNull()
        if (existing == null) {
            val disc = discoveredDevicesState.value[deviceId]
            if (disc != null) {
                database.sendChatDatabaseQueries.insertOrReplacePairedDevice(
                    id = disc.id,
                    name = disc.name,
                    platform = disc.platform.name,
                    publicKeyFingerprint = disc.publicKeyFingerprint,
                    lastKnownIp = disc.ip,
                    lastKnownPort = disc.port.toLong(),
                    pairedAt = disc.lastSeen,
                    autoSyncClipboard = if (enabled) 1L else 0L,
                    isMuted = 0L
                )
            }
        } else {
            database.sendChatDatabaseQueries.updatePairedDeviceClipboardSync(
                if (enabled) 1L else 0L,
                deviceId
            )
        }
    }

    override suspend fun updateDeviceMute(deviceId: String, isMuted: Boolean) = withContext(dispatcher) {
        val existing = database.sendChatDatabaseQueries.selectPairedDeviceById(deviceId).executeAsOneOrNull()
        if (existing == null) {
            val disc = discoveredDevicesState.value[deviceId]
            if (disc != null) {
                database.sendChatDatabaseQueries.insertOrReplacePairedDevice(
                    id = disc.id,
                    name = disc.name,
                    platform = disc.platform.name,
                    publicKeyFingerprint = disc.publicKeyFingerprint,
                    lastKnownIp = disc.ip,
                    lastKnownPort = disc.port.toLong(),
                    pairedAt = disc.lastSeen,
                    autoSyncClipboard = 0L,
                    isMuted = if (isMuted) 1L else 0L
                )
            }
        } else {
            database.sendChatDatabaseQueries.updatePairedDeviceMute(
                if (isMuted) 1L else 0L,
                deviceId
            )
        }
    }

    override suspend fun updateDiscoveredDevices(devices: List<Device>) = withContext(dispatcher) {
        discoveredDevicesState.value = devices.associateBy { it.id }
    }

    override suspend fun updateLastSeen(deviceId: String, ip: String, port: Int, timestamp: Long) = withContext(dispatcher) {
        database.sendChatDatabaseQueries.updatePairedDeviceLastSeen(ip, port.toLong(), deviceId)
    }

    override suspend fun getPairedDeviceById(deviceId: String): PairedDevice? = withContext(dispatcher) {
        database.sendChatDatabaseQueries.selectPairedDeviceById(deviceId).executeAsOneOrNull()?.toDomain()
    }

    private fun Paired_device_entity.toDomain(): PairedDevice {
        return PairedDevice(
            id = id,
            name = name,
            platform = DevicePlatform.fromString(platform),
            publicKeyFingerprint = publicKeyFingerprint,
            lastKnownIp = lastKnownIp,
            lastKnownPort = lastKnownPort.toInt(),
            pairedAt = pairedAt,
            autoSyncClipboard = autoSyncClipboard == 1L,
            isMuted = isMuted == 1L
        )
    }
}
