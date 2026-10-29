package com.ollie.standoff2cheat

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var roundButton: View? = null
    private var menuView: View? = null

    private val roundParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 40
        y = 200
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createRoundButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createRoundButton() {
        val button = ImageView(this).apply {
            setImageResource(R.drawable.round_button)
            contentDescription = "Open cheat menu"
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = roundParams.x
                    initialY = roundParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                    roundParams.x = initialX + dx
                    roundParams.y = initialY + dy
                    windowManager.updateViewLayout(v, roundParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) toggleMenu()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(button, roundParams)
        roundButton = button
    }

    private fun toggleMenu() {
        if (menuView == null) showMenu() else hideMenu()
    }

    private fun showMenu() {
        val view = LayoutInflater.from(this).inflate(R.layout.menu_overlay, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val rootStatus = view.findViewById<TextView>(R.id.rootStatus)
        rootStatus.text = if (CheatNative.isRooted()) "Root: найден" else "Root: не найден (ограниченный режим)"

        val aimSwitch = view.findViewById<Switch>(R.id.aimSwitch)
        val aimFovSeek = view.findViewById<SeekBar>(R.id.aimFovSeek)
        val aimSmoothSeek = view.findViewById<SeekBar>(R.id.aimSmoothSeek)
        val espSwitch = view.findViewById<Switch>(R.id.espSwitch)
        val noRecoilSwitch = view.findViewById<Switch>(R.id.noRecoilSwitch)
        val recoilSeek = view.findViewById<SeekBar>(R.id.recoilSeek)
        val closeBtn = view.findViewById<Button>(R.id.closeMenuButton)

        aimSwitch.setOnCheckedChangeListener { _, checked ->
            CheatNative.setFeature(CheatNative.FEATURE_AIM, checked)
            CheatNative.patchGame(checked || espSwitch.isChecked || noRecoilSwitch.isChecked)
        }
        aimFovSeek.setOnSeekBarChangeListener(simpleSeek { p ->
            CheatNative.setFloat(CheatNative.FLOAT_AIM_FOV, p.toFloat())
        })
        aimSmoothSeek.setOnSeekBarChangeListener(simpleSeek { p ->
            CheatNative.setFloat(CheatNative.FLOAT_AIM_SMOOTH, p.toFloat())
        })
        espSwitch.setOnCheckedChangeListener { _, checked ->
            CheatNative.setFeature(CheatNative.FEATURE_ESP, checked)
            CheatNative.patchGame(aimSwitch.isChecked || checked || noRecoilSwitch.isChecked)
        }
        noRecoilSwitch.setOnCheckedChangeListener { _, checked ->
            CheatNative.setFeature(CheatNative.FEATURE_NO_RECOIL, checked)
            CheatNative.patchGame(aimSwitch.isChecked || espSwitch.isChecked || checked)
        }
        recoilSeek.setOnSeekBarChangeListener(simpleSeek { p ->
            CheatNative.setFloat(CheatNative.FLOAT_RECOIL_MULT, p / 100f)
        })
        closeBtn.setOnClickListener { hideMenu() }

        windowManager.addView(view, params)
        menuView = view
    }

    private fun hideMenu() {
        menuView?.let { windowManager.removeView(it) }
        menuView = null
    }

    private fun simpleSeek(onProgress: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onProgress(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }

    override fun onDestroy() {
        roundButton?.let { windowManager.removeView(it) }
        hideMenu()
        super.onDestroy()
    }
}
