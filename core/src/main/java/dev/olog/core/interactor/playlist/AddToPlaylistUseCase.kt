package dev.olog.core.interactor.playlist

import dev.olog.core.MediaId
import dev.olog.core.entity.track.Playlist
import dev.olog.core.gateway.ImageVersionGateway
import dev.olog.core.gateway.podcast.PodcastPlaylistGateway
import dev.olog.core.gateway.track.PlaylistGateway
import dev.olog.core.interactor.songlist.GetSongListByParamUseCase
import javax.inject.Inject

class AddToPlaylistUseCase @Inject constructor(
    private val playlistGateway: PlaylistGateway,
    private val podcastPlaylistGateway: PodcastPlaylistGateway,
    private val getSongListByParamUseCase: GetSongListByParamUseCase,
    private val imageVersionGateway: ImageVersionGateway

) {

    suspend operator fun invoke(playlist: Playlist, mediaId: MediaId) {
        imageVersionGateway.increaseCurrentVersion(mediaId)

        if (mediaId.isLeaf && mediaId.isPodcast) {
            podcastPlaylistGateway.addSongsToPlaylist(playlist.id, listOf(mediaId.resolveId))
            return
        }

        if (mediaId.isLeaf) {
            playlistGateway.addSongsToPlaylist(playlist.id, listOf(mediaId.resolveId))
            return
        }

        val songList = getSongListByParamUseCase(mediaId).map { it.id }
        if (mediaId.isAnyPodcast) {
            podcastPlaylistGateway.addSongsToPlaylist(playlist.id, songList)
        } else {
            playlistGateway.addSongsToPlaylist(playlist.id, songList)
        }
    }
}