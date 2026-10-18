package com.bytegif.sendchat.discovery

import com.bytegif.sendchat.domain.model.Device
import com.bytegif.sendchat.domain.model.DevicePlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

class JvmJmDnsDiscoveryManager : DiscoveryManager {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    override val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices.asStateFlow()

    private val deviceMap = ConcurrentHashMap<String, Device>()
    private val jmDnsInstances = mutableListOf<JmDNS>()
    private var registeredServiceInfo: ServiceInfo? = null
    private var udpHelper: UdpDiscoveryHelper? = null
    private var cleanupJob: Job? = null
    private var localDeviceId: String = ""

    private val SERVICE_TYPE = "_sendchat._tcp.local."

    override suspend fun start(localDevice: Device) {
        stop()
        localDeviceId = localDevice.id

        // 1. Start UDP Broadcast Beacon Discovery
        udpHelper = UdpDiscoveryHelper(scope) { discovered ->
            if (discovered.id != localDeviceId) {
                deviceMap[discovered.id] = discovered
                _discoveredDevices.value = deviceMap.values.toList()
            }
        }.also { it.start(localDevice) }

        // 2. Start JmDNS Discovery (mDNS)
        val ips = getLocalIpAddresses()
        for (ip in ips) {
            runCatching {
                val jmdns = JmDNS.create(ip, ip.hostAddress)
                jmDnsInstances.add(jmdns)

                // Register listener
                jmdns.addServiceListener(SERVICE_TYPE, object : ServiceListener {
                    override fun serviceAdded(event: ServiceEvent?) {
                        event?.info?.let { resolveService(jmdns, event.name) }
                    }

                    override fun serviceRemoved(event: ServiceEvent?) {
                        val name = event?.name ?: return
                        val removed = deviceMap.values.firstOrNull { it.name == name || name.contains(it.id.take(8)) }
                        if (removed != null) {
                            deviceMap.remove(removed.id)
                            _discoveredDevices.value = deviceMap.values.toList()
                        }
                    }

                    override fun serviceResolved(event: ServiceEvent?) {
                        val info = event?.info ?: return
                        handleResolvedService(info)
                    }
                })

                // Register advertisement
                val properties = mapOf(
                    "id" to localDevice.id,
                    "name" to localDevice.name,
                    "platform" to localDevice.platform.name,
                    "fp" to localDevice.publicKeyFingerprint
                )
                val serviceName = "SendChat-${localDevice.id.take(8)}"
                val serviceInfo = ServiceInfo.create(
                    SERVICE_TYPE,
                    serviceName,
                    localDevice.port,
                    0,
                    0,
                    properties
                )
                registeredServiceInfo = serviceInfo
                jmdns.registerService(serviceInfo)
            }
        }

        // 3. Periodic cleanup of stale devices (not seen in 30s)
        cleanupJob = scope.launch {
            while (isActive) {
                delay(10_000)
                val now = System.currentTimeMillis()
                var changed = false
                deviceMap.entries.removeIf { (_, dev) ->
                    val isStale = (now - dev.lastSeen) > 30_000
                    if (isStale) changed = true
                    isStale
                }
                if (changed) {
                    _discoveredDevices.value = deviceMap.values.toList()
                }
            }
        }
    }

    private fun resolveService(jmdns: JmDNS, name: String) {
        scope.launch {
            runCatching {
                val info = jmdns.getServiceInfo(SERVICE_TYPE, name, 3000)
                if (info != null) {
                    handleResolvedService(info)
                }
            }
        }
    }

    private fun handleResolvedService(info: ServiceInfo) {
        val id = info.getPropertyString("id") ?: return
        if (id == localDeviceId) return // Do not add self

        val name = info.getPropertyString("name") ?: info.name
        val platformStr = info.getPropertyString("platform") ?: "UNKNOWN"
        val fp = info.getPropertyString("fp") ?: ""

        val hostAddresses = info.inet4Addresses
        val ip = if (hostAddresses.isNotEmpty()) {
            hostAddresses[0].hostAddress
        } else {
            info.hostAddresses.firstOrNull() ?: return
        }

        val device = Device(
            id = id,
            name = name,
            platform = DevicePlatform.fromString(platformStr),
            ip = ip,
            port = info.port,
            publicKeyFingerprint = fp,
            isPaired = false,
            isOnline = true,
            lastSeen = System.currentTimeMillis()
        )

        deviceMap[id] = device
        _discoveredDevices.value = deviceMap.values.toList()
    }

    override suspend fun refresh() {
        udpHelper?.broadcastNow()
        for (jmdns in jmDnsInstances) {
            runCatching {
                val services = jmdns.list(SERVICE_TYPE)
                for (info in services) {
                    handleResolvedService(info)
                }
            }
        }
    }

    override suspend fun stop() {
        cleanupJob?.cancel()
        cleanupJob = null
        udpHelper?.stop()
        udpHelper = null

        for (jmdns in jmDnsInstances) {
            runCatching {
                registeredServiceInfo?.let { jmdns.unregisterService(it) }
                jmdns.unregisterAllServices()
                jmdns.close()
            }
        }
        jmDnsInstances.clear()
        deviceMap.clear()
        _discoveredDevices.value = emptyList()
    }

    private fun getLocalIpAddresses(): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        addresses.add(addr)
                    }
                }
            }
        }
        return addresses
    }
}

actual fun createDiscoveryManager(): DiscoveryManager = JvmJmDnsDiscoveryManager()
