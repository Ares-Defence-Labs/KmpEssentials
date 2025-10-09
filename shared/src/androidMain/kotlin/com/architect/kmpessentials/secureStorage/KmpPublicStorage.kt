package com.architect.kmpessentials.secureStorage

import android.content.Context
import androidx.preference.PreferenceManager
import com.architect.kmpessentials.KmpAndroid
import com.architect.kmpessentials.internal.ActionBoolNullParams
import com.architect.kmpessentials.internal.ActionDoubleNullParams
import com.architect.kmpessentials.internal.ActionFloatNullParams
import com.architect.kmpessentials.internal.ActionIntNullParams
import com.architect.kmpessentials.internal.ActionLongNullParams
import com.architect.kmpessentials.internal.ActionStringNullParams

actual class KmpPublicStorage {
    actual companion object {
        private lateinit var localContext: Context
        private val sharedPreference by lazy {
            PreferenceManager.getDefaultSharedPreferences(
                KmpAndroid.applicationContext ?: localContext
            )
        }

        fun setCustomContext(customContext: Context) {
            localContext = customContext
        }

        actual fun clearEntireStore() {
            sharedPreference.edit().clear().apply()
        }

        actual fun deleteDataForKey(key: String) {
            sharedPreference.edit().remove(key).apply()
        }

        actual fun <TData> persistData(key: String, item: TData) {
            when (item) {
                is Float -> sharedPreference.edit().putFloat(key, item)
                is Boolean -> sharedPreference.edit().putBoolean(key, item)
                is String -> sharedPreference.edit().putString(key, item)
                is Int -> sharedPreference.edit().putInt(key, item)
                is Double ->  sharedPreference.edit().putLong(key, java.lang.Double.doubleToRawLongBits(item))
                is Long -> sharedPreference.edit().putLong(key, item)
                else -> sharedPreference.edit().putString(key, "$item")
            }.apply()
        }

        actual fun getStringFromKey(key: String): String? {
            return sharedPreference.getString(key, "")
        }

        actual fun getLongFromKey(key: String): Long? {
            try {
                return sharedPreference.getLong(key, 0L)
            }
            catch (ex: Exception){
                return sharedPreference.getInt(key, 0).toLong()
            }
        }

        actual fun getIntFromKey(key: String): Int? {
            try {
                return sharedPreference.getInt(key, 0)
            }
            catch (ex: Exception){
                return sharedPreference.getLong(key, 0L).toInt()
            }
        }

        actual fun getFloatFromKey(key: String): Float? {
            return sharedPreference.getFloat(key, 0f)
        }

        actual fun getBooleanFromKey(key: String): Boolean? {
            return sharedPreference.getBoolean(key, false)
        }

        actual fun getDoubleFromKey(key: String): Double? {
            return java.lang.Double.longBitsToDouble(sharedPreference.getLong(key, 0L))
        }

        fun getAllKeys(): List<String> {
            return sharedPreference.all.keys.toList()
        }

        actual fun getStringFromKey(key: String, defValue: String?): String? {
            return sharedPreference.getString(key, defValue)
        }

        actual fun getIntFromKey(key: String, defValue: Int): Int {
            return sharedPreference.getInt(key, defValue)
        }

        actual fun getLongFromKey(key: String, defValue: Long): Long {
            return sharedPreference.getLong(key, defValue)
        }

        actual fun getFloatFromKey(key: String, defValue: Float): Float {
            return sharedPreference.getFloat(key, defValue)
        }

        actual fun getDoubleFromKey(key: String, defValue: Double): Double {
            val bits = sharedPreference.getLong(
                key,
                java.lang.Double.doubleToRawLongBits(defValue)
            )
            return java.lang.Double.longBitsToDouble(bits)
        }

        actual fun getBooleanFromKey(
            key: String,
            defValue: Boolean
        ): Boolean {
            return sharedPreference.getBoolean(key, defValue)
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
    }
}