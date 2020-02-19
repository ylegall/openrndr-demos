package org.ygl.openrndr.demos.util


class RingBuffer<T>
constructor(
        val capacity: Int,
        private val buffer: MutableList<T>
): MutableList<T> by buffer {

    constructor(capacity: Int): this(capacity, mutableListOf())

    constructor(buffer: MutableList<T>): this(buffer.size, buffer)

    constructor(collection: Collection<T>): this(collection.size, ArrayList(collection))

    init {
        check(capacity > 0) { "capacity must be greater than 0" }
    }

    private var end = 0

    override fun add(element: T): Boolean {
        if (buffer.size < capacity) {
            buffer.add(element)
        } else {
            buffer[end] = element
            end = (end + 1) % buffer.size
        }
        return true
    }

    override operator fun get(index: Int) = buffer[(this.end + index) % buffer.size]

    override fun iterator(): MutableIterator<T> {
        return object: MutableIterator<T> {
            private var idx = 0

            override fun hasNext() = idx < buffer.size

            override fun next() = get(idx++)

            override fun remove() {
                buffer.removeAt(idx)
            }
        }
    }

    fun first() = get(0)

    fun last() = get(buffer.size - 1)

    override fun toString() = buffer.indices.map { get(it) }.toString()
}

fun <T> ringBufferOf(vararg items: T) = RingBuffer(items.size, arrayListOf(items))
