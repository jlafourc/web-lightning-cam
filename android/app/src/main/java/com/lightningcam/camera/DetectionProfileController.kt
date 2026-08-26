package com.lightningcam.camera

import com.lightningcam.detector.DetectionProfile

class DetectionProfileController(
    initial: DetectionProfile = DetectionProfile.BALANCED,
) {
    var current: DetectionProfile = initial
        private set

    fun next(): DetectionProfile {
        current = when (current) {
            DetectionProfile.STRICT -> DetectionProfile.BALANCED
            DetectionProfile.BALANCED -> DetectionProfile.SENSITIVE
            DetectionProfile.SENSITIVE -> DetectionProfile.STRICT
        }
        return current
    }
}
