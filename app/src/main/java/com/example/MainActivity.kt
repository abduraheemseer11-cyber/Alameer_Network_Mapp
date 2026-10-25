package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
                // 1. إعداد الأذونات المطلوبة
                val permissionsToRequest = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )

                // 2. مشغل طلب الأذونات
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissionsResult ->
                    // يمكنك هنا إضافة منطق إذا تم رفض إذن معين
                    // مثلاً: إذا تم رفض إذن الموقع، يمكنك إظهار رسالة للمستخدم
                }

                // 3. طلب الأذونات تلقائياً عند فتح التطبيق
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(permissionsToRequest)
                }

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
