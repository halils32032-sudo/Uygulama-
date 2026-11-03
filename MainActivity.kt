package com.example.ghostguard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import android.app.Activity

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("gg", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val info = TextView(this).apply {
            textSize = 16f
            text = "Ekran kenarlarına görünmez koruma şeritleri koyar. " +
                "Hayalet dokunmalar genelde kenarlarda olur. Bildirimdeki " +
                "\"Dokunmatik kilidi\" düğmesi ekranı tamamen kilitler."
        }
        val edgeLabel = TextView(this).apply { textSize = 15f }
        val seek = SeekBar(this).apply {
            max = 60
            progress = prefs.getInt("edge_dp", 24)
        }
        fun upd() { edgeLabel.text = "Kenar kalınlığı: ${seek.progress} dp" }
        upd()
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { upd() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {
                prefs.edit().putInt("edge_dp", seek.progress).apply()
            }
        })
        val start = Button(this).apply { text = "Korumayı başlat" }
        val stop = Button(this).apply { text = "Korumayı durdur" }

        start.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Önce \"üstte gösterme\" iznini ver", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            if (Build.VERSION.SDK_INT >= 33)
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            prefs.edit().putInt("edge_dp", seek.progress).apply()
            startForegroundService(Intent(this, GuardService::class.java))
        }
        stop.setOnClickListener { stopService(Intent(this, GuardService::class.java)) }

        root.addView(info); root.addView(edgeLabel); root.addView(seek)
        root.addView(start); root.addView(stop)
        setContentView(root)
    }
}
