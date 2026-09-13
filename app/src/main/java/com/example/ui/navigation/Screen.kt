package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Challenge : Screen("challenge")
    object Success : Screen("success")
    object AppSelection : Screen("app_selection")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
}
