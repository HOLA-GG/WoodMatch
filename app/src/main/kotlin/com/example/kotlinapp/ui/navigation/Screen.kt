package com.example.kotlinapp.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Dashboard : Screen("dashboard")
    object ReportTree : Screen("report_tree")
    object ViewReports : Screen("view_reports")
    object Species : Screen("species")
    object Profile : Screen("profile")
    object AdminUsers : Screen("admin_users")
    object TreeMap : Screen("tree_map")
}
