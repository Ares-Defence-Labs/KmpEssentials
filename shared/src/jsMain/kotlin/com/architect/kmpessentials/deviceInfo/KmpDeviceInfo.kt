package com.architect.kmpessentials.deviceInfo

import kotlinx.browser.window
import kotlinx.datetime.TimeZone

actual class KmpDeviceInfo {
    actual companion object {

        actual fun getDeviceTimeZone(): String {
            return TimeZone.currentSystemDefault().id
        }

        actual fun getRunningPlatform(): DevicePlatform {
            return DevicePlatform.JS
        }

        actual fun isLocationAvailable(): Boolean {
            return false
        }

        actual fun getDeviceSpecs(): DeviceSpecs {
            return DeviceSpecs(
                window.navigator.platform,
                window.navigator.appVersion,
                window.navigator.appName
            )
        }
    }
}

