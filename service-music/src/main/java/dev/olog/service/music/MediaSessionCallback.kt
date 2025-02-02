package dev.olog.service.music

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.KeyEvent
import dev.olog.core.MediaId
import dev.olog.core.gateway.FavoriteGateway
import dev.olog.injection.dagger.PerService
import dev.olog.service.music.interfaces.IPlayer
import dev.olog.service.music.interfaces.IQueue
import dev.olog.service.music.model.PlayerMediaEntity
import dev.olog.service.music.model.SkipType
import dev.olog.service.music.queue.SKIP_TO_PREVIOUS_THRESHOLD
import dev.olog.service.music.state.MusicServicePlaybackState
import dev.olog.service.music.state.MusicServiceRepeatMode
import dev.olog.service.music.state.MusicServiceShuffleMode
import dev.olog.shared.CustomScope
import dev.olog.intents.MusicServiceCustomAction
import dev.olog.shared.android.utils.assertBackgroundThread
import kotlinx.coroutines.*
import javax.inject.Inject

@PerService
internal class MediaSessionCallback @Inject constructor(
    private val queue: IQueue,
    private val player: IPlayer,
    private val repeatMode: MusicServiceRepeatMode,
    private val shuffleMode: MusicServiceShuffleMode,
    private val mediaButton: MediaButton,
    private val playerState: MusicServicePlaybackState,
    private val favoriteGateway: FavoriteGateway

) : MediaSessionCompat.Callback(), CoroutineScope by CustomScope() {

    companion object {
        @JvmStatic
        private val TAG = "SM:${MediaSessionCallback::class.java.simpleName}"
    }

    private var retrieveDataJob: Job? = null

    override fun onPrepare() {
        launch(Dispatchers.Main) {
            val track = queue.prepare()
            if (track != null) {
                player.prepare(track)
            }
            Log.v(TAG, "onPrepare with track=${track?.mediaEntity?.title}")
        }
    }

    private fun retrieveAndPlay(retrieve: suspend () -> PlayerMediaEntity?) {
        retrieveDataJob?.cancel()
        retrieveDataJob = launch {
            assertBackgroundThread()
            val entity = retrieve()
            if (entity != null) {
                withContext(Dispatchers.Main) {
                    player.play(entity)
                }
            } else {
                onEmptyQueue()
            }
        }
    }

    private fun onEmptyQueue() {
        // TODO
    }

    override fun onPlayFromMediaId(stringMediaId: String, extras: Bundle?) {
        Log.v(TAG, "onPlayFromMediaId mediaId=$stringMediaId, extras=$extras")

        retrieveAndPlay {
            updatePodcastPosition()

            when (val mediaId = MediaId.fromString(stringMediaId)) {
                MediaId.shuffleId() -> {
                    // android auto call 'onPlayFromMediaId' with 'MediaId.shuffleId()'
                    queue.handlePlayShuffle(mediaId, null)
                }
                else -> {
                    val filter = extras?.getString(MusicServiceCustomAction.ARGUMENT_FILTER)
                    queue.handlePlayFromMediaId(mediaId, filter)
                }
            }
        }
    }

    override fun onPlay() {
        Log.v(TAG, "onPlay")
        player.resume()
    }

    override fun onPlayFromSearch(query: String, extras: Bundle) {
        Log.v(TAG, "onPlayFromSearch query=$query, extras=$extras")

        retrieveAndPlay {
            updatePodcastPosition()
            queue.handlePlayFromGoogleSearch(query, extras)
        }
    }

    override fun onPlayFromUri(uri: Uri, extras: Bundle?) {
        Log.v(TAG, "onPlayFromUri uri=$uri, extras=$extras")

        retrieveAndPlay {
            updatePodcastPosition()
            queue.handlePlayFromUri(uri)
        }
    }

    override fun onPause() {
        Log.v(TAG, "onPause")
        updatePodcastPosition()
        player.pause(true)
    }

    override fun onStop() {
        Log.v(TAG, "onStop")
        onPause()
    }

    override fun onSkipToNext() {
        Log.v(TAG, "onSkipToNext")
        onSkipToNext(false)
    }

    override fun onSkipToPrevious() {
        launch(Dispatchers.Main) {
            Log.v(TAG, "onSkipToPrevious")

            updatePodcastPosition()
            queue.handleSkipToPrevious(player.getBookmark())?.let { metadata ->

                val skipType = if (!metadata.mediaEntity.isPodcast && player.getBookmark() > SKIP_TO_PREVIOUS_THRESHOLD)
                        SkipType.RESTART else SkipType.SKIP_PREVIOUS
                player.playNext(metadata, skipType)
            }
        }
    }

    private fun onTrackEnded() {
        Log.v(TAG, "onTrackEnded")
        onSkipToNext(true)
    }

    /**
     * Try to skip to next song, if can't, restart current and pause
     */
    private fun onSkipToNext(trackEnded: Boolean) = launch(Dispatchers.Main) {
        Log.v(TAG, "onSkipToNext internal track ended=$trackEnded")
        updatePodcastPosition()
        val metadata = queue.handleSkipToNext(trackEnded)
        if (metadata != null) {
            val skipType = if (trackEnded) SkipType.TRACK_ENDED else SkipType.SKIP_NEXT
            player.playNext(metadata, skipType)
        } else {
            val currentSong = queue.getPlayingSong()
            if (currentSong != null) {
                player.play(currentSong)
                player.pause(true)
                player.seekTo(0L)
            } else {
                onEmptyQueue()
            }
        }
    }

    override fun onSkipToQueueItem(id: Long) {
        launch(Dispatchers.Main) {
            Log.v(TAG, "onSkipToQueueItem id=$id")

            updatePodcastPosition()
            val mediaEntity = queue.handleSkipToQueueItem(id)
            if (mediaEntity != null) {
                player.play(mediaEntity)
            } else {
                onEmptyQueue()
            }
        }
    }

    override fun onSeekTo(pos: Long) {
        Log.v(TAG, "onSeekTo pos=$pos")
        updatePodcastPosition()
        player.seekTo(pos)
    }

    override fun onSetRating(rating: RatingCompat?) {
        onSetRating(rating, null)
    }

    override fun onSetRating(rating: RatingCompat?, extras: Bundle?) {
        Log.v(TAG, "onSetRating rating=$rating, extras=$extras")
        launch { favoriteGateway.toggleFavorite() }
    }

    override fun onCustomAction(action: String, extras: Bundle?) {
        Log.v(TAG, "onCustomAction action=$action, extras=$extras")

        when (MusicServiceCustomAction.valueOf(action)) {
            MusicServiceCustomAction.SHUFFLE -> {
                requireNotNull(extras)
                val mediaId = extras.getString(MusicServiceCustomAction.ARGUMENT_MEDIA_ID)!!
                val filter = extras.getString(MusicServiceCustomAction.ARGUMENT_FILTER)
                retrieveAndPlay {
                    updatePodcastPosition()
                    queue.handlePlayShuffle(MediaId.fromString(mediaId), filter)
                }
            }
            MusicServiceCustomAction.SWAP -> {
                requireNotNull(extras)
                val from = extras.getInt(MusicServiceCustomAction.ARGUMENT_SWAP_FROM, 0)
                val to = extras.getInt(MusicServiceCustomAction.ARGUMENT_SWAP_TO, 0)
                queue.handleSwap(from, to)
            }
            MusicServiceCustomAction.SWAP_RELATIVE -> {
                requireNotNull(extras)
                val from = extras.getInt(MusicServiceCustomAction.ARGUMENT_SWAP_FROM, 0)
                val to = extras.getInt(MusicServiceCustomAction.ARGUMENT_SWAP_TO, 0)
                queue.handleSwapRelative(from, to)
            }
            MusicServiceCustomAction.REMOVE -> {
                requireNotNull(extras)
                val position = extras.getInt(MusicServiceCustomAction.ARGUMENT_POSITION, -1)
                queue.handleRemove(position)
            }
            MusicServiceCustomAction.REMOVE_RELATIVE -> {
                requireNotNull(extras)
                val position = extras.getInt(MusicServiceCustomAction.ARGUMENT_POSITION, -1)
                queue.handleRemoveRelative(position)
            }
            MusicServiceCustomAction.PLAY_RECENTLY_ADDED -> {
                requireNotNull(extras)
                val mediaId = extras.getString(MusicServiceCustomAction.ARGUMENT_MEDIA_ID)!!
                retrieveAndPlay {
                    updatePodcastPosition()
                    queue.handlePlayRecentlyAdded(MediaId.fromString(mediaId))
                }
            }
            MusicServiceCustomAction.PLAY_MOST_PLAYED -> {
                requireNotNull(extras)
                val mediaId = extras.getString(MusicServiceCustomAction.ARGUMENT_MEDIA_ID)!!
                retrieveAndPlay {
                    updatePodcastPosition()
                    queue.handlePlayMostPlayed(MediaId.fromString(mediaId))
                }
            }
            MusicServiceCustomAction.FORWARD_10 -> player.forwardTenSeconds()
            MusicServiceCustomAction.FORWARD_30 -> player.forwardThirtySeconds()
            MusicServiceCustomAction.REPLAY_10 -> player.replayTenSeconds()
            MusicServiceCustomAction.REPLAY_30 -> player.replayThirtySeconds()
            MusicServiceCustomAction.TOGGLE_FAVORITE -> onSetRating(null)
            MusicServiceCustomAction.ADD_TO_PLAY_LATER -> {
                launch {
                    requireNotNull(extras)
                    val mediaIds =
                        extras.getLongArray(MusicServiceCustomAction.ARGUMENT_MEDIA_ID_LIST)!!
                    val isPodcast = extras.getBoolean(MusicServiceCustomAction.ARGUMENT_IS_PODCAST)

                    val position = queue.playLater(mediaIds.toList(), isPodcast)
                    playerState.toggleSkipToActions(position)
                }
            }
            MusicServiceCustomAction.ADD_TO_PLAY_NEXT -> {
                launch {
                    requireNotNull(extras)
                    val mediaIds =
                        extras.getLongArray(MusicServiceCustomAction.ARGUMENT_MEDIA_ID_LIST)!!
                    val isPodcast = extras.getBoolean(MusicServiceCustomAction.ARGUMENT_IS_PODCAST)

                    val position = queue.playNext(mediaIds.toList(), isPodcast)
                    playerState.toggleSkipToActions(position)
                }
            }
            MusicServiceCustomAction.MOVE_RELATIVE -> {
                requireNotNull(extras)
                val position = extras.getInt(MusicServiceCustomAction.ARGUMENT_POSITION)
                queue.handleMoveRelative(position)
            }
        }
    }

    override fun onSetRepeatMode(repeatMode: Int) {
        Log.v(TAG, "onSetRepeatMode")

        this.repeatMode.update()
        playerState.toggleSkipToActions(queue.getCurrentPositionInQueue())
        queue.onRepeatModeChanged()
    }

    override fun onSetShuffleMode(unused: Int) {
        Log.v(TAG, "onSetShuffleMode")

        val newShuffleMode = this.shuffleMode.update()
        if (newShuffleMode) {
            queue.shuffle()
        } else {
            queue.sort()
        }
        playerState.toggleSkipToActions(queue.getCurrentPositionInQueue())
    }

    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
        val event = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)!!
        Log.v(TAG, "onMediaButtonEvent, action=${event.action}, keycode=${event.keyCode}")
        if (event.action == KeyEvent.ACTION_DOWN) {

            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> handlePlayPause()
                KeyEvent.KEYCODE_MEDIA_NEXT -> onSkipToNext()
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> onSkipToPrevious()
                KeyEvent.KEYCODE_MEDIA_STOP -> player.stopService()
                KeyEvent.KEYCODE_MEDIA_PAUSE -> player.pause(false)
                KeyEvent.KEYCODE_MEDIA_PLAY -> onPlay()
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> onTrackEnded()
                KeyEvent.KEYCODE_HEADSETHOOK -> mediaButton.onHeatSetHookClick()
                else -> throw IllegalArgumentException("not handled")
            }
        }

        return true
    }

    /**
     * this function DO NOT KILL service on pause
     */
    fun handlePlayPause() {
        Log.v(TAG, "handlePlayPause")

        if (player.isPlaying()) {
            player.pause(false)
        } else {
            onPlay()
        }
    }

    private fun updatePodcastPosition() {
        Log.v(TAG, "updatePodcastPosition")

        GlobalScope.launch {
            val bookmark = withContext(Dispatchers.Main) { player.getBookmark() }
            queue.updatePodcastPosition(bookmark)
        }
    }

}