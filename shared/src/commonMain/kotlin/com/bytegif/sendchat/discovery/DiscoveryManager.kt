package com.bytegif.sendchat.discovery

import com.bytegif.sendchat.domain.model.Device
import kotlinx.coroutines.flow.StateFlow

interface DiscoveryManager {
    val discoveredDevices: StateFlow<List<Device>>

    suspend fun start(localDevice: Device)
    suspend fun stop()
    suspend fun refresh()
}

expect fun createDiscoveryManager(): DiscoveryManager
