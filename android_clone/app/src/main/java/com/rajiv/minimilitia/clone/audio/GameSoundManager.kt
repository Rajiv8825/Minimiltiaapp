package com.rajiv.minimilitia.clone.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import kotlin.random.Random

/**
 * GameSoundManager: High-performance, low-latency audio engine for Mini Militia.
 * Uses SoundPool for instant weapon and jetpack SFX, and MediaPlayer for background themes.
 */
class GameSoundManager(private val context: Context) {

    private val tag = "GameSoundManager"
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private var mediaPlayer: MediaPlayer? = null
    private var jetpackStreamId: Int = 0
    private var isJetpackPlaying: Boolean = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(audioAttributes)
            .build()

        preloadSounds()
    }

    private fun preloadSounds() {
        val soundFiles = listOf(
            "ak47.wav", "m16.wav", "shotgun.wav", "rocket.wav", "uzi.wav",
            "deagle.wav", "magnum.wav", "thrust.wav", "explode.wav", "reload.wav",
            "throw.wav", "grenade1.wav", "grenade2.wav", "pickup.wav", "switch.wav",
            "dryfire.wav", "impact.wav", "impact2.wav", "ricochet.wav", "gas.wav", "saw.wav",
            "cmonboy.wav", "getsome.wav", "bringit.wav", "niceshot.wav", "yeah.wav",
            "death1.wav", "death2.wav", "death3.wav", "death4.wav", "death5.wav"
        )

        for (fileName in soundFiles) {
            try {
                val afd: AssetFileDescriptor = context.assets.openFd("audio/$fileName")
                val soundId = soundPool.load(afd, 1)
                soundMap[fileName] = soundId
                // Also map without extension for clean call-sites
                soundMap[fileName.substringBeforeLast(".")] = soundId
            } catch (e: Exception) {
                Log.w(tag, "Could not load audio asset: audio/$fileName - ${e.message}")
            }
        }
    }

    fun playSfx(name: String, volume: Float = 1.0f, pitch: Float = 1.0f): Int {
        val soundId = soundMap[name] ?: soundMap["$name.wav"] ?: return 0
        return soundPool.play(soundId, volume, volume, 1, 0, pitch)
    }

    fun playWeaponFire(weaponId: String) {
        val soundKey = when (weaponId.lowercase()) {
            "ak47" -> "ak47"
            "m16", "m4" -> "m16"
            "shotgun" -> "shotgun"
            "rocket", "smaw" -> "rocket"
            "uzi" -> "uzi"
            "deagle", "deserteagle" -> "deagle"
            "magnum" -> "magnum"
            "m14", "sniper" -> "m16" // crisp rifle report
            else -> "ak47"
        }
        playSfx(soundKey, volume = 0.9f)
    }

    fun playReload() {
        playSfx("reload", volume = 0.85f)
    }

    fun playWeaponSwitch() {
        playSfx("switch", volume = 0.8f)
    }

    fun playPickup() {
        playSfx("pickup", volume = 0.85f)
    }

    fun playDryFire() {
        playSfx("dryfire", volume = 0.6f)
    }

    fun playGrenadeThrow() {
        playSfx("throw", volume = 0.9f)
    }

    fun playExplosion() {
        playSfx("explode", volume = 1.0f)
    }

    fun playRandomDeath() {
        val deathIndex = Random.nextInt(1, 6)
        playSfx("death$deathIndex", volume = 0.9f)
    }

    fun playSargeQuote() {
        val quotes = listOf("cmonboy", "getsome", "bringit", "niceshot", "yeah")
        val quote = quotes.random()
        playSfx(quote, volume = 1.0f)
    }

    fun startJetpackThrust() {
        if (!isJetpackPlaying) {
            val soundId = soundMap["thrust"] ?: return
            jetpackStreamId = soundPool.play(soundId, 0.45f, 0.45f, 2, -1, 1.0f)
            isJetpackPlaying = true
        }
    }

    fun stopJetpackThrust() {
        if (isJetpackPlaying) {
            if (jetpackStreamId != 0) {
                soundPool.stop(jetpackStreamId)
                jetpackStreamId = 0
            }
            isJetpackPlaying = false
        }
    }

    fun startThemeMusic() {
        if (mediaPlayer == null) {
            try {
                val afd = context.assets.openFd("audio/presMix.mp3")
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    isLooping = true
                    setVolume(0.35f, 0.35f)
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not start theme music: ${e.message}")
            }
        } else if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun stopThemeMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    fun release() {
        stopJetpackThrust()
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        soundPool.release()
        soundMap.clear()
    }
}
