package com.deepbass.app

import android.app.Activity
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.AudioManager
import android.media.MediaCodecList
import android.media.MediaCodecInfo
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import android.graphics.drawable.GradientDrawable
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MainActivity : Activity() {
    private var bass: BassBoost? = null
    private var eq: Equalizer? = null
    private var player: ExoPlayer? = null
    private var playerStatus: TextView? = null
    private var playerButton: Button? = null
    private var selectedAudio: android.net.Uri? = null
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

        content.addView(section("DOLBY ATMOS / SPATIAL AUDIO"))
        content.addView(card().apply {
            addView(TextView(this@MainActivity).apply {
                text = "DEVICE SUPPORT"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }, matchWrap())
            addView(TextView(this@MainActivity).apply {
                text = spatialAudioSummary()
                textSize = 12f
                setTextColor(Color.LTGRAY)
                setPadding(0, dp(8), 0, dp(12))
            }, matchWrap())
            val settingsButton = Button(this@MainActivity).apply {
                text = "OPEN PHONE SOUND SETTINGS"
                textSize = 11f
                setTextColor(Color.WHITE)
                background = rounded(panel, 14)
                setOnClickListener {
                    try {
                        startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                    } catch (_: Exception) {
                        Toast.makeText(this@MainActivity, "Open Settings > Sound manually", Toast.LENGTH_LONG).show()
                    }
                }
            }
            addView(settingsButton, matchWrap())
            addView(TextView(this@MainActivity).apply {
                text = "DeepBass cannot switch on Dolby Atmos directly. If your phone has Dolby Atmos, enable it in the system sound settings. This app's bass/equalizer effects are separate."
                textSize = 11f
                setTextColor(Color.LTGRAY)
                setPadding(0, dp(8), 0, 0)
            }, matchWrap())
        })

        content.addView(section("ATMOS-CAPABLE PLAYER"))
        content.addView(card().apply {
            addView(TextView(this@MainActivity).apply {
                text = "Play a local audio file"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }, matchWrap())
            addView(TextView(this@MainActivity).apply {
                text = "Choose an audio file from your phone. If it contains a supported Dolby Atmos format and your device supports the required decoding and output, Android may play it with available spatial audio processing."
                textSize = 11f
                setTextColor(Color.LTGRAY)
                setPadding(0, dp(8), 0, dp(12))
            }, matchWrap())
            val chooseButton = Button(this@MainActivity).apply {
                text = "CHOOSE AUDIO FILE"
                textSize = 11f
                setTextColor(Color.WHITE)
                background = rounded(panel, 14)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "audio/*"
                    }
                    startActivityForResult(intent, 2401)
                }
            }
            addView(chooseButton, matchWrap())
            playerButton = Button(this@MainActivity).apply {
                text = "PLAY / PAUSE"
                textSize = 11f
                setTextColor(Color.WHITE)
                background = rounded(cyan, 14)
                setOnClickListener { togglePlayback() }
                isEnabled = false
            }
            addView(playerButton, matchWrap())
            playerStatus = TextView(this@MainActivity).apply {
                text = "No audio selected"
                textSize = 11f
                setTextColor(cyan)
                setPadding(0, dp(8), 0, 0)
            }
            addView(playerStatus, matchWrap())
        })

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

    @Deprecated("Use Activity Result APIs in a future update")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 2401 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        selectedAudio = uri
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {}
        try {
            player?.release()
            player = ExoPlayer.Builder(this).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        playerStatus?.text = when (playbackState) {
                            Player.STATE_BUFFERING -> "Preparing audio…"
                            Player.STATE_READY -> "Audio ready • device support varies"
                            Player.STATE_ENDED -> "Playback finished"
                            else -> playerStatus?.text ?: "Audio selected"
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        playerStatus?.text = "Cannot play this file. Try another audio format."
                    }
                })
                prepare()
            }
            playerStatus?.text = "Audio selected"
            playerButton?.isEnabled = true
            playerButton?.text = "PLAY"
        } catch (_: Exception) {
            playerStatus?.text = "Player initialization failed"
            playerButton?.isEnabled = false
        }
    }

    private fun togglePlayback() {
        val audioPlayer = player ?: return
        if (audioPlayer.isPlaying) {
            audioPlayer.pause()
            playerButton?.text = "PLAY"
        } else {
            audioPlayer.play()
            playerButton?.text = "PAUSE"
            playerStatus?.text = "Playing in-app audio • Atmos is not guaranteed"
        }
    }

    private fun spatialAudioSummary(): String {
        val atmosCodec = try {
            val codecs = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
            codecs.any { info ->
                info.supportedTypes.any { type ->
                    type.equals("audio/eac3-joc", true) || type.equals("audio/ac4", true)
                }
            }
        } catch (_: Exception) { false }

        val spatial = if (Build.VERSION.SDK_INT >= 32) {
            try {
                val manager = getSystemService(AudioManager::class.java)
                val s = manager?.spatializer
                if (s != null && s.isAvailable) {
                    if (s.isEnabled) "Android spatial audio is available and enabled." else "Android spatial audio is supported but currently disabled."
                } else "Android spatial audio is not reported as available."
            } catch (_: Exception) { "Spatial audio status could not be read." }
        } else {
            "Android spatializer status requires Android 12 or newer."
        }

        val codec = if (atmosCodec) {
            "Device reports a Dolby-capable audio codec (E-AC-3 JOC / AC-4); this does not confirm Dolby Atmos processing is active."
        } else {
            "No Dolby Atmos-capable decoder was detected through standard codec names."
        }
        return "$codec\n$spatial\nAtmos availability and controls depend on your phone's manufacturer and firmware."
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
        try { player?.release() } catch (_: Exception) {}
        player = null
        try { bass?.release() } catch (_: Exception) {}
        try { eq?.release() } catch (_: Exception) {}
        bass = null
        eq = null
        super.onDestroy()
    }
}
