package com.example.map

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

object MercatorProjection {
    private const val TILE_SIZE = 256.0

    // Converts Latitude and Longitude to world pixel coordinates at a given zoom level
    fun latLngToWorldPixel(lat: Double, lng: Double, zoom: Double): Offset {
        val scale = TILE_SIZE * 2.0.pow(zoom)
        val x = (lng + 180.0) / 360.0 * scale

        val sinLat = sin(Math.toRadians(lat)).coerceIn(-0.9999, 0.9999)
        val y = (0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * Math.PI)) * scale

        return Offset(x.toFloat(), y.toFloat())
    }

    // Converts screen pixel back to Latitude and Longitude
    fun worldPixelToLatLng(worldX: Double, worldY: Double, zoom: Double): Pair<Double, Double> {
        val scale = TILE_SIZE * 2.0.pow(zoom)
        val lng = (worldX / scale) * 360.0 - 180.0

        val n = Math.PI - 2.0 * Math.PI * worldY / scale
        val lat = Math.toDegrees(atan(sinh(n)))

        return Pair(lat.coerceIn(-85.0511, 85.0511), lng.coerceIn(-180.0, 180.0))
    }

    // Great-circle distance between two points in meters (Haversine formula)
    fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2.0).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2.0).pow(2.0)

        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return earthRadius * c
    }

    // Formats distance in Arabic
    fun formatDistanceArabic(meters: Double): String {
        return if (meters >= 1000.0) {
            String.format(java.util.Locale.US, "%.2f كم", meters / 1000.0)
        } else {
            String.format(java.util.Locale.US, "%.1f متر", meters)
        }
    }
}
