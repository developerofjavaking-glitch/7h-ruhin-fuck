package com.example.apkextractor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.apkextractor.data.ApkStorage
import com.example.apkextractor.data.AppEntry
import com.example.apkextractor.ui.ApkExtractorViewModel
import com.example.apkextractor.ui.ApkUiEvent
import com.example.apkextractor.ui.DetailsViewModel
import com.example.apkextractor.ui.screens.AppDetailsScreen
import com.example.apkextractor.ui.screens.AppListScreen
import com.example.apkextractor.ui.screens.SplashScreen
import com.example.apkextractor.ui.theme.ApkExtractorTheme

private object Routes {
    const val SPLASH = "splash"
    const val APP_LIST = "app_list"
    const val APP_DETAILS = "app_details/{packageName}"
    fun appDetails(packageName: String): String = "app_details/${Uri.encode(packageName)}"
}

class MainActivity : ComponentActivity() {

    private val mainViewModel: ApkExtractorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ApkExtractorTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

                val storagePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    mainViewModel.onStoragePermissionResult(granted)
                }

                // Listen for package install / uninstall / update events
                DisposableEffect(Unit) {
                    val packageReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            mainViewModel.refreshApps()
                        }
                    }
                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_PACKAGE_ADDED)
                        addAction(Intent.ACTION_PACKAGE_REMOVED)
                        addAction(Intent.ACTION_PACKAGE_REPLACED)
                        addDataScheme("package")
                    }
                    ContextCompat.registerReceiver(
                        this@MainActivity,
                        packageReceiver,
                        filter,
                        ContextCompat.RECEIVER_EXPORTED
                    )
                    onDispose {
                        unregisterReceiver(packageReceiver)
                    }
                }

                LaunchedEffect(mainViewModel) {
                    mainViewModel.events.collect { event ->
                        when (event) {
                            ApkUiEvent.RequestStoragePermission -> {
                                storagePermissionLauncher.launch(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            }

                            is ApkUiEvent.LaunchShareIntent -> {
                                launchShareChooser(event.uris)
                            }

                            is ApkUiEvent.ShowMessage -> {
                                snackbarHostState.showSnackbar(event.message)
                            }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.SPLASH
                ) {
                    composable(Routes.SPLASH) {
                        SplashScreen(
                            onTimeout = {
                                navController.navigate(Routes.APP_LIST) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(Routes.APP_LIST) {
                        AppListScreen(
                            state = uiState,
                            snackbarHostState = snackbarHostState,
                            onSearchActiveChange = mainViewModel::setSearchActive,
                            onSearchQueryChange = mainViewModel::onSearchQueryChange,
                            onToggleShowSystem = mainViewModel::toggleShowSystemApps,
                            onExtractAll = mainViewModel::promptExtractAll,
                            onConfirmExtractAll = mainViewModel::confirmExtractAll,
                            onDismissExtractAll = mainViewModel::dismissExtractAllPrompt,
                            onExportList = mainViewModel::exportAppList,
                            onRefresh = mainViewModel::refreshApps,
                            onRowClick = mainViewModel::onRowClick,
                            onRowLongClick = mainViewModel::onRowLongClick,
                            onSelectAll = mainViewModel::selectAllVisible,
                            onClearSelection = mainViewModel::clearSelection,
                            onExtractApps = mainViewModel::extractApps,
                            onShareApps = mainViewModel::shareApps,
                            onDismissActionSheet = mainViewModel::dismissActionSheet,
                            onDismissExtractionResult = mainViewModel::dismissExtractionResult,
                            onOpenDetails = { packageName ->
                                navController.navigate(Routes.appDetails(packageName))
                            },
                            onUninstallApp = ::uninstallApp,
                            onLaunchApp = ::launchApp,
                            selectedAppsProvider = mainViewModel::selectedApps
                        )
                    }

                    composable(
                        route = Routes.APP_DETAILS,
                        arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedPackage = backStackEntry.arguments?.getString("packageName")
                        val packageName = encodedPackage?.let(Uri::decode).orEmpty()
                        val detailsViewModel: DetailsViewModel = viewModel()
                        val detailsState by detailsViewModel.state.collectAsStateWithLifecycle()

                        LaunchedEffect(packageName) {
                            if (packageName.isNotEmpty()) {
                                detailsViewModel.load(packageName)
                            }
                        }

                        AppDetailsScreen(
                            state = detailsState,
                            snackbarHostState = snackbarHostState,
                            extractionProgress = uiState.extractionProgress,
                            extractionResult = uiState.extractionResult,
                            onBack = { navController.popBackStack() },
                            onExtract = { app -> mainViewModel.extractApps(listOf(app)) },
                            onShare = { app -> mainViewModel.shareApps(listOf(app)) },
                            onDismissExtractionResult = mainViewModel::dismissExtractionResult
                        )
                    }
                }
            }
        }
    }

    private fun launchShareChooser(uris: ArrayList<Uri>) {
        if (uris.isEmpty()) return
        val intent = Intent().apply {
            type = ApkStorage.MIME_APK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (uris.size == 1) {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            clipData = ClipData.newRawUri(null, uris[0]).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_with)))
    }

    private fun uninstallApp(app: AppEntry) {
        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
    }

    private fun launchApp(app: AppEntry) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, R.string.cannot_launch, Toast.LENGTH_SHORT).show()
        }
    }
}
