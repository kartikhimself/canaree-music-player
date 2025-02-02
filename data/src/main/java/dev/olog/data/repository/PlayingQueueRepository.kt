package dev.olog.data.repository

import dev.olog.core.entity.PlayingQueueSong
import dev.olog.core.entity.track.Song
import dev.olog.core.gateway.PlayingQueueGateway
import dev.olog.core.gateway.podcast.PodcastGateway
import dev.olog.core.gateway.track.SongGateway
import dev.olog.core.interactor.UpdatePlayingQueueUseCaseRequest
import dev.olog.data.db.dao.AppDatabase
import dev.olog.data.utils.assertBackground
import dev.olog.data.utils.assertBackgroundThread
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class PlayingQueueRepository @Inject constructor(
    database: AppDatabase,
    private val songGateway: SongGateway,
    private val podcastGateway: PodcastGateway

) : PlayingQueueGateway {

    private val playingQueueDao = database.playingQueueDao()

    override fun getAll(): List<PlayingQueueSong> {
        assertBackgroundThread()
        val playingQueue =
            playingQueueDao.getAllAsSongs(songGateway.getAll(), podcastGateway.getAll())
        if (playingQueue.isNotEmpty()) {
            return playingQueue
        }
        return songGateway.getAll().mapIndexed { index, song -> song.toPlayingQueueSong(index) }
    }

    override fun observeAll(): Flow<List<PlayingQueueSong>> {
        return playingQueueDao.observeAllAsSongs(songGateway, podcastGateway)
            .assertBackground()
    }

    override fun update(list: List<UpdatePlayingQueueUseCaseRequest>) {
        assertBackgroundThread()
        playingQueueDao.insert(list)
    }

    private fun Song.toPlayingQueueSong(progressive: Int): PlayingQueueSong {
        return PlayingQueueSong(
            this.copy(idInPlaylist = progressive),
            getMediaId()
        )
    }

}
