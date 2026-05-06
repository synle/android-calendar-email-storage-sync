package com.unifiedhub.app.ui.screen.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.unifiedhub.app.data.model.TimelineItemType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    activeFilters: Set<TimelineItemType>,
    onToggleFilter: (TimelineItemType) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Filter Sources",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterOption(
                    label = "Email",
                    icon = Icons.Outlined.Email,
                    isSelected = TimelineItemType.EMAIL in activeFilters,
                    onClick = { onToggleFilter(TimelineItemType.EMAIL) },
                    modifier = Modifier.weight(1f)
                )
                FilterOption(
                    label = "Calendar",
                    icon = Icons.Outlined.CalendarMonth,
                    isSelected = TimelineItemType.CALENDAR_EVENT in activeFilters,
                    onClick = { onToggleFilter(TimelineItemType.CALENDAR_EVENT) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterOption(
                    label = "SMS",
                    icon = Icons.Outlined.Sms,
                    isSelected = TimelineItemType.SMS in activeFilters,
                    onClick = { onToggleFilter(TimelineItemType.SMS) },
                    modifier = Modifier.weight(1f)
                )
                FilterOption(
                    label = "Calls",
                    icon = Icons.Outlined.Phone,
                    isSelected = TimelineItemType.CALL_LOG in activeFilters,
                    onClick = { onToggleFilter(TimelineItemType.CALL_LOG) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FilterOption(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(label)
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier
    )
}
