// =============================================================================
// MainActivity.kt — The entry point of our app
// =============================================================================
//
// WHAT IS AN ACTIVITY?
//
// An Activity is a single "screen" in an Android app. It's the container
// that holds your UI. When you open an app, Android creates an Activity.
//
// Think of an Activity as a WINDOW — it takes up the full screen and
// displays your content.
//
// OUR MAIN ACTIVITY:
//   1. Sets up Jetpack Compose
//   2. Handles permission requests
//   3. Decides which screen to show (permissions or timeline)
//   4. Creates the ViewModel
//
// FLOW:
//   App opens → Check permissions → Not granted? Show permission screen
//                                 → Granted? Show timeline screen
// =============================================================================

package com.example.unifiedhub

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.unifiedhub.ui.screens.PermissionScreen
import com.example.unifiedhub.ui.screens.TimelineScreen
import com.example.unifiedhub.ui.screens.requiredPermissions
import com.example.unifiedhub.ui.theme.UnifiedHubTheme
import com.example.unifiedhub.viewmodel.TimelineViewModel
import com.example.unifiedhub.viewmodel.TimelineViewModelFactory

class MainActivity : ComponentActivity() {

    // --- The ViewModel (our "brain") ---
    // lateinit means "I'll initialize this later" (in onCreate)
    // WHY lateinit? Because we need the Activity to be created first
    // before we can create the ViewModel.
    private lateinit var viewModel: TimelineViewModel

    // --- Track whether permissions have been granted ---
    // mutableStateOf creates a Compose-observable state variable.
    // When this changes, any Composable reading it will automatically redraw.
    private var permissionsGranted = mutableStateOf(false)

    // --- The permission request launcher ---
    // WHY this pattern? Android requires you to register the launcher BEFORE
    // the Activity is fully created (in the class body, not in onCreate).
    // This "contract" handles the permission dialog and gives us the results.
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // This code runs AFTER the user responds to the permission dialog
        // 'permissions' is a Map<String, Boolean> — permission name → granted?

        // Check if ANY permission was granted
        val anyGranted = permissions.values.any { it }
        if (anyGranted) {
            permissionsGranted.value = true
            // Load data with whatever permissions we got
            viewModel.loadData(getGrantedPermissions())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Create the ViewModel ---
        // We use a Factory because our ViewModel needs a ContentResolver
        val factory = TimelineViewModelFactory(contentResolver)
        viewModel = ViewModelProvider(this, factory)[TimelineViewModel::class.java]

        // --- Check if permissions are already granted ---
        // (They might be if the user granted them in a previous session)
        permissionsGranted.value = hasAnyPermission()

        // If permissions are already granted, load data immediately
        if (permissionsGranted.value) {
            viewModel.loadData(getGrantedPermissions())
        }

        // --- Set up the Compose UI ---
        // setContent is where we connect Compose to the Activity
        // Everything inside { } is Compose code
        setContent {
            UnifiedHubTheme {
                // Collect the ViewModel's state as Compose state
                // WHY collectAsState()? It converts a StateFlow (Kotlin)
                // into a State (Compose), so the UI auto-updates.
                val uiState by viewModel.uiState.collectAsState()

                // --- Decide which screen to show ---
                if (permissionsGranted.value) {
                    // Show the main timeline
                    TimelineScreen(
                        uiState = uiState,
                        onSelectTab = { viewModel.selectTab(it) },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onToggleSort = { viewModel.toggleSortDirection() },
                        onGenerateDigest = { viewModel.generateDailyDigest() },
                        onDismissDigest = { viewModel.clearDigest() },
                        onShareDigest = { text -> shareDigest(text) }
                    )
                } else {
                    // Show the permission request screen
                    PermissionScreen(
                        onRequestPermissions = { requestPermissions() }
                    )
                }
            }
        }
    }

    // --- Request all permissions ---
    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }

    // --- Check if we have at least one permission ---
    private fun hasAnyPermission(): Boolean {
        return requiredPermissions.any { permission ->
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    // --- Get a set of all GRANTED permissions ---
    private fun getGrantedPermissions(): Set<String> {
        return requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }.toSet()
    }

    // --- Share the daily digest via Android's share sheet ---
    // WHY an Intent? Android uses "Intents" to communicate between apps.
    // A "share intent" tells Android: "I have some text, show me apps
    // that can handle it" (email, WhatsApp, Notes, etc.)
    private fun shareDigest(text: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND          // "I want to SEND something"
            type = "text/plain"                  // "It's plain text"
            putExtra(Intent.EXTRA_SUBJECT, "Unified Hub - Daily Digest")
            putExtra(Intent.EXTRA_TEXT, text)     // The actual text
        }
        // createChooser shows a nice picker dialog
        startActivity(Intent.createChooser(shareIntent, "Share Daily Digest"))
    }
}
