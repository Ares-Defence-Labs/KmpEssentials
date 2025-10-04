package com.architect.kmpessentials.connectivity

import com.architect.kmpessentials.internal.ActionBoolParams
import kotlin.concurrent.Volatile

import kotlinx.cinterop.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.*
import platform.darwin.*
import platform.posix.*

private object NWConnectivity {
    private val monitor = nw_path_monitor_create()

    @Volatile
    private var started = false

    @Volatile
    var latestPath: nw_path_t? = null
        private set

    fun ensureStarted() {
        if (started) return
        started = true

        // Deliver updates on the main queue (works fine for UI + simplicity)
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_set_update_handler(monitor) { path ->
            latestPath = path
        }
        nw_path_monitor_start(monitor)
    }

    fun isConnectedNow(): Boolean {
        ensureStarted()
        val status = latestPath?.let { nw_path_get_status(it) } ?: nw_path_status_unsatisfied
        return status == nw_path_status_satisfied
    }

    fun currentInterfaceName(): String? {
        ensureStarted()
        val path = latestPath ?: return null
        // Prefer Wi-Fi, then cellular, then wired
        return when {
            nw_path_uses_interface_type(path, nw_interface_type_wifi) -> "Wi-Fi"
            nw_path_uses_interface_type(path, nw_interface_type_cellular) -> "Cellular"
            nw_path_uses_interface_type(path, nw_interface_type_wired) -> "Wired"
            else -> null
        }
    }

    fun observeConnectivity() = callbackFlow<Boolean> {
        // Create a dedicated monitor for this flow so it gets callbacks even
        // if the singleton already started before app launch.
        val flowMonitor = nw_path_monitor_create()
        nw_path_monitor_set_queue(flowMonitor, dispatch_get_main_queue())
        nw_path_monitor_set_update_handler(flowMonitor) { path ->
            trySend(nw_path_get_status(path) == nw_path_status_satisfied)
        }
        nw_path_monitor_start(flowMonitor)

        awaitClose {
            nw_path_monitor_cancel(flowMonitor)
        }
    }
}

/** Helpers to read IPv4/IPv6 from active interfaces using getifaddrs */
@OptIn(ExperimentalForeignApi::class)
private fun getIpAddress(preferWifi: Boolean, ipv6: Boolean): String? = memScoped {
    val ifaddrPtr = alloc<CPointerVar<ifaddrs>>()
    if (getifaddrs(ifaddrPtr.ptr) != 0) return null
    val head = ifaddrPtr.value ?: return null

    try {
        // Common iOS interface names:
        //  - "en0": Wi-Fi
        //  - "pdp_ip0": Cellular
        fun scoreInterface(name: String): Int = when (name) {
            "en0" -> if (preferWifi) 2 else 1
            "pdp_ip0" -> if (preferWifi) 1 else 2
            else -> 0
        }

        var bestScore = -1
        var bestAddress: String? = null

        var cursor: CPointer<ifaddrs>? = head
        while (cursor != null) {
            val ifa = cursor.pointed
            val name = ifa.ifa_name?.toKString() ?: ""
            val addr = ifa.ifa_addr
            if (addr != null) {
                val family = addr.pointed.sa_family.convert<Int>()
                val wantFamily = if (ipv6) AF_INET6 else AF_INET
                if (family == wantFamily) {
                    // Convert sockaddr to string
                    val hostBuf = ByteArray(NI_MAXHOST)
                    val res = getnameinfo(
                        addr,
                        (if (ipv6) sizeOf<sockaddr_in6>() else sizeOf<sockaddr_in>()).convert(),
                        hostBuf.refTo(0),
                        NI_MAXHOST.convert(),
                        null,
                        0u,
                        NI_NUMERICHOST
                    )
                    if (res == 0) {
                        val ip = hostBuf.toKString()
                        val s = scoreInterface(name)
                        if (s > bestScore) {
                            bestScore = s
                            bestAddress = ip
                        }
                    }
                }
            }
            cursor = ifa.ifa_next
        }
        bestAddress
    } finally {
        freeifaddrs(head)
    }
}

actual class KmpConnectivity {
    actual companion object {
        actual fun isConnected(): Boolean {
            return NWConnectivity.isConnectedNow()
        }

        /**
         * Returns a human-readable transport: "Wi-Fi", "Cellular", or "Wired".
         * NOTE: SSID/BSSID is NOT returned—Apple restricts that behind the
         * Access WiFi Information entitlement + location permissions.
         */
        actual fun getCurrentNetworkName(): String? {
            return NWConnectivity.currentInterfaceName()
        }

        actual suspend fun listenToConnectionChange(connectionState: ActionBoolParams) {
            NWConnectivity.observeConnectivity().distinctUntilChanged().collect { isUp ->
                connectionState(isUp)
            }
        }

        actual suspend fun getCurrentNetworkIPv4(): String? {
            // Prefer Wi-Fi if available
            val preferWifi = NWConnectivity.currentInterfaceName() == "Wi-Fi"
            return getIpAddress(preferWifi = preferWifi, ipv6 = false)
        }

        actual suspend fun getCurrentNetworkIPv6(): String? {
            val preferWifi = NWConnectivity.currentInterfaceName() == "Wi-Fi"
            return getIpAddress(preferWifi = preferWifi, ipv6 = true)
        }
    }
}