package com.proteahealth.app

import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.tasks.await

// PASTE YOUR WEB CLIENT ID HERE (from Firebase Console > Authentication > Sign-in method > Google)
private const val WEB_CLIENT_ID = "442968786967-1dfpdbi8f1s63t6qvjjj3lqh96h3mq26.apps.googleusercontent.com"
/**
 * AuthViewModel owns all state and logic for the login/signup screen.
 *
 * Why a ViewModel? Two reasons:
 * 1. It survives configuration changes (like screen rotation) — the UI can be
 *    destroyed and recreated without losing what the user typed.
 * 2. It keeps Firebase logic OUT of our Composables. Composables should just
 *    describe "what the screen looks like given this data" — they shouldn't
 *    know HOW to talk to Firebase. This separation makes the code testable
 *    (you can test this class without any UI at all) and easier to reason about.
 */
class AuthViewModel : ViewModel() {

    // FirebaseAuth.getInstance() gives us the single shared connection to
    // Firebase Authentication for this app.
    private val firebaseAuth = FirebaseAuth.getInstance()

    // _uiState is private and mutable — only THIS class can change it.
    // uiState is public and read-only — the UI can only observe it, not change it directly.
    // This is a deliberate safety rail: it prevents the UI from randomly mutating
    // state.g in ways the ViewModel doesn't know about.
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ---------- Field update functions ----------
    // Each of these is called from the UI every time the user types a character.
    // We re-run validation on every change so error messages update live.

    fun onFullNameChange(value: String) {
        _uiState.value = _uiState.value.copy(
            fullName = value,
            fullNameError = if (value.isBlank()) null else validateFullName(value)
        )
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value,
            emailError = if (value.isBlank()) null else validateEmail(value)
        )
    }

    fun onPasswordChange(value: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            password = value,
            passwordError = if (value.isBlank()) null else validatePassword(value),
            // Re-check confirm password too, in case they already typed it
            confirmPasswordError = if (current.confirmPassword.isNotBlank())
                validateConfirmPassword(value, current.confirmPassword) else null
        )
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = value,
            confirmPasswordError = validateConfirmPassword(_uiState.value.password, value)
        )
    }

    fun onTermsCheckedChange(checked: Boolean) {
        _uiState.value = _uiState.value.copy(agreedToTerms = checked, termsError = null)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
    }

    fun toggleMode() {
        // Switching between Sign In / Sign Up: reset the form so old errors
        // and values from the other mode don't leak through.
        _uiState.value = AuthUiState(isSignUpMode = !_uiState.value.isSignUpMode)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // ---------- Validation ----------
    // These are pure functions: given input, they return an error message or
    // null. Pure functions are easy to test and reason about in isolation.

    private fun validateFullName(name: String): String? {
        return if (name.trim().length < 2) "Please enter your full name" else null
    }

    private fun validateEmail(email: String): String? {
        val pattern = android.util.Patterns.EMAIL_ADDRESS
        return if (!pattern.matcher(email).matches()) "Enter a valid email address" else null
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.length < 8 -> "Password must be at least 8 characters"
            !password.any { it.isDigit() } -> "Password must include at least one number"
            else -> null
        }
    }

    private fun validateConfirmPassword(password: String, confirm: String): String? {
        return if (password != confirm) "Passwords do not match" else null
    }

    // ---------- Firebase: Email/Password Sign Up ----------

    fun signUp() {
        val state = _uiState.value

        // Final validation sweep before hitting the network — catches the case
        // where a field was never touched (so no error was ever generated) but
        // is still empty or invalid.
        val fullNameError = validateFullName(state.fullName)
        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password)
        val confirmPasswordError = validateConfirmPassword(state.password, state.confirmPassword)
        val termsError = if (!state.agreedToTerms) "You must agree to the Terms and Conditions" else null

        if (listOf(fullNameError, emailError, passwordError, confirmPasswordError, termsError).any { it != null }) {
            _uiState.value = state.copy(
                fullNameError = fullNameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError,
                termsError = termsError
            )
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        // viewModelScope.launch runs this coroutine tied to the ViewModel's
        // lifecycle — if the ViewModel is cleared, this work cancels automatically.
        viewModelScope.launch {
            try {
                // .await() (from kotlinx-coroutines-play-services, pulled in
                // transitively) turns Firebase's Task-based API into a suspend
                // call, so we can write this like linear code instead of nesting
                // callbacks.
                val result = firebaseAuth
                    .createUserWithEmailAndPassword(state.email, state.password)
                    .await()

                // Optionally set the display name on the new user's profile.
                result.user?.updateProfile(
                    com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(state.fullName)
                        .build()
                )?.await()

                _uiState.value = _uiState.value.copy(isLoading = false)
                // At this point, sign-up succeeded. In MainActivity/navigation,
                // you'd now navigate to your app's main/home screen.
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapFirebaseError(e)
                )
            }
        }
    }

    // ---------- Firebase: Email/Password Sign In ----------

    fun signIn() {
        val state = _uiState.value
        val emailError = validateEmail(state.email)
        val passwordError = if (state.password.isBlank()) "Enter your password" else null

        if (emailError != null || passwordError != null) {
            _uiState.value = state.copy(emailError = emailError, passwordError = passwordError)
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                firebaseAuth.signInWithEmailAndPassword(state.email, state.password).await()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapFirebaseError(e)
                )
            }
        }
    }

    // ---------- Google Sign-In (via Credential Manager) ----------
    // Credential Manager is Google's modern replacement for the old
    // GoogleSignInClient API. It needs an Android Context, which is why this
    // function takes one in as a parameter instead of the ViewModel holding
    // a Context itself (ViewModels should never hold a long-lived reference
    // to a Context/Activity — it causes memory leaks).

    fun signInWithGoogle(context: android.content.Context) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)

                // A nonce is a random one-time value that helps prevent replay
                // attacks on the sign-in flow.
                val rawNonce = UUID.randomUUID().toString()
                val hashedNonce = MessageDigest.getInstance("SHA-256")
                    .digest(rawNonce.toByteArray())
                    .joinToString("") { "%02x".format(it) }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(result.credential.data)

                val firebaseCredential = GoogleAuthProvider.getCredential(
                    googleIdTokenCredential.idToken, null
                )

                firebaseAuth.signInWithCredential(firebaseCredential).await()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: GetCredentialException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Google sign-in was cancelled or unavailable."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapFirebaseError(e)
                )
            }
        }
    }

    // ---------- Error mapping ----------
    // Firebase throws specific exception types. We map each to a message a
    // real user can understand, instead of showing a raw stack trace message.

    private fun mapFirebaseError(e: Exception): String {
        return when (e) {
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Try a longer, less common one."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
            is FirebaseAuthInvalidUserException -> "No account found with this email."
            is FirebaseNetworkException -> "Network error. Check your connection and try again."
            else -> e.localizedMessage ?: "Something went wrong. Please try again."
        }
    }
}
