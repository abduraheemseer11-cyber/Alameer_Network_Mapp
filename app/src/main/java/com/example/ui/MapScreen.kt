package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NodeType
import com.example.map.NetworkMapView
import com.example.ui.components.*

@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    val allNodes by viewModel.allNodes.collectAsStateWithLifecycle()
    val allLinks by viewModel.allLinks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val faultCount by viewModel.faultCount.collectAsStateWithLifecycle()

    val centerLat by viewModel.centerLat.collectAsStateWithLifecycle()
    val centerLng by viewModel.centerLng.collectAsStateWithLifecycle()
    val zoomLevel by viewModel.zoomLevel.collectAsStateWithLifecycle()
    val selectedNode by viewModel.selectedNode.collectAsStateWithLifecycle()
    val mapTileType by viewModel.mapTileType.collectAsStateWithLifecycle()

    val isMeasureMode by viewModel.isMeasureMode.collectAsStateWithLifecycle()
    val measurePoints by viewModel.measurePoints.collectAsStateWithLifecycle()

    val showAddEditDialog by viewModel.showAddEditDialog.collectAsStateWithLifecycle()
    val nodeToEdit by viewModel.nodeToEdit.collectAsStateWithLifecycle()
    val newMarkerLat by viewModel.newMarkerLat.collectAsStateWithLifecycle()
    val newMarkerLng by viewModel.newMarkerLng.collectAsStateWithLifecycle()

    val showBackupDialog by viewModel.showBackupDialog.collectAsStateWithLifecycle()
    val showAboutDialog by viewModel.showAboutDialog.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()

    @SuppressLint("MissingPermission")
    fun moveToMyLocation() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val loc = if (isGpsEnabled) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } else if (isNetworkEnabled) {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } else null

        if (loc != null) {
            viewModel.onMapCenterChanged(loc.latitude, loc.longitude, 17.5)
            Toast.makeText(context, "تم التوجه لموقعك الحالي", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "تعذر تحديد الموقع، يرجى تفعيل GPS", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            moveToMyLocation()
        } else {
            Toast.makeText(context, "إذن الموقع مطلوب للتحديد التلقائي", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Interactive Satellite / Technical Map Canvas
        NetworkMapView(
            centerLat = centerLat,
            centerLng = centerLng,
            zoomLevel = zoomLevel,
            nodes = allNodes,
            links = allLinks,
            selectedNodeId = selectedNode?.id,
            mapTileType = mapTileType,
            isMeasureMode = isMeasureMode,
            measurePoints = measurePoints,
            onMapCenterChanged = { lat, lng, zoom ->
                viewModel.onMapCenterChanged(lat, lng, zoom)
            },
            onNodeClicked = { node ->
                viewModel.selectNode(node)
            },
            onMapLongClicked = { lat, lng ->
                viewModel.openAddNodeAt(lat, lng)
            },
            onMeasurePointAdded = { lat, lng ->
                viewModel.addMeasurePoint(lat, lng)
            }
        )

        // 2. Floating Top Search & Control Header
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            SearchHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.searchQuery.value = it },
                searchResults = searchResults,
                isSearchActive = isSearchActive,
                onSearchActiveChange = { viewModel.isSearchActive.value = it },
                onSelectNode = { node -> viewModel.selectNode(node) },
                isMeasureMode = isMeasureMode,
                onToggleMeasureMode = { viewModel.toggleMeasureMode() },
                currentMapType = mapTileType,
                onSelectMapType = { viewModel.mapTileType.value = it },
                faultCount = faultCount,
                onOpenFilterFaults = { viewModel.focusOnFaults() },
                onOpenBackup = { viewModel.showBackupDialog.value = true },
                onOpenAbout = { viewModel.showAboutDialog.value = true }
            )
        }

        // 3. Floating Map Controls (Zoom +, Zoom -, My Location, Add Marker)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (isMeasureMode) 140.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Zoom In (+)
            SmallFloatingActionButton(
                onClick = {
                    viewModel.onMapCenterChanged(centerLat, centerLng, (zoomLevel + 1.0).coerceAtMost(19.5))
                },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "تكبير الخريطة")
            }

            // Zoom Out (-)
            SmallFloatingActionButton(
                onClick = {
                    viewModel.onMapCenterChanged(centerLat, centerLng, (zoomLevel - 1.0).coerceAtLeast(3.0))
                },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "تصغير الخريطة")
            }

            // My GPS Location
            FloatingActionButton(
                onClick = {
                    val fineGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (fineGranted) {
                        moveToMyLocation()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "موقعي الحالي")
            }

            // Add Node FAB (+ إضافة جهاز أو مودم)
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.openAddNodeAt(centerLat, centerLng)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.AddLocationAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة جهاز")
            }
        }

        // 4. Distance Measurement Overlay
        if (isMeasureMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                DistanceMeasureOverlay(
                    measurePoints = measurePoints,
                    onClearPoints = { viewModel.clearMeasurePoints() },
                    onCloseMeasureMode = { viewModel.toggleMeasureMode() }
                )
            }
        }

        // 5. Node Details Bottom Sheet
        selectedNode?.let { node ->
            val parentBox = allNodes.find { it.id == node.parentBoxId }
            NodeDetailSheet(
                node = node,
                parentBoxName = parentBox?.let { "#${it.networkNumber} ${it.name}" },
                onDismiss = { viewModel.clearSelectedNode() },
                onEditNode = { n -> viewModel.openEditNode(n) },
                onDeleteNode = { n -> viewModel.deleteNode(n) },
                onStatusChanged = { n, status -> viewModel.updateNodeStatus(n, status) }
            )
        }

        // 6. Add / Edit Node Dialog
        if (showAddEditDialog) {
            val availableBoxes = allNodes.filter { it.deviceType == NodeType.WATERPROOF_BOX || it.deviceType == NodeType.MIKROTIK_ROUTER }
            AddEditNodeDialog(
                initialNode = nodeToEdit,
                initialLat = newMarkerLat,
                initialLng = newMarkerLng,
                availableBoxes = availableBoxes,
                onDismiss = { viewModel.showAddEditDialog.value = false },
                onSave = { nodeToSave -> viewModel.saveNode(nodeToSave) }
            )
        }

        // 7. Backup & Restore Dialog
        if (showBackupDialog) {
            BackupRestoreDialog(
                repository = viewModel.repository,
                tileManager = viewModel.tileManager,
                userEmail = userEmail,
                onDismiss = { viewModel.showBackupDialog.value = false },
                onDataRestored = {
                    // Force refresh
                    viewModel.onMapCenterChanged(centerLat, centerLng, zoomLevel)
                }
            )
        }

        // 8. About Dialog
        if (showAboutDialog) {
            AboutDialog(onDismiss = { viewModel.showAboutDialog.value = false })
        }
    }
}
