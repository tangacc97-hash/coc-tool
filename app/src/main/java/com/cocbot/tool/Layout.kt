package com.cocbot.tool

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF

/** Toạ độ tương đối (0..1), màn hình ngang. Giá trị mặc định là ƯỚC LƯỢNG - cần chỉnh trong app (mục Tọa độ). */
object Layout {
    private val pts = linkedMapOf(
        "attack" to PointF(0.07f, 0.88f), "findMatch" to PointF(0.14f, 0.70f),
        "next" to PointF(0.92f, 0.72f), "surrender" to PointF(0.07f, 0.80f),
        "confirm" to PointF(0.58f, 0.62f), "returnHome" to PointF(0.50f, 0.88f),
        "slot0" to PointF(0.12f, 0.93f), "slotStep" to PointF(0.065f, 0f),
        "dropA" to PointF(0.30f, 0.45f), "dropB" to PointF(0.30f, 0.65f),
        "settings" to PointF(0.96f, 0.88f), "supercellId" to PointF(0.50f, 0.50f),
        "account1" to PointF(0.50f, 0.35f), "account2" to PointF(0.50f, 0.45f),
        "account3" to PointF(0.50f, 0.55f), "account4" to PointF(0.50f, 0.65f),
        "clanChat" to PointF(0.04f, 0.45f), "closeChat" to PointF(0.04f, 0.45f),
        "sendRequest" to PointF(0.60f, 0.65f), "donateTroop1" to PointF(0.30f, 0.60f)
    )
    private val regions = linkedMapOf(
        "lootGold" to RectF(0.04f, 0.20f, 0.16f, 0.26f), "lootElixir" to RectF(0.04f, 0.27f, 0.16f, 0.33f),
        "lootDark" to RectF(0.04f, 0.34f, 0.16f, 0.40f), "resGold" to RectF(0.80f, 0.04f, 0.96f, 0.10f),
        "resElixir" to RectF(0.80f, 0.11f, 0.96f, 0.17f), "resDark" to RectF(0.80f, 0.18f, 0.96f, 0.24f)
    )

    fun pt(name: String, w: Float, h: Float): PointF { val p = pts[name] ?: PointF(0.5f, 0.5f); return PointF(p.x * w, p.y * h) }
    fun region(name: String): RectF = regions[name] ?: RectF(0f, 0f, 0.1f, 0.1f)
    fun slot(i: Int, w: Float, h: Float): PointF {
        val a = pts["slot0"]!!; val s = pts["slotStep"]!!
        return PointF((a.x + s.x * i) * w, (a.y + s.y * i) * h)
    }

    fun dump(): String =
        pts.entries.joinToString("\n") { "pt ${it.key} ${it.value.x} ${it.value.y}" } + "\n" +
        regions.entries.joinToString("\n") { "rg ${it.key} ${it.value.left} ${it.value.top} ${it.value.right} ${it.value.bottom}" }

    fun text(ctx: Context): String = ctx.getSharedPreferences("bot", 0).getString("layout", null) ?: dump()
    fun save(ctx: Context, t: String) { ctx.getSharedPreferences("bot", 0).edit().putString("layout", t).apply(); parse(t) }
    fun load(ctx: Context) { ctx.getSharedPreferences("bot", 0).getString("layout", null)?.let { parse(it) } }

    private fun parse(t: String) {
        t.lines().forEach { line ->
            val p = line.trim().split(" ")
            try {
                if (p[0] == "pt" && p.size == 4) pts[p[1]] = PointF(p[2].toFloat(), p[3].toFloat())
                if (p[0] == "rg" && p.size == 6) regions[p[1]] = RectF(p[2].toFloat(), p[3].toFloat(), p[4].toFloat(), p[5].toFloat())
            } catch (_: Exception) {}
        }
    }
}
