package com.cocbot.tool

import android.content.Context

enum class Mode(val label: String) {
    FARM("Đấu thưởng & Đấu rank"),
    DONATE_ONLY("Chỉ cho lính"),
    REQUEST_ONLY("Chỉ xin lính")
}

class BotConfig(ctx: Context) {
    private val p = ctx.getSharedPreferences("bot", Context.MODE_PRIVATE)

    var mode: Mode
        get() = runCatching { Mode.valueOf(p.getString("mode", "")!!) }.getOrDefault(Mode.FARM)
        set(v) = p.edit().putString("mode", v.name).apply()
    var battlesPerAccount: Int get() = p.getInt("battles", 5); set(v) = p.edit().putInt("battles", v).apply()
    var accounts: Int get() = p.getInt("accounts", 2); set(v) = p.edit().putInt("accounts", v).apply()
    var switchWhenFull: Boolean get() = p.getBoolean("switchFull", false); set(v) = p.edit().putBoolean("switchFull", v).apply()
    var spamSurrender: Boolean get() = p.getBoolean("spam", false); set(v) = p.edit().putBoolean("spam", v).apply()
    var maxGold: Long get() = p.getLong("maxGold", 0); set(v) = p.edit().putLong("maxGold", v).apply()
    var maxElixir: Long get() = p.getLong("maxElixir", 0); set(v) = p.edit().putLong("maxElixir", v).apply()
    var maxDark: Long get() = p.getLong("maxDark", 0); set(v) = p.edit().putLong("maxDark", v).apply()
    var minGold: Long get() = p.getLong("minGold", 0); set(v) = p.edit().putLong("minGold", v).apply()
    var minElixir: Long get() = p.getLong("minElixir", 0); set(v) = p.edit().putLong("minElixir", v).apply()
    var minDark: Long get() = p.getLong("minDark", 0); set(v) = p.edit().putLong("minDark", v).apply()
    var troopSlots: Int get() = p.getInt("slots", 4); set(v) = p.edit().putInt("slots", v).apply()
    var battleSeconds: Int get() = p.getInt("battleSec", 180); set(v) = p.edit().putInt("battleSec", v).apply()
    var idleSeconds: Int get() = p.getInt("idleSec", 60); set(v) = p.edit().putInt("idleSec", v).apply()
}

object BotLog {
    private val lines = ArrayDeque<String>()
    @Synchronized fun add(s: String) { lines.addLast(s); while (lines.size > 40) lines.removeFirst() }
    @Synchronized fun text(): String = lines.joinToString("\n")
}
