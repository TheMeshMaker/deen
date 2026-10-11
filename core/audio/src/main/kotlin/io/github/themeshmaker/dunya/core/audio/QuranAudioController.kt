package io.github.themeshmaker.dunya.core.audio

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.themeshmaker.dunya.core.model.Reciter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * What is being recited. [ayah] 0 is the Bismillah that opens a surah (its own recording, 1:1).
 * [positionMs] is within the current verse's recording, refreshed many times a second while playing.
 */
data class QuranAudioState(
    val surah: Int? = null,
    val ayah: Int? = null,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0,
    val reciter: Reciter = Reciter.ALAFASY,
    /** Couldn't load the recitation (usually no internet). */
    val error: Boolean = false
) {
    val active: Boolean get() = surah != null
}

/** The app's handle on [QuranPlaybackService]: play a surah from a verse, pause, skip, follow along. */
@Singleton
class QuranAudioController @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(QuranAudioState())
    val state: StateFlow<QuranAudioState> = _state

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh(player)

        override fun onPlayerError(error: PlaybackException) {
            Log.w(TAG, "Recitation failed", error)
            _state.update { it.copy(error = true, playing = false, buffering = false) }
        }
    }

    /**
     * Plays [surah] from [fromAyah] to its end. A surah that opens with the Bismillah starts with
     * it when played from the first verse.
     */
    fun play(surah: Int, fromAyah: Int, ayahCount: Int, reciter: Reciter, title: String, reciterName: String) {
        scope.launch {
            val player = connect() ?: return@launch
            val items = buildList {
                if (fromAyah == 1 && surah != 1 && surah != 9) add(item(reciter, 1, 1, "$surah:0", title, reciterName))
                for (ayah in fromAyah.coerceIn(1, ayahCount)..ayahCount) add(item(reciter, surah, ayah, "$surah:$ayah", title, reciterName))
            }
            _state.update {
                it.copy(reciter = reciter, error = false, surah = surah, ayah = if (items.first().mediaId.endsWith(":0")) 0 else fromAyah)
            }
            player.setMediaItems(items)
            player.prepare()
            player.play()
        }
    }

    fun toggle() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        controller?.takeIf { it.hasNextMediaItem() }?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.takeIf { it.hasPreviousMediaItem() }?.seekToPreviousMediaItem()
    }

    fun stop() {
        controller?.run {
            stop()
            clearMediaItems()
        }
        _state.value = QuranAudioState(reciter = _state.value.reciter)
    }

    private suspend fun connect(): MediaController? {
        controller?.let { return it }
        return try {
            val token = SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
            MediaController.Builder(context, token).buildAsync().await().also {
                it.addListener(listener)
                controller = it
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not connect to the player", e)
            _state.update { it.copy(error = true) }
            null
        }
    }

    private fun refresh(player: Player) {
        val parts = player.currentMediaItem?.mediaId?.split(':')
        _state.update {
            it.copy(
                surah = parts?.getOrNull(0)?.toIntOrNull(),
                ayah = parts?.getOrNull(1)?.toIntOrNull(),
                playing = player.isPlaying,
                buffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = player.currentPosition.coerceAtLeast(0),
                error = it.error && player.playerError != null
            )
        }
        if (player.isPlaying) startTicker(player) else ticker?.cancel()
    }

    /** ~30 times a second while playing: enough for the word highlight to feel exact. */
    private fun startTicker(player: Player) {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive && player.isPlaying) {
                _state.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0)) }
                delay(TICK_MS)
            }
        }
    }

    private fun item(reciter: Reciter, surah: Int, ayah: Int, id: String, title: String, reciterName: String): MediaItem {
        val file = String.format(Locale.ROOT, "%03d%03d", surah, ayah)
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri("https://everyayah.com/data/${reciter.id}/$file.mp3")
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(reciterName).build())
            .build()
    }

    private companion object {
        const val TAG = "QuranAudio"
        const val TICK_MS = 33L
    }
}
