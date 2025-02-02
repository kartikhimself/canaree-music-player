package dev.olog.service.music.state

import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.util.Log
import dev.olog.core.prefs.MusicPreferencesGateway
import dev.olog.injection.dagger.PerService
import javax.inject.Inject
import kotlin.properties.Delegates

@PerService
internal class MusicServiceShuffleMode @Inject constructor(
    private val mediaSession: MediaSessionCompat,
    private val musicPreferencesUseCase: MusicPreferencesGateway
) {

    companion object {
        @JvmStatic
        private val TAG = "SM:${MusicServiceShuffleMode::class.java.simpleName}"
    }

    private var state by Delegates.observable(SHUFFLE_MODE_INVALID) { _, _, new ->
        musicPreferencesUseCase.setShuffleMode(new)
        mediaSession.setShuffleMode(new)
    }

    init {
        state = musicPreferencesUseCase.getShuffleMode()
        Log.v(TAG, "setup state=$state")
    }

    fun isEnabled(): Boolean = state != SHUFFLE_MODE_NONE

    fun setEnabled(enabled: Boolean) {
        Log.v(TAG, "set enabled=$enabled")
        val shuffleMode = if (enabled) SHUFFLE_MODE_ALL else SHUFFLE_MODE_NONE
        musicPreferencesUseCase.setShuffleMode(shuffleMode)
        mediaSession.setShuffleMode(shuffleMode)
    }

    /**
     * @return true if new shuffle state is enabled
     */
    fun update(): Boolean {
        val oldState = state

        this.state = if (oldState == SHUFFLE_MODE_NONE) {
            SHUFFLE_MODE_ALL
        } else {
            SHUFFLE_MODE_NONE
        }

        Log.v(TAG, "update old state=$oldState, new state=${this.state}")

        return this.state != SHUFFLE_MODE_NONE
    }

}
