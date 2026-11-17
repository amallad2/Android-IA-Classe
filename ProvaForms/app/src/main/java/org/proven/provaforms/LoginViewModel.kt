package org.proven.provaforms

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class LoginViewModel : ViewModel() {


    private val _uiState = MutableStateFlow(LoginFormState())
    val uiState: StateFlow<LoginFormState> = _uiState.asStateFlow()


    fun onEmailChanged(email: String) {
        _uiState.update { currentState ->
            val error = validateEmail(email)
            val updatedState = currentState.copy(email = email, emailError = error)
            updatedState.copy(isFormValid = isFormValid(updatedState))
        }
    }


    fun onPasswordChanged(password: String) {
        _uiState.update { currentState ->
            val error = validatePassword(password)
            val updatedState = currentState.copy(password = password, passwordError = error)
            updatedState.copy(isFormValid = isFormValid(updatedState))
        }
    }


    fun onSubmit() {
        if (_uiState.value.isFormValid) {
            _uiState.update { it.copy(isLoading = true) }
            // Aquí s'executaria la crida a l'API o base de dades
        }
    }


    // --- REGLES DE VALIDACIÓ ---
    private fun validateEmail(email: String): String? {
        return when {
            email.isEmpty() -> "El correu no pot estar buit"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Format de correu no vàlid"
            else -> null
        }
    }


    private fun validatePassword(password: String): String? {
        return when {
            password.isEmpty() -> "La contrasenya no pot estar buida"
            password.length < 6 -> "La contrasenya ha de tenir almenys 6 caràcters"
            else -> null
        }
    }


    private fun isFormValid(state: LoginFormState): Boolean {
        return state.emailError == null &&
                state.passwordError == null &&
                state.email.isNotEmpty() &&
                state.password.isNotEmpty()
    }
}
