package com.cocbot.tool

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BotAccessibilityService : AccessibilityService() {
    companion object { @Volatile var instance: BotAccessibilityService? = null }

    @Volatile var currentPackage: String = ""
    lateinit var engine: BotEngine
    private var overlay: TextView? = null
    private var lastShot = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        engine = BotEngine(this)
        addOverlay()
        BotLog.add("Accessibility đã kết nối")
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val pkg = e?.packageName?.toString() ?: return
        if (pkg != packageName && pkg != "com.android.systemui") currentPackage = pkg
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (::engine.isInitialized) engine.stop()
        overlay?.let { runCatching { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } }
        instance = null
        return super.onUnbind(intent)
    }

    fun toggle() {
        if (engine.running) { engine.stop(); overlay?.text = "▶" } else { engine.start(); overlay?.text = "■" }
    }

    private fun addOverlay() {
        val tv = TextView(this).apply {
            text = "▶"; textSize = 22f; setTextColor(Color.WHITE)
            setBackgroundColor(0xAA000000.toInt()); setPadding(28, 14, 28, 14)
            setOnClickListener { toggle() }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        (getSystemService(WINDOW_SERVICE) as WindowManager).addView(tv, lp)
        overlay = tv
    }

    suspend fun tap(x: Float, y: Float): Boolean =
        stroke(Path().apply { moveTo(x, y); lineTo(x + 1f, y) }, 50)

    suspend fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long = 300): Boolean =
        stroke(Path().apply { moveTo(x1, y1); lineTo(x2, y2) }, ms)

    private suspend fun stroke(path: Path, ms: Long): Boolean = suspendCancellableCoroutine { c ->
        val g = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, ms)).build()
        val ok = dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { if (c.isActive) c.resume(true) }
            override fun onCancelled(d: GestureDescription?) { if (c.isActive) c.resume(false) }
        }, null)
        if (!ok && c.isActive) c.resume(false)
    }

    suspend fun screenshot(): Bitmap? {
        repeat(3) {
            val wait = 500 - (System.currentTimeMillis() - lastShot)
            if (wait > 0) delay(wait)
            lastShot = System.currentTimeMillis()
            val b = grab()
            if (b != null) return b
        }
        return null
    }

    private suspend fun grab(): Bitmap? = suspendCancellableCoroutine<Bitmap?> { c ->
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                val hw = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                val bmp = hw?.copy(Bitmap.Config.ARGB_8888, false)
                hw?.recycle()
                screenshot.hardwareBuffer.close()
                if (c.isActive) c.resume(bmp)
            }
            override fun onFailure(errorCode: Int) { if (c.isActive) c.resume(null) }
        })
    }
}
