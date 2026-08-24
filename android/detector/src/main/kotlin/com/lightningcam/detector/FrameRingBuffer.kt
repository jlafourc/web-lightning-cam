package com.lightningcam.detector

data class TimedValue<T>(val timestampMs: Long, val value: T)

class FrameRingBuffer<T>(private val capacity: Int) {
    private val entries = ArrayDeque<TimedValue<T>>(capacity)

    init {
        require(capacity > 0)
    }

    fun add(timestampMs: Long, value: T) {
        if (entries.size == capacity) entries.removeFirst()
        entries.addLast(TimedValue(timestampMs, value))
    }

    fun snapshot(): List<TimedValue<T>> = entries.sortedBy { it.timestampMs }

    fun clear() = entries.clear()
}
