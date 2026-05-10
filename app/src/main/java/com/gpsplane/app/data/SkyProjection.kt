package com.gpsplane.app.data

/**
 * Single source of truth for sky plot angle math.
 *
 * Call sites (satellites, cardinal labels, magnetic-north marker, tick
 * marks, orientation fan) all project through here. No Canvas transform
 * is involved — every drawn element computes its own (x, y) so there is
 * exactly one formula to be right about, and that formula is covered by
 * [SkyProjectionTest].
 *
 * Coordinate conventions:
 *  - `worldAzDeg` and `rotationDeg` use the compass convention:
 *    0° = north, increasing clockwise, wrapped to [0, 360).
 *  - `rotationDeg` is the world bearing that should appear at the top
 *    of the dial. NORTH-UP = 0, TRK-UP = GPS bearing, HDG-UP = phone
 *    compass heading.
 *  - Returned offsets are normalized screen-space (pixels-per-radius):
 *    (+x, 0)=right, (0, +y)=down. Top-of-dial is y = −1.
 */
object SkyProjection {

    /**
     * Project a point on the outer ring (full radius).
     *
     * Worked example — phone pointing east (rotationDeg = 90):
     *  - world east (az=90) → (sin 0, -cos 0) = (0, -1): top ✓
     *  - true north (az=0) → (sin -90, -cos -90) = (-1, 0): left ✓
     */
    fun projectOnRing(worldAzDeg: Float, rotationDeg: Float): Pair<Float, Float> {
        val rad = Math.toRadians((worldAzDeg - rotationDeg).toDouble())
        val x = kotlin.math.sin(rad).toFloat()
        val y = -kotlin.math.cos(rad).toFloat()
        return x to y
    }

    /**
     * Project a satellite at the given azimuth and elevation. Elevation
     * 0° lands on the ring; 90° (zenith) lands at the centre.
     */
    fun projectWithElevation(
        worldAzDeg: Float,
        elevationDeg: Float,
        rotationDeg: Float,
    ): Pair<Float, Float> {
        val el = elevationDeg.coerceIn(0f, 90f)
        val dist = 1f - el / 90f
        val (x, y) = projectOnRing(worldAzDeg, rotationDeg)
        return (x * dist) to (y * dist)
    }
}
