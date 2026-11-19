package com.example.cardcounter.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import com.example.cardcounter.core.ActiveSession
import com.example.cardcounter.core.LatestFrameHolder
import kotlinx.coroutines.*

class ScreenCaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "capture"
        private const val NOTIF_ID = 1
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var recognizer: CardRecognizer
    private var dedup = DetectionDeduplicator()
    private var frameIntervalMs = 500L
    private var minConfidence = 0.6f

    override fun onCreate() {
        super.onCreate()
        recognizer = CardRecognizerFactory.create(this)
        val t = TuningConfig.load(this)
        dedup = DetectionDeduplicator(t.dedupWindowMs, t.positionTolerancePx)
        frameIntervalMs = t.frameIntervalMs
        minConfidence = t.minConfidence
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: return START_NOT_STICKY
        val data = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA) ?: return START_NOT_STICKY

        startForeground(NOTIF_ID, buildNotification())

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection?.stop()
        mediaProjection = mpm.getMediaProjection(resultCode, data)

        val dm = resources.displayMetrics
        imageReader?.close()
        imageReader = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 2)

        virtualDisplay?.release()
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "CardCounterCapture",
            dm.widthPixels, dm.heightPixels, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        startFrameLoop()
        return START_STICKY
    }

    private fun startFrameLoop() {
        scope.launch {
            while (isActive) {
                runCatching { processFrame() }
                delay(frameIntervalMs)
            }
        }
    }

    private fun processFrame() {
        val reader = imageReader ?: return
        val image = reader.acquireLatestImage() ?: return
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val bmp = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(buffer)

            val roi = RoiConfig.load(this)
            val cropped = if (roi.isValid(bmp.width, bmp.height)) {
                Bitmap.createBitmap(bmp, roi.left, roi.top, roi.width, roi.height)
            } else bmp

            LatestFrameHolder.set(cropped)

            val detected = recognizer.recognize(cropped)
            val regions = PlayerRegionConfig.load(this)
            val w = cropped.width.toFloat()
            val h = cropped.height.toFloat()
            dedup.filter(detected)
                .filter { it.confidence >= minConfidence }
                .forEach { d ->
                    val player = PlayerRegionConfig.playerAt(
                        regions, d.box.centerX(), d.box.centerY(), w, h
                    )
                    ActiveSession.recordRankFor(d.rank, player)
                }
        } finally {
            image.close()
        }
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "记牌识别", NotificationManager.IMPORTANCE_LOW)
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("扑克记牌器")
            .setContentText("正在识别牌面…")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        runCatching { mediaProjection?.stop() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
