/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.themeshmaker.dunya.core.model.AdhanVoice
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The adhan recordings: the bundled ones (docs/CONTENT_SOURCES.md) and up to two the user picked
 * (one for Fajr, one for the other prayers). Picked files are *copied* into device-protected
 * storage, so they still play before the first unlock after a reboot and if the original is deleted.
 */
@Singleton
class AdhanVoices @Inject constructor(@ApplicationContext private val context: Context) {

    data class Bundled(val id: String, @StringRes val name: Int, val seconds: Int)

    enum class ImportResult {
        OK,
        TOO_LARGE,
        NOT_AUDIO,
        FAILED
    }

    val bundled: List<Bundled> = listOf(
        Bundled(AdhanVoice.DEFAULT, R.string.core_alarm_voice_default, 154),
        Bundled(AdhanVoice.TRADITIONAL, R.string.core_alarm_voice_traditional, 183),
        Bundled(AdhanVoice.AAQIB, R.string.core_alarm_voice_aaqib, 87)
    )

    private val dir: File get() = File(context.createDeviceProtectedStorageContext().filesDir, "adhan")

    private fun customFile(fajr: Boolean) = File(dir, if (fajr) "custom_fajr" else "custom")

    private fun nameFile(fajr: Boolean) = File(dir, if (fajr) "custom_fajr.name" else "custom.name")

    fun hasCustom(fajr: Boolean): Boolean = customFile(fajr).length() > 0

    /** The picked file's original name, for display. */
    fun customName(fajr: Boolean): String? = nameFile(fajr).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }

    suspend fun importCustom(uri: Uri, fajr: Boolean): ImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var displayName: String? = null
        var size = -1L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(0)
                    size = if (cursor.isNull(1)) -1 else cursor.getLong(1)
                }
            }
        }
        if (size > MAX_BYTES) return@withContext ImportResult.TOO_LARGE

        dir.mkdirs()
        val temp = File(dir, "import.tmp")
        try {
            val copied = resolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext ImportResult.FAILED
            if (copied > MAX_BYTES) return@withContext ImportResult.TOO_LARGE
            if (!isPlayableAudio(temp)) return@withContext ImportResult.NOT_AUDIO
            val target = customFile(fajr)
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
            }
            nameFile(fajr).writeText(displayName.orEmpty())
            ImportResult.OK
        } catch (e: IOException) {
            Log.w(TAG, "Could not import adhan", e)
            ImportResult.FAILED
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not read adhan", e)
            ImportResult.FAILED
        } finally {
            temp.delete()
        }
    }

    private fun isPlayableAudio(file: File): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            val millis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            hasAudio && millis in MIN_MILLIS..MAX_MILLIS
        } catch (_: RuntimeException) {
            false
        } finally {
            runCatching { retriever.release() }
        }
    }

    @RawRes
    private fun rawFor(voice: String): Int = when (voice) {
        AdhanVoice.TRADITIONAL -> R.raw.adhan_traditional
        AdhanVoice.AAQIB -> R.raw.adhan_aaqib
        else -> R.raw.adhan_default
    }

    /**
     * Points [player] at [voice] for the Fajr or regular slot. A missing custom file falls back to
     * the default recording, so an adhan is never silent.
     */
    internal fun setSource(player: MediaPlayer, voice: String, fajr: Boolean) {
        val custom = customFile(fajr)
        if (voice == AdhanVoice.CUSTOM && custom.length() > 0) {
            player.setDataSource(custom.absolutePath)
        } else {
            player.setDataSource(context, rawUri(if (voice == AdhanVoice.CUSTOM) AdhanVoice.DEFAULT else voice))
        }
    }

    internal fun defaultUri(): Uri = rawUri(AdhanVoice.DEFAULT)

    private fun rawUri(voice: String): Uri = "android.resource://${context.packageName}/${rawFor(voice)}".toUri()

    private companion object {
        const val TAG = "AdhanVoices"
        const val MAX_BYTES = 25L * 1024 * 1024
        const val MIN_MILLIS = 3_000L
        const val MAX_MILLIS = 15 * 60_000L
    }
}

/** Plays a voice in Settings so the user can hear it before choosing. Call [release] when done. */
class AdhanPreview @Inject constructor(private val voices: AdhanVoices) {
    private var player: MediaPlayer? = null

    var playing: String? = null
        private set

    fun play(voice: String, fajr: Boolean, onDone: () -> Unit) {
        stop()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            try {
                voices.setSource(this, voice, fajr)
                setOnCompletionListener {
                    stop()
                    onDone()
                }
                prepare()
                start()
                playing = voice
            } catch (e: Exception) {
                Log.w("AdhanPreview", "Preview failed", e)
                release()
                player = null
                onDone()
            }
        }
    }

    fun stop() {
        player?.runCatching {
            stop()
            release()
        }
        player = null
        playing = null
    }

    fun release() = stop()
}
