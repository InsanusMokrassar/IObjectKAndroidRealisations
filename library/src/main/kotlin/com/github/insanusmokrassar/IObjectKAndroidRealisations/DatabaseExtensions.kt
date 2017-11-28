package com.github.insanusmokrassar.IObjectKAndroidRealisations

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

val nativeTypesMap = mapOf(
        Pair(
                Int::class,
                "INTEGER"
        ),
        Pair(
                Long::class,
                "LONG"
        ),
        Pair(
                Float::class,
                "FLOAT"
        ),
        Pair(
                Double::class,
                "DOUBLE"
        ),
        Pair(
                String::class,
                "TEXT"
        ),
        Pair(
                Boolean::class,
                "BOOLEAN"
        )
)

internal fun KClass<*>.tableName(): String {
    return java.simpleName
}

fun Map<KProperty<*>, Any>.toContentValues(): ContentValues {
    val cv = ContentValues()
    keys.forEach {
        val prop = it
        val value = get(prop)!!
        when(value::class) {
            Boolean::class -> cv.put(prop.name, value as Boolean)
            Int::class -> cv.put(prop.name, value as Int)
            Long::class -> cv.put(prop.name, value as Long)
            Float::class -> cv.put(prop.name, value as Float)
            Double::class -> cv.put(prop.name, value as Double)
            Byte::class -> cv.put(prop.name, value as Byte)
            ByteArray::class -> cv.put(prop.name, value as ByteArray)
            String::class -> cv.put(prop.name, value as String)
            Short::class -> cv.put(prop.name, value as Short)
        }
    }
    return cv
}

fun Any.toContentValues(): ContentValues {
    return toValuesMap().toContentValues()
}

fun KClass<*>.getVariablesMap(): Map<String, KProperty<*>> {
    val futureMap = LinkedHashMap<String, KProperty<*>>()
    this.getVariables().forEach {
        futureMap.put(it.name, it)
    }
    return futureMap
}

fun Any.toValuesMap() : Map<KProperty<*>, Any> {
    val values = HashMap<KProperty<*>, Any>()

    this::class.getVariablesMap().values.filter {
        it.intsanceKClass() != Any::class && (!it.returnType.isMarkedNullable || it.call(this) != null)
    }.forEach {
        it.call(this)?.let { value ->
            values.put(
                    it,
                    value
            )
        }
    }
    return values
}

fun Any.getPrimaryFieldsSearchQuery(): String {
    return toValuesMap().filter {
        it.key.isPrimaryField()
    }.map {
        "${it.key.name}=${it.value}"
    }.joinToString(
            " AND "
    )
}

fun <M: Any> Iterable<M>.getPrimaryFieldsSearchQuery(): String {
    return joinToString(") OR (", "(", ")") {
        it.getPrimaryFieldsSearchQuery()
    }
}

fun <M: Any> KClass<M>.fromValuesMap(values : Map<KProperty<*>, Any?>): M {
    if (constructors.isEmpty()) {
        throw IllegalStateException("For some of reason, can't create correct realisation of model")
    } else {
        val resultModelConstructor = constructors.first {
            it.parameters.size == values.size
        }
        val paramsList = ArrayList<Any?>()
        resultModelConstructor.parameters.forEach {
            parameter ->
            val key = values.keys.firstOrNull { parameter.name == it.name }
            key ?. let {
                paramsList.add(
                        values[key]
                )
            } ?: paramsList.add(null)
        }
        return resultModelConstructor.call(*paramsList.toTypedArray())
    }
}

fun <M: Any> Cursor.extract(modelClass: KClass<M>): M {
    val properties = modelClass.getVariablesMap()
    val values = HashMap<KProperty<*>, Any?>()
    properties.values.forEach {
        val columnIndex = getColumnIndex(it.name)
        val value: Any = when(it.returnClass()) {
            Boolean::class -> getInt(columnIndex) == 1
            Int::class -> getInt(columnIndex)
            Long::class -> getLong(columnIndex)
            Float::class -> getFloat(columnIndex)
            Double::class -> getDouble(columnIndex)
            Byte::class -> getInt(columnIndex)
            ByteArray::class -> getInt(columnIndex)
            Short::class -> getShort(columnIndex)
            else -> getString(columnIndex)
        }
        values.put(
                it,
                value
        )
    }
    return modelClass.fromValuesMap(values)
}

fun <M: Any> Cursor.extractAll(modelClass: KClass<M>, close: Boolean = true): List<M> {
    val result = ArrayList<M>()
    if (moveToFirst()) {
        do {
            result.add(extract(modelClass))
        } while (moveToNext())
    }
    if (close) {
        close()
    }
    return result
}

fun <M : Any> SQLiteDatabase.createTableIfNotExist(modelClass: KClass<M>) {
    val fieldsBuilder = StringBuilder()

    modelClass.getVariables().forEach {
        if (it.isReturnNative()) {
            fieldsBuilder.append("${it.name} ${nativeTypesMap[it.returnClass()]}")
            if (it.isPrimaryField()) {
                fieldsBuilder.append(" PRIMARY KEY")
            }
            if (!it.isNullable()) {
                fieldsBuilder.append(" NOT NULL")
            }
            if (it.isAutoincrement()) {
                fieldsBuilder.append(" AUTOINCREMENT")
            }
        } else {
            TODO()
        }
        fieldsBuilder.append(", ")
    }

    try {
        execSQL("CREATE TABLE IF NOT EXISTS ${modelClass.tableName()} " +
                "(${fieldsBuilder.replace(Regex(", $"), "")});")
        Log.i("createTableIfNotExist", "Table ${modelClass.tableName()} was created")
    } catch (e: Exception) {
        Log.e("createTableIfNotExist", "init", e)
        throw IllegalArgumentException("Can't create table ${modelClass.tableName()}", e)
    }
}
