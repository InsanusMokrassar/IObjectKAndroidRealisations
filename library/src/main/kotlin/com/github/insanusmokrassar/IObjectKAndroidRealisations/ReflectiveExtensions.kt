package com.github.insanusmokrassar.IObjectKAndroidRealisations

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberProperties

@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class PrimaryKey

@Target(AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class Autoincrement

/**
 * List of classes which can be primitive
 */
val nativeTypes = listOf(
        Int::class,
        Long::class,
        Float::class,
        Double::class,
        String::class,
        Boolean::class
)

/**
 * @return Экземпляр KClass, содержащий данный KCallable объект.
 */
fun <T> KCallable<T>.intsanceKClass() : KClass<*> =
        this.instanceParameter?.type?.classifier as KClass<*>

/**
 * @return true если значение параметра может быть null.
 */
fun KCallable<*>.isNullable() : Boolean =
        this.returnType.isMarkedNullable

/**
 * @return Экземпляр KClass, возвращаемый KCallable.
 */
fun KCallable<*>.returnClass() : KClass<*> =
        this.returnType.classifier as KClass<*>

/**
 * @return true, если возвращает некоторый примитив.
 */
fun KCallable<*>.isReturnNative() : Boolean =
        nativeTypes.contains(this.returnClass())

/**
 * @return true если объект помечен аннотацией [PrimaryKey].
 */
fun KProperty<*>.isPrimaryField() : Boolean =
        this.annotations.firstOrNull { it.annotationClass == PrimaryKey::class } != null

/**
 * @return true если объект помечен аннотацией [Autoincrement].
 */
fun KProperty<*>.isAutoincrement() : Boolean {
    this.annotations.forEach {
        if (it.annotationClass == Autoincrement::class) {
            return@isAutoincrement true
        }
    }
    return false
}

/**
 * @return Список полей класса.
 */
fun KClass<*>.getVariables() : List<KProperty<*>> =
        this.memberProperties.toList()
