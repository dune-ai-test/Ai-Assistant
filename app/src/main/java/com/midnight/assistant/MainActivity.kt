package com.midnight.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.midnight.assistant.navigation.AppNavHost
import com.midnight.assistant.ui.theme.MidnightAssistantTheme
import com.midnight.assistant.ui.theme.MidnightColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MidnightApp()
        }
    }
}

@Composable
private fun MidnightApp() {
    MidnightAssistantTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MidnightColors.background
        ) {
            AppNavHost()
        }
    }
}
