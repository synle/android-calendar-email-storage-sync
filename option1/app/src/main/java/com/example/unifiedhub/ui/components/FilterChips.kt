// =============================================================================
// FilterChips.kt — Filter toggles at the top of the timeline
// =============================================================================
// These "chips" let users show/hide specific types of items.
// For example, tapping "SMS" will hide all SMS messages from the timeline.
//
// WHAT IS A FilterChip?
// It's a small, tappable button that toggles on/off. Think of it like
// a light switch — tap to turn a category on or off.
// =============================================================================

package com.example.unifiedhub.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.unifiedhub.data.model.ItemType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipRow(
    activeFilters: Set<ItemType>,      // Which filters are currently ON
    onToggleFilter: (ItemType) -> Unit // Called when user taps a chip
) {
    // horizontalScroll makes the row scrollable if chips don't fit on screen
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between chips
    ) {
        // Create one chip for each item type
        ItemType.entries.forEach { type ->
            val isSelected = type in activeFilters

            FilterChip(
                selected = isSelected,
                onClick = { onToggleFilter(type) },
                label = {
                    Text(
                        text = getTypeLabel(type),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = getTypeIcon(type),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            getTypeColor(type)
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = getTypeColor(type).copy(alpha = 0.2f)
                )
            )
        }
    }
}
