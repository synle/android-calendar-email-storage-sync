// =============================================================================
// TimelineScreen.kt — The main screen of our app
// =============================================================================
// This is the HEART of our UI. It contains:
//   1. A top bar with the app title
//   2. Filter chips to show/hide item types
//   3. A scrollable list of timeline items
//   4. A floating button to generate the daily digest
//
// WHAT IS LazyColumn?
//
// LazyColumn is like a RecyclerView (if you've heard of that) but easier.
// It creates a scrollable vertical list, but here's the clever part:
// it only creates UI for items that are VISIBLE on screen.
//
// If you have 1000 items, only ~10 are visible at once. LazyColumn
// creates only those ~10 items, then recycles them as you scroll.
// This makes the list FAST even with lots of data.
//
// Think of it like a conveyor belt — items appear as they come into view
// and disappear as they scroll off screen.
// =============================================================================

package com.example.unifiedhub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.unifiedhub.data.model.ItemType
import com.example.unifiedhub.data.model.TimelineItem
import com.example.unifiedhub.ui.components.FilterChipRow
import com.example.unifiedhub.ui.components.TimelineItemCard
import com.example.unifiedhub.viewmodel.TimelineUiState

// --- The main timeline screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    uiState: TimelineUiState,                  // Current state of the UI
    onToggleFilter: (ItemType) -> Unit,        // When user taps a filter
    onGenerateDigest: () -> Unit,              // When user taps "Daily Digest"
    onDismissDigest: () -> Unit,               // When user closes the digest
    onShareDigest: (String) -> Unit            // When user wants to share the digest
) {
    // Scaffold = a layout structure that provides a top bar, bottom bar,
    // floating action button, etc. Think of it as the "frame" of a screen.
    Scaffold(
        // --- Top App Bar ---
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Unified Hub",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },

        // --- Floating Action Button (the digest button) ---
        // FAB = a round button that floats above the content
        // WHY a FAB? Because "Generate Digest" is the PRIMARY action,
        // and FABs are designed for primary actions.
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onGenerateDigest,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Summarize,
                        contentDescription = "Generate Daily Digest"
                    )
                },
                text = { Text("Daily Digest") }
            )
        }
    ) { paddingValues ->
        // paddingValues accounts for the top bar height, so our content
        // doesn't hide behind it.

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Filter chips ---
            FilterChipRow(
                activeFilters = uiState.activeFilters,
                onToggleFilter = onToggleFilter
            )

            // --- Item count ---
            Text(
                text = "${uiState.filteredItems.size} items",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // --- The timeline list ---
            when {
                // Show loading spinner while data is being fetched
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Show message if no items match the filters
                uiState.filteredItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items to show.\nTry enabling more filters.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Show the actual list
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // "items" tells LazyColumn what data to show
                        // "key" helps LazyColumn efficiently track which items
                        // changed (similar to keys in React, if you know that)
                        items(
                            items = uiState.filteredItems,
                            key = { it.id }
                        ) { item ->
                            TimelineItemCard(item = item)
                        }

                        // Add space at the bottom so the FAB doesn't cover the last item
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        // --- Daily Digest Dialog ---
        // Show the digest when digestText is not null
        if (uiState.digestText != null) {
            DigestDialog(
                digestText = uiState.digestText,
                onDismiss = onDismissDigest,
                onShare = { onShareDigest(uiState.digestText) }
            )
        }
    }
}

// --- The Daily Digest popup dialog ---
@Composable
fun DigestDialog(
    digestText: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Daily Digest",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            // Make the digest text scrollable (it might be long)
            LazyColumn {
                item {
                    Text(
                        text = digestText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onShare) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
