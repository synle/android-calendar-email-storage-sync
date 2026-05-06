package com.unifiedhub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.unifiedhub.app.ui.navigation.UnifiedHubNavGraph
import com.unifiedhub.app.ui.theme.UnifiedHubTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnifiedHubTheme {
                UnifiedHubNavGraph()
            }
        }
    }
}
