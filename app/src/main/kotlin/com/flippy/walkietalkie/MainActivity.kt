package com.flippy.walkietalkie

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.flippy.walkietalkie.ui.viewmodel.AuthViewModel
import com.flippy.walkietalkie.ui.viewmodel.FlippyViewModel

// Rutas de autenticación (sin barra inferior)
private const val RouteSplash = "splash"
private const val RouteWelcome = "welcome"
private const val RouteEmailAuth = "email_auth"
private const val RouteOnboarding = "onboarding"
private const val RoutePlaybackMode = "playback_mode"
private const val RouteProfileSetup = "profile_setup"

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Ptt : Screen("ptt", "Radio", Icons.Default.Radio)
    object Channels : Screen("channels", "Canales", Icons.Default.CellTower)
    object Contacts : Screen("contacts", "Amigos", Icons.Default.People)
    object History : Screen("history", "Audios", Icons.Default.GraphicEq)
    object QrProfile : Screen("qr_profile", "Mi QR", Icons.Default.QrCode)
    object QrScanner : Screen("qr_scanner", "Escanear", Icons.Default.QrCodeScanner)
}

class MainActivity : ComponentActivity() {

    private val viewModel: FlippyViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

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
                    color = Background
                ) {
                    FlippyAppNavigation(viewModel, authViewModel)
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
        // Micrófono y notificaciones ahora se piden en el onboarding (1 de 3 y 2 de 3)
        // con su explicación. Aquí solo queda la cámara, que el escáner QR usa directo.
        val needed = listOf(Manifest.permission.CAMERA).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}

@Composable
fun FlippyAppNavigation(viewModel: FlippyViewModel, authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionStatus by authViewModel.sessionStatus.collectAsStateWithLifecycle()
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

    // Tras autenticarse: onboarding (solo la primera vez) o directo a la app
    fun navigateAfterAuth() {
        val prefs = navController.context.getSharedPreferences("flippy_prefs", Context.MODE_PRIVATE)
        val destination =
            if (prefs.getBoolean("onboarding_done", false)) Screen.Ptt.route else RouteOnboarding
        navController.navigate(destination) {
            popUpTo(0) { inclusive = true }
        }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Surface,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp
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
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                indicatorColor = AccentContainer,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = RouteSplash,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Splash 2s: decide si va a login o directo a la app ──
            composable(RouteSplash) {
                SplashScreen(
                    sessionStatus = sessionStatus,
                    onFinished = { isAuthenticated ->
                        if (isAuthenticated) {
                            navigateAfterAuth()
                        } else {
                            navController.navigate(RouteWelcome) {
                                popUpTo(RouteSplash) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(RouteWelcome) {
                WelcomeScreen(
                    authViewModel = authViewModel,
                    onOpenEmail = { navController.navigate(RouteEmailAuth) },
                    onAuthenticated = { navigateAfterAuth() }
                )
            }

            composable(RouteEmailAuth) {
                EmailAuthScreen(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onAuthenticated = { navigateAfterAuth() }
                )
            }

            // ── Onboarding de 3 pasos (permisos con explicación, una sola vez) ──
            composable(RouteOnboarding) {
                OnboardingScreen(
                    onFinish = {
                        navController.context
                            .getSharedPreferences("flippy_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("onboarding_done", true)
                            .apply()
                        navController.navigate(Screen.Ptt.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Ptt.route) {
                MainConsoleScreen(
                    uiState = uiState,
                    micLevel = micLevel,
                    waveformSamples = waveformSamples,
                    onStartPtt = { viewModel.startPtt() },
                    onStopPtt = { viewModel.stopPtt() },
                    onSelectFilter = { viewModel.selectFilter(it) },
                    onOpenScanner = { navController.navigate(Screen.QrScanner.route) },
                    onAddFriends = {
                        navController.navigate(Screen.Contacts.route) {
                            popUpTo(Screen.Ptt.route) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onToggleAutoPlay = { viewModel.toggleAutoPlayIncoming() },
                    onOpenPlaybackModes = { navController.navigate(RoutePlaybackMode) },
                    onAvatarClick = {
                        // Si aún no configuró su perfil: flujo 1 de 2 / 2 de 2
                        if (!uiState.profileCreated) {
                            navController.navigate(RouteProfileSetup)
                        } else {
                            navController.navigate(Screen.QrProfile.route)
                        }
                    },
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(RouteWelcome) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
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
                    onOpenQrProfile = { navController.navigate(Screen.QrProfile.route) },
                    myPhotoPath = uiState.profilePhotoPath
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

            // ── Seleccionar modo de reproducción (automática / silencio) ──
            composable(RoutePlaybackMode) {
                PlaybackModeScreen(
                    uiState = uiState,
                    onSelectAutoPlay = { viewModel.setAutoPlayIncoming(it) },
                    onSetDndSync = { viewModel.setDndSync(it) },
                    onSetQuietHours = { viewModel.setQuietHours(it) },
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Configuración de perfil: nombre + foto (1 de 2 / 2 de 2) ──
            composable(RouteProfileSetup) {
                ProfileSetupScreen(
                    onFinished = { first, last, photoPath ->
                        viewModel.saveUserProfile(first, last, photoPath)
                        navController.navigate(Screen.Ptt.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}
