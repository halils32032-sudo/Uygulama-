package com.example.ghostguard

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class GuardService : Service() {
    private lateinit var wm: WindowManager
    private val edges = mutableListOf<View>()
    private var lockView: View? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val ch = NotificationChannel("gg", "Koruma", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        startForeground(1, buildNotification())
        addEdges()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "TOGGLE_LOCK") toggleLock()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getService(this, 0,
            Intent(this, GuardService::class.java).setAction("TOGGLE_LOCK"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, "gg")
            .setContentTitle("Hayalet dokunma koruması açık")
            .setContentText(if (lockView == null) "Kenar şeritleri aktif" else "Ekran KİLİTLİ")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Dokunmatik kilidi aç/kapat", pi).build())
            .build()
    }

    private fun params(w: Int, h: Int, gravity: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply { this.gravity = gravity }

    private fun blocker(): View = View(this).apply { setOnTouchListener { _, _ -> true } }

    private fun addEdges() {
        val dp = getSharedPreferences("gg", MODE_PRIVATE).getInt("edge_dp", 24)
        val px = (dp * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val MATCH = WindowManager.LayoutParams.MATCH_PARENT
        listOf(
            params(px, MATCH, Gravity.START or Gravity.TOP),
            params(px, MATCH, Gravity.END or Gravity.TOP),
            params(MATCH, px, Gravity.BOTTOM)
        ).forEach { p ->
            val v = blocker()
            wm.addView(v, p)
            edges.add(v)
        }
    }

    private fun toggleLock() {
        if (lockView == null) {
            val v = blocker()
            wm.addView(v, params(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT, Gravity.TOP))
            lockView = v
        } else {
            wm.removeView(lockView); lockView = null
        }
        getSystemService(NotificationManager::class.java).notify(1, buildNotification())
    }

    override fun onDestroy() {
        edges.forEach { runCatching { wm.removeView(it) } }
        lockView?.let { runCatching { wm.removeView(it) } }
        super.onDestroy()
    }
}
