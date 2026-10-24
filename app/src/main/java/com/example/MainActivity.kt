package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.MainViewModel
import com.example.ui.MapScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Mandatory Arabic RTL Layout Direction for the entire application
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        var isAuthenticated by remember {
                            mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
                        }

                        if (!isAuthenticated) {
                            AuthScreen(
                                onAuthSuccess = { email ->
                                    viewModel.userEmail.value = email ?: "abduraheemseer11@gmail.com"
                                    isAuthenticated = true
                                }
                            )
                        } else {
                            MapScreen(
                                viewModel = viewModel,
                                onSignOut = {
                                    FirebaseAuth.getInstance().signOut()
                                    isAuthenticated = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
