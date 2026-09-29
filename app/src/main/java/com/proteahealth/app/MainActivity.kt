package com.proteahealth.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.proteahealth.app.ui.theme.Health1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Health1Theme {
                AppRoot()
            }
        }
    }
}

/**
 * AppRoot is a very simple router: it just asks "is someone logged in right
 * now?" and shows either AuthScreen or HomeScreen accordingly.
 *
 * This is intentionally minimal — a real app would likely use the Navigation
 * Compose library for multi-screen flows, but for proving your login/signup
 * works end-to-end, this simple boolean-driven approach is enough and easy
 * to understand.
 */
@Composable
fun AppRoot() {
    // We check FirebaseAuth.currentUser once, on first composition, to decide
    // the starting screen (e.g. if the user closed and reopened the app while
    // still logged in from before).
    var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    if (isLoggedIn) {
        HomeScreen(onSignOut = {
            FirebaseAuth.getInstance().signOut()
            isLoggedIn = false
        })
    } else {
        AuthScreen(onAuthSuccess = { isLoggedIn = true })
    }
}

@Composable
fun HomeScreen(onSignOut: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "You're signed in!",
                style = MaterialTheme.typography.headlineSmall
            )
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(text = "Email: ${user?.email ?: "N/A"}")
                Text(text = "Name: ${user?.displayName ?: "N/A"}")
            }
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Button(onClick = onSignOut) {
                    Text("Sign out")
                }
            }
        }
    }
}

// condition , gender , allergy ,
// user role