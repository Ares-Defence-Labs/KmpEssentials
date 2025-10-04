package com.architect.testclient

import com.architect.kmpessentials.backgrounding.KmpBackgrounding
import com.architect.kmpessentials.connectivity.KmpConnectivity
import com.architect.kmpessentials.launcher.KmpLauncher
import com.architect.kmpessentials.lifecycle.KmpLifecycle
import com.architect.kmpessentials.localNotifications.KmpLocalNotifications
import com.architect.kmpessentials.logging.KmpLogging
import com.architect.kmpessentials.permissions.KmpPermissionsManager
import com.architect.kmpessentials.permissions.Permission
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

actual class SharedComponent {
    actual companion object {
        fun runProcess() {

        }

        actual fun runNativeHandler() {

            KmpLogging.writeInfo("", "IS CONNECTED - ${KmpConnectivity.isConnected()}")

//            GlobalScope.launch {
//                KmpConnectivity.listenToConnectionChange {
//                    KmpLogging.writeInfo("", "CONNECTION STATE - $it")
//                }
//            }

            GlobalScope.launch {

                delay(5000)
                KmpLogging.writeInfo("", "IS CONNECTED AGAIN - ${KmpConnectivity.isConnected()}")
            }

        }
    }

}