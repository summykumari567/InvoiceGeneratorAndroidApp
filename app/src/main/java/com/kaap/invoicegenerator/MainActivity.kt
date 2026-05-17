package com.kaap.invoicegenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kaap.invoicegenerator.ui.navigation.AppNavigation
import com.kaap.invoicegenerator.ui.theme.InvoiceGeneratorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InvoiceGeneratorTheme {
                AppNavigation()
            }
        }
    }
}
