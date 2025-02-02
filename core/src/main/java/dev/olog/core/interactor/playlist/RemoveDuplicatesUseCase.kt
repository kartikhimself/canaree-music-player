package dev.olog.core.interactor.playlist

import dev.olog.core.MediaId
import dev.olog.core.gateway.ImageVersionGateway
import dev.olog.core.gateway.podcast.PodcastPlaylistGateway
import dev.olog.core.gateway.track.PlaylistGateway
import javax.inject.Inject

class RemoveDuplicatesUseCase @Inject constructor(
    private val playlistGateway: PlaylistGateway,
    private val podcastPlaylistGateway: PodcastPlaylistGateway,
    private val imageVersionGateway: ImageVersionGateway

) {

    suspend operator fun invoke(mediaId: MediaId) {
        val playlistId = mediaId.resolveId

        imageVersionGateway.increaseCurrentVersion(mediaId)

        if (mediaId.isPodcastPlaylist) {
            return podcastPlaylistGateway.removeDuplicated(playlistId)
        }
        return playlistGateway.removeDuplicated(playlistId)
    }
}