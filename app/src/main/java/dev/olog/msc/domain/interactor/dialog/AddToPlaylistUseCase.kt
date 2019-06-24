package dev.olog.msc.domain.interactor.dialog

import dev.olog.core.entity.track.Playlist
import dev.olog.core.executor.IoScheduler
import dev.olog.msc.domain.gateway.PodcastPlaylistGateway
import dev.olog.msc.domain.interactor.all.GetSongListByParamUseCase
import dev.olog.core.interactor.CompletableUseCaseWithParam
import dev.olog.msc.domain.interactor.item.GetPodcastUseCase
import dev.olog.msc.domain.interactor.item.GetSongUseCase
import dev.olog.core.MediaId
import dev.olog.core.gateway.PlaylistGateway2
import dev.olog.shared.mapToList
import io.reactivex.Completable
import javax.inject.Inject

class AddToPlaylistUseCase @Inject constructor(
    scheduler: IoScheduler,
    private val playlistGateway: PlaylistGateway2,
    private val getSongUseCase: GetSongUseCase,
    private val podcastPlaylistGateway: PodcastPlaylistGateway,
    private val getPodcastUseCase: GetPodcastUseCase,
    private val getSongListByParamUseCase: GetSongListByParamUseCase

) : CompletableUseCaseWithParam<Pair<Playlist, MediaId>>(scheduler) {

    override fun buildUseCaseObservable(param: Pair<Playlist, MediaId>): Completable {
        val (playlist, mediaId) = param

        if (mediaId.isLeaf && mediaId.isPodcast){
            return getPodcastUseCase.execute(mediaId)
                    .firstOrError()
                    .flatMapCompletable { podcastPlaylistGateway.addSongsToPlaylist(playlist.id, listOf(mediaId.resolveId)) }
        }

        if (mediaId.isLeaf) {
            return getSongUseCase.execute(mediaId)
                    .firstOrError()
                    .flatMapCompletable { playlistGateway.addSongsToPlaylist(playlist.id, listOf(mediaId.resolveId)) }
        }

        return getSongListByParamUseCase.execute(mediaId)
                .firstOrError()
                .mapToList { it.id }
                .flatMapCompletable {
                    if (mediaId.isAnyPodcast){
                        podcastPlaylistGateway.addSongsToPlaylist(playlist.id, it)
                    } else {
                        playlistGateway.addSongsToPlaylist(playlist.id, it)
                    }

                }
    }
}