// =============================================================================
// TimelineItemCard.kt — How each item looks in the timeline
// =============================================================================
//
// WHAT IS A @Composable FUNCTION?
//
// In Jetpack Compose, UI is built using regular Kotlin functions marked
// with @Composable. Each function describes a piece of UI.
//
// Think of it like building with LEGO blocks:
//   - TimelineItemCard = one LEGO brick (shows one item)
//   - TimelineScreen = the whole LEGO structure (shows the list)
//   - Each brick is a @Composable function
//
// KEY DIFFERENCE from traditional Android:
//   OLD way: XML layout files + Activity code to populate them
//   NEW way: Everything is Kotlin code — layout AND data together
//
// HOW COMPOSE WORKS:
//   1. You write functions that DESCRIBE what the UI should look like
//   2. When data changes, Compose REDRAWS only the parts that changed
//   3. You never manually say "update this TextView" — it's automatic!
// =============================================================================

package com.example.unifiedhub.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.unifiedhub.data.model.ItemType
import com.example.unifiedhub.data.model.TimelineItem
import java.text.SimpleDateFormat
import java.util.*

// --- The card that displays one timeline item ---
@Composable
fun TimelineItemCard(item: TimelineItem) {
    // Card = a Material Design container with rounded corners and elevation
    Card(
        modifier = Modifier
            .fillMaxWidth()         // Take full width of screen
            .padding(horizontal = 16.dp, vertical = 4.dp),  // Space around card
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface  // Card background
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),       // Space inside the card
            verticalAlignment = Alignment.Top
        ) {
            // --- Left side: colored icon ---
            // Each type gets a unique icon and color so users can quickly
            // scan the timeline and identify item types at a glance.
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = getTypeColor(item.type).copy(alpha = 0.15f)  // Light tint background
            ) {
                Icon(
                    imageVector = getTypeIcon(item.type),
                    contentDescription = item.type.name,
                    tint = getTypeColor(item.type),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // --- Right side: text content ---
            Column(modifier = Modifier.weight(1f)) {
                // --- Top row: type label + time ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type label badge (e.g., "EMAIL", "SMS")
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = getTypeColor(item.type).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = getTypeLabel(item.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = getTypeColor(item.type),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Time display
                    Text(
                        text = formatTimestamp(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // --- Title ---
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis  // Show "..." if text is too long
                )

                // --- Description (if present) ---
                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // --- Extra info badge (e.g., "Missed", "Received") ---
                if (item.extraInfo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.extraInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.extraInfo == "Missed") {
                            Color(0xFFE53935)  // Red for missed calls
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                }
            }
        }
    }
}

// --- Helper: Get the right icon for each type ---
// Icons help users quickly identify what type each item is
fun getTypeIcon(type: ItemType): ImageVector = when (type) {
    ItemType.EMAIL -> Icons.Default.Email
    ItemType.SMS -> Icons.Default.Sms
    ItemType.CALL -> Icons.Default.Phone
    ItemType.CALENDAR_EVENT -> Icons.Default.CalendarMonth
}

// --- Helper: Get the right color for each type ---
fun getTypeColor(type: ItemType): Color = when (type) {
    ItemType.EMAIL -> Color(0xFF1976D2)        // Blue
    ItemType.SMS -> Color(0xFF43A047)          // Green
    ItemType.CALL -> Color(0xFFFF8F00)         // Amber/Orange
    ItemType.CALENDAR_EVENT -> Color(0xFF8E24AA) // Purple
}

// --- Helper: Get a human-readable label ---
fun getTypeLabel(type: ItemType): String = when (type) {
    ItemType.EMAIL -> "EMAIL"
    ItemType.SMS -> "SMS"
    ItemType.CALL -> "CALL"
    ItemType.CALENDAR_EVENT -> "EVENT"
}

// --- Helper: Format timestamp to a readable string ---
// Shows "Today 2:30 PM", "Yesterday 9:15 AM", or "Feb 26, 3:00 PM"
fun formatTimestamp(timestamp: Long): String {
    val now = Calendar.getInstance()
    val itemTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    return when {
        // Same day
        now.get(Calendar.DAY_OF_YEAR) == itemTime.get(Calendar.DAY_OF_YEAR) &&
                now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) -> {
            "Today ${timeFormat.format(Date(timestamp))}"
        }
        // Yesterday
        now.get(Calendar.DAY_OF_YEAR) - itemTime.get(Calendar.DAY_OF_YEAR) == 1 &&
                now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) -> {
            "Yesterday ${timeFormat.format(Date(timestamp))}"
        }
        // Older
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
