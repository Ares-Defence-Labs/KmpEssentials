package com.architect.kmpessentials.deviceInfo

import kotlinx.datetime.TimeZone
import java.net.InetAddress
import java.util.Locale

actual class KmpDeviceInfo {
    actual companion object {
        actual fun getDeviceTimeZone(): String {
            return TimeZone.currentSystemDefault().id
        }

        actual fun isLocationAvailable(): Boolean {
            return false
        }

        actual fun getRunningPlatform(): DevicePlatform {
            val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
            if (osName.contains("win")) {
                return DevicePlatform.Windows
            } else if (osName.contains("mac")) {
                return DevicePlatform.MacOS
            } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                return DevicePlatform.Linux
            } else {
                return DevicePlatform.Unknown
            }
        }

        actual fun getDeviceSpecs(): DeviceSpecs {
            return DeviceSpecs(
                getDeviceModelName(),
                System.getProperty("os.version"),
                getDeviceManufacturer()
            )
        }

        private fun getDeviceModelName(): String {
            return try {
                InetAddress.getLocalHost().hostName
            } catch (e: Exception) {
                ""
            }
        }

        private fun getDeviceManufacturer(): String {
            return when (getRunningPlatform()) {
                DevicePlatform.Windows -> "Microsoft"
                DevicePlatform.MacOS -> "Apple"
                else -> "Linux"
            }
        }
    }
}

