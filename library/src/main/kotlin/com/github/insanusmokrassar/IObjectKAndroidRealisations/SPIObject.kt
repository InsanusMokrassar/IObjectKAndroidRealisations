package com.github.insanusmokrassar.IObjectKAndroidRealisations

import android.content.Context
import android.content.SharedPreferences
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectK.realisations.SimpleIObject
import com.github.insanusmokrassar.IObjectKAndroidRealisations.utils.canBeSerializable
import com.github.insanusmokrassar.IObjectKAndroidRealisations.utils.deserialize
import com.github.insanusmokrassar.IObjectKAndroidRealisations.utils.serialize
import java.io.*

private val cache = HashMap<String, MutableMap<String, SPIObject>>()

fun Context.keyValueStore(
        name: String = getString(R.string.standardSharedPreferencesName)
): IObject<Any> {
    val className = this::class.java.simpleName
    return if (cache[className] ?.get(name) == null) {
        cache.put(
                className,
                mutableMapOf(
                        Pair(
                                name,
                                SPIObject(this, name)
                        )
                )
        )
        keyValueStore(name)
    } else {
        cache[className]!![name]!!
    }
}

class SPIObject internal constructor (
        c: Context,
        preferencesName: String
) : IObject<Any>, SharedPreferences.OnSharedPreferenceChangeListener {
    private val sharedPreferences = c.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    private val cachedData = SimpleIObject()

    private val syncObject = Object()

    init {
        sharedPreferences.all.forEach {
            if (it.value != null) {
                cachedData.put(it.key, it.value as Any)
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        synchronized(syncObject, {
            val value = sp.all[key]
            if (value != null) {
                cachedData.put(key, value)
            } else {
                cachedData.remove(key)
            }
        })
    }

    override fun put(key: String, value: Any) {
        synchronized(syncObject, {
            sharedPreferences.edit()
                    .put(key, value)
                    .apply()
        })
    }

    override fun <T: Any> get(key: String): T {
        synchronized(syncObject, {
            val value = cachedData.get<Any>(key)
            return when(value) {
                !is String -> value
                else -> {
                    if (canBeSerializable(value)) {
                        try {
                            deserialize<Serializable>(value)
                        } catch (e: ClassCastException) {
                            value
                        }
                    } else {
                        value
                    }
                }
            } as T
        })
    }

    override fun keys(): Set<String> {
        synchronized(syncObject, {
            return cachedData.keys()
        })
    }

    override fun putAll(toPutMap: Map<String, Any>) {
        synchronized(syncObject, {
            val editor = sharedPreferences.edit()
            toPutMap.forEach {
                editor.put(it.key, it.value)
            }
            editor.apply()
        })
    }

    override fun remove(key: String) {
        synchronized(syncObject, {
            sharedPreferences.edit()
                    .remove(key)
                    .apply()
        })
    }

    private fun SharedPreferences.Editor.put(key: String, value: Any): SharedPreferences.Editor {
        when(value) {
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is String -> putString(key, value)
            is Boolean -> putBoolean(key, value)
            is Set<*> -> try {
                putStringSet(key, value as Set<String>)
            } catch (e: ClassCastException) {
                putStringSet(key, LinkedHashSet(value.map { it.toString() }))
            }
            is Serializable -> putString(key, serialize(value))
            else -> throw IllegalArgumentException(
                    "You can put into SPIObject only: Int, Long, Float, String, Boolean or some set"
            )
        }
        return this
    }
}
