package com.ctf.askpdf.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ctf.askpdf.data.app
import java.util.Locale
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val PREFERENCE_FILE_NAME = "ask_pdf_shared_prefs"

private val sharedPreferences: SharedPreferences
    get() = app.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)

/**
 * 以属性名作为 key 存储 Boolean 类型偏好。
 */
class BooleanPreference(private val defaultValue: Boolean) : ReadWriteProperty<Any?, Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return sharedPreferences.getBoolean(property.name, defaultValue)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        sharedPreferences.edit(commit = true) { putBoolean(property.name, value) }
    }
}

/**
 * 以属性名作为 key 存储 Int 类型偏好。
 */
class IntPreference(private val defaultValue: Int) : ReadWriteProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return sharedPreferences.getInt(property.name, defaultValue)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        sharedPreferences.edit(commit = true) { putInt(property.name, value) }
    }
}

/**
 * 以属性名作为 key 存储 Long 类型偏好。
 */
class LongPreference(private val defaultValue: Long) : ReadWriteProperty<Any?, Long> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
        return sharedPreferences.getLong(property.name, defaultValue)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
        sharedPreferences.edit(commit = true) { putLong(property.name, value) }
    }
}

/**
 * 以属性名作为 key 存储 Float 类型偏好。
 */
class FloatPreference(private val defaultValue: Float) : ReadWriteProperty<Any?, Float> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float {
        return sharedPreferences.getFloat(property.name, defaultValue)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
        sharedPreferences.edit(commit = true) { putFloat(property.name, value) }
    }
}

/**
 * 以属性名作为 key 存储 Double 类型偏好。
 */
class DoublePreference(private val defaultValue: Double = 0.0) : ReadWriteProperty<Any?, Double> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double {
        return sharedPreferences.getString(property.name, defaultValue.toString())?.toDoubleOrNull() ?: defaultValue
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
        sharedPreferences.edit(commit = true) { putString(property.name, value.toString()) }
    }
}

/**
 * 以属性名作为 key 存储 String 类型偏好。
 */
class StringPreference(private val defaultValue: String = "") : ReadWriteProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return sharedPreferences.getString(property.name, defaultValue) ?: defaultValue
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        sharedPreferences.edit(commit = true) { putString(property.name, value) }
    }
}

/**
 * 偏好存储辅助方法，适合处理动态 key 或清理数据。
 */
object SharedPreferencesUtil {

    /**
     * 判断指定 key 是否已经写入偏好存储。
     */
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    /**
     * 删除指定 key 对应的偏好值。
     */
    fun remove(key: String) {
        sharedPreferences.edit(commit = true) { remove(key) }
    }

    /**
     * 清空当前文件内的全部偏好值。
     */
    fun clearAll() {
        sharedPreferences.edit(commit = true) { clear() }
    }
}

var isFirstLaunch by BooleanPreference(true)

var selectedLanguageTag by StringPreference("")
