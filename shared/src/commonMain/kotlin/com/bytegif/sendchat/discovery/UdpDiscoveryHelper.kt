package com.bytegif.sendchat.discovery

import com.bytegif.sendchat.domain.model.Device
import com.bytegif.sendchat.domain.model.DevicePlatform
import com.bytegif.sendchat.network.protocol.UdpBeaconPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

class UdpDiscoveryHelper(
    private val scope: CoroutineScope,
    private val onDeviceDiscovered: (Device) -> Unit
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private var socket: DatagramSocket? = null
    private var broadcastJob: Job? = null
    private var receiveJob: Job? = null
    private var localDevice: Device? = null

    private val UDP_PORT = 42832

    fun start(device: Device) {
        stop()
        localDevice = device

        // Start UDP Receiver
        receiveJob = scope.launch(Dispatchers.IO) {
            runCatching {
                val s = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(UDP_PORT))
                }
                socket = s
                val buffer = ByteArray(4096)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    s.receive(packet)

                    val senderIp = packet.address.hostAddress ?: continue
                    val rawData = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)

                    runCatching {
                        val beacon = json.decodeFromString<UdpBeaconPayload>(rawData)
                        if (beacon.deviceId != device.id && beacon.protocol.startsWith("sendchat")) {
                            val discoveredDevice = Device(
                                id = beacon.deviceId,
                                name = beacon.deviceName,
                                platform = DevicePlatform.fromString(beacon.platform),
                                ip = senderIp,
                                port = beacon.port,
                                publicKeyFingerprint = beacon.publicKeyFingerprint,
                                isPaired = false,
                                isOnline = true,
                                lastSeen = System.currentTimeMillis()
                            )
                            onDeviceDiscovered(discoveredDevice)

                            // If this was an initial broadcast, reply directly with a unicast beacon
                            if (!beacon.isReply) {
                                sendDirectReply(packet.address, packet.port, device)
                            }
                        }
                    }
                }
            }
        }

        // Start Periodic Broadcaster
        broadcastJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                broadcastBeacon(isReply = false)
                delay(2500)
            }
        }
    }

    fun broadcastNow() {
        scope.launch(Dispatchers.IO) {
            broadcastBeacon(isReply = false)
        }
    }

    private fun sendDirectReply(targetAddress: InetAddress, targetPort: Int, currentDevice: Device) {
        runCatching {
            val replyPayload = UdpBeaconPayload(
                protocol = "sendchat_v1",
                deviceId = currentDevice.id,
                deviceName = currentDevice.name,
                platform = currentDevice.platform.name,
                port = currentDevice.port,
                publicKeyFingerprint = currentDevice.publicKeyFingerprint,
                isReply = true
            )
            val bytes = json.encodeToString(replyPayload).toByteArray(Charsets.UTF_8)
            val replyPacket = DatagramPacket(bytes, bytes.size, targetAddress, UDP_PORT)
            socket?.send(replyPacket)
        }
    }

    private fun broadcastBeacon(isReply: Boolean) {
        val current = localDevice ?: return
        val payload = UdpBeaconPayload(
            protocol = "sendchat_v1",
            deviceId = current.id,
            deviceName = current.name,
            platform = current.platform.name,
            port = current.port,
            publicKeyFingerprint = current.publicKeyFingerprint,
            isReply = isReply
        )
        val bytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)

        val broadcastAddresses = getBroadcastAddresses()
        for (addr in broadcastAddresses) {
            runCatching {
                val packet = DatagramPacket(bytes, bytes.size, addr, UDP_PORT)
                socket?.send(packet)
            }
        }
    }

    private fun getBroadcastAddresses(): Set<InetAddress> {
        val broadcastSet = mutableSetOf<InetAddress>()
        runCatching {
            broadcastSet.add(InetAddress.getByName("255.255.255.255"))
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                for (interfaceAddress in iface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast is Inet4Address) {
                        broadcastSet.add(broadcast)
                    }
                }
            }
        }
        return broadcastSet
    }

    fun stop() {
        broadcastJob?.cancel()
        broadcastJob = null
        receiveJob?.cancel()
        receiveJob = null
        runCatching { socket?.close() }
        socket = null
        localDevice = null
    }
}
