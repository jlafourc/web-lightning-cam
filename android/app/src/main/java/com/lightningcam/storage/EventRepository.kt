package com.lightningcam.storage

interface EventRepository {
    fun save(event: LightningEvent)
    fun events(): List<LightningEvent>
    fun delete(id: Long)
}

class InMemoryEventRepository : EventRepository {
    private val stored = mutableListOf<LightningEvent>()

    override fun save(event: LightningEvent) {
        stored.removeAll { it.id == event.id }
        stored += event
    }

    override fun events(): List<LightningEvent> = stored.sortedByDescending { it.timestampMs }

    override fun delete(id: Long) {
        stored.removeAll { it.id == id }
    }
}
