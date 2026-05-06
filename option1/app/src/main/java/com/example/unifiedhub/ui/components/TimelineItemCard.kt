package com.example.unifiedhub.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.unifiedhub.data.model.ItemType
import com.example.unifiedhub.data.model.TimelineItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Accordion-style card. Tap the header to expand and see full content.
 * Collapsed view shows: type badge, date, title (subject/contact/event), and a one-line summary.
 * Expanded view adds: full description (email body / SMS text / event description) and any extra info.
 */
@Composable
fun TimelineItemCard(item: TimelineItem) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.small,
                    color = getTypeColor(item.type).copy(alpha = 0.15f)
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

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                        Text(
                            text = formatTimestamp(item.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (item.extraInfo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = secondaryLine(item),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (expanded && item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * The secondary line shown under the title in the collapsed view.
 * - Email: sender ("From: Sarah Johnson")
 * - SMS:   direction ("Received" / "Sent")
 * - Calendar: location, if any
 * - Call: type ("Missed", "Received", etc.)
 */
private fun secondaryLine(item: TimelineItem): String = when (item.type) {
    ItemType.EMAIL -> "From: ${item.extraInfo}"
    ItemType.SMS -> item.extraInfo
    ItemType.CALENDAR_EVENT -> if (item.extraInfo.isNotBlank()) "📍 ${item.extraInfo}" else ""
    ItemType.CALL -> item.extraInfo
}

fun getTypeIcon(type: ItemType): ImageVector = when (type) {
    ItemType.EMAIL -> Icons.Default.Email
    ItemType.SMS -> Icons.Default.Sms
    ItemType.CALL -> Icons.Default.Phone
    ItemType.CALENDAR_EVENT -> Icons.Default.CalendarMonth
}

fun getTypeColor(type: ItemType): Color = when (type) {
    ItemType.EMAIL -> Color(0xFF1976D2)
    ItemType.SMS -> Color(0xFF43A047)
    ItemType.CALL -> Color(0xFFFF8F00)
    ItemType.CALENDAR_EVENT -> Color(0xFF8E24AA)
}

fun getTypeLabel(type: ItemType): String = when (type) {
    ItemType.EMAIL -> "EMAIL"
    ItemType.SMS -> "SMS"
    ItemType.CALL -> "CALL"
    ItemType.CALENDAR_EVENT -> "EVENT"
}

fun formatTimestamp(timestamp: Long): String {
    val now = Calendar.getInstance()
    val itemTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    return when {
        now.get(Calendar.DAY_OF_YEAR) == itemTime.get(Calendar.DAY_OF_YEAR) &&
            now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) -> {
            "Today ${timeFormat.format(Date(timestamp))}"
        }
        now.get(Calendar.DAY_OF_YEAR) - itemTime.get(Calendar.DAY_OF_YEAR) == 1 &&
            now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) -> {
            "Yesterday ${timeFormat.format(Date(timestamp))}"
        }
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
