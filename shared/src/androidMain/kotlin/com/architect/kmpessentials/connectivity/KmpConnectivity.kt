package com.architect.kmpessentials.connectivity

import com.architect.kmpessentials.internal.ActionBoolParams
import dev.tmapps.konnection.Konnection
import kotlinx.coroutines.flow.distinctUntilChanged

actual class KmpConnectivity {
    actual companion object {
        private val konnection = Konnection.instance
        actual fun isConnected(): Boolean {
            return konnection.isConnected()
        }

        actual fun getCurrentNetworkName(): String? {
            return konnection.getCurrentNetworkConnection()?.name
        }

        actual suspend fun listenToConnectionChange(connectionState: ActionBoolParams) {
            konnection.observeHasConnection().distinctUntilChanged().collect { hasConnection ->
                connectionState(hasConnection)
            }
        }

        actual suspend  fun getCurrentNetworkIPv4(): String?{
            return konnection.getInfo()?.ipv4
        }

        actual suspend  fun getCurrentNetworkIPv6(): String?{
            return konnection.getInfo()?.ipv6
        }
    }
}