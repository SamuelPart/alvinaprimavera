package com.flippy.walkietalkie

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
private const val RoutePhoneVerify = "phone_verify"
private const val RouteFriendRequests = "friend_requests"

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La cámara YA NO se pide aquí: solo cuando se va a usar
        // (escáner QR o foto de perfil) — mejor para Play Store ✅
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
            viewModel.addContactById(contactId.uppercase()) { msg ->
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun FlippyAppNavigation(viewModel: FlippyViewModel, authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionStatus by authViewModel.sessionStatus.collectAsStateWithLifecycle()
    val currentPlayingId by viewModel.currentPlayingId.collectAsStateWithLifecycle()
    val sessionKicked by viewModel.sessionKicked.collectAsStateWithLifecycle()
    val friendRequests by viewModel.friendRequests.collectAsStateWithLifecycle()
    val appContext = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // «Amigos» ya no vive en el menú inferior: la pantalla de añadir
    // amigos se abre desde los botones 👋 / 👤+ de la consola principal.
    val bottomNavItems = listOf(
        Screen.Ptt,
        Screen.Channels,
        Screen.History
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    // Tras autenticarse: restaura/crea tu perfil en la nube (aquí ya hay
    // sesión con correo) y luego onboarding (1ra vez) o directo a la app
    fun navigateAfterAuth() {
        viewModel.registerOrRestoreProfile()
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                    onStartPtt = { viewModel.startPtt() },
                    onStopPtt = { viewModel.stopPtt() },
                    onOpenScanner = { navController.navigate(Screen.QrScanner.route) },
                    onAddFriends = {
                        navController.navigate(Screen.Contacts.route) {
                            popUpTo(Screen.Ptt.route) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onCreateGroup = {
                        navController.navigate(Screen.Channels.route) {
                            popUpTo(Screen.Ptt.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
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
                    onSelectContact = { viewModel.selectDirectContact(it) },
                    onPlayMessage = { viewModel.playMessage(it) },
                    onStopPlayback = { viewModel.stopPlayback() },
                    onOpenHistory = { navController.navigate(Screen.History.route) },
                    currentPlayingId = currentPlayingId
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
                    uiState = uiState,
                    deviceContacts = viewModel.deviceContacts
                        .collectAsStateWithLifecycle().value,
                    hasContactsPermission = viewModel.hasContactsPermission(),
                    onRefreshDeviceContacts = { viewModel.refreshDeviceContacts() },
                    onSelectContact = {
                        viewModel.selectDirectContact(it)
                        navController.popBackStack()
                    },
                    onAddContactById = { id ->
                        viewModel.addContactById(id) { msg ->
                            android.widget.Toast.makeText(
                                appContext, msg, android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    requestCount = friendRequests.size,
                    onOpenRequests = { navController.navigate(RouteFriendRequests) },
                    onOpenScanner = { navController.navigate(Screen.QrScanner.route) },
                    onOpenPhoneVerify = { navController.navigate(RoutePhoneVerify) },
                    onCreateGroup = {
                        navController.navigate(Screen.Channels.route) {
                            popUpTo(Screen.Ptt.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onChatWithAi = {
                        viewModel.openAiBot()
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Solicitudes de amistad: aceptar ✅ o rechazar ❌ ──
            composable(RouteFriendRequests) {
                LaunchedEffect(Unit) { viewModel.refreshFriendRequests() }
                FriendRequestsScreen(
                    requests = friendRequests,
                    onAccept = { viewModel.acceptFriendRequest(it) },
                    onReject = { viewModel.rejectFriendRequest(it) },
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Verificar número (país con bandera + código automático) ──
            composable(RoutePhoneVerify) {
                PhoneVerifyScreen(
                    onSavePhone = { viewModel.saveAccountPhone(it) },
                    onBack = { navController.popBackStack() }
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
                    onContactScanned = { scannedId ->
                        viewModel.addContactById(scannedId) { msg ->
                            android.widget.Toast.makeText(
                                appContext, msg, android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
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

        // 🔒 Un solo dispositivo por cuenta: si tu correo abre sesión en
        // otro teléfono, este recibe el aviso y se cierra obligatoriamente
        if (sessionKicked) {
            AlertDialog(
                onDismissRequest = { /* obligatorio: solo con Aceptar */ },
                title = {
                    Text(
                        text = "Sesión iniciada en otro dispositivo 📲",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Se inició sesión con tu cuenta en otro dispositivo. " +
                            "Esta sesión se cerrará."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.ackSessionKick()
                            authViewModel.signOut()
                            navController.navigate(RouteWelcome) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Aceptar", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        }
    }
}
