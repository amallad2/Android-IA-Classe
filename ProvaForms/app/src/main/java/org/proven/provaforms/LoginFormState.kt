package org.proven.provaforms

// LoginFormState.kt
data class LoginFormState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false
)
