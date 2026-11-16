package com.example.apkextractor.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apkextractor.R
import com.example.apkextractor.data.ApkStorage
import com.example.apkextractor.data.AppEntry
import com.example.apkextractor.data.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExtractionProgress(
    val current: Int,
    val total: Int
)

data class ExtractionResult(
    val succeeded: Int,
    val failed: Int,
    val folderDisplay: String
)

data class MainUiState(
    val isLoading: Boolean = true,
    val allApps: List<AppEntry> = emptyList(),
    val filteredApps: List<AppEntry> = emptyList(),
    val showSystemApps: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val isSelectionMode: Boolean = false,
    val selectedPackages: Set<String> = emptySet(),
    val actionSheetApp: AppEntry? = null,
    val confirmExtractAllCount: Int? = null,
    val extractionProgress: ExtractionProgress? = null,
    val extractionResult: ExtractionResult? = null,
    val isPreparingShare: Boolean = false
)

sealed interface ApkUiEvent {
    data object RequestStoragePermission : ApkUiEvent
    data class LaunchShareIntent(val uris: ArrayList<Uri>) : ApkUiEvent
    data class ShowMessage(val message: String) : ApkUiEvent
}

class ApkExtractorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ApkUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ApkUiEvent> = _events.asSharedFlow()

    private var pendingStorageAction: (() -> Unit)? = null

    init {
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val loaded = try {
                AppRepository.loadInstalledApps(getApplication())
            } catch (_: Exception) {
                emptyList()
            }
            _uiState.update { state ->
                val updated = state.copy(
                    isLoading = false,
                    allApps = loaded
                )
                applyFilterAndSelection(updated)
            }
        }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update { state ->
            val updated = state.copy(
                isSearchActive = active,
                searchQuery = if (active) state.searchQuery else ""
            )
            applyFilterAndSelection(updated)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            applyFilterAndSelection(state.copy(searchQuery = query))
        }
    }

    fun toggleShowSystemApps() {
        _uiState.update { state ->
            applyFilterAndSelection(state.copy(showSystemApps = !state.showSystemApps))
        }
    }

    fun onRowClick(app: AppEntry) {
        if (_uiState.value.isSelectionMode) {
            toggleSelection(app)
        } else {
            _uiState.update { it.copy(actionSheetApp = app) }
        }
    }

    fun onRowLongClick(app: AppEntry) {
        _uiState.update { state ->
            if (!state.isSelectionMode) {
                state.copy(
                    isSelectionMode = true,
                    selectedPackages = setOf(app.packageName)
                )
            } else {
                state
            }
        }
        if (_uiState.value.isSelectionMode && app.packageName !in _uiState.value.selectedPackages) {
            toggleSelection(app)
        }
    }

    fun toggleSelection(app: AppEntry) {
        _uiState.update { state ->
            val next = state.selectedPackages.toMutableSet()
            if (!next.add(app.packageName)) {
                next.remove(app.packageName)
            }
            state.copy(
                selectedPackages = next,
                isSelectionMode = next.isNotEmpty()
            )
        }
    }

    fun selectAllVisible() {
        _uiState.update { state ->
            val allVisible = state.filteredApps.map { it.packageName }.toSet()
            state.copy(
                isSelectionMode = allVisible.isNotEmpty(),
                selectedPackages = allVisible
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(isSelectionMode = false, selectedPackages = emptySet()) }
    }

    fun selectedApps(): List<AppEntry> {
        val state = _uiState.value
        return state.filteredApps.filter { it.packageName in state.selectedPackages }
    }

    fun dismissActionSheet() {
        _uiState.update { it.copy(actionSheetApp = null) }
    }

    fun promptExtractAll() {
        val count = _uiState.value.filteredApps.size
        if (count > 0) {
            _uiState.update { it.copy(confirmExtractAllCount = count) }
        }
    }

    fun dismissExtractAllPrompt() {
        _uiState.update { it.copy(confirmExtractAllCount = null) }
    }

    fun confirmExtractAll() {
        val apps = _uiState.value.filteredApps
        _uiState.update { it.copy(confirmExtractAllCount = null) }
        extractApps(apps)
    }

    fun dismissExtractionResult() {
        _uiState.update { it.copy(extractionResult = null) }
    }

    fun extractApps(apps: List<AppEntry>) {
        if (apps.isEmpty()) return
        withStoragePermission {
            runExtraction(apps)
        }
    }

    fun exportAppList() {
        val apps = _uiState.value.filteredApps
        if (apps.isEmpty()) return
        withStoragePermission {
            viewModelScope.launch {
                val context = getApplication<Application>()
                try {
                    withContext(Dispatchers.IO) {
                        ApkStorage.exportAppList(context, apps)
                    }
                    _events.tryEmit(
                        ApkUiEvent.ShowMessage(
                            context.getString(
                                R.string.list_exported,
                                "Download/${ApkStorage.FOLDER}/${ApkStorage.APP_LIST_FILE}"
                            )
                        )
                    )
                } catch (e: Exception) {
                    emitError(e)
                }
            }
        }
    }

    fun shareApps(apps: List<AppEntry>) {
        if (apps.isEmpty() || _uiState.value.isPreparingShare) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingShare = true) }
            try {
                val uris = withContext(Dispatchers.IO) {
                    ApkStorage.prepareShare(getApplication(), apps)
                }
                _events.tryEmit(ApkUiEvent.LaunchShareIntent(uris))
            } catch (e: Exception) {
                emitError(e)
            } finally {
                _uiState.update { it.copy(isPreparingShare = false) }
            }
        }
    }

    fun onStoragePermissionResult(granted: Boolean) {
        val action = pendingStorageAction
        pendingStorageAction = null
        if (granted) {
            action?.invoke()
        } else {
            val msg = getApplication<Application>().getString(R.string.storage_denied)
            _events.tryEmit(ApkUiEvent.ShowMessage(msg))
        }
    }

    private fun withStoragePermission(action: () -> Unit) {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            pendingStorageAction = action
            _events.tryEmit(ApkUiEvent.RequestStoragePermission)
        }
    }

    private fun runExtraction(apps: List<AppEntry>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            var ok = 0
            var failed = 0
            _uiState.update {
                it.copy(extractionProgress = ExtractionProgress(current = 0, total = apps.size))
            }
            withContext(Dispatchers.IO) {
                apps.forEachIndexed { index, app ->
                    try {
                        ApkStorage.extract(context, app)
                        ok++
                    } catch (_: Exception) {
                        failed++
                    }
                    _uiState.update {
                        it.copy(
                            extractionProgress = ExtractionProgress(
                                current = index + 1,
                                total = apps.size
                            )
                        )
                    }
                }
            }
            _uiState.update {
                it.copy(
                    extractionProgress = null,
                    extractionResult = ExtractionResult(
                        succeeded = ok,
                        failed = failed,
                        folderDisplay = "Download/${ApkStorage.FOLDER}"
                    )
                )
            }
        }
    }

    private fun emitError(e: Exception) {
        val context = getApplication<Application>()
        val detail = e.message ?: e.javaClass.simpleName
        _events.tryEmit(ApkUiEvent.ShowMessage(context.getString(R.string.error_generic, detail)))
    }

    private fun applyFilterAndSelection(state: MainUiState): MainUiState {
        val q = state.searchQuery.trim()
        val filtered = state.allApps.filter { app ->
            (state.showSystemApps || !app.isSystem) &&
                (q.isEmpty() ||
                    app.label.contains(q, ignoreCase = true) ||
                    app.packageName.contains(q, ignoreCase = true))
        }
        val visiblePackages = filtered.map { it.packageName }.toSet()
        val retainedSelection = state.selectedPackages.intersect(visiblePackages)
        return state.copy(
            filteredApps = filtered,
            selectedPackages = retainedSelection,
            isSelectionMode = state.isSelectionMode && retainedSelection.isNotEmpty()
        )
    }
}
