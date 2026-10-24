package com.example.map

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.CableLinkEntity
import com.example.data.model.NetworkNodeEntity
import com.example.data.model.NodeStatus
import com.example.data.model.NodeType
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.*

@Composable
fun NetworkMapView(
    modifier: Modifier = Modifier,
    centerLat: Double,
    centerLng: Double,
    zoomLevel: Double,
    nodes: List<NetworkNodeEntity>,
    links: List<CableLinkEntity>,
    selectedNodeId: Long?,
    mapTileType: MapTileType,
    isMeasureMode: Boolean,
    measurePoints: List<Pair<Double, Double>>,
    onMapCenterChanged: (Double, Double, Double) -> Unit,
    onNodeClicked: (NetworkNodeEntity) -> Unit,
    onMapLongClicked: (Double, Double) -> Unit,
    onMeasurePointAdded: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tileRefreshTrigger by remember { mutableIntStateOf(0) }

    val tileManager = remember {
        TileManager(context, scope).apply {
            onTileLoaded = { tileRefreshTrigger++ }
        }
    }

    // Pulse animation for selected or faulty markers
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Current camera state
    var currentLat by remember(centerLat) { mutableDoubleStateOf(centerLat) }
    var currentLng by remember(centerLng) { mutableDoubleStateOf(centerLng) }
    var currentZoom by remember(zoomLevel) { mutableDoubleStateOf(zoomLevel) }

    // Map screen bounds
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newZoom = (currentZoom + ln(zoom.toDouble()) / ln(2.0)).coerceIn(3.0, 19.5)
                    val centerPixel = MercatorProjection.latLngToWorldPixel(currentLat, currentLng, newZoom)
                    val newPixelX = centerPixel.x - pan.x
                    val newPixelY = centerPixel.y - pan.y
                    val (newLat, newLng) = MercatorProjection.worldPixelToLatLng(newPixelX.toDouble(), newPixelY.toDouble(), newZoom)

                    currentLat = newLat
                    currentLng = newLng
                    currentZoom = newZoom
                    onMapCenterChanged(newLat, newLng, newZoom)
                }
            }
            .pointerInput(isMeasureMode, nodes) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val centerWorldPixel = MercatorProjection.latLngToWorldPixel(currentLat, currentLng, currentZoom)
                        val screenCenterX = canvasSize.width / 2f
                        val screenCenterY = canvasSize.height / 2f

                        val worldX = centerWorldPixel.x + (tapOffset.x - screenCenterX)
                        val worldY = centerWorldPixel.y + (tapOffset.y - screenCenterY)
                        val (clickedLat, clickedLng) = MercatorProjection.worldPixelToLatLng(worldX.toDouble(), worldY.toDouble(), currentZoom)

                        if (isMeasureMode) {
                            onMeasurePointAdded(clickedLat, clickedLng)
                        } else {
                            // Hit test for markers
                            var hitNode: NetworkNodeEntity? = null
                            for (node in nodes) {
                                val nodePixel = MercatorProjection.latLngToWorldPixel(node.latitude, node.longitude, currentZoom)
                                val screenX = screenCenterX + (nodePixel.x - centerWorldPixel.x)
                                val screenY = screenCenterY + (nodePixel.y - centerWorldPixel.y)

                                val dx = tapOffset.x - screenX
                                val dy = tapOffset.y - screenY
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist <= 48f) { // 48dp touch target radius
                                    hitNode = node
                                    break
                                }
                            }

                            if (hitNode != null) {
                                onNodeClicked(hitNode)
                            }
                        }
                    },
                    onLongPress = { longPressOffset ->
                        val centerWorldPixel = MercatorProjection.latLngToWorldPixel(currentLat, currentLng, currentZoom)
                        val screenCenterX = canvasSize.width / 2f
                        val screenCenterY = canvasSize.height / 2f
                        val worldX = centerWorldPixel.x + (longPressOffset.x - screenCenterX)
                        val worldY = centerWorldPixel.y + (longPressOffset.y - screenCenterY)
                        val (longLat, longLng) = MercatorProjection.worldPixelToLatLng(worldX.toDouble(), worldY.toDouble(), currentZoom)

                        onMapLongClicked(longLat, longLng)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size
            @Suppress("UNUSED_VARIABLE")
            val trigger = tileRefreshTrigger // trigger recomposition when tiles arrive

            val screenCenterX = size.width / 2f
            val screenCenterY = size.height / 2f
            val centerWorld = MercatorProjection.latLngToWorldPixel(currentLat, currentLng, currentZoom)

            // 1. Draw Satellite or Vector Grid Base Tiles
            drawMapTiles(
                tileManager = tileManager,
                mapType = mapTileType,
                zoom = currentZoom.toInt().coerceIn(1, 19),
                centerWorld = centerWorld,
                screenCenterX = screenCenterX,
                screenCenterY = screenCenterY
            )

            // 2. Draw Cable Links (Topology lines between devices)
            val nodeMap = nodes.associateBy { it.id }
            for (link in links) {
                val fromNode = nodeMap[link.fromNodeId]
                val toNode = nodeMap[link.toNodeId]
                if (fromNode != null && toNode != null) {
                    val p1 = MercatorProjection.latLngToWorldPixel(fromNode.latitude, fromNode.longitude, currentZoom)
                    val p2 = MercatorProjection.latLngToWorldPixel(toNode.latitude, toNode.longitude, currentZoom)

                    val x1 = screenCenterX + (p1.x - centerWorld.x)
                    val y1 = screenCenterY + (p1.y - centerWorld.y)
                    val x2 = screenCenterX + (p2.x - centerWorld.x)
                    val y2 = screenCenterY + (p2.y - centerWorld.y)

                    val isCut = link.status == NodeStatus.FAULT_CUT || fromNode.status == NodeStatus.FAULT_CUT || toNode.status == NodeStatus.FAULT_CUT
                    val lineColor = if (isCut) Color(0xFFEF4444) else Color(0xFF06B6D4)

                    // Draw glowing cable shadow
                    drawLine(
                        color = lineColor.copy(alpha = 0.3f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )

                    // Draw primary cable line
                    drawLine(
                        color = lineColor,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round,
                        pathEffect = if (isCut) PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f) else null
                    )
                }
            }

            // 3. Draw Distance Measurement Tool Overlay
            if (measurePoints.isNotEmpty()) {
                var prevOffset: Offset? = null
                for (i in measurePoints.indices) {
                    val pt = measurePoints[i]
                    val worldPt = MercatorProjection.latLngToWorldPixel(pt.first, pt.second, currentZoom)
                    val screenX = screenCenterX + (worldPt.x - centerWorld.x)
                    val screenY = screenCenterY + (worldPt.y - centerWorld.y)
                    val currentOffset = Offset(screenX, screenY)

                    // Line from previous point
                    if (prevOffset != null) {
                        drawLine(
                            color = Color(0xFFF59E0B),
                            start = prevOffset,
                            end = currentOffset,
                            strokeWidth = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                        )
                    }

                    // Measurement point marker
                    drawCircle(color = Color(0xFFF59E0B), radius = 10f, center = currentOffset)
                    drawCircle(color = Color.White, radius = 6f, center = currentOffset)

                    prevOffset = currentOffset
                }
            }

            // 4. Draw Markers and Visible Badges
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
            }

            val badgeBgPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            for (node in nodes) {
                val nodePixel = MercatorProjection.latLngToWorldPixel(node.latitude, node.longitude, currentZoom)
                val screenX = screenCenterX + (nodePixel.x - centerWorld.x)
                val screenY = screenCenterY + (nodePixel.y - centerWorld.y)

                // Skip drawing if outside screen view frustum with margin
                if (screenX < -150f || screenX > size.width + 150f || screenY < -150f || screenY > size.height + 150f) {
                    continue
                }

                val isSelected = node.id == selectedNodeId
                val isFaulty = node.status == NodeStatus.FAULT_CUT

                val markerColor = when {
                    isFaulty -> Color(0xFFEF4444) // أحمر - عطل
                    node.status == NodeStatus.MAINTENANCE -> Color(0xFFF59E0B) // برتقالي - صيانة
                    node.deviceType == NodeType.MIKROTIK_ROUTER -> Color(0xFF3B82F6) // أزرق - ميكروتك
                    node.deviceType == NodeType.WATERPROOF_BOX -> Color(0xFF6366F1) // بنفسجي - وتر بروف
                    node.deviceType == NodeType.OUTDOOR_ANTENNA -> Color(0xFF8B5CF6) // لافندر - أنتينة
                    else -> Color(0xFF10B981) // أخضر - مودم عميل سليم
                }

                // Selected / Fault pulse ring
                if (isSelected || isFaulty) {
                    drawCircle(
                        color = (if (isFaulty) Color(0xFFEF4444) else Color(0xFF38BDF8)).copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = Offset(screenX, screenY),
                        style = Stroke(width = 4f)
                    )
                }

                // Outer Pin Circle
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 22f else 18f,
                    center = Offset(screenX, screenY)
                )

                // Inner Colored Circle
                drawCircle(
                    color = markerColor,
                    radius = if (isSelected) 18f else 14f,
                    center = Offset(screenX, screenY)
                )

                // Type Icon Glyph
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 7f else 5f,
                    center = Offset(screenX, screenY)
                )

                // MANDATORY: Visible Floating Badge / Label above marker showing Name and Network Number!
                val labelText = if (node.networkNumber.isNotBlank()) {
                    "#${node.networkNumber} ${node.name.ifBlank { node.customerName }}"
                } else {
                    node.name.ifBlank { node.customerName.ifBlank { "جهاز" } }
                }

                val textBounds = android.graphics.Rect()
                textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
                val textWidth = textBounds.width().toFloat()
                val textHeight = textBounds.height().toFloat()

                val badgePaddingX = 20f
                val badgePaddingY = 12f
                val badgeWidth = textWidth + badgePaddingX * 2
                val badgeHeight = textHeight + badgePaddingY * 2
                val badgeTop = screenY - 55f - badgeHeight

                // Badge Background with status color border
                drawRoundRect(
                    color = Color(0xEE0F172A), // Dark slate semi-transparent
                    topLeft = Offset(screenX - badgeWidth / 2f, badgeTop),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                drawRoundRect(
                    color = markerColor,
                    topLeft = Offset(screenX - badgeWidth / 2f, badgeTop),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = CornerRadius(16f, 16f),
                    style = Stroke(width = 2.5f)
                )

                // Badge text in Arabic
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    screenX,
                    badgeTop + badgeHeight - badgePaddingY + 4f,
                    textPaint
                )
            }
        }
    }
}

