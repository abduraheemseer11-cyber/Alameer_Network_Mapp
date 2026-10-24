package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CableLinkEntity
import com.example.data.model.NetworkNodeEntity
import com.example.data.model.NodeStatus
import com.example.data.model.NodeType
import com.example.data.repository.NetworkRepository
import com.example.map.MapTileType
import com.example.map.TileManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = NetworkRepository(application)
    val tileManager = TileManager(application, viewModelScope)

    val allNodes: StateFlow<List<NetworkNodeEntity>> = repository.allNodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLinks: StateFlow<List<CableLinkEntity>> = repository.allLinks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val isSearchActive = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<NetworkNodeEntity>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.searchNodes(query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val faultCount: StateFlow<Int> = allNodes.map { nodes ->
        nodes.count { it.status == NodeStatus.FAULT_CUT }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Camera center & zoom
    val centerLat = MutableStateFlow(15.3694)
    val centerLng = MutableStateFlow(44.1910)
    val zoomLevel = MutableStateFlow(16.5)

    // Current selected node for bottom sheet details
    val selectedNode = MutableStateFlow<NetworkNodeEntity?>(null)

    // Add / Edit node dialog state
    val showAddEditDialog = MutableStateFlow(false)
    val nodeToEdit = MutableStateFlow<NetworkNodeEntity?>(null)
    val newMarkerLat = MutableStateFlow(15.3694)
    val newMarkerLng = MutableStateFlow(44.1910)

    // Other dialogs
    val showBackupDialog = MutableStateFlow(false)
    val showAboutDialog = MutableStateFlow(false)
    val mapTileType = MutableStateFlow(MapTileType.SATELLITE)

    // Distance measurement mode
    val isMeasureMode = MutableStateFlow(false)
    val measurePoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())

    // Authenticated user
    val userEmail = MutableStateFlow<String?>("abduraheemseer11@gmail.com")

    init {
        viewModelScope.launch {
            repository.prepopulateSampleDataIfNeeded()
        }
    }

    fun onMapCenterChanged(lat: Double, lng: Double, zoom: Double) {
        centerLat.value = lat
        centerLng.value = lng
        zoomLevel.value = zoom
    }

    fun selectNode(node: NetworkNodeEntity) {
        selectedNode.value = node
        centerLat.value = node.latitude
        centerLng.value = node.longitude
        zoomLevel.value = 18.0 // Zoom in to device
    }

    fun clearSelectedNode() {
        selectedNode.value = null
    }

    fun openAddNodeAt(lat: Double, lng: Double) {
        nodeToEdit.value = null
        newMarkerLat.value = lat
        newMarkerLng.value = lng
        showAddEditDialog.value = true
    }

    fun openEditNode(node: NetworkNodeEntity) {
        nodeToEdit.value = node
        showAddEditDialog.value = true
    }

    fun saveNode(node: NetworkNodeEntity) {
        viewModelScope.launch {
            val id = repository.saveNode(node)
            // If editing currently selected node, refresh selection
            if (selectedNode.value?.id == node.id) {
                selectedNode.value = node.copy(id = id)
            }
        }
    }

    fun updateNodeStatus(node: NetworkNodeEntity, status: NodeStatus) {
        viewModelScope.launch {
            repository.updateNodeStatus(node.id, status)
            selectedNode.value = node.copy(status = status)
        }
    }

    fun deleteNode(node: NetworkNodeEntity) {
        viewModelScope.launch {
            repository.deleteNode(node.id)
            if (selectedNode.value?.id == node.id) {
                selectedNode.value = null
            }
        }
    }

    // Distance Tool
    fun toggleMeasureMode() {
        isMeasureMode.value = !isMeasureMode.value
        if (!isMeasureMode.value) {
            measurePoints.value = emptyList()
        }
    }

    fun addMeasurePoint(lat: Double, lng: Double) {
        measurePoints.value = measurePoints.value + Pair(lat, lng)
    }

    fun clearMeasurePoints() {
        measurePoints.value = emptyList()
    }

    fun focusOnFaults() {
        val firstFault = allNodes.value.firstOrNull { it.status == NodeStatus.FAULT_CUT }
        if (firstFault != null) {
            selectNode(firstFault)
        }
    }
}
