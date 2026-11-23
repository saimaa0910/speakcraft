package com.example.speech.video

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/** Head orientation in degrees. Yaw: turning left/right; pitch: nodding up/down; roll: tilting. */
data class HeadAngles(val yawDeg: Float, val pitchDeg: Float, val rollDeg: Float)

/**
 * Converts MediaPipe's facial transformation matrix (4×4, column-major, as returned by
 * `FaceLandmarkerResult.facialTransformationMatrixes()`) into head angles.
 *
 * The rotation part R is decomposed as R = Ry(yaw) · Rx(pitch) · Rz(roll):
 * pitch = asin(−r12), yaw = atan2(r02, r22), roll = atan2(r10, r11). A frontal face gives
 * angles near zero. Pure math, so it is unit-tested on the JVM.
 */
object HeadPose {

    fun fromMatrix(m: FloatArray): HeadAngles {
        require(m.size >= 16) { "expected a 4x4 matrix (16 values)" }
        // column-major: element (row r, col c) = m[c * 4 + r]
        fun r(row: Int, col: Int) = m[col * 4 + row].toDouble()
        // Normalise columns to remove any scale in the transform.
        val sx = sqrt(r(0, 0) * r(0, 0) + r(1, 0) * r(1, 0) + r(2, 0) * r(2, 0)).takeIf { it > 1e-9 } ?: 1.0
        val sy = sqrt(r(0, 1) * r(0, 1) + r(1, 1) * r(1, 1) + r(2, 1) * r(2, 1)).takeIf { it > 1e-9 } ?: 1.0
        val sz = sqrt(r(0, 2) * r(0, 2) + r(1, 2) * r(1, 2) + r(2, 2) * r(2, 2)).takeIf { it > 1e-9 } ?: 1.0
        val r02 = r(0, 2) / sz
        val r12 = (r(1, 2) / sz).coerceIn(-1.0, 1.0)
        val r22 = r(2, 2) / sz
        val r10 = r(1, 0) / sx
        val r11 = r(1, 1) / sy
        val pitch = asin(-r12)
        val yaw = atan2(r02, r22)
        val roll = atan2(r10, r11)
        return HeadAngles(Math.toDegrees(yaw).toFloat(), Math.toDegrees(pitch).toFloat(), Math.toDegrees(roll).toFloat())
    }
}
