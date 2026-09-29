package com.proteahealth.app


import androidx.compose.ui.composed
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The top-level Auth screen. It owns navigation to the Terms screen (via a
 * simple boolean flag) and renders either the Terms screen or the main
 * login/signup form.
 *
 * Notice this Composable takes almost nothing as parameters, and instead
 * gets its own ViewModel via viewModel(). Compose's viewModel() function
 * automatically creates (or reuses, on recomposition) an AuthViewModel tied
 * to this screen's lifecycle — we don't have to manage that manually.
 */
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val viewModel: AuthViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var showTerms by rememberSaveable { mutableStateOf(false) }

    // React to successful auth: FirebaseAuth.currentUser becomes non-null the
    // moment sign-in/sign-up succeeds. We check this as a side effect rather
    // than inside the ViewModel's functions, keeping "what happens on success"
    // as a UI/navigation concern, not an auth-logic concern.
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
            onAuthSuccess()
        }
    }

    if (showTerms) {
        TermsAndConditionsScreen(onBack = { showTerms = false })
    } else {
        AuthFormContent(
            uiState = uiState,
            viewModel = viewModel,
            onShowTerms = { showTerms = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthFormContent(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    onShowTerms: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Whenever an error message appears in state, show it as a Snackbar, then
    // dismiss it in the ViewModel so it doesn't reappear on recomposition.
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.height(60.dp)) {}

            // --- Brand / logo placeholder ---
            Text(
                text = "Protea Health",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Column(modifier = Modifier.height(8.dp)) {}
            Text(
                text = if (uiState.isSignUpMode) "Create your account" else "Welcome back",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.height(32.dp)) {}

            // --- Google Sign-In button ---
            // Following Google's branding guidance: outlined style, "G" style
            // wording, not a solid colored button pretending to be Google's.
            OutlinedButton(
                onClick = { viewModel.signInWithGoogle(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading
            ) {
                Text("Continue with Google")
            }

            Column(modifier = Modifier.height(20.dp)) {}

            // --- "or" divider ---
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = "  or continue with email  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(modifier = Modifier.weight(1f))
            }

            Column(modifier = Modifier.height(20.dp)) {}

            // --- Full name (sign-up only) ---
            if (uiState.isSignUpMode) {
                OutlinedTextField(
                    value = uiState.fullName,
                    onValueChange = viewModel::onFullNameChange,
                    label = { Text("Full name") },
                    isError = uiState.fullNameError != null,
                    supportingText = {
                        uiState.fullNameError?.let { Text(it) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(modifier = Modifier.height(12.dp)) {}
            }

            // --- Email ---
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                isError = uiState.emailError != null,
                supportingText = {
                    uiState.emailError?.let { Text(it) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Column(modifier = Modifier.height(12.dp)) {}

            // --- Password ---
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                isError = uiState.passwordError != null,
                supportingText = {
                    uiState.passwordError?.let { Text(it) }
                },
                singleLine = true,
                visualTransformation = if (uiState.isPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible)
                                Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (uiState.isPasswordVisible)
                                "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // --- "Forgot password?" (sign-in only) ---
            if (!uiState.isSignUpMode) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { /* Hook up password-reset flow here */ }) {
                        Text("Forgot password?")
                    }
                }
            } else {
                Column(modifier = Modifier.height(12.dp)) {}
            }

            // --- Confirm password (sign-up only) ---
            if (uiState.isSignUpMode) {
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = { Text("Confirm password") },
                    isError = uiState.confirmPasswordError != null,
                    supportingText = {
                        uiState.confirmPasswordError?.let { Text(it) }
                    },
                    singleLine = true,
                    visualTransformation = if (uiState.isPasswordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Column(modifier = Modifier.height(12.dp)) {}

                // --- Terms and Conditions checkbox ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.agreedToTerms,
                        onCheckedChange = { viewModel.onTermsCheckedChange(it) }
                    )
                    Text(text = "I agree to the ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Terms and Conditions",
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickableText { onShowTerms() }
                    )
                }
                uiState.termsError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(modifier = Modifier.height(20.dp)) {}

            // --- Submit button ---
            Button(
                onClick = { if (uiState.isSignUpMode) viewModel.signUp() else viewModel.signIn() },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (uiState.isSignUpMode) "Create account" else "Sign in")
                }
            }

            Column(modifier = Modifier.height(24.dp)) {}

            // --- Mode toggle ---
            Row {
                Text(
                    text = if (uiState.isSignUpMode) "Already have an account? " else "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (uiState.isSignUpMode) "Sign in" else "Sign up",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickableText { viewModel.toggleMode() }
                )
            }

            Column(modifier = Modifier.height(40.dp)) {}
        }
    }
}

/**
 * Small helper so we can attach a click action to a Text without pulling in
 * a full clickable-text library. This wraps Modifier.clickable with sensible
 * defaults (no ripple needed for inline text links).
 */
private fun Modifier.clickableText(onClick: () -> Unit): Modifier = this.composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

