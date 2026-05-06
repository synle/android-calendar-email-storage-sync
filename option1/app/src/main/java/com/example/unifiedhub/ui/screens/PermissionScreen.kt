// =============================================================================
// PermissionScreen.kt — Asks the user for permissions
// =============================================================================
//
// WHY DO WE NEED RUNTIME PERMISSIONS?
//
// Starting with Android 6.0 (API 23), "dangerous" permissions must be
// granted by the user AT RUNTIME — not just declared in the manifest.
//
// This is a TWO-STEP process:
//   1. Declare in AndroidManifest.xml → "My app MIGHT need this"
//   2. Ask at runtime → "Can I please access your SMS right now?"
//
// The user can:
//   - GRANT the permission → we can access the data
//   - DENY the permission → we must handle this gracefully
//   - DENY + "Don't ask again" → we can't ask again, must send to Settings
//
// BEST PRACTICES:
//   - Explain WHY you need each permission before asking
//   - Don't force the user — the app should work with partial permissions
//   - Only ask when you actually need it (not all at once on first launch)
//
// For simplicity, we ask for all permissions on a welcome screen.
// =============================================================================

package com.example.unifiedhub.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// --- The list of permissions we need ---
// We define them here so they're easy to reference throughout the app
val requiredPermissions = arrayOf(
    Manifest.permission.READ_CALENDAR,
    Manifest.permission.READ_SMS,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.READ_CONTACTS
)

// --- The permission request screen ---
@Composable
fun PermissionScreen(
    onRequestPermissions: () -> Unit  // Called when user taps "Grant Permissions"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- App icon / title ---
        Icon(
            imageVector = Icons.Default.Hub,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to Unified Hub",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "To show your timeline, we need access to some of your data.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- Explain each permission ---
        PermissionExplanation(
            icon = Icons.Default.CalendarMonth,
            title = "Calendar",
            description = "View your upcoming events and meetings"
        )
        PermissionExplanation(
            icon = Icons.Default.Sms,
            title = "SMS Messages",
            description = "Show text messages in your timeline"
        )
        PermissionExplanation(
            icon = Icons.Default.Phone,
            title = "Call History",
            description = "Display recent calls and missed calls"
        )
        PermissionExplanation(
            icon = Icons.Default.Contacts,
            title = "Contacts",
            description = "Show contact names instead of phone numbers"
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- The "Grant Permissions" button ---
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permissions")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You can change these later in Settings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

// --- A row explaining one permission ---
@Composable
fun PermissionExplanation(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
