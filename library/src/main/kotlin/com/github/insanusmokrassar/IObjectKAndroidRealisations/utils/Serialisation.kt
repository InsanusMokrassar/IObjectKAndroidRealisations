package com.github.insanusmokrassar.IObjectKAndroidRealisations.utils

import java.io.*


val serializableControlWord = "Serializable"

fun addKeywordToSerializable(addTo: String): String
        = "$serializableControlWord$addTo"

fun removeKeywordFromSerializable(removeFrom: String): String
        = removeFrom.replaceFirst(serializableControlWord, "")

fun canBeSerializable(what: String): Boolean
        = what.startsWith(serializableControlWord)


fun serialize(from: Serializable): String {
    val byteArrayOS = ByteArrayOutputStream()
    val objectOS = ObjectOutputStream(byteArrayOS)
    objectOS.writeObject(from)
    objectOS.close()
    return addKeywordToSerializable(String(byteArrayOS.toByteArray()))
}

fun <T: Serializable> deserialize(from: String): T {
    try {
        return ObjectInputStream(
                ByteArrayInputStream(
                        removeKeywordFromSerializable(from).toByteArray()
                )
        ).readObject() as T
    } catch (e: IOException) {
        throw IllegalArgumentException("Input data is not serialized string: $from", e)
    } catch (e: ClassCastException) {
        throw IllegalArgumentException("Input data is not target serialized class: $from", e)
    }
}
