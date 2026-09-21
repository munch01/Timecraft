package com.emeric.timecraft

interface BiometryManager {
    fun canAuthenticate(): Boolean
    fun authenticate(
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
}

expect fun getBiometryManager(): BiometryManager?
