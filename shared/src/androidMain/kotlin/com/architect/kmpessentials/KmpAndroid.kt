package com.architect.kmpessentials

import ApplicationLifecycleObserver
import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ProcessLifecycleOwner
import com.architect.kmpessentials.alerts.KmpWearAlert
import com.architect.kmpessentials.aliases.DefaultAction
import com.architect.kmpessentials.battery.KmpBattery
import com.architect.kmpessentials.camera.KmpCamera
import com.architect.kmpessentials.filePicker.File
import com.architect.kmpessentials.filePicker.KmpFilePicker
import com.architect.kmpessentials.filePicker.internals.utilities.appConst.Const
import com.architect.kmpessentials.internals.FilePickingMode
import com.architect.kmpessentials.localNotifications.KmpLocalNotifications
import com.architect.kmpessentials.mediaPicker.KmpMediaPicker
import com.architect.kmpessentials.permissions.KmpPermissionsManager
import com.architect.kmpessentials.wearables.WearComposeAlertService

class KmpAndroid {
    companion object {
        private var hasRegistered = false
        internal var customBackAction: DefaultAction? = null
        internal val backButtonCallBack = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (customBackAction != null) {
                    customBackAction!!.invoke()
                    customBackAction = null
                }
            }
        }

        internal var allowWorkersToRunBeyondApp: Boolean = true
        internal var applicationContext: Application? = null
        internal var clientAppContext: FragmentActivity? = null
        internal val sensorManagerObserver = SensorObserver()
        internal var guserDisabledPermission: DefaultAction? = null
        internal var guserDisabledPermissionOnInvoke: DefaultAction? = null

        fun getCurrentActivityContext(): FragmentActivity {
            return clientAppContext!!
        }

        fun getCurrentApplicationContext(): Application {
            return applicationContext!!
        }

        fun setAllowWorkersToRunBeyondAppContext(allowWorkersToRunBeyondApp: Boolean) {
            this.allowWorkersToRunBeyondApp = allowWorkersToRunBeyondApp
        }

        fun preRegisterApplicationContext(
            appContext: Application,
        ) {
            KmpWearAlert.initialise(WearComposeAlertService())
            if (!hasRegistered) {
                applicationContext = appContext
                if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) { // battery services require Lolliop and above to work
                    KmpBattery.initializeBatteryService()
                }

                applicationContext?.registerActivityLifecycleCallbacks(ActivityLifecycleObserver())
                ProcessLifecycleOwner.get().lifecycle.addObserver(ApplicationLifecycleObserver()) // application lifecycle observer
                KmpLocalNotifications.prepareStorageNotifications()
                KmpLocalNotifications.bootLoopTimerCheckForActiveNotifications()

                hasRegistered = true
            }
        }

        fun initializeApp(
            context: FragmentActivity,
            userDisabledPermission: DefaultAction? = null,
        ) {
            clientAppContext = context
            guserDisabledPermission = userDisabledPermission

            // register application
            preRegisterApplicationContext(context.application)

            // back button control
            if (clientAppContext != null) {
                registerAllContracts()
                clientAppContext!!.onBackPressedDispatcher.addCallback(
                    clientAppContext!!,
                    backButtonCallBack
                )
            }
        }

        private fun registerAllContracts() {
            // camera apis
            KmpCamera.resultLauncher =
                clientAppContext!!.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    loadFileDataViaIntent(it, FilePickingMode.TakeDataFromCamera)
                }

            // image picker
            KmpMediaPicker.galleryLauncher =
                clientAppContext!!.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    loadFileDataViaIntent(it, FilePickingMode.PickMediaFromGallery)
                }

            // permission manager
            KmpPermissionsManager.resultLauncher =
                clientAppContext!!.registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                    if (result) {
                        KmpPermissionsManager.successAction()
                    } else {
                        // permission has not been enabled
                        guserDisabledPermissionOnInvoke?.invoke()
                            ?: guserDisabledPermission?.invoke()
                        guserDisabledPermissionOnInvoke = null
                    }
                }

            KmpPermissionsManager.resultManyLauncher =
                clientAppContext!!.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                    if (result.values.any { !it }) { // all permissions must be enabled for this to work
                        guserDisabledPermissionOnInvoke?.invoke() ?: guserDisabledPermission?.invoke()
                        guserDisabledPermissionOnInvoke = null
                    } else {
                        KmpPermissionsManager.successAction()
                    }
                }

            // file picker
            KmpFilePicker.resultLauncher =
                clientAppContext!!.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    loadFileDataViaIntent(it, FilePickingMode.PickFileFromFileSystem)
                }
        }

        private fun loadFileDataViaIntent(
            it: ActivityResult,
            mode: FilePickingMode
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val singlePath =
                    it.data!!.getStringExtra(Const.BundleExtras.FILE_PATH)
                if (singlePath != null) {
                    val singleFile =
                        java.io.File(singlePath)

                    val pickedFile = File(
                        name = singleFile.name,
                        absolutePath = singleFile.absolutePath,
                        createdISO = "",
                        isProtected = singleFile.isHidden,
                        modifiedISO = ""
                    )
                    when (mode) {
                        FilePickingMode.PickFileFromFileSystem -> {
                            KmpFilePicker.actionResultSingleFile(
                                pickedFile
                            )
                        }

                        FilePickingMode.PickMediaFromGallery -> {
                            KmpMediaPicker.imagePickerResult(
                                pickedFile.absolutePath
                            )
                        }

                        FilePickingMode.TakeDataFromCamera -> {
                            KmpCamera.actionResult(
                                pickedFile.absolutePath
                            )
                        }
                    }
                } else {
                    if (mode == FilePickingMode.PickFileFromFileSystem) {
                        val filePaths =
                            it.data?.getStringArrayListExtra(Const.BundleExtras.FILE_PATH_LIST)
                                ?.map { file ->
                                    val singleFile =
                                        java.io.File(file)
                                    File(
                                        name = singleFile.name,
                                        absolutePath = singleFile.absolutePath,
                                        createdISO = "",
                                        isProtected = singleFile.isHidden,
                                        modifiedISO = ""
                                    )
                                }

                        KmpFilePicker.actionResultManyFiles(
                            filePaths
                        )
                    }
                }
            }
        }
    }
}


