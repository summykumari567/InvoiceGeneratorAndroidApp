package com.kaap.invoicegenerator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kaap.invoicegenerator.ui.InvoiceViewModel
import com.kaap.invoicegenerator.ui.screens.InvoiceFormScreen
import com.kaap.invoicegenerator.ui.screens.PreviewScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: InvoiceViewModel = viewModel()

    NavHost(navController = navController, startDestination = "form") {
        composable("form") {
            InvoiceFormScreen(
                viewModel = viewModel,
                onPreview = { navController.navigate("preview") }
            )
        }
        composable("preview") {
            PreviewScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
