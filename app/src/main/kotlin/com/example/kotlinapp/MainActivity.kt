package com.example.kotlinapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kotlinapp.data.model.SeasonalTheme
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.ReportRepository
import com.example.kotlinapp.ui.components.WoodMatchBottomNav
import com.example.kotlinapp.ui.navigation.Screen
import com.example.kotlinapp.ui.screens.AdminUsersScreen
import com.example.kotlinapp.ui.screens.DashboardScreen
import com.example.kotlinapp.ui.screens.ForgotPasswordScreen
import com.example.kotlinapp.ui.screens.LoginScreen
import com.example.kotlinapp.ui.screens.ProfileScreen
import com.example.kotlinapp.ui.screens.RegisterScreen
import com.example.kotlinapp.ui.screens.ReportTreeScreen
import com.example.kotlinapp.ui.screens.SettingsScreen
import com.example.kotlinapp.ui.screens.SpeciesScreen
import com.example.kotlinapp.ui.screens.SplashScreen
import com.example.kotlinapp.ui.screens.TreeMapScreen
import com.example.kotlinapp.ui.screens.ViewReportsScreen
import com.example.kotlinapp.ui.theme.WoodMatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ReportRepository.init(applicationContext)

        setContent {
            var currentSeason by remember { mutableStateOf(SeasonalTheme.SUMMER) }

            WoodMatchTheme(season = currentSeason) {
                WoodMatchApp(
                    currentSeason = currentSeason,
                    onSeasonChange = { newSeason -> currentSeason = newSeason }
                )
            }
        }
    }
}

@Composable
fun WoodMatchApp(
    currentSeason: SeasonalTheme,
    onSeasonChange: (SeasonalTheme) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Splash.route

    val bottomNavRoutes = listOf(
        Screen.Dashboard.route,
        Screen.Species.route,
        Screen.TreeMap.route,
        Screen.ViewReports.route,
        Screen.Profile.route
    )

    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                WoodMatchBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { targetRoute ->
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Auth Screens
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(Screen.ForgotPassword.route)
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateBackToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Main Tabs & Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    currentSeason = currentSeason,
                    onNavigateToReportTree = { navController.navigate(Screen.ReportTree.route) },
                    onNavigateToViewReports = { navController.navigate(Screen.ViewReports.route) },
                    onNavigateToSpecies = { navController.navigate(Screen.Species.route) },
                    onNavigateToMap = { navController.navigate(Screen.TreeMap.route) },
                    onNavigateToAdminUsers = { navController.navigate(Screen.AdminUsers.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Species.route) {
                SpeciesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.TreeMap.route) {
                TreeMapScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ViewReports.route) {
                ViewReportsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreateReport = { navController.navigate(Screen.ReportTree.route) }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onLogout = {
                        AuthRepository.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    currentSeason = currentSeason,
                    onSeasonChange = onSeasonChange,
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        AuthRepository.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Report Tree Screen
            composable(Screen.ReportTree.route) {
                ReportTreeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToReports = {
                        navController.navigate(Screen.ViewReports.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }

            // Admin Users Management
            composable(Screen.AdminUsers.route) {
                AdminUsersScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
