package com.example.cardcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cardcounter.ui.home.HomeScreen
import com.example.cardcounter.ui.roi.RoiSelectionScreen
import com.example.cardcounter.ui.session.SessionScreen
import com.example.cardcounter.ui.theme.CardCounterTheme
import com.example.cardcounter.ui.tuning.TuningScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CardCounterTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onStart = { nav.navigate("session") },
                            onOpenTuning = { nav.navigate("tuning") }
                        )
                    }
                    composable("session") {
                        SessionScreen(
                            onBack = { nav.popBackStack() },
                            onOpenRoi = { nav.navigate("roi") },
                            onOpenTuning = { nav.navigate("tuning") }
                        )
                    }
                    composable("roi") { RoiSelectionScreen(onBack = { nav.popBackStack() }) }
                    composable("tuning") { TuningScreen(onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}
