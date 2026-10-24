package com.example.kotlinapp.ui.screens

import androidx.compose.runtime.Composable
import com.example.kotlinapp.ui.pilot.PublicationWizard

@Composable
fun ReportTreeScreen(onNavigateBack: () -> Unit, onNavigateToReports: () -> Unit, onNavigateToMap: () -> Unit = {}) {
    PublicationWizard(onDone = onNavigateToReports, onBack = onNavigateBack)
}
