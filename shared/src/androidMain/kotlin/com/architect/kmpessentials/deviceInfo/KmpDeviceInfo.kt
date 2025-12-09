package com.architect.kmpessentials.deviceInfo

import android.content.Context
import android.location.LocationManager
import android.os.Build
import com.architect.kmpessentials.KmpAndroid
import kotlinx.datetime.TimeZone

actual class KmpDeviceInfo {
    actual companion object {
        actual fun getDeviceTimeZone(): String {
            return TimeZone.currentSystemDefault().id
        }

        actual fun isLocationAvailable(): Boolean {
            val lm = KmpAndroid.getCurrentApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE) as LocationManager

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Checks the global “Location” toggle (not app permission)
                lm.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        @Suppress("DEPRECATION")
                        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        }

        actual fun getRunningPlatform(): DevicePlatform {
            return DevicePlatform.Android
        }

        actual fun getDeviceSpecs(): DeviceSpecs {
            return DeviceSpecs(Build.MODEL, "${Build.VERSION.SDK_INT}", Build.MANUFACTURER)
        }
    }
}

