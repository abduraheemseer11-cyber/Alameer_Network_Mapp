package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.MercatorProjection

@Composable
fun DistanceMeasureOverlay(
    measurePoints: List<Pair<Double, Double>>,
    onClearPoints: () -> Unit,
    onCloseMeasureMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate total cumulative distance
    var totalMeters = 0.0
    for (i in 0 until measurePoints.size - 1) {
        val p1 = measurePoints[i]
        val p2 = measurePoints[i + 1]
        totalMeters += MercatorProjection.distanceInMeters(p1.first, p1.second, p2.first, p2.second)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "أداة قياس المسافة الحية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onCloseMeasureMode) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (measurePoints.isEmpty()) {
                Text(
                    text = "المس أي نقطتين أو أكثر على الخريطة لحساب المسافة بينهما ومسار الكابلات.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "عدد النقاط: ${measurePoints.size}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "المسافة: ${MercatorProjection.formatDistanceArabic(totalMeters)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                    }

                    OutlinedButton(
                        onClick = onClearPoints,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسح النقاط")
                    }
                }
            }
        }
    }
}
