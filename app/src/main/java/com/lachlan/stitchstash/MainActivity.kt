package com.lachlan.stitchstash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lachlan.stitchstash.ui.navigation.AppNavigation
import com.lachlan.stitchstash.ui.theme.StitchStashTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StitchStashTheme {
                AppNavigation()
            }
        }
    }
}
