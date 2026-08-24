package com.lightningcam

data class AppState(val productName: String) {
    companion object {
        fun initial() = AppState(productName = "Lightning Cam")
    }
}
