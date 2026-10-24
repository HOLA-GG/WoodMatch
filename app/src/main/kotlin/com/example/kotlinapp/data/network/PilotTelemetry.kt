package com.example.kotlinapp.data.network

import com.google.firebase.crashlytics.FirebaseCrashlytics

object PilotTelemetry {
    @Volatile var enabled = false

    fun remoteFailure(operation: String, status: Int) {
        if (!enabled) return
        // Deliberately omit response bodies, user IDs, URLs, coordinates and exception causes.
        runCatching {
            FirebaseCrashlytics.getInstance()
                .recordException(
                    IllegalStateException(
                        "WoodMatch operation=${operation.take(40)} status=$status"
                    )
                )
        }
    }
}
