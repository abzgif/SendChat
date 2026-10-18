package com.bytegif.sendchat.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.bytegif.sendchat.data.db.AndroidAppContextHolder
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class AndroidNsdDiscoveryManager(private val context: Context? = null) : DiscoveryManager {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    override val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices.asStateFlow()

    private val deviceMap = ConcurrentHashMap<String, Device>()
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private var multicastLock: WifiManager.MulticastLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var udpHelper: UdpDiscoveryHelper? = null
    private var cleanupJob: Job? = null

    private var localDeviceId: String = ""
    private val SERVICE_TYPE = "_sendchat._tcp."
    private val resolveMutex = Mutex()

    private fun getContext(): Context? {
        return context ?: AndroidAppContextHolder.appContext
    }

    private fun getNsd(): NsdManager? {
        if (nsdManager != null) return nsdManager
        val ctx = getContext() ?: return null
        nsdManager = ctx.getSystemService(Context.NSD_SERVICE) as? NsdManager
        return nsdManager
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks() {
        val ctx = getContext() ?: return
        runCatching {
            val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                if (multicastLock == null) {
                    multicastLock = wifiManager.createMulticastLock("SendChat:MulticastLock").apply {
                        setReferenceCounted(true)
                        acquire()
                    }
                }
                if (wifiLock == null) {
                    wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "SendChat:WifiLock").apply {
                        setReferenceCounted(true)
                        acquire()
                    }
                }
            }
        }
    }

    private fun releaseLocks() {
        runCatching {
            multicastLock?.let { if (it.isHeld) it.release() }
            multicastLock = null
            wifiLock?.let { if (it.isHeld) it.release() }
            wifiLock = null
        }
    }

    override suspend fun start(localDevice: Device) {
        stop()
        localDeviceId = localDevice.id
        acquireLocks()

        // 1. Start UDP Broadcast Beacon Discovery
        udpHelper = UdpDiscoveryHelper(scope) { discovered ->
            if (discovered.id != localDeviceId) {
                deviceMap[discovered.id] = discovered
                _discoveredDevices.value = deviceMap.values.toList()
            }
        }.also { it.start(localDevice) }

        // 2. Start Android NSD Discovery (mDNS)
        val nsd = getNsd()
        if (nsd != null) {
            // Register Service
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "SendChat-${localDevice.id.take(8)}"
                serviceType = SERVICE_TYPE
                port = localDevice.port
                setAttribute("id", localDevice.id)
                setAttribute("name", localDevice.name)
                setAttribute("platform", localDevice.platform.name)
                setAttribute("fp", localDevice.publicKeyFingerprint)
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(p0: NsdServiceInfo?) {}
                override fun onRegistrationFailed(p0: NsdServiceInfo?, p1: Int) {}
                override fun onServiceUnregistered(p0: NsdServiceInfo?) {}
                override fun onUnregistrationFailed(p0: NsdServiceInfo?, p1: Int) {}
            }
            runCatching { nsd.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener) }

            // Start Discovery
            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String?) {}
                override fun onServiceFound(service: NsdServiceInfo?) {
                    if (service == null) return
                    val type = service.serviceType
                    if (type.contains("sendchat")) {
                        scope.launch {
                            resolveServiceSafely(service)
                        }
                    }
                }

                override fun onServiceLost(service: NsdServiceInfo?) {
                    if (service == null) return
                    val removed = deviceMap.values.firstOrNull {
                        it.name == service.serviceName || service.serviceName.contains(it.id.take(8))
                    }
                    if (removed != null) {
                        deviceMap.remove(removed.id)
                        _discoveredDevices.value = deviceMap.values.toList()
                    }
                }

                override fun onDiscoveryStopped(serviceType: String?) {}
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            }
            runCatching { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener) }
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

    @Suppress("DEPRECATION")
    private suspend fun resolveServiceSafely(serviceInfo: NsdServiceInfo) {
        val nsd = getNsd() ?: return
        resolveMutex.withLock {
            runCatching {
                nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(service: NsdServiceInfo?, errorCode: Int) {}

                    override fun onServiceResolved(resolved: NsdServiceInfo?) {
                        if (resolved == null) return
                        val attributes = resolved.attributes

                        val id = attributes["id"]?.let { String(it) } ?: return
                        if (id == localDeviceId) return

                        val name = attributes["name"]?.let { String(it) } ?: resolved.serviceName
                        val platformStr = attributes["platform"]?.let { String(it) } ?: "ANDROID"
                        val fp = attributes["fp"]?.let { String(it) } ?: ""
                        val host = resolved.host?.hostAddress ?: return

                        val device = Device(
                            id = id,
                            name = name,
                            platform = DevicePlatform.fromString(platformStr),
                            ip = host,
                            port = resolved.port,
                            publicKeyFingerprint = fp,
                            isPaired = false,
                            isOnline = true,
                            lastSeen = System.currentTimeMillis()
                        )

                        deviceMap[id] = device
                        _discoveredDevices.value = deviceMap.values.toList()
                    }
                })
            }
            delay(100) // Brief yield to prevent Android NSD resolve backlog
        }
    }

    override suspend fun refresh() {
        udpHelper?.broadcastNow()
    }

    override suspend fun stop() {
        cleanupJob?.cancel()
        cleanupJob = null
        udpHelper?.stop()
        udpHelper = null

        val nsd = getNsd()
        runCatching { registrationListener?.let { nsd?.unregisterService(it) } }
        runCatching { discoveryListener?.let { nsd?.stopServiceDiscovery(it) } }
        registrationListener = null
        discoveryListener = null

        releaseLocks()
        deviceMap.clear()
        _discoveredDevices.value = emptyList()
    }
}

actual fun createDiscoveryManager(): DiscoveryManager = AndroidNsdDiscoveryManager()
