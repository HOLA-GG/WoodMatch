package com.example.kotlinapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(lifecycle, mode) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME)
                        season = PilotPreferences.resolvedSeason(mode)
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            WoodMatchTheme(season = season, appearance = appearance) {
                WoodMatchApp(
                    mode,
                    appearance,
                    onSeason = {
                        mode = it
                        season = PilotPreferences.resolvedSeason(it)
                        prefs.edit().putString("season", it).apply()
                    },
                    onAppearance = {
                        appearance = it
                        prefs.edit().putString("appearance", it).apply()
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
) {
    val user by AuthRepository.currentUser.collectAsState()
    LaunchedEffect(user?.uid) { WoodWasteRepository.refreshSession() }
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route.orEmpty()
    val tabs = listOf("dashboard", "tree_map", "report_tree", "my_activity", "profile")
    val items by WoodWasteRepository.items.collectAsState()
    var importJson by rememberSaveable { mutableStateOf<String?>(null) }
    fun go(target: String) {
        nav.navigate(target) { launchSingleTop = true }
    }
    fun edit(item: WoodWasteItem) {
        go("edit/${item.id}")
    }
    fun logout() {
        nav.navigate("login") { popUpTo(0) { inclusive = true } }
    }
    Scaffold(
        bottomBar = {
            if (route in tabs)
                WoodMatchBottomNav(route) { target ->
                    nav.navigate(target) {
                        popUpTo("dashboard") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "splash",
            modifier = Modifier.padding(padding),
        ) {
            composable("splash") {
                SplashScreen(
                    onNavigateToDashboard = {
                        nav.navigate("dashboard") { popUpTo("splash") { inclusive = true } }
                    },
                    onNavigateToLogin = {
                        nav.navigate("login") { popUpTo("splash") { inclusive = true } }
                    },
                )
            }
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        nav.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                    },
                    onNavigateToRegister = { go("register") },
                    onNavigateToForgotPassword = { go("forgot_password") },
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        nav.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                    },
                    onNavigateBackToLogin = { nav.popBackStack() },
                )
            }
            composable("forgot_password") {
                ForgotPasswordScreen(onNavigateBack = { nav.popBackStack() })
            }
            composable("dashboard") {
                PilotHome(
                    onMap = { go("tree_map") },
                    onCreate = { go("report_tree") },
                    onSpecies = { go("species") },
                    onCatalog = { go("view_reports") },
                )
            }
            composable("tree_map") {
                CommunityScreen(mapFirst = true, onCreate = { go("report_tree") }, onEdit = ::edit)
            }
            composable("view_reports") {
                CommunityScreen(onCreate = { go("report_tree") }, onEdit = ::edit)
            }
            composable("report_tree") {
                PublicationWizard(onDone = { go("my_activity") }, onBack = { nav.popBackStack() })
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
                PilotProfile(seasonMode, appearance, onSeason, onAppearance, ::logout)
            }
            composable("settings") {
                PilotProfile(seasonMode, appearance, onSeason, onAppearance, ::logout)
            }
            composable("species") {
                SpeciesScreen(
                    onNavigateBack = { nav.popBackStack() },
                    onNavigateToCreateReport = { go("report_tree") },
                )
            }
            composable("admin_users") { AdminUsersScreen(onNavigateBack = { nav.popBackStack() }) }
        }
    }
}
