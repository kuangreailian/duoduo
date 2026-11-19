package com.example.cardcounter.capture

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.cardcounter.data.model.Rank
import com.example.cardcounter.overlay.CountOverlayService
import kotlin.math.abs

class DetectionDeduplicator(
    private val windowMs: Long = 2000L,
    private val positionTolerancePx: Float = 120f
) {
    private data class Seen(val rank: Rank, val cx: Float, val cy: Float, val at: Long)
    private val recent = mutableListOf<Seen>()

    fun filter(detections: List<DetectedRank>, now: Long = System.currentTimeMillis()): List<DetectedRank> {
        recent.removeAll { now - it.at > windowMs }
        val accepted = mutableListOf<DetectedRank>()
        for (d in detections) {
            val cx = d.box.centerX()
            val cy = d.box.centerY()
            val dup = recent.any {
                it.rank == d.rank &&
                    abs(it.cx - cx) < positionTolerancePx &&
                    abs(it.cy - cy) < positionTolerancePx
            }
            if (!dup) {
                recent.add(Seen(d.rank, cx, cy, now))
                accepted.add(d)
            }
        }
        return accepted
    }
}

data class Roi(val left: Int, val top: Int, val width: Int, val height: Int) {
    fun isValid(imgW: Int, imgH: Int): Boolean =
        width > 0 && height > 0 &&
            left >= 0 && top >= 0 &&
            left + width <= imgW && top + height <= imgH
}

object RoiConfig {
    private const val PREFS = "roi_config"
    fun load(context: Context): Roi {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Roi(p.getInt("left", 0), p.getInt("top", 0), p.getInt("width", 0), p.getInt("height", 0))
    }
    fun save(context: Context, roi: Roi) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("left", roi.left).putInt("top", roi.top)
            .putInt("width", roi.width).putInt("height", roi.height).apply()
    }
}

data class Tuning(
    val frameIntervalMs: Long = 500L,
    val minConfidence: Float = 0.6f,
    val dedupWindowMs: Long = 2000L,
    val positionTolerancePx: Float = 120f
)

object TuningConfig {
    private const val PREFS = "tuning_config"
    fun load(context: Context): Tuning {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Tuning(
            frameIntervalMs = p.getLong("frame_interval_ms", 500L),
            minConfidence = p.getFloat("min_confidence", 0.6f),
            dedupWindowMs = p.getLong("dedup_window_ms", 2000L),
            positionTolerancePx = p.getFloat("position_tolerance_px", 120f)
        )
    }
    fun save(context: Context, t: Tuning) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("frame_interval_ms", t.frameIntervalMs)
            .putFloat("min_confidence", t.minConfidence)
            .putLong("dedup_window_ms", t.dedupWindowMs)
            .putFloat("position_tolerance_px", t.positionTolerancePx)
            .apply()
    }
}

object CaptureController {

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun openOverlayPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun startCapture(context: Context, resultCode: Int, data: Intent) {
        val intent = Intent(context, ScreenCaptureService::class.java).apply {
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopCapture(context: Context) {
        context.stopService(Intent(context, ScreenCaptureService::class.java))
    }

    fun showOverlay(context: Context) {
        context.startService(Intent(context, CountOverlayService::class.java))
    }

    fun hideOverlay(context: Context) {
        context.stopService(Intent(context, CountOverlayService::class.java))
    }
}
