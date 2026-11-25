package com.remlauncher.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.File

class GameActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var controlsLayer: FrameLayout
    private lateinit var fpsLabel: TextView

    private var guiVisible = true
    private var fpsVisible = false

    private val background = Color.BLACK
    private val accent = 0xFF10B981.toInt()

    private var nickname = "RemPlayer"
    private var renderer = "KlodexGpu"
    private var ram = 4096

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nickname = intent.getStringExtra("nickname") ?: "RemPlayer"
        renderer = intent.getStringExtra("renderer") ?: "KlodexGpu"
        ram = intent.getIntExtra("ram", 4096)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        window.statusBarColor = background
        window.navigationBarColor = background

        window.decorView.systemUiVisibility =
            (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )

        buildGameScreen()
    }

    private fun buildGameScreen() {
        root = FrameLayout(this).apply {
            setBackgroundColor(background)
        }

        val loading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(background)
        }

        loading.addView(
            TextView(this).apply {
                text = "REM LAUNCHER"
                textSize = 24f
                setTextColor(accent)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
        )

        loading.addView(
            TextView(this).apply {
                text = "Preparando Minecraft Java Edition 1.21.4"
                textSize = 13f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(10, 12, 10, 12)
            }
        )

        loading.addView(
            TextView(this).apply {
                text = "Nickname: $nickname\n" +
                    "RAM: $ram MB\n" +
                    "Renderizador: $renderer\n\n" +
                    "Runtime Java / LWJGL ainda não integrado."
                textSize = 12f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
            }
        )

        root.addView(
            loading,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        controlsLayer = FrameLayout(this)

        root.addView(
            controlsLayer,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        fpsLabel = TextView(this).apply {
            text = "FPS: --"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(0x99000000.toInt())
            setPadding(10, 6, 10, 6)
            visibility = View.GONE
        }

        val fpsParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END
        )

        fpsParams.setMargins(0, 12, 65, 0)

        root.addView(fpsLabel, fpsParams)

        val settingsButton = Button(this).apply {
            text = "⚙"
            textSize = 20f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(0x99000000.toInt())

            setOnClickListener {
                showGameMenu()
            }
        }

        val gearParams = FrameLayout.LayoutParams(
            54,
            48,
            Gravity.TOP or Gravity.END
        )

        gearParams.setMargins(0, 6, 8, 0)

        root.addView(settingsButton, gearParams)

        setContentView(root)

        loadVirtualControls()
    }

    private fun loadVirtualControls() {
        controlsLayer.removeAllViews()

        val file = File(
            File(filesDir, "rem_launcher"),
            "controls.json"
        )

        if (!file.exists()) {
            return
        }

        try {
            val config = JSONObject(file.readText())

            val scale = config.optDouble(
                "scale",
                1.0
            ).toFloat()

            val opacity = config.optDouble(
                "opacity",
                0.75
            ).toFloat().coerceIn(0.1f, 1f)

            val buttons = config.getJSONArray("buttons")

            for (i in 0 until buttons.length()) {
                val item = buttons.getJSONObject(i)

                if (!item.optBoolean("visible", true)) {
                    continue
                }

                val name = item.optString("name")

                val x = item.optDouble("x", 0.5).toFloat()
                val y = item.optDouble("y", 0.5).toFloat()

                val virtualButton = Button(this).apply {
                    text = name
                    textSize = 11f
                    isAllCaps = false
                    alpha = opacity
                    setTextColor(Color.WHITE)
                    setBackgroundColor(0xFF3F3F46.toInt())

                    setOnClickListener {
                        Toast.makeText(
                            this@GameActivity,
                            "Controle $name: aguardando integração JNI.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                val params = FrameLayout.LayoutParams(
                    (54 * scale).toInt().coerceAtLeast(36),
                    (48 * scale).toInt().coerceAtLeast(34)
                )

                params.leftMargin = x.coerceIn(0f, 1f)
                    .let { (it * resources.displayMetrics.widthPixels).toInt() }

                params.topMargin = y.coerceIn(0f, 1f)
                    .let { (it * resources.displayMetrics.heightPixels).toInt() }

                controlsLayer.addView(
                    virtualButton,
                    params
                )
            }

        } catch (error: Exception) {
            Toast.makeText(
                this,
                "Erro ao carregar controls.json: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showGameMenu() {
        val options = arrayOf(
            if (guiVisible) "Ocultar GUI" else "Mostrar GUI",
            if (fpsVisible) "Desativar contador FPS" else "Ativar contador FPS",
            "Voltar ao Launcher"
        )

        AlertDialog.Builder(this)
            .setTitle("Rem Launcher")
            .setItems(options) { _, position ->

                when (position) {

                    0 -> {
                        guiVisible = !guiVisible

                        controlsLayer.visibility =
                            if (guiVisible) View.VISIBLE else View.GONE
                    }

                    1 -> {
                        fpsVisible = !fpsVisible

                        fpsLabel.visibility =
                            if (fpsVisible) View.VISIBLE else View.GONE

                        fpsLabel.text =
                            "FPS: -- (runtime não iniciado)"
                    }

                    2 -> {
                        returnToLauncher()
                    }
                }
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun returnToLauncher() {
        finish()
    }

    override fun onBackPressed() {
        showGameMenu()
    }
}
