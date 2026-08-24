package com.lightningcam.storage

import com.lightningcam.detector.TriggerType

data class LightningEvent(
    val id: Long,
    val timestampMs: Long,
    val triggerType: TriggerType,
    val globalScore: Double,
    val localizedScore: Double,
    val detectionLatencyMs: Double,
    val photoUri: String,
    val videoUri: String?,
    val iso: Int? = null,
    val exposureTimeNs: Long? = null,
    val focusDistance: Float? = null,
)
