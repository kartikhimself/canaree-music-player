package dev.olog.core.entity.track

import dev.olog.core.MediaId
import dev.olog.core.MediaIdCategory
import java.io.File

data class Song(
    @JvmField
    val id: Long,
    @JvmField
    val artistId: Long,
    @JvmField
    val albumId: Long,
    @JvmField
    val title: String,
    @JvmField
    val artist: String,
    @JvmField
    val albumArtist: String,
    @JvmField
    val album: String,
    @JvmField
    val duration: Long,
    @JvmField
    val dateAdded: Long,
    @JvmField
    val dateModified: Long,
    @JvmField
    val path: String,
    @JvmField
    val trackColumn: Int,
    @JvmField
    val idInPlaylist: Int,
    @JvmField
    val isPodcast: Boolean

) {


    val discNumber: Int
        get() {
            if (trackColumn >= 1000) {
                return trackColumn / 1000
            }
            return 0
        }

    val trackNumber: Int
        get() {
            if (trackColumn >= 1000) {
                return trackColumn % 1000
            }
            return trackColumn
        }

    val folderPath: String
        get() = path.substring(0, path.lastIndexOf(File.separator))

    fun getMediaId(): MediaId {
        val category = if (isPodcast) MediaIdCategory.PODCASTS else MediaIdCategory.SONGS
        val mediaId = MediaId.createCategoryValue(category, "")
        return MediaId.playableItem(mediaId, id)
    }

    fun getAlbumMediaId(): MediaId {
        val category = if (isPodcast) MediaIdCategory.PODCASTS_ALBUMS else MediaIdCategory.ALBUMS
        return MediaId.createCategoryValue(category, this.albumId.toString())
    }

    fun getArtistMediaId(): MediaId {
        val category = if (isPodcast) MediaIdCategory.PODCASTS_ARTISTS else MediaIdCategory.ARTISTS
        return MediaId.createCategoryValue(category, this.artistId.toString())
    }

    fun withInInPlaylist(idInPlaylist: Int): Song {
        return Song(
            id = id,
            artistId = artistId,
            albumId = albumId,
            title = title,
            artist = artist,
            albumArtist = albumArtist,
            album = album,
            duration = duration,
            dateAdded = dateAdded,
            dateModified = dateModified,
            path = path,
            trackColumn = trackColumn,
            idInPlaylist = idInPlaylist,
            isPodcast = isPodcast
        )
    }

}