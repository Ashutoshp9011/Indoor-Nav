package com.ashutosh.corridor360.mapping

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sync status shown on MappingScreen. Kept separate from MappingViewModel's
 * other state since sync has its own lifecycle (can be triggered independently
 * of navigation/mapping actions).
 */
sealed class SyncUiState {
    object Idle : SyncUiState()
    object Syncing : SyncUiState()
    data class Synced(val timestamp: Long) : SyncUiState()
    data class Error(val message: String) : SyncUiState()
}

/**
 * Add this as a delegate/composed piece inside MappingViewModel, or expose
 * it as a separate ViewModel if MappingViewModel is already large — either
 * works, this assumes it's folded into MappingViewModel's existing scope.
 */
class SyncController(
    private val syncManager: GitHubSyncManagerContract,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    fun triggerSync() {
        if (_syncState.value == SyncUiState.Syncing) return // prevent double-tap

        _syncState.value = SyncUiState.Syncing
        scope.launch {
            try {
                syncManager.syncNow()
                _syncState.value = SyncUiState.Synced(System.currentTimeMillis())
            } catch (e: Exception) {
                _syncState.value = SyncUiState.Error(e.message ?: "Sync failed")
            }
        }
    }
}

/** Minimal contract so SyncController doesn't need your full GitHubSyncManager API surface. */
interface GitHubSyncManagerContract {
    suspend fun syncNow()
}

/**
 * Drop this into MappingScreen's top bar or wherever makes sense in your layout.
 */
@Composable
fun SyncStatusButton(
    syncState: SyncUiState,
    onSyncClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        when (syncState) {
            is SyncUiState.Idle -> {
                IconButton(onClick = onSyncClick) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Sync,
                        contentDescription = "Sync with GitHub"
                    )
                }
            }
            is SyncUiState.Syncing -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp
                )
            }
            is SyncUiState.Synced -> {
                IconButton(onClick = onSyncClick) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.CloudDone,
                        contentDescription = "Synced",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
            is SyncUiState.Error -> {
                IconButton(onClick = onSyncClick) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.SyncProblem,
                        contentDescription = "Sync failed — tap to retry: ${syncState.message}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}