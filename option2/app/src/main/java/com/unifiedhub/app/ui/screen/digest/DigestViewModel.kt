package com.unifiedhub.app.ui.screen.digest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifiedhub.app.data.model.DailyDigest
import com.unifiedhub.app.data.repository.UnifiedTimelineRepository
import com.unifiedhub.app.worker.DigestScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class DigestUiState(
    val isLoading: Boolean = false,
    val digest: DailyDigest? = null,
    val formattedText: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val scheduledTime: LocalTime? = null,
    val isScheduled: Boolean = false
)

@HiltViewModel
class DigestViewModel @Inject constructor(
    private val repository: UnifiedTimelineRepository,
    private val digestScheduler: DigestScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigestUiState())
    val uiState: StateFlow<DigestUiState> = _uiState.asStateFlow()

    init {
        generateDigest(LocalDate.now())
    }

    fun generateDigest(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedDate = date) }
            try {
                val digest = repository.generateDailyDigest(date)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        digest = digest,
                        formattedText = digest.toFormattedText()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun copyToClipboard() {
        val text = _uiState.value.formattedText
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Daily Digest", text))
    }

    fun getShareIntent(): Intent {
        val text = _uiState.value.formattedText
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Daily Digest — ${_uiState.value.selectedDate}")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun scheduleDaily(time: LocalTime) {
        digestScheduler.scheduleDailyDigest(time)
        _uiState.update { it.copy(scheduledTime = time, isScheduled = true) }
    }

    fun cancelSchedule() {
        digestScheduler.cancelDailyDigest()
        _uiState.update { it.copy(scheduledTime = null, isScheduled = false) }
    }
}
