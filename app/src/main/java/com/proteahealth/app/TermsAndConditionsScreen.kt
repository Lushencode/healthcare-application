package com.proteahealth.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A single section of the Terms and Conditions: a heading plus its body text.
 * Modeling it as a data class (rather than hardcoding a big string) lets us
 * render it as a scrollable LazyColumn, which performs well even if the
 * document grows long.
 */
private data class TermsSection(val heading: String, val body: String)

private val termsSections = listOf(
    TermsSection(
        "1. Acceptance of Terms",
        "By creating an account or using Protea Health (\"the App\"), you agree to be bound by these Terms and Conditions. If you do not agree, please do not use the App."
    ),
    TermsSection(
        "2. Eligibility",
        "You must be at least 18 years old, or the age of legal majority in your jurisdiction, to create an account. By registering, you confirm that the information you provide is accurate and that you meet this requirement."
    ),
    TermsSection(
        "3. Your Account and Responsibilities",
        "You are responsible for maintaining the confidentiality of your login credentials and for all activity that occurs under your account. Notify us immediately if you suspect unauthorized access."
    ),
    TermsSection(
        "4. Acceptable Use",
        "You agree not to misuse the App, including attempting to access data that isn't yours, disrupting the service, or using it for any unlawful purpose. We may suspend or terminate accounts that violate this policy."
    ),
    TermsSection(
        "5. Privacy and Data Handling",
        "We collect and process personal data as described in our Privacy Policy, including account details and usage information, to provide and improve our services. We do not sell your personal data to third parties."
    ),
    TermsSection(
        "6. Intellectual Property",
        "All content, branding, and technology within the App are owned by Protea Health or its licensors. You may not copy, modify, or distribute this content without permission."
    ),
    TermsSection(
        "7. Termination",
        "We may suspend or terminate your account if you violate these terms. You may also delete your account at any time through the App's settings."
    ),
    TermsSection(
        "8. Limitation of Liability",
        "The App is provided \"as is.\" To the fullest extent permitted by law, Protea Health is not liable for indirect, incidental, or consequential damages arising from your use of the App."
    ),
    TermsSection(
        "9. Changes to These Terms",
        "We may update these Terms from time to time. Continued use of the App after changes take effect constitutes acceptance of the revised Terms."
    ),
    TermsSection(
        "10. Governing Law",
        "These Terms are governed by the laws of the jurisdiction in which Protea Health is registered, without regard to conflict-of-law principles."
    )
)

private const val LAST_UPDATED = "Last updated: [DATE — replace before launch]"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms and Conditions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = LAST_UPDATED,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            items(termsSections) { section ->
                Column {
                    Text(
                        text = section.heading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = section.body,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            item {
                // Bottom spacer so the last section isn't flush against the screen edge
                Column(modifier = Modifier.padding(bottom = 24.dp)) {}
            }
        }
    }
}
