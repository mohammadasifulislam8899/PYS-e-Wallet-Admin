package com.droidnest.tech.pysadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.droidnest.tech.pysadmin.presentation.navigation.AdminApp
import com.droidnest.tech.pysadmin.presentation.ui.theme.PYSADMINTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PYSADMINTheme {
                Surface(
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AdminApp()
                }
            }
        }
    }
}