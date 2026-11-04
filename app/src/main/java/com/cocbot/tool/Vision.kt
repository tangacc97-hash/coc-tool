package com.cocbot.tool

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object Vision {
    private val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private suspend fun recognize(bmp: Bitmap): Text? = suspendCancellableCoroutine<Text?> { c ->
        rec.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { if (c.isActive) c.resume(it) }
            .addOnFailureListener { if (c.isActive) c.resume(null) }
    }

    suspend fun allText(bmp: Bitmap): String = recognize(bmp)?.text ?: ""

    /** Tìm dòng chữ chứa một trong các từ khóa, trả về tâm (toạ độ pixel). */
    suspend fun findText(bmp: Bitmap, vararg keys: String): PointF? {
        val r = recognize(bmp) ?: return null
        for (b in r.textBlocks) for (l in b.lines) {
            if (keys.any { l.text.contains(it, ignoreCase = true) }) {
                val bb = l.boundingBox ?: continue
                return PointF(bb.exactCenterX(), bb.exactCenterY())
            }
        }
        return null
    }

    /** Đọc một số (vàng/dầu/...) trong vùng tương đối. */
    suspend fun readNumber(bmp: Bitmap, r: RectF): Long {
        val x = (r.left * bmp.width).toInt().coerceIn(0, bmp.width - 2)
        val y = (r.top * bmp.height).toInt().coerceIn(0, bmp.height - 2)
        val cw = (r.width() * bmp.width).toInt().coerceIn(2, bmp.width - x)
        val ch = (r.height() * bmp.height).toInt().coerceIn(2, bmp.height - y)
        val big = Bitmap.createScaledBitmap(Bitmap.createBitmap(bmp, x, y, cw, ch), cw * 2, ch * 2, true)
        val t = recognize(big)?.text ?: return 0L
        return t.filter { it.isDigit() }.toLongOrNull() ?: 0L
    }
}