private fun DrawScope.drawMapTiles(
    tileManager: TileManager,
    mapType: MapTileType,
    zoom: Int,
    centerWorld: Offset,
    screenCenterX: Float,
    screenCenterY: Float
) {
    val tileSize = 256
    val scale = tileSize * 2.0.pow(zoom.toDouble())
    val maxTileIndex = (1 shl zoom) - 1

    val leftTile = floor((centerWorld.x - screenCenterX) / tileSize).toInt()
    val rightTile = ceil((centerWorld.x + screenCenterX) / tileSize).toInt()
    val topTile = floor((centerWorld.y - screenCenterY) / tileSize).toInt()
    val bottomTile = ceil((centerWorld.y + screenCenterY) / tileSize).toInt()

    // Background color based on satellite vs dark tech grid
    val bgColor = if (mapType == MapTileType.SATELLITE) Color(0xFF0F1E2E) else Color(0xFF0B132B)
    drawRect(color = bgColor)

    for (tx in leftTile..rightTile) {
        val wrappedX = ((tx % (maxTileIndex + 1)) + (maxTileIndex + 1)) % (maxTileIndex + 1)
        for (ty in topTile..bottomTile) {
            if (ty < 0 || ty > maxTileIndex) continue

            val tileScreenX = screenCenterX + (tx * tileSize - centerWorld.x)
            val tileScreenY = screenCenterY + (ty * tileSize - centerWorld.y)

            val tileBitmap = tileManager.getTile(zoom, wrappedX, ty, mapType)
            if (tileBitmap != null) {
                drawImage(
                    image = tileBitmap,
                    topLeft = Offset(tileScreenX, tileScreenY)
                )
            } else {
                // Subtle technical grid pattern for offline / uncached tiles
                drawRect(
                    color = Color(0xFF1E293B).copy(alpha = 0.4f),
                    topLeft = Offset(tileScreenX, tileScreenY),
                    size = Size(tileSize.toFloat(), tileSize.toFloat()),
                    style = Stroke(width = 1f)
                )
            }
        }
    }
}
