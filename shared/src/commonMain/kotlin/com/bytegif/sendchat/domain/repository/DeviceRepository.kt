package com.bytegif.sendchat.domain.repository

import com.bytegif.sendchat.domain.model.Device
import com.bytegif.sendchat.domain.model.PairedDevice
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getPairedDevices(): Flow<List<PairedDevice>>
    fun getDiscoveredDevices(): Flow<List<Device>>
    fun getAllKnownDevices(): Flow<List<Device>>
    suspend fun savePairedDevice(device: PairedDevice)
    suspend fun removePairedDevice(deviceId: String)
    suspend fun updateDeviceName(deviceId: String, newName: String)
    suspend fun updateDeviceClipboardSync(deviceId: String, enabled: Boolean)
    suspend fun updateDeviceMute(deviceId: String, isMuted: Boolean)
    suspend fun updateDiscoveredDevices(devices: List<Device>)
    suspend fun updateLastSeen(deviceId: String, ip: String, port: Int, timestamp: Long)
    suspend fun getPairedDeviceById(deviceId: String): PairedDevice?
}
