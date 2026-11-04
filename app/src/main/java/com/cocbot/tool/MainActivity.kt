package com.cocbot.tool

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    private lateinit var cfg: BotConfig
    private lateinit var logView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() { logView.text = BotLog.text(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        cfg = BotConfig(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 48) }
        fun label(t: String) = TextView(this).apply { text = t; textSize = 14f; setPadding(0, 20, 0, 4) }
        fun button(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }
        fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

        root.addView(button("1. Bật Trợ năng (Accessibility) cho CoC Tool") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })

        root.addView(label("Chế độ"))
        val group = RadioGroup(this)
        Mode.values().forEach { m -> group.addView(RadioButton(this).apply { text = m.label; tag = m.name; id = generateViewId(); isChecked = m == cfg.mode }) }
        root.addView(group)

        val fields = linkedMapOf<String, EditText>()
        fun num(title: String, key: String, v: Long) {
            root.addView(label(title))
            val e = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; setText(v.toString()) }
            fields[key] = e; root.addView(e)
        }
        num("Số trận trước khi đổi acc", "battles", cfg.battlesPerAccount.toLong())
        num("Số acc (Supercell ID)", "accounts", cfg.accounts.toLong())
        num("Kho đạt ngưỡng Vàng thì đổi acc (0 = tắt)", "maxGold", cfg.maxGold)
        num("Kho đạt ngưỡng Dầu (0 = tắt)", "maxElixir", cfg.maxElixir)
        num("Kho đạt ngưỡng Dầu đen (0 = tắt)", "maxDark", cfg.maxDark)
        num("Loot tối thiểu: Vàng", "minGold", cfg.minGold)
        num("Loot tối thiểu: Dầu", "minElixir", cfg.minElixir)
        num("Loot tối thiểu: Dầu đen", "minDark", cfg.minDark)
        num("Số ô lính sẽ thả", "slots", cfg.troopSlots.toLong())
        num("Thời gian tối đa mỗi trận (giây)", "battleSec", cfg.battleSeconds.toLong())
        num("Nghỉ giữa các lượt cho/xin lính (giây)", "idleSec", cfg.idleSeconds.toLong())

        val swFull = CheckBox(this).apply { text = "Tự đổi acc khi kho đầy"; isChecked = cfg.switchWhenFull }
        val swSpam = CheckBox(this).apply { text = "Spam đầu hàng (thả 1 lính rồi đầu hàng)"; isChecked = cfg.spamSurrender }
        root.addView(swFull); root.addView(swSpam)

        root.addView(button("Lưu cài đặt") {
            fun n(k: String) = fields[k]!!.text.toString().toLongOrNull() ?: 0L
            val sel = group.findViewById<RadioButton>(group.checkedRadioButtonId)
            if (sel != null) cfg.mode = Mode.valueOf(sel.tag as String)
            cfg.battlesPerAccount = n("battles").toInt().coerceAtLeast(1)
            cfg.accounts = n("accounts").toInt().coerceAtLeast(1)
            cfg.maxGold = n("maxGold"); cfg.maxElixir = n("maxElixir"); cfg.maxDark = n("maxDark")
            cfg.minGold = n("minGold"); cfg.minElixir = n("minElixir"); cfg.minDark = n("minDark")
            cfg.troopSlots = n("slots").toInt().coerceAtLeast(1)
            cfg.battleSeconds = n("battleSec").toInt().coerceAtLeast(10)
            cfg.idleSeconds = n("idleSec").toInt().coerceAtLeast(5)
            cfg.switchWhenFull = swFull.isChecked; cfg.spamSurrender = swSpam.isChecked
            toast("Đã lưu")
        })

        root.addView(button("Mở Clash of Clans") {
            packageManager.getLaunchIntentForPackage("com.supercell.clashofclans")?.let { startActivity(it) } ?: toast("Chưa cài CoC")
        })
        root.addView(button("▶ Bắt đầu / ■ Dừng (hoặc dùng nút nổi ở mép phải)") {
            BotAccessibilityService.instance?.toggle() ?: toast("Hãy bật Trợ năng trước")
        })
        root.addView(button("Chụp thử màn hình (lưu Thư viện + đọc chữ)") {
            BotAccessibilityService.instance?.engine?.debugShot() ?: toast("Hãy bật Trợ năng trước")
        })

        root.addView(label("Tọa độ / vùng đọc số (pt tên x y | rg tên trái trên phải dưới)"))
        val layoutEdit = EditText(this).apply { setText(Layout.text(this@MainActivity)); textSize = 12f }
        root.addView(layoutEdit)
        root.addView(button("Lưu tọa độ") { Layout.save(this, layoutEdit.text.toString()); toast("Đã lưu tọa độ") })

        root.addView(label("Nhật ký"))
        logView = TextView(this).apply { textSize = 12f }
        root.addView(logView)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() { super.onResume(); handler.post(ticker) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(ticker) }
}
