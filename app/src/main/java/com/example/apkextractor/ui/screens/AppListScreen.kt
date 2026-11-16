package com.example.apkextractor.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.text.format.Formatter
import com.example.apkextractor.R
import com.example.apkextractor.data.AppEntry
import com.example.apkextractor.ui.ExtractionProgress
import com.example.apkextractor.ui.ExtractionResult
import com.example.apkextractor.ui.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    state: MainUiState,
    snackbarHostState: SnackbarHostState,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleShowSystem: () -> Unit,
    onExtractAll: () -> Unit,
    onConfirmExtractAll: () -> Unit,
    onDismissExtractAll: () -> Unit,
    onExportList: () -> Unit,
    onRefresh: () -> Unit,
    onRowClick: (AppEntry) -> Unit,
    onRowLongClick: (AppEntry) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onExtractApps: (List<AppEntry>) -> Unit,
    onShareApps: (List<AppEntry>) -> Unit,
    onDismissActionSheet: () -> Unit,
    onDismissExtractionResult: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onUninstallApp: (AppEntry) -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    selectedAppsProvider: () -> List<AppEntry>
) {
    BackHandler(enabled = state.isSelectionMode) {
        onClearSelection()
    }
    BackHandler(enabled = !state.isSelectionMode && state.isSearchActive) {
        onSearchActiveChange(false)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AnimatedContent(
                targetState = state.isSelectionMode,
                label = "topbar_mode"
            ) { selectionMode ->
                if (selectionMode) {
                    SelectionTopBar(
                        selectedCount = state.selectedPackages.size,
                        onClose = onClearSelection,
                        onShare = {
                            onShareApps(selectedAppsProvider())
                            onClearSelection()
                        },
                        onExtract = {
                            onExtractApps(selectedAppsProvider())
                            onClearSelection()
                        },
                        onSelectAll = onSelectAll
                    )
                } else if (state.isSearchActive) {
                    SearchTopBar(
                        query = state.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onClose = { onSearchActiveChange(false) }
                    )
                } else {
                    MainTopBar(
                        appCount = state.filteredApps.size,
                        showSystemApps = state.showSystemApps,
                        onOpenSearch = { onSearchActiveChange(true) },
                        onToggleShowSystem = onToggleShowSystem,
                        onExtractAll = onExtractAll,
                        onExportList = onExportList,
                        onRefresh = onRefresh
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
            ) {
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("loading_indicator")
                            )
                        }
                    }

                    state.filteredApps.isEmpty() -> {
                        EmptyAppsView(modifier = Modifier.fillMaxSize())
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("apps_lazy_column"),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(
                                items = state.filteredApps,
                                key = { it.packageName }
                            ) { app ->
                                val isSelected = app.packageName in state.selectedPackages
                                AppListRow(
                                    app = app,
                                    isSelectionMode = state.isSelectionMode,
                                    isSelected = isSelected,
                                    onClick = { onRowClick(app) },
                                    onLongClick = { onRowLongClick(app) },
                                    onOpenDetails = { onOpenDetails(app.packageName) },
                                    onUninstall = { onUninstallApp(app) },
                                    onLaunch = { onLaunchApp(app) }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 74.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    state.actionSheetApp?.let { app ->
        AppActionsDialog(
            app = app,
            onDismiss = onDismissActionSheet,
            onExtract = {
                onDismissActionSheet()
                onExtractApps(listOf(app))
            },
            onShare = {
                onDismissActionSheet()
                onShareApps(listOf(app))
            },
            onAppInfo = {
                onDismissActionSheet()
                onOpenDetails(app.packageName)
            }
        )
    }

    state.confirmExtractAllCount?.let { count ->
        AlertDialog(
            onDismissRequest = onDismissExtractAll,
            title = { Text(stringResource(R.string.extract_all_confirm_title)) },
            text = { Text(stringResource(R.string.extract_all_confirm, count)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmExtractAll,
                    modifier = Modifier.testTag("confirm_extract_all_button")
                ) {
                    Text(stringResource(R.string.menu_extract))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExtractAll) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    state.extractionProgress?.let { progress ->
        ExtractionProgressDialog(progress = progress)
    }

    state.extractionResult?.let { result ->
        ExtractionResultDialog(
            result = result,
            onDismiss = onDismissExtractionResult
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    appCount: Int,
    showSystemApps: Boolean,
    onOpenSearch: () -> Unit,
    onToggleShowSystem: () -> Unit,
    onExtractAll: () -> Unit,
    onExportList: () -> Unit,
    onRefresh: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        title = {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.apps_count, appCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        },
        actions = {
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.menu_search)
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("main_overflow_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.menu_more_options)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_show_system)) },
                        trailingIcon = {
                            Checkbox(
                                checked = showSystemApps,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleShowSystem()
                        },
                        modifier = Modifier.testTag("menu_show_system")
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_extract_all)) },
                        onClick = {
                            menuExpanded = false
                            onExtractAll()
                        },
                        modifier = Modifier.testTag("menu_extract_all")
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_export_list)) },
                        onClick = {
                            menuExpanded = false
                            onExportList()
                        },
                        modifier = Modifier.testTag("menu_export_list")
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_refresh)) },
                        onClick = {
                            menuExpanded = false
                            onRefresh()
                        },
                        modifier = Modifier.testTag("menu_refresh")
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        navigationIcon = {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("search_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
        },
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("search_input")
                )
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("search_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_clear_search)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onExtract: () -> Unit,
    onSelectAll: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        navigationIcon = {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("exit_selection_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_close_selection)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.selected_count, selectedCount),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
        },
        actions = {
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("action_share_selected")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.menu_share)
                )
            }
            IconButton(
                onClick = onExtract,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("action_extract_selected")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = stringResource(R.string.menu_extract)
                )
            }
            IconButton(
                onClick = onSelectAll,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("action_select_all")
            ) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = stringResource(R.string.menu_select_all)
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListRow(
    app: AppEntry,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOpenDetails: () -> Unit,
    onUninstall: () -> Unit,
    onLaunch: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var rowMenuExpanded by remember { mutableStateOf(false) }

    val formattedSize = remember(app.sizeBytes) {
        Formatter.formatShortFileSize(context, app.sizeBytes)
    }
    val subtitleText = remember(app.packageName, app.versionName, formattedSize) {
        buildString {
            append(app.packageName)
            if (app.versionName.isNotEmpty()) {
                append("  •  v").append(app.versionName)
            }
            append("  •  ").append(formattedSize)
        }
    }

    val rowBackground = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)
            .testTag("app_row_${app.packageName}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = app.label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (app.isSystem) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(R.string.system_app_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("select_checkbox_${app.packageName}")
            )
        } else {
            Box {
                IconButton(
                    onClick = { rowMenuExpanded = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("more_button_${app.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_app_actions),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = rowMenuExpanded,
                    onDismissRequest = { rowMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_app_info)) },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        },
                        onClick = {
                            rowMenuExpanded = false
                            onOpenDetails()
                        },
                        modifier = Modifier.testTag("ctx_info")
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_uninstall)) },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        },
                        onClick = {
                            rowMenuExpanded = false
                            onUninstall()
                        },
                        modifier = Modifier.testTag("ctx_uninstall")
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_run)) },
                        leadingIcon = {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                        },
                        onClick = {
                            rowMenuExpanded = false
                            onLaunch()
                        },
                        modifier = Modifier.testTag("ctx_run")
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAppsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_apps),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.no_apps_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppActionsDialog(
    app: AppEntry,
    onDismiss: () -> Unit,
    onExtract: () -> Unit,
    onShare: () -> Unit,
    onAppInfo: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Column {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DialogActionRow(
                    icon = Icons.Default.Download,
                    label = stringResource(R.string.action_extract_apk),
                    testTag = "dialog_extract_apk",
                    onClick = onExtract
                )
                DialogActionRow(
                    icon = Icons.Default.Share,
                    label = stringResource(R.string.action_share_apk),
                    testTag = "dialog_share_apk",
                    onClick = onShare
                )
                DialogActionRow(
                    icon = Icons.Default.Info,
                    label = stringResource(R.string.action_app_info),
                    testTag = "dialog_app_info",
                    onClick = onAppInfo
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DialogActionRow(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ExtractionProgressDialog(progress: ExtractionProgress) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.extracting)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (progress.total > 1) {
                    LinearProgressIndicator(
                        progress = { progress.current.toFloat() / progress.total.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(
                            R.string.extracting_progress,
                            progress.current,
                            progress.total
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun ExtractionResultDialog(
    result: ExtractionResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.extract_done)) },
        text = {
            Text(
                text = stringResource(
                    R.string.extract_result,
                    result.succeeded,
                    result.failed,
                    result.folderDisplay
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("extract_result_ok_button")
            ) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}
