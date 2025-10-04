package com.architect.kmpessentials.permissions

import com.architect.kmpessentials.internal.ActionBoolParams
import com.architect.kmpessentials.internal.ActionNoParams
import com.architect.kmpessentials.internal.ActionPermissionStatusParams
import com.architect.kmpessentials.mainThread.KmpMainThread
import com.architect.kmpessentials.permissions.delegates.LocationPermissionsDelegate
import com.architect.kmpessentials.secureStorage.KmpPublicStorage
import kotlinx.cinterop.UnsafeNumber
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionUndetermined
import platform.Contacts.CNAuthorizationStatusAuthorized
import platform.Contacts.CNAuthorizationStatusDenied
import platform.Contacts.CNAuthorizationStatusRestricted
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNNotificationSettingEnabled
import platform.UserNotifications.UNUserNotificationCenter

@OptIn(UnsafeNumber::class)
actual class KmpPermissionsManager {
    actual companion object {
        private val checkForLocationPermission = "checkForLocationPermission"
        actual fun getCurrentPermissionState(
            permission: Permission,
            actionResult: ActionPermissionStatusParams
        ) {
            KmpMainThread.runViaMainThread {
                if (permission == Permission.PushNotifications) {
                    UNUserNotificationCenter.currentNotificationCenter()
                        .getNotificationSettingsWithCompletionHandler {
                            val response = when (it?.alertSetting()) {
                                UNAuthorizationStatusAuthorized -> PermissionStatus.Granted
                                UNAuthorizationStatusDenied -> PermissionStatus.Denied
                                else -> PermissionStatus.NotDetermined
                            }

                            actionResult(response)
                        }
                } else {
                    if (permission == Permission.Contacts) {
                        actionResult(
                            when (CNContactStore.authorizationStatusForEntityType(
                                CNEntityType.CNEntityTypeContacts
                            )) {
                                CNAuthorizationStatusAuthorized -> PermissionStatus.Granted
                                CNAuthorizationStatusDenied -> PermissionStatus.Denied
                                CNAuthorizationStatusRestricted -> PermissionStatus.DeniedAlways
                                else -> PermissionStatus.NotDetermined
                            }
                        )
                    }
                    if (permission == Permission.Location) {

                        actionResult(
                            when (CLLocationManager.authorizationStatus()) {
                                kCLAuthorizationStatusRestricted -> PermissionStatus.DeniedAlways
                                kCLAuthorizationStatusDenied -> PermissionStatus.Denied
                                kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> PermissionStatus.Granted
                                else -> PermissionStatus.NotDetermined
                            }
                        )
                    }

                    if (permission == Permission.Microphone) {
                        actionResult(
                            when (AVAudioSession.sharedInstance()
                                .recordPermission()) {
                                AVAudioSessionRecordPermissionGranted -> PermissionStatus.Granted
                                AVAudioSessionRecordPermissionDenied -> PermissionStatus.Denied
                                else -> PermissionStatus.NotDetermined
                            }
                        )
                    }
                }
            }
        }

        actual fun requestPermissionVerifyIfDenied(
            permission: Permission,
            runAction: ActionNoParams,
            onDenied: ActionNoParams,
        ) {
            onRequestPermission(permission, runAction, onDenied)
        }

        actual fun requestPermission(
            permission: Permission,
            runAction: ActionNoParams
        ) {
            onRequestPermission(permission, runAction)
        }

        private fun onRequestPermission(
            permission: Permission,
            runAction: ActionNoParams,
            onDenied: ActionNoParams? = null
        ) {
            KmpMainThread.runViaMainThread {
                when (permission) {
                    Permission.PushNotifications -> {
                        UNUserNotificationCenter.currentNotificationCenter()
                            .requestAuthorizationWithOptions(
                                UNAuthorizationOptionAlert
                            ) { res, error ->
                                if (res) {
                                    runAction()
                                } else {
                                    onDenied?.invoke()
                                }
                            }
                    }

                    Permission.Microphone -> {
                        AVAudioSession.sharedInstance().requestRecordPermission {
                            if (it) {
                                runAction()
                            } else {
                                onDenied?.invoke()
                            }
                        }
                    }

                    Permission.Location -> {
                        val location = CLLocationManager()
                        location.delegate = LocationPermissionsDelegate(runAction, onDenied)
                        location.requestWhenInUseAuthorization()
                    }

                    else -> {

                    }
                }
            }

        }

        // DEPRECATED API
        actual fun isPermissionGranted(permission: Permission): Boolean {
            return false
        }

        actual fun isPermissionGranted(permission: Permission, actionResult: ActionBoolParams) {
            KmpMainThread.runViaMainThread {
                if (permission == Permission.PushNotifications) {
                    UNUserNotificationCenter.currentNotificationCenter()
                        .getNotificationSettingsWithCompletionHandler {
                            actionResult(it?.alertSetting() == UNNotificationSettingEnabled)
                        }
                } else {
                    actionResult(
                        when (permission) {
                            Permission.Location -> CLLocationManager().authorizationStatus() == kCLAuthorizationStatusAuthorizedWhenInUse || CLLocationManager().authorizationStatus() == kCLAuthorizationStatusAuthorizedAlways
                            Permission.Microphone -> AVAudioSession.sharedInstance()
                                .recordPermission() == AVAudioSessionRecordPermissionGranted

                            else -> false
                        }
                    )
                }
            }
        }

        actual fun canShowPromptDialog(permission: Permission, actionResult: ActionBoolParams) {
            KmpMainThread.runViaMainThread {
                val status = when (permission) {
                    Permission.Location -> {
                        val hasPrompted =
                            KmpPublicStorage.getBooleanFromKey(checkForLocationPermission)
                                ?: false
                        if (!hasPrompted) {
                            KmpPublicStorage.persistData(checkForLocationPermission, true)
                            true
                        } else {
                            CLLocationManager().authorizationStatus() == kCLAuthorizationStatusNotDetermined
                        }
                    }

                    Permission.Microphone -> AVAudioSession.sharedInstance()
                        .recordPermission() == AVAudioSessionRecordPermissionUndetermined

                    else -> false
                }

                actionResult(
                    status
                )
            }
        }
    }
}