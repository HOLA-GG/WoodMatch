package com.example.kotlinapp

import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.kotlinapp.data.model.*
import com.example.kotlinapp.data.repository.*
import com.example.kotlinapp.ui.components.WoodMatchBottomNav
import com.example.kotlinapp.ui.pilot.*
import com.example.kotlinapp.ui.screens.*
import com.example.kotlinapp.ui.theme.WoodMatchTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MainActivity : FragmentActivity() {
    private var openNotifications by mutableStateOf(false)

    private fun receiveNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("openNotifications", false) == true) {
            openNotifications = true
            intent.removeExtra("openNotifications")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveNotificationIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pending_notifications_navigation", openNotifications)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openNotifications = savedInstanceState?.getBoolean("pending_notifications_navigation", false) ?: false
        receiveNotificationIntent(intent)
        ReportRepository.init(applicationContext)
        WoodWasteRepository.init(applicationContext)
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            // Diagnostics are off until the user opts in. Pilot sideloads do not depend on Play
            // Integrity.
            com.example.kotlinapp.data.network.PilotTelemetry.enabled =
                PilotPreferences.store(this).getBoolean("diagnostics", false)
            FirebaseCrashlytics.getInstance()
                .setCrashlyticsCollectionEnabled(
                    com.example.kotlinapp.data.network.PilotTelemetry.enabled
                )
        }
        setContent {
            val prefs = remember { PilotPreferences.store(this) }
            var mode by remember { mutableStateOf(prefs.getString("season", "AUTO")!!) }
            var appearance by remember { mutableStateOf(prefs.getString("appearance", "SYSTEM")!!) }
            var season by remember { mutableStateOf(PilotPreferences.resolvedSeason(mode)) }
            var textScale by remember { mutableFloatStateOf(PilotPreferences.normalizedTextScale(prefs.getFloat("text_scale", 1f))) }
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(lifecycle, mode) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME)
                        season = PilotPreferences.resolvedSeason(mode)
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            WoodMatchTheme(season = season, appearance = appearance, textScale = textScale) {
                WoodMatchApp(
                    seasonMode = mode,
                    appearance = appearance,
                    textScale = textScale,
                    openNotifications = openNotifications,
                    onNotificationsConsumed = { openNotifications = false },
                    onSeason = {
                        mode = it
                        season = PilotPreferences.resolvedSeason(it)
                        prefs.edit().putString("season", it).apply()
                    },
                    onAppearance = {
                        appearance = it
                        prefs.edit().putString("appearance", it).apply()
                    },
                    onTextScale = {
                        textScale = PilotPreferences.normalizedTextScale(it)
                        prefs.edit().putFloat("text_scale", textScale).apply()
                    },
                )
            }
        }
    }
}

@Composable
fun WoodMatchApp(
    seasonMode: String,
    appearance: String,
    onSeason: (String) -> Unit,
    onAppearance: (String) -> Unit,
    textScale: Float = 1f,
    onTextScale: (Float) -> Unit = {},
    openNotifications: Boolean = false,
    onNotificationsConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val user by AuthRepository.currentUser.collectAsState()
    LaunchedEffect(user?.uid) { WoodWasteRepository.refreshSession() }
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route.orEmpty()
    val support = user?.isSupport == true
    // Wait for session restoration/login and the normal landing route before opening the inbox.
    // This avoids racing SplashScreen's navigation and consuming a tap before authentication.
    LaunchedEffect(openNotifications, user?.uid, route) {
        val startup = setOf("", "splash", "login", "register", "onboarding", "forgot_password")
        if (openNotifications && AuthRepository.isAuthenticated && route !in startup) {
            onNotificationsConsumed()
            if (route != "notifications") nav.navigate("notifications") { launchSingleTop = true }
        }
    }
    LaunchedEffect(support, route) {
        val allowed = setOf("splash", "login", "support_home", "support_inbox", "support_moderation", "profile", "settings", "edit_profile", "change_password", "forgot_password", "notifications")
        if (support && route.isNotBlank() && route !in allowed) {
            nav.navigate("support_home") { popUpTo(0) { inclusive = true }; launchSingleTop = true }
        }
    }
    val tabs = listOf("dashboard", "tree_map", "report_tree", "view_reports", "my_activity", "profile", "entities", "species", "guest", "guest_map", "support_home", "support_inbox", "support_moderation")
    val items by WoodWasteRepository.items.collectAsState()
    var importJson by rememberSaveable { mutableStateOf<String?>(null) }
    fun go(target: String) {
        nav.navigate(target) { launchSingleTop = true }
    }
    fun navigateTopLevel(target: String) {
        nav.navigate(target) {
            popUpTo(if (support) "support_home" else "dashboard") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun edit(item: WoodWasteItem) {
        go("edit/${item.id}")
    }
    fun logout() {
        nav.navigate("login") { popUpTo(0) { inclusive = true } }
    }
    fun enterGuest() {
        AuthRepository.loginAsGuest()
        nav.navigate("guest") { popUpTo(0) { inclusive = true } }
    }
    Scaffold(
        bottomBar = {
            if (route in tabs)
                WoodMatchBottomNav(route, user?.navigationRole.orEmpty(), ::navigateTopLevel)
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "splash",
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(120)) },
            modifier = Modifier.padding(padding),
        ) {
            composable("splash") {
                SplashScreen(
                    onNavigateToDashboard = {
                        nav.navigate(if (AuthRepository.currentUser.value?.isSupport == true) "support_home" else "dashboard") { popUpTo("splash") { inclusive = true } }
                    },
                    onNavigateToLogin = {
                        nav.navigate("login") { popUpTo("splash") { inclusive = true } }
                    },
                    onNavigateToOnboarding = {
                        nav.navigate("onboarding") { popUpTo("splash") { inclusive = true } }
                    },
                )
            }
            composable("onboarding") {
                OnboardingScreen(
                    onFinish = {
                        PilotPreferences.completeOnboarding(context)
                        nav.navigate("login") { popUpTo("onboarding") { inclusive = true } }
                    },
                    onLogin = {
                        PilotPreferences.completeOnboarding(context)
                        nav.navigate("login") { popUpTo("onboarding") { inclusive = true } }
                    },
                    onExplore = {
                        PilotPreferences.completeOnboarding(context)
                        enterGuest()
                    },
                )
            }
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        nav.navigate(if (AuthRepository.currentUser.value?.isSupport == true) "support_home" else "dashboard") { popUpTo("login") { inclusive = true } }
                    },
                    onNavigateToRegister = { go("register") },
                    onNavigateToForgotPassword = { go("forgot_password") },
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        nav.navigate(if (AuthRepository.currentUser.value?.isSupport == true) "support_home" else "dashboard") { popUpTo("login") { inclusive = true } }
                    },
                    onNavigateBackToLogin = { nav.popBackStack() },
                )
            }
            composable("forgot_password") {
                ForgotPasswordScreen(onNavigateBack = { nav.popBackStack() })
            }
            composable("dashboard") {
                if (support) SupportHomeScreen(onCases = { navigateTopLevel("support_inbox") }, onModeration = { navigateTopLevel("support_moderation") }, onNotifications = { go("notifications") }) else PilotHome(
                    onMap = { navigateTopLevel("tree_map") },
                    onCreate = { go("report_tree") },
                    onSpecies = { navigateTopLevel("species") },
                    onCatalog = { navigateTopLevel("view_reports") },
                    onEntities = { navigateTopLevel("entities") },
                )
            }
            composable("guest") {
                GuestExploreScreen(
                    onRegister = { go("register") },
                    onLogin = { go("login") },
                    onMap = { navigateTopLevel("guest_map") },
                )
            }
            composable("guest_map") {
                GuestMapScreen(onRegister = { go("register") })
            }
            composable("tree_map") {
                CommunityScreen(mapFirst = true, onCreate = { go("report_tree") }, onEdit = ::edit,
                    onEntities = { navigateTopLevel("entities") })
            }
            composable("view_reports") {
                CommunityScreen(onCreate = { go("report_tree") }, onEdit = ::edit,
                    onEntities = { navigateTopLevel("entities") })
            }
            composable("entities") {
                EntityDirectoryScreen(onBack = { nav.popBackStack() })
            }
            composable("report_tree") {
                PublicationWizard(
                    onDone = {
                        nav.navigate("my_activity") {
                            popUpTo("report_tree") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                "edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val item = items.find { it.id == entry.arguments?.getString("id") }
                if (item != null && item.authorId == user?.uid)
                    PublicationWizard(
                        existing = item,
                        onDone = { nav.popBackStack() },
                        onBack = { nav.popBackStack() },
                    )
                else
                    AccessMessage("La publicación no está disponible para editar.") {
                        nav.popBackStack()
                    }
            }
            composable(
                "import/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val report =
                    importJson
                        ?.let { LegacyReports.decode(it) }
                        ?.takeIf { it.id == entry.arguments?.getString("id") }
                if (report != null)
                    PublicationWizard(
                        imported = report,
                        onDone = { nav.popBackStack() },
                        onBack = { nav.popBackStack() },
                    )
                else
                    AccessMessage("Vuelve a seleccionar el reporte que quieres importar.") {
                        nav.popBackStack()
                    }
            }
            composable("my_activity") {
                MyActivityScreen(
                    onExplore = { go("view_reports") },
                    onEdit = ::edit,
                    onImport = {
                        importJson = LegacyReports.encode(it)
                        go("import/${it.id}")
                    },
                )
            }
            composable("profile") {
                PilotProfile(
                    seasonMode = seasonMode,
                    appearance = appearance,
                    textScale = textScale,
                    onSeason = onSeason,
                    onAppearance = onAppearance,
                    onTextScale = onTextScale,
                    onEditProfile = { go("edit_profile") },
                    onChangePassword = { go("change_password") },
                    onSupport = { go(if (support) "support_inbox" else "support") },
                    onNotifications = { go("notifications") },
                    onLogout = ::logout,
                )
            }
            composable("settings") {
                PilotProfile(
                    seasonMode = seasonMode,
                    appearance = appearance,
                    textScale = textScale,
                    onSeason = onSeason,
                    onAppearance = onAppearance,
                    onTextScale = onTextScale,
                    onEditProfile = { go("edit_profile") },
                    onChangePassword = { go("change_password") },
                    onSupport = { go(if (support) "support_inbox" else "support") },
                    onNotifications = { go("notifications") },
                    onLogout = ::logout,
                )
            }
            composable("edit_profile") {
                EditProfileScreen(onBack = { nav.popBackStack() })
            }
            composable("change_password") {
                ChangePasswordScreen(
                    onNavigateBack = { nav.popBackStack() },
                    onRecoverPassword = { go("forgot_password") },
                )
            }
            composable("notifications") {
                if (AuthRepository.isAuthenticated) NotificationsScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { requestId, caseId ->
                        if (caseId.isNotBlank()) go(if (support) "support_inbox" else "support")
                        else if (!support && requestId.isNotBlank()) go("my_activity")
                        else go(if (support) "support_home" else "dashboard")
                    },
                ) else AccessMessage("Inicia sesión para consultar tus avisos.") { nav.popBackStack() }
            }
            composable("support") { SupportScreen(onBack = { nav.popBackStack() }) }
            composable("support_home") {
                if (support) SupportHomeScreen(onCases = { navigateTopLevel("support_inbox") }, onModeration = { navigateTopLevel("support_moderation") }, onNotifications = { go("notifications") })
                else AccessMessage("Esta sección es exclusiva de soporte.") { nav.popBackStack() }
            }
            composable("support_inbox") {
                if (support) SupportScreen(onBack = { navigateTopLevel("support_home") }, operatorMode = true)
                else AccessMessage("Esta sección es exclusiva de soporte.") { nav.popBackStack() }
            }
            composable("support_moderation") {
                if (support) SupportScreen(onBack = { navigateTopLevel("support_home") }, operatorMode = true, moderationOnly = true)
                else AccessMessage("Esta sección es exclusiva de soporte.") { nav.popBackStack() }
            }
            composable("species") {
                SpeciesScreen(onNavigateBack = { nav.popBackStack() })
            }
            composable("admin_users") { AdminUsersScreen(onNavigateBack = { nav.popBackStack() }) }
        }
    }
}

