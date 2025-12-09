package com.architect.kmpessentials.deviceInfo

import kotlinx.datetime.TimeZone
import platform.CoreLocation.CLLocationManager

actual class KmpDeviceInfo {
    actual companion object {

        actual fun getDeviceTimeZone(): String {
            return TimeZone.currentSystemDefault().id
        }

        actual fun isLocationAvailable(): Boolean {
            return CLLocationManager.locationServicesEnabled()
        }

        actual fun getRunningPlatform(): DevicePlatform {
            return DevicePlatform.AppleWatch
        }

        actual fun getDeviceSpecs(): DeviceSpecs {
            return DeviceSpecs(
                "",
                "",
                "Apple Inc"
            )
        }
    }
}

