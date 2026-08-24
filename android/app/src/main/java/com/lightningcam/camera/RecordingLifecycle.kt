package com.lightningcam.camera

class RecordingLifecycle {
    private var closed = false
    fun close() { closed = true }
    fun shouldRestartAfterFinalize(): Boolean = !closed
}
