package com.architect.kmpessentials.permissions.delegates

import com.architect.kmpessentials.internal.ActionNoParams
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject

class LocationPermissionsDelegate(
    val runAction: ActionNoParams,
    val onDenied: ActionNoParams? = null
) : NSObject(),
    CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        when (manager.authorizationStatus()) {
            kCLAuthorizationStatusNotDetermined -> {// Not Determined
                onDenied?.invoke()
            }

            kCLAuthorizationStatusDenied -> // denied
            {
                onDenied?.invoke()
            }

            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> { // granted
                runAction()
            }
        }
    }
}