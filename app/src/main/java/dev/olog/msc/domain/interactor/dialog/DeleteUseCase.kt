package dev.olog.msc.domain.interactor.dialog

import dev.olog.core.MediaId
import dev.olog.core.executor.IoScheduler
import dev.olog.core.gateway.PlaylistGateway2
import dev.olog.core.gateway.PodcastGateway2
import dev.olog.core.gateway.SongGateway2
import dev.olog.core.interactor.CompletableUseCaseWithParam
import dev.olog.msc.domain.interactor.all.GetSongListByParamUseCase
import io.reactivex.Completable
import javax.inject.Inject

class DeleteUseCase @Inject constructor(
    scheduler: IoScheduler,
    private val playlistGateway: PlaylistGateway2,
    private val podcastGateway: PodcastGateway2,
    private val songGateway: SongGateway2,
    private val getSongListByParamUseCase: GetSongListByParamUseCase

) : CompletableUseCaseWithParam<MediaId>(scheduler) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun buildUseCaseObservable(mediaId: MediaId): Completable {
        if (mediaId.isLeaf && mediaId.isPodcast){
            return podcastGateway.deleteSingle(mediaId.resolveId)
        }

        if (mediaId.isLeaf) {
            return songGateway.deleteSingle(mediaId.resolveId)
        }

        return when {
            mediaId.isPodcastPlaylist -> playlistGateway.deletePlaylist(mediaId.categoryValue.toLong())
            mediaId.isPlaylist -> playlistGateway.deletePlaylist(mediaId.categoryValue.toLong())
            else -> getSongListByParamUseCase.execute(mediaId)
                    .flatMapCompletable { songGateway.deleteGroup(it) }
        }
    }
}