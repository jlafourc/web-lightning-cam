package com.lightningcam.camera

data class AppliedCameraSettings(
    val exposureCompensation: Int,
    val focusDistance: Float?,
    val manualExposure: Boolean,
    val iso: Int?,
    val exposureTimeNs: Long?,
    val warning: String?,
)

object CameraControlMapper {
    fun map(settings: CameraSettings, capabilities: CameraCapabilities): AppliedCameraSettings {
        val manual = settings.manualExposure && capabilities.supportsManualSensor
        val warning = when {
            settings.manualExposure && !capabilities.supportsManualSensor -> "Manual sensor controls unavailable"
            settings.focusMode == FocusMode.INFINITY && !capabilities.supportsInfinityFocus -> "Infinity focus unavailable"
            else -> null
        }
        return AppliedCameraSettings(
            exposureCompensation = settings.exposureCompensation.coerceIn(capabilities.exposureCompensationRange),
            focusDistance = if (settings.focusMode == FocusMode.INFINITY && capabilities.supportsInfinityFocus) 0f else null,
            manualExposure = manual,
            iso = if (manual) settings.iso.coerceIn(capabilities.isoRange ?: settings.iso..settings.iso) else null,
            exposureTimeNs = if (manual) {
                settings.exposureTimeNs.coerceIn(capabilities.exposureTimeNsRange ?: settings.exposureTimeNs..settings.exposureTimeNs)
            } else null,
            warning = warning,
        )
    }
}
