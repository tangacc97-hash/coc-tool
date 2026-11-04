package com.cocbot.tool

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import kotlinx.coroutines.*

class BotEngine(private val svc: BotAccessibilityService) {
    private val cfg = BotConfig(svc)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    @Volatile private var w = 2400f
    @Volatile private var h = 1080f
    private val game = "com.supercell.clashofclans"

    val running: Boolean get() = job?.isActive == true

    fun start() {
        if (running) return
        Layout.load(svc)
        job = scope.launch {
            BotLog.add("▶ ${cfg.mode.label}")
            try { loop() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { BotLog.add("Lỗi: ${e.message}") }
            finally { BotLog.add("■ Dừng") }
        }
    }

    fun stop() { job?.cancel() }

    /** Chụp thử: lưu ảnh vào Thư viện/CocTool và ghi log chữ OCR đọc được (để chỉnh tọa độ). */
    fun debugShot() {
        scope.launch {
            val b = shot()
            if (b == null) { BotLog.add("Không chụp được màn hình (cần Android 11+)"); return@launch }
            val v = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "coc_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CocTool")
            }
            val uri = svc.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)
            if (uri != null) svc.contentResolver.openOutputStream(uri)?.use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
            BotLog.add("Đã lưu ảnh. OCR: " + Vision.allText(b).replace("\n", " | ").take(300))
        }
    }

    // ---------- vòng lặp chính ----------
    private suspend fun loop() {
        var battles = 0
        var acct = 0
        while (currentCoroutineContext().isActive) {
            ensureGame()
            when (cfg.mode) {
                Mode.FARM -> {
                    val full = cfg.switchWhenFull && storageFull()
                    if (full || battles >= cfg.battlesPerAccount) {
                        BotLog.add(if (full) "Kho đầy" else "Đủ ${battles} trận")
                        acct = (acct + 1) % cfg.accounts.coerceAtLeast(1)
                        switchAccount(acct); battles = 0
                    } else if (attackOnce()) battles++ else { BotLog.add("Tấn công lỗi, thử lại"); recoverHome() }
                }
                Mode.DONATE_ONLY -> { donate(); delay(cfg.idleSeconds * 1000L) }
                Mode.REQUEST_ONLY -> { requestTroops(); delay(cfg.idleSeconds * 1000L) }
            }
        }
    }

    private suspend fun ensureGame() {
        if (svc.currentPackage == game) return
        BotLog.add("Mở Clash of Clans")
        svc.packageManager.getLaunchIntentForPackage(game)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); svc.startActivity(it)
        }
        delay(12_000)
    }

    private suspend fun storageFull(): Boolean {
        val b = shot() ?: return false
        val g = Vision.readNumber(b, Layout.region("resGold"))
        val e = Vision.readNumber(b, Layout.region("resElixir"))
        val d = Vision.readNumber(b, Layout.region("resDark"))
        BotLog.add("Kho: $g / $e / $d")
        return (cfg.maxGold > 0 && g >= cfg.maxGold) ||
            (cfg.maxElixir > 0 && e >= cfg.maxElixir) ||
            (cfg.maxDark > 0 && d >= cfg.maxDark)
    }

    // ---------- đánh ----------
    private suspend fun attackOnce(): Boolean {
        tapRel("attack"); delay(1500)
        tapText("Find a Match", "Find Match", fallback = "findMatch"); delay(6000)

        var skips = 0
        while (skips < 60) {
            val b = shot() ?: return false
            val g = Vision.readNumber(b, Layout.region("lootGold"))
            val e = Vision.readNumber(b, Layout.region("lootElixir"))
            val d = Vision.readNumber(b, Layout.region("lootDark"))
            if (g >= cfg.minGold && e >= cfg.minElixir && d >= cfg.minDark) { BotLog.add("Đánh base: $g/$e/$d"); break }
            BotLog.add("Bỏ qua: $g/$e/$d")
            tapRel("next"); delay(3500); skips++
        }
        if (skips >= 60) return false

        deploy()
        return finishBattle()
    }

    private suspend fun deploy() {
        val slots = if (cfg.spamSurrender) 1 else cfg.troopSlots
        val a = Layout.pt("dropA", w, h)
        val b = Layout.pt("dropB", w, h)
        val taps = if (cfg.spamSurrender) 1 else 12
        for (i in 0 until slots) {
            val s = Layout.slot(i, w, h)
            svc.tap(s.x, s.y); delay(300)
            for (k in 0 until taps) {
                val t = if (taps == 1) 0f else k / (taps - 1f)
                svc.tap(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t); delay(80)
            }
        }
    }

    private suspend fun finishBattle(): Boolean {
        val limit = if (cfg.spamSurrender) 3 else cfg.battleSeconds
        val deadline = System.currentTimeMillis() + limit * 1000L
        while (System.currentTimeMillis() < deadline) {
            delay(3000)
            val b = shot() ?: continue
            if (Vision.findText(b, "Return Home") != null) { tapText("Return Home", fallback = "returnHome"); delay(3000); return true }
        }
        tapRel("surrender"); delay(800)
        tapText("Okay", "OK", fallback = "confirm"); delay(2500)
        tapText("Return Home", fallback = "returnHome"); delay(3000)
        return true
    }

    private suspend fun recoverHome() {
        tapText("Return Home", "Okay", "Close"); delay(2000)
    }

    // ---------- đổi acc ----------
    private suspend fun switchAccount(index: Int) {
        BotLog.add("Đổi acc → #${index + 1}")
        tapRel("settings"); delay(1500)
        tapText("Supercell ID", fallback = "supercellId"); delay(2500)
        tapRel("account${index + 1}"); delay(1500)
        tapText("Load", "Confirm", "Okay"); delay(1500)
        tapText("Okay", "Confirm"); delay(15_000)
    }

    // ---------- clan ----------
    private suspend fun requestTroops() {
        tapRel("clanChat"); delay(1500)
        if (tapText("Request")) { delay(1000); tapText("Send", fallback = "sendRequest"); delay(1000) }
        tapRel("closeChat")
    }

    private suspend fun donate() {
        tapRel("clanChat"); delay(1500)
        repeat(10) {
            val b = shot() ?: return@repeat
            val p = Vision.findText(b, "Donate") ?: return@repeat
            svc.tap(p.x, p.y); delay(1200)
            repeat(5) { tapRel("donateTroop1") }
            delay(500)
        }
        tapRel("closeChat")
    }

    // ---------- tiện ích ----------
    private suspend fun shot(): Bitmap? {
        val b = svc.screenshot()
        if (b != null) { w = b.width.toFloat(); h = b.height.toFloat() }
        return b
    }

    private suspend fun tapRel(name: String) {
        val p = Layout.pt(name, w, h); svc.tap(p.x, p.y); delay(700)
    }

    private suspend fun tapText(vararg keys: String, fallback: String? = null): Boolean {
        val b = shot()
        val p = if (b != null) Vision.findText(b, *keys) else null
        if (p != null) { svc.tap(p.x, p.y); delay(700); return true }
        if (fallback != null) { tapRel(fallback); return true }
        return false
    }
}
