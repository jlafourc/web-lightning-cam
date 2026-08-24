package com.lightningcam.ui

data class CameraUiState(
    val needsPermission: Boolean = true,
    val armed: Boolean = false,
    val status: String = "Autorisation caméra requise",
)

class CameraStateController {
    var state: CameraUiState = CameraUiState()
        private set

    fun onPermission(granted: Boolean) {
        state = state.copy(
            needsPermission = !granted,
            armed = granted,
            status = if (granted) "Initialisation caméra…" else "Autorisation caméra requise",
        )
    }

    fun toggleArmed() {
        state = state.copy(armed = !state.armed)
    }

    fun onStatus(status: String) {
        state = state.copy(status = status, armed = state.armed || status.startsWith("Armé"))
    }
}
