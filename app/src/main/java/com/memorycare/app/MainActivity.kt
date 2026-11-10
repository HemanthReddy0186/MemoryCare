package com.memorycare.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.memorycare.app.navigation.MemoryCareNavGraph
import com.memorycare.app.ui.theme.ElderlyBgLight
import com.memorycare.app.ui.theme.MemoryCareTheme
import com.memorycare.app.viewmodel.MemoryCareViewModel

class MainActivity : ComponentActivity() {

    // Single SharedViewModel instance across the entire application lifecycle
    private val sharedViewModel: MemoryCareViewModel by viewModels()

    private val appPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            sharedViewModel.startAutonomousGpsTracking()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check or request essential background permissions (Location + Notifications for lockscreen alarms)
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            appPermissionLauncher.launch(ungranted.toTypedArray())
        } else {
            sharedViewModel.startAutonomousGpsTracking()
        }

        setContent {
            MemoryCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ElderlyBgLight
                ) {
                    MemoryCareNavGraph(viewModel = sharedViewModel)
                }
            }
        }
    }
}
