package com.lightningcam.storage

import com.lightningcam.detector.TriggerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventRepositoryTest {
    @Test
    fun `stores newest event first and accepts missing video`() {
        val repository = InMemoryEventRepository()
        repository.save(event(1, 100, "photo-1", null))
        repository.save(event(2, 200, "photo-2", "video-2"))

        assertEquals(listOf(2L, 1L), repository.events().map { it.id })
        assertNull(repository.events().last().videoUri)
    }

    @Test
    fun `deletes event`() {
        val repository = InMemoryEventRepository()
        repository.save(event(1, 100, "photo", null))
        repository.delete(1)
        assertEquals(emptyList(), repository.events())
    }

    private fun event(id: Long, timestamp: Long, photo: String, video: String?) = LightningEvent(
        id = id,
        timestampMs = timestamp,
        triggerType = TriggerType.LOCALIZED,
        globalScore = 10.0,
        localizedScore = 80.0,
        detectionLatencyMs = 4.0,
        photoUri = photo,
        videoUri = video,
    )
}
