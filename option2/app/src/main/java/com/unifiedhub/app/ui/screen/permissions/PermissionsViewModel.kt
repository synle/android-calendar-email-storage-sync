package com.unifiedhub.app.ui.screen.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PermissionState(
    val permission: String,
    val label: String,
    val description: String,
    val isGranted: Boolean
)

data class PermissionsUiState(
    val permissions: List<PermissionState> = emptyList(),
    val allGranted: Boolean = false
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    private val requiredPermissions = listOf(
        Triple(
            Manifest.permission.READ_CALENDAR,
            "Calendar",
            "Access calendar events for your timeline"
        ),
        Triple(
            Manifest.permission.READ_SMS,
            "SMS Messages",
            "Read text messages to include in timeline"
        ),
        Triple(
            Manifest.permission.READ_CALL_LOG,
            "Call History",
            "Access call logs for your timeline"
        ),
        Triple(
            Manifest.permission.READ_CONTACTS,
            "Contacts",
            "Resolve contact names and avatars"
        ),
        Triple(
            Manifest.permission.POST_NOTIFICATIONS,
            "Notifications",
            "Show digest notifications"
        )
    )

    init {
        refreshPermissionStates()
    }

    fun refreshPermissionStates() {
        val states = requiredPermissions.map { (permission, label, description) ->
            PermissionState(
                permission = permission,
                label = label,
                description = description,
                isGranted = ContextCompat.checkSelfPermission(
                    context, permission
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
        _uiState.update {
            PermissionsUiState(
                permissions = states,
                allGranted = states.all { it.isGranted }
            )
        }
    }

    fun getUngrantedPermissions(): Array<String> =
        _uiState.value.permissions
            .filterNot { it.isGranted }
            .map { it.permission }
            .toTypedArray()
}
