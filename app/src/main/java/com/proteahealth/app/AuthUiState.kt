package com.proteahealth.app

data class AuthUiState(
    val isSignUpMode: Boolean = false,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val agreedToTerms: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Field-specific validation errors, shown inline under each field
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null
) {
    // Computed property: the form is only submittable if there are no
    // validation errors AND (in sign-up mode) the user agreed to terms.
    val isFormValid: Boolean
        get() {
            val baseFieldsValid = email.isNotBlank() && password.isNotBlank() &&
                    emailError == null && passwordError == null

            return if (isSignUpMode) {
                baseFieldsValid && fullName.isNotBlank() && confirmPassword.isNotBlank() &&
                        fullNameError == null && confirmPasswordError == null &&
                        agreedToTerms
            } else {
                baseFieldsValid
            }
        }
}

