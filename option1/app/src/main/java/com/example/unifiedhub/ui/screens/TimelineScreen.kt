package com.example.unifiedhub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.unifiedhub.ui.components.TimelineItemCard
import com.example.unifiedhub.viewmodel.TabType
import com.example.unifiedhub.viewmodel.TimelineUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    uiState: TimelineUiState,
    onSelectTab: (TabType) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleSort: () -> Unit,
    onGenerateDigest: () -> Unit,
    onDismissDigest: () -> Unit,
    onShareDigest: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Unified Hub",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onToggleSort) {
                        Icon(
                            imageVector = if (uiState.sortDescending)
                                Icons.Default.ArrowDownward
                            else
                                Icons.Default.ArrowUpward,
                            contentDescription = if (uiState.sortDescending)
                                "Sort: newest first"
                            else
                                "Sort: oldest first"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Tab row ---
            TabRow(
                selectedTabIndex = TabType.entries.indexOf(uiState.selectedTab)
            ) {
                TabType.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        text = { Text(tab.label) },
                        icon = {
                            Icon(
                                imageVector = iconForTab(tab),
                                contentDescription = tab.label
                            )
                        }
                    )
                }
            }

            // --- Search bar ---
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search ${uiState.selectedTab.label.lowercase()}…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true
            )

            // --- Item count + sort indicator ---
            Text(
                text = buildString {
                    append("${uiState.visibleItems.size} ${uiState.selectedTab.label.lowercase()} ")
                    append(if (uiState.visibleItems.size == 1) "item" else "items")
                    append(" · ")
                    append(if (uiState.sortDescending) "newest first" else "oldest first")
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // --- The list ---
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.visibleItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotBlank())
                                "No matches for \"${uiState.searchQuery}\"."
                            else
                                "No ${uiState.selectedTab.label.lowercase()} items.\nGrant permission or check back later.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = uiState.visibleItems,
                            key = { it.id }
                        ) { item ->
                            TimelineItemCard(item = item)
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        if (uiState.digestText != null) {
            DigestDialog(
                digestText = uiState.digestText,
                onDismiss = onDismissDigest,
                onShare = { onShareDigest(uiState.digestText) }
            )
        }
    }
}

private fun iconForTab(tab: TabType): ImageVector = when (tab) {
    TabType.EMAIL -> Icons.Default.Email
    TabType.CALENDAR -> Icons.Default.CalendarMonth
    TabType.SMS -> Icons.Default.Sms
}

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
