package com.deepbass.app

import android.app.Activity
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {
    private var bass: BassBoost? = null
    private var eq: Equalizer? = null
    private var enabled = true
    private var bassStrength = 650
    private var status: TextView? = null
    private val cyan = Color.rgb(45, 224, 255)
    private val bg = Color.rgb(8, 12, 20)
    private val panel = Color.rgb(17, 24, 36)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        initializeEffects()
    }

    private fun initializeEffects() {
        try {
            // Session 0 requests an output-mix effect. Device support is not guaranteed.
            bass = BassBoost(0, 0).apply {
                setStrength(bassStrength.toShort())
                enabled = this@MainActivity.enabled
            }
            eq = Equalizer(0, 0).apply {
                enabled = this@MainActivity.enabled
            }
            status?.text = "Audio effects connected • device support varies"
        } catch (_: Exception) {
            status?.text = "System-wide effect unavailable on this device"
        }
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(28))
        }
        scroll.addView(content)
        setContentView(scroll)

        content.addView(TextView(this).apply {
            text = "DEEPBASS"
            textSize = 25f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            letterSpacing = 0.12f
        })
        content.addView(TextView(this).apply {
            text = "LOW FREQUENCY CONTROL"
            textSize = 11f
            letterSpacing = 0.16f
            setTextColor(cyan)
            setPadding(0, dp(4), 0, dp(22))
        })

        val toggle = Button(this).apply {
            text = "BASS ENGINE  •  ON"
            setTextColor(Color.WHITE)
            background = rounded(cyan, 18)
            setOnClickListener {
                enabled = !enabled
                try { bass?.enabled = enabled; eq?.enabled = enabled } catch (_: Exception) {}
                text = if (enabled) "BASS ENGINE  •  ON" else "BASS ENGINE  •  OFF"
                status?.text = if (enabled) "Bass processing enabled" else "Bass processing disabled"
            }
        }
        content.addView(toggle, matchWrap())

        content.addView(section("BASS INTENSITY"))
        content.addView(card().apply {
            val value = TextView(this@MainActivity).apply {
                text = "65%"
                textSize = 34f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(value, matchWrap())
            val seek = SeekBar(this@MainActivity).apply {
                max = 100
                progress = 65
                progressTintList = android.content.res.ColorStateList.valueOf(cyan)
                thumbTintList = android.content.res.ColorStateList.valueOf(cyan)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        bassStrength = (p * 10).coerceIn(0, 1000)
                        value.text = "$p%"
                        try { bass?.setStrength(bassStrength.toShort()) } catch (_: Exception) {}
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }
            addView(seek, matchWrap())
            addView(TextView(this@MainActivity).apply {
                text = "Use moderate levels to reduce distortion and protect hearing."
                textSize = 12f
                setTextColor(Color.LTGRAY)
                setPadding(0, dp(8), 0, 0)
            }, matchWrap())
        })

        content.addView(section("SOUND PRESETS"))
        val presets = listOf("Deep Bass", "Punchy", "Balanced", "Soft")
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        presets.forEach { preset ->
            val b = Button(this).apply {
                text = preset
                textSize = 11f
                setTextColor(Color.WHITE)
                background = rounded(panel, 14)
                setOnClickListener { applyPreset(preset); status?.text = "$preset preset selected" }
            }
            row.addView(b, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            })
        }
        content.addView(row)

        content.addView(section("EQUALIZER"))
        content.addView(card().apply {
            val labels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
            labels.forEachIndexed { index, label ->
                addView(TextView(this@MainActivity).apply {
                    text = label
                    textSize = 12f
                    setTextColor(Color.LTGRAY)
                    setPadding(0, dp(7), 0, 0)
                })
                val seek = SeekBar(this@MainActivity).apply {
                    max = 100
                    progress = 50
                    progressTintList = android.content.res.ColorStateList.valueOf(cyan)
                    thumbTintList = android.content.res.ColorStateList.valueOf(cyan)
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                            setEqBand(index, p - 50)
                        }
                        override fun onStartTrackingTouch(s: SeekBar?) {}
                        override fun onStopTrackingTouch(s: SeekBar?) {}
                    })
                }
                addView(seek, matchWrap())
            }
        })

        status = TextView(this).apply {
            text = "Initializing audio effects…"
            textSize = 12f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, dp(10))
        }
        content.addView(status, matchWrap())
        content.addView(TextView(this).apply {
            text = "DeepBass • Android audio effects prototype"
            textSize = 10f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }, matchWrap())
    }

    private fun applyPreset(name: String) {
        val strength = when (name) { "Deep Bass" -> 850; "Punchy" -> 700; "Balanced" -> 350; else -> 180 }
        bassStrength = strength
        try { bass?.setStrength(strength.toShort()) } catch (_: Exception) {}
        val bands = try { eq?.numberOfBands?.toInt() ?: 0 } catch (_: Exception) { 0 }
        if (bands > 0) {
            val range = eq!!.bandLevelRange
            for (i in 0 until bands) {
                val level = when (name) {
                    "Deep Bass" -> if (i < 2) 500 else 0
                    "Punchy" -> if (i == 0) 350 else if (i == 1) 250 else 0
                    "Balanced" -> 0
                    else -> if (i < 2) 100 else -100
                }
                try { eq?.setBandLevel(i.toShort(), level.coerceIn(range[0].toInt(), range[1].toInt()).toShort()) } catch (_: Exception) {}
            }
        }
    }

    private fun setEqBand(index: Int, value: Int) {
        try {
            val effect = eq ?: return
            if (index >= effect.numberOfBands) return
            val range = effect.bandLevelRange
            val level = (value * 10).coerceIn(range[0].toInt(), range[1].toInt())
            effect.setBandLevel(index.toShort(), level.toShort())
        } catch (_: Exception) {}
    }

    private fun section(title: String) = TextView(this).apply {
        text = title
        textSize = 11f
        letterSpacing = 0.12f
        setTextColor(cyan)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(24), 0, dp(10))
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(panel, 20)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        try { bass?.release() } catch (_: Exception) {}
        try { eq?.release() } catch (_: Exception) {}
        bass = null
        eq = null
        super.onDestroy()
    }
}
