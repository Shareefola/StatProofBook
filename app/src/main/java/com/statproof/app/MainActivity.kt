package com.statproof.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.statproof.app.navigation.StatProofNavGraph
import com.statproof.app.ui.theme.StatProofTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity host for the StatProof application.
 *
 * All navigation occurs within Compose's NavGraph — there is only
 * one Activity in this application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StatProofTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    StatProofNavGraph()
                }
            }
        }
    }
}
