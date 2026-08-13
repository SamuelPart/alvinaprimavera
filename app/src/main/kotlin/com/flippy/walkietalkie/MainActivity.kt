package com.flippy.walkietalkie

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flippy.walkietalkie.ui.screens.*
import com.flippy.walkietalkie.ui.theme.*
import com.flippy.walkietalkie.ui.viewmodel.FlippyViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Ptt : Screen("ptt", "PTT", Icons.Default.Radio)
    object Channels : Screen("channels", "Canales", Icons.Default.CellTower)
    object Contacts : Screen("contacts", "Amigos", Icons.Default.People)
    object History : Screen("history", "Audios", Icons.Default.FormatListBulleted)
    object QrProfile : Screen("qr_profile", "Mi QR", Icons.Default.QrCode)
    object QrScanner : Screen("qr_scanner", "Escanear", Icons.Default.QrCodeScanner)
}

class MainActivity : ComponentActivity() {

    private val viewModel: FlippyViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()
        handleIncomingDeepLink(intent)

        setContent {
            FlippyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BlackBackground
                ) {
                    FlippyAppNavigation(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIncomingDeepLink(intent)
    }

    private fun handleIncomingDeepLink(intent: android.content.Intent?) {
        val data = intent?.data ?: return
        // Handle flippy://connect?id=FLIP-XXXX
        val contactId = data.getQueryParameter("id") 
            ?: data.lastPathSegment // For https://flippy.app/connect/FLIP-XXXX
        
        if (!contactId.isNullOrEmpty() && contactId.startsWith("FLIP-", ignoreCase = true)) {
            viewModel.addContactById(contactId.uppercase())
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}

@Composable
fun FlippyAppNavigation(viewModel: FlippyViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val micLevel by viewModel.micLevel.collectAsStateWithLifecycle()
    val waveformSamples by viewModel.waveformSamples.collectAsStateWithLifecycle()
    val currentPlayingId by viewModel.currentPlayingId.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Ptt,
        Screen.Channels,
        Screen.Contacts,
        Screen.History
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = PanelBackground,
                    contentColor = TextWhite,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Ptt.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BlackBackground,
                                selectedTextColor = NeonEmerald,
                                indicatorColor = NeonEmerald,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Ptt.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Ptt.route) {
                MainConsoleScreen(
                    uiState = uiState,
                    micLevel = micLevel,
                    waveformSamples = waveformSamples,
                    onStartPtt = { viewModel.startPtt() },
                    onStopPtt = { viewModel.stopPtt() },
                    onSelectFilter = { viewModel.selectFilter(it) },
                    onOpenQr = { navController.navigate(Screen.QrProfile.route) },
                    onOpenScanner = { navController.navigate(Screen.QrScanner.route) },
                    onUpdateServerUrl = { viewModel.updateServerUrl(it) }
                )
            }

            composable(Screen.Channels.route) {
                ChannelsScreen(
                    channels = uiState.channels,
                    activeChannelId = uiState.activeChannel.id,
                    onSelectChannel = {
                        viewModel.selectChannel(it)
                        navController.navigate(Screen.Ptt.route)
                    },
                    onCreateChannel = { name, freq, desc, pin ->
                        viewModel.createCustomChannel(name, freq, desc, pin)
                        navController.navigate(Screen.Ptt.route)
                    }
                )
            }

            composable(Screen.Contacts.route) {
                ContactsScreen(
                    userProfile = uiState.userProfile,
                    contacts = uiState.contacts,
                    activeContactId = uiState.activeContact?.id,
                    onSelectContact = {
                        viewModel.selectDirectContact(it)
                        navController.navigate(Screen.Ptt.route)
                    },
                    onAddContactById = { viewModel.addContactById(it) },
                    onOpenScanner = { navController.navigate(Screen.QrScanner.route) },
                    onOpenQrProfile = { navController.navigate(Screen.QrProfile.route) }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    messages = uiState.messages,
                    currentPlayingId = currentPlayingId,
                    onPlayMessage = { viewModel.playMessage(it) },
                    onStopPlayback = { viewModel.stopPlayback() }
                )
            }

            composable(Screen.QrProfile.route) {
                QrProfileScreen(
                    userProfile = uiState.userProfile,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.QrScanner.route) {
                QrScannerScreen(
                    onContactScanned = {
                        viewModel.addContactById(it)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
