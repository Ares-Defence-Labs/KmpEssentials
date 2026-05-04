package com.architect.kmpessentials.secureStorage

import android.content.Context
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Log
import com.architect.kmpessentials.KmpAndroid
import com.architect.kmpessentials.internal.ActionBoolNullParams
import com.architect.kmpessentials.internal.ActionDoubleNullParams
import com.architect.kmpessentials.internal.ActionFloatNullParams
import com.architect.kmpessentials.internal.ActionIntNullParams
import com.architect.kmpessentials.internal.ActionLongNullParams
import com.architect.kmpessentials.internal.ActionStringNullParams
import com.liftric.kvault.KVault
import java.io.File
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException

actual class KmpSecureStorage {
    actual companion object {
        private const val TAG = "KmpSecureStorage"
        private const val DEFAULT_PREFERENCE_FILE_NAME = "secure-shared-preferences"
        private const val ANDROIDX_MASTER_KEY_ALIAS = "_androidx_security_master_key_"

        private var droidPreferenceName: String? = null
        private var keyVault: KVault? = null
        private var recoveryAttempted = false

        private fun vault(): KVault {
            val context = KmpAndroid.applicationContext
                ?: throw IllegalStateException("KmpAndroid application context is not initialized.")

            return keyVault ?: KVault(context, droidPreferenceName).also {
                keyVault = it
            }
        }

        actual suspend fun getLongFromKeyAsync(key: String, action: ActionLongNullParams) {
            action(getLongFromKey(key))
        }

        actual suspend fun getStringFromKeyAsync(key: String, action: ActionStringNullParams) {
            action(getStringFromKey(key))
        }

        actual suspend fun getIntFromKeyAsync(key: String, action: ActionIntNullParams) {
            action(getIntFromKey(key))
        }

        actual suspend fun getFloatFromKeyAsync(key: String, action: ActionFloatNullParams) {
            action(getFloatFromKey(key))
        }

        actual suspend fun getDoubleFromKeyAsync(key: String, action: ActionDoubleNullParams) {
            action(getDoubleFromKey(key))
        }

        actual suspend fun getBooleanFromKeyAsync(key: String, action: ActionBoolNullParams) {
            action(getBooleanFromKey(key))
        }

        actual fun clearEntireStore() {
            write { vault().clear() }
        }

        actual fun deleteDataForKey(key: String) {
            write { vault().deleteObject(key) }
        }

        actual fun <TData> persistData(key: String, item: TData) {
            write {
                when (item) {
                    is Float -> vault().set(key, item)
                    is Double -> vault().set(key, item)
                    is Boolean -> vault().set(key, item)
                    is String -> vault().set(key, item)
                    is Long -> vault().set(key, item)
                    is Int -> vault().set(key, item)
                    else -> true
                }
            }
        }

        actual fun getStringFromKey(key: String): String? {
            return read { vault().string(key) }
        }

        actual fun getIntFromKey(key: String): Int? {
            return read { vault().int(key) }
        }

        actual fun getLongFromKey(key: String): Long? {
            return read { vault().long(key) }
        }

        actual fun getFloatFromKey(key: String): Float? {
            return read { vault().float(key) }
        }

        actual fun getBooleanFromKey(key: String): Boolean? {
            return read { vault().bool(key) }
        }

        actual fun getDoubleFromKey(key: String): Double? {
            return read { vault().double(key) }
        }

        fun configureDroidPreferenceFileName(preferenceFileName: String) {
            droidPreferenceName = preferenceFileName
            keyVault = null
            recoveryAttempted = false
        }

        private inline fun <T> read(operation: () -> T?): T? {
            return try {
                operation()
            } catch (ex: Exception) {
                if (recoverFromSecureStorageFailure(ex)) null else throw ex
            }
        }

        private inline fun write(operation: () -> Boolean) {
            try {
                operation()
            } catch (ex: Exception) {
                if (!recoverFromSecureStorageFailure(ex)) {
                    throw ex
                }

                try {
                    operation()
                } catch (retryEx: Exception) {
                    Log.e(TAG, "Secure storage write failed after recovery.", retryEx)
                    throw retryEx
                }
            }
        }

        private fun recoverFromSecureStorageFailure(error: Exception): Boolean {
            if (!isRecoverableSecureStorageFailure(error)) {
                return false
            }

            Log.e(TAG, "Recovering from Android secure storage failure.", error)
            keyVault = null

            if (!recoveryAttempted) {
                recoveryAttempted = true
                clearEncryptedPreferenceFile()
                deleteAndroidxMasterKey()
            }

            return true
        }

        private fun isRecoverableSecureStorageFailure(error: Exception): Boolean {
            var current: Throwable? = error

            while (current != null) {
                if (
                    current is KeyStoreException ||
                    current is KeyPermanentlyInvalidatedException ||
                    current is GeneralSecurityException ||
                    current is InvalidKeyException ||
                    current is AEADBadTagException ||
                    current is BadPaddingException
                ) {
                    return true
                }

                val text = listOfNotNull(
                    current.javaClass.simpleName,
                    current.message
                ).joinToString(" ").lowercase()

                if (
                    text.contains("invalid key blob") ||
                    text.contains("androidkeystore") ||
                    text.contains("keystore") ||
                    text.contains("aeadbadtagexception") ||
                    text.contains("failed to decrypt") ||
                    text.contains("failed to encrypt") ||
                    text.contains("could not decrypt key") ||
                    text.contains("invalid key")
                ) {
                    return true
                }

                current = current.cause
            }

            return false
        }

        private fun clearEncryptedPreferenceFile() {
            val context = KmpAndroid.applicationContext ?: return
            val preferenceFileName = droidPreferenceName ?: DEFAULT_PREFERENCE_FILE_NAME

            try {
                context.getSharedPreferences(preferenceFileName, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
            } catch (ex: Exception) {
                Log.w(TAG, "Failed to clear encrypted shared preferences before deleting file.", ex)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.deleteSharedPreferences(preferenceFileName)
                } else {
                    File(context.applicationInfo.dataDir, "shared_prefs/$preferenceFileName.xml").delete()
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Failed to delete encrypted shared preferences file.", ex)
            }
        }

        private fun deleteAndroidxMasterKey() {
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)

                if (keyStore.containsAlias(ANDROIDX_MASTER_KEY_ALIAS)) {
                    keyStore.deleteEntry(ANDROIDX_MASTER_KEY_ALIAS)
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Failed to delete AndroidX master key.", ex)
            }
        }
    }
}
