package com.example.apkextractor.ui.screens

import android.os.Build
import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.apkextractor.R
import com.example.apkextractor.data.AppDetails
import com.example.apkextractor.data.AppEntry
import com.example.apkextractor.ui.DetailsUiState
import com.example.apkextractor.ui.ExtractionProgress
import com.example.apkextractor.ui.ExtractionResult
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    state: DetailsUiState,
    snackbarHostState: SnackbarHostState,
    extractionProgress: ExtractionProgress?,
    extractionResult: ExtractionResult?,
    onBack: () -> Unit,
    onExtract: (AppEntry) -> Unit,
    onShare: (AppEntry) -> Unit,
    onDismissExtractionResult: () -> Unit
) {
    val activeEntry = (state as? DetailsUiState.Success)?.details?.entry

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.title_app_info),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    if (activeEntry != null) {
                        IconButton(
                            onClick = { onShare(activeEntry) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("details_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.menu_share)
                            )
                        }
                        IconButton(
                            onClick = { onExtract(activeEntry) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("details_extract_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = stringResource(R.string.menu_extract)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (state) {
                DetailsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                DetailsUiState.NotFound -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.app_not_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is DetailsUiState.Success -> {
                    DetailsBody(
                        details = state.details,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 800.dp)
                    )
                }
            }
        }
    }

    if (extractionProgress != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.extracting)) },
            text = { androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
            confirmButton = {}
        )
    }

    if (extractionResult != null) {
        ExtractionResultDialog(
            result = extractionResult,
            onDismiss = onDismissExtractionResult
        )
    }
}

@Composable
private fun DetailsBody(
    details: AppDetails,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val entry = details.entry
    val installedFormatted = remember(details.firstInstallTime) {
        DateFormat.getMediumDateFormat(context).format(Date(details.firstInstallTime)) + " " +
            DateFormat.getTimeFormat(context).format(Date(details.firstInstallTime))
    }
    val updatedFormatted = remember(details.lastUpdateTime) {
        DateFormat.getMediumDateFormat(context).format(Date(details.lastUpdateTime)) + " " +
            DateFormat.getTimeFormat(context).format(Date(details.lastUpdateTime))
    }
    val sizeFormatted = remember(entry.sizeBytes) {
        Formatter.formatFileSize(context, entry.sizeBytes)
    }
    val minSdkText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && details.minSdkVersion > 0) {
        "API ${details.minSdkVersion}"
    } else {
        "-"
    }
    val noneText = stringResource(R.string.none)

    SelectionContainer {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Hero Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (entry.icon != null) {
                    Image(
                        bitmap = entry.icon,
                        contentDescription = entry.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = entry.label,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (entry.isSystem) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            text = stringResource(
                                if (entry.isSystem) R.string.system_app_badge else R.string.user_app_badge
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (entry.isSystem) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (entry.splitDirs.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.split_apks_count, entry.splitDirs.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            DetailSectionCard(title = stringResource(R.string.section_overview)) {
                DetailRow(
                    label = stringResource(R.string.label_name),
                    value = entry.label,
                    monospace = false
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                DetailRow(
                    label = stringResource(R.string.label_package),
                    value = entry.packageName,
                    monospace = true
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                DetailRow(
                    label = stringResource(R.string.label_version),
                    value = "${entry.versionName.ifEmpty { "-" }} (${entry.versionCode})",
                    monospace = true
                )
            }

            DetailSectionCard(title = stringResource(R.string.section_build)) {
                DetailRow(
                    label = stringResource(R.string.label_min_sdk),
                    value = minSdkText,
                    monospace = true
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                DetailRow(
                    label = stringResource(R.string.label_target_sdk),
                    value = "API ${details.targetSdkVersion}",
                    monospace = true
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                DetailRow(
                    label = stringResource(R.string.label_installed),
                    value = installedFormatted,
                    monospace = false
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                DetailRow(
                    label = stringResource(R.string.label_updated),
                    value = updatedFormatted,
                    monospace = false
                )
            }

            DetailSectionCard(title = stringResource(R.string.section_storage)) {
                DetailRow(
                    label = stringResource(R.string.label_path),
                    value = entry.sourceDir,
                    monospace = true
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                DetailRow(
                    label = stringResource(R.string.label_size),
                    value = sizeFormatted,
                    monospace = true
                )
            }

            DetailSectionCard(title = stringResource(R.string.section_capabilities)) {
                DetailRow(
                    label = stringResource(R.string.label_permissions),
                    value = details.permissions.joinToString("\n").ifEmpty { noneText },
                    monospace = true
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                DetailRow(
                    label = stringResource(R.string.label_features),
                    value = details.features.joinToString("\n").ifEmpty { noneText },
                    monospace = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    monospace: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (monospace) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
