package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NetworkNodeEntity
import com.example.data.model.NodeStatus
import com.example.map.MapTileType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<NetworkNodeEntity>,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onSelectNode: (NetworkNodeEntity) -> Unit,
    isMeasureMode: Boolean,
    onToggleMeasureMode: () -> Unit,
    currentMapType: MapTileType,
    onSelectMapType: (MapTileType) -> Unit,
    faultCount: Int,
    onOpenFilterFaults: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var showMapTypeMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Main Search Bar with rounded glass styling
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = {
                        onSearchQueryChange(it)
                        onSearchActiveChange(it.isNotBlank())
                    },
                    placeholder = {
                        Text(
                            "ابحث برقم الشبكة (#105) أو العميل...",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSearchQueryChange("")
                            onSearchActiveChange(false)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "مسح",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Measurement tool button
                IconButton(
                    onClick = onToggleMeasureMode,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isMeasureMode) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = "قياس المسافة",
                        tint = if (isMeasureMode) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Map layer switcher
                Box {
                    IconButton(
                        onClick = { showMapTypeMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = "طبقات الخريطة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMapTypeMenu,
                        onDismissRequest = { showMapTypeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("قمر صناعي (أقمار عالمية مجانية)") },
                            onClick = {
                                onSelectMapType(MapTileType.SATELLITE)
                                showMapTypeMenu = false
                            },
                            leadingIcon = {
                                RadioButton(
                                    selected = currentMapType == MapTileType.SATELLITE,
                                    onClick = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("خريطة شوارع (OpenStreetMap)") },
                            onClick = {
                                onSelectMapType(MapTileType.STREET_MAP)
                                showMapTypeMenu = false
                            },
                            leadingIcon = {
                                RadioButton(
                                    selected = currentMapType == MapTileType.STREET_MAP,
                                    onClick = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("شبكة تخطيطية (أوفلاين 100%)") },
                            onClick = {
                                onSelectMapType(MapTileType.OFFLINE_GRID)
                                showMapTypeMenu = false
                            },
                            leadingIcon = {
                                RadioButton(
                                    selected = currentMapType == MapTileType.OFFLINE_GRID,
                                    onClick = null
                                )
                            }
                        )
                    }
                }

                // Backup & Settings
                IconButton(
                    onClick = onOpenBackup,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudSync,
                        contentDescription = "نسخ واستيراد",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // About app
                IconButton(
                    onClick = onOpenAbout,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "عن التطبيق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Fault Hotspot Quick Filter Chip (if any faults exist)
        if (faultCount > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                FilterChip(
                    selected = true,
                    onClick = onOpenFilterFaults,
                    label = { Text("يوجد $faultCount أعطال / أسلاك مقطوعة", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFEE2E2),
                        selectedLabelColor = Color(0xFFB91C1C)
                    )
                )
            }
        }

        // Live Dropdown Search Results
        AnimatedVisibility(
            visible = isSearchActive && searchResults.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(searchResults) { node ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectNode(node)
                                    onSearchActiveChange(false)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status / Type Icon
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (node.status == NodeStatus.FAULT_CUT) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (node.status == NodeStatus.FAULT_CUT) Icons.Default.Warning else Icons.Default.Router,
                                    contentDescription = null,
                                    tint = if (node.status == NodeStatus.FAULT_CUT) Color(0xFFDC2626) else Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (node.networkNumber.isNotBlank()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Text(
                                                text = "#${node.networkNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = node.name.ifBlank { node.customerName },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${node.deviceType.titleAr} | ${node.ipAddress.ifBlank { "بدون IP" }} | ${node.customerPhone}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "تحديد على الخريطة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
