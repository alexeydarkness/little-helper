package ru.littlehelper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.littlehelper.app.navigation.AppNavHost
import ru.littlehelper.app.ui.theme.LittleHelperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LittleHelperTheme {
                AppNavHost()
            }
        }
    }
}
