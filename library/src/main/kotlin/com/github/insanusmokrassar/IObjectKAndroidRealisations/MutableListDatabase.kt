package com.github.insanusmokrassar.IObjectKAndroidRealisations

import android.content.Context
import android.util.Log
import kotlin.reflect.KClass

open class MutableListDatabase<M: Any> (
        modelClass: KClass<M>,
        context: Context,
        databaseName: String,
        version: Int,
        defaultOrderBy: String? = null
) : MutableList<M> {
    private val db = SimpleDatabase(
            modelClass,
            context,
            databaseName,
            version,
            defaultOrderBy
    )

    override val size: Int
        get() = db.size().toInt()

    override fun contains(element: M): Boolean = db.find(element) != null

    override fun containsAll(elements: Collection<M>): Boolean =
            db.find(elements.getPrimaryFieldsSearchQuery()).size == elements.size

    override fun get(index: Int): M =
            db.findPage(index, 1).firstOrNull() ?: throw IndexOutOfBoundsException("Index: $index, db size: $size")

    override fun indexOf(element: M): Int {
        forEachIndexed { index, m -> if (m == element) return index }
        return -1
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<M> = MutableDatabaseListIterator(this)

    override fun lastIndexOf(element: M): Int = indexOf(element)

    override fun add(element: M): Boolean = db.insert(element)

    override fun add(index: Int, element: M) {
        db.beginTransaction()
        try {
            val after = db.find(index, size - index)
            db.remove(after)
            mutableListOf(element).plus(after).forEach {
                db.insert(it)
            }
            db.acceptTransaction()
        } catch (e: Exception) {
            Log.e(MutableListDatabase::class.java.simpleName, e.message, e)
            db.abortTransaction()
        }
    }

    override fun addAll(index: Int, elements: Collection<M>): Boolean {
        db.beginTransaction()
        return try {
            val after = db.find(index, size - index)
            removeAll(after)
            elements.plus(after).forEach {
                db.insert(it)
            }
            db.acceptTransaction()
            true
        } catch (e: Exception) {
            Log.e(MutableListDatabase::class.java.simpleName, e.message, e)
            db.abortTransaction()
            false
        }
    }

    override fun addAll(elements: Collection<M>): Boolean {
        db.beginTransaction()
        return try {
            elements.forEach {
                db.insert(it)
            }
            db.acceptTransaction()
            true
        } catch (e: Exception) {
            Log.e(MutableListDatabase::class.java.simpleName, e.message, e)
            db.abortTransaction()
            false
        }
    }

    override fun clear() {
        db.remove()
    }

    override fun listIterator(): MutableListIterator<M> = MutableDatabaseListIterator(this)

    override fun listIterator(index: Int): MutableListIterator<M> {
        val listIterator = listIterator()
        while (listIterator.hasNext() && listIterator.nextIndex() != index + 1) {
            listIterator.next()
        }
        return listIterator
    }

    override fun remove(element: M): Boolean =
            db.remove(elements = element)

    override fun removeAll(elements: Collection<M>): Boolean =
            db.remove(elements)

    override fun removeAt(index: Int): M {
        val toDelete = get(index)
        remove(toDelete)
        return toDelete
    }

    override fun retainAll(elements: Collection<M>): Boolean {
        return removeAll(
                this.filter {
                    !elements.contains(it)
                }
        )
    }

    override fun set(index: Int, element: M): M {
        val old = get(index)
        db.update(element, old.getPrimaryFieldsSearchQuery())
        return old
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<M> =
            db.find(limit = buildLimit(fromIndex, toIndex - fromIndex)).toMutableList()

}

private class MutableDatabaseListIterator<T: Any>(
        private val dbList: MutableListDatabase<T>,
        private val pageSize: Int = 20
): MutableListIterator<T> {

    private var currentPage = -1
    private var currentList = ArrayList<T>(pageSize)
    private var currentObject: T? = null

    private val index: Int
        get() = currentPage * pageSize + (pageSize - currentList.size)
    private var previous: T? = null

    override fun hasNext(): Boolean {
        return if (currentList.isNotEmpty()) {
            true
        } else {
            currentPage++
            refillList()
            currentList.isNotEmpty()
        }
    }

    override fun remove() {
        currentObject ?. let {
            dbList.remove(it)
        }
    }

    override fun next(): T {
        previous = currentObject
        currentObject = currentList.removeAt(0)
        return currentObject!!
    }

    override fun hasPrevious(): Boolean = previous != null

    override fun nextIndex(): Int = index + 1

    override fun previous(): T = previous!!

    override fun previousIndex(): Int = index - 1

    override fun add(element: T) {
        val currentSize = currentList.size
        dbList.add(index, element)
        refillList()
        while (currentList.size > currentSize) {
            currentList.removeAt(0)
        }
    }

    override fun set(element: T) {
        dbList[index] = element
        currentObject = element
    }

    private fun refillList() {
        currentList.clear()
        currentList.addAll(dbList.subList(currentPage, (currentPage + 1) * pageSize))
    }
}
