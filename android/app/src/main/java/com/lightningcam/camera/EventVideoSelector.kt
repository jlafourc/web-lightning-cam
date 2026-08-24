package com.lightningcam.camera

data class EventVideoSelection(val retainedUri: String, val discardUri: String?)

object EventVideoSelector {
    fun select(boundarySegment: String?, postSegment: String): EventVideoSelection =
        if (boundarySegment != null) {
            EventVideoSelection(retainedUri = boundarySegment, discardUri = postSegment)
        } else {
            EventVideoSelection(retainedUri = postSegment, discardUri = null)
        }
}
