package dev.olog.data.db.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.olog.data.db.entities.*


@Database(
    entities = arrayOf(
        PlayingQueueEntity::class,
        FolderMostPlayedEntity::class,
        PlaylistMostPlayedEntity::class,
        GenreMostPlayedEntity::class,

        FavoriteEntity::class,
        FavoritePodcastEntity::class,

        RecentSearchesEntity::class,

        HistoryEntity::class,
        PodcastHistoryEntity::class,

        LastPlayedAlbumEntity::class,
        LastPlayedArtistEntity::class,
        LastPlayedPodcastAlbumEntity::class,
        LastPlayedPodcastArtistEntity::class,

        LastFmTrackEntity::class,
        LastFmAlbumEntity::class,
        LastFmArtistEntity::class,

        OfflineLyricsEntity::class,

        UsedTrackImageEntity::class,
        UsedAlbumImageEntity::class,
        UsedArtistImageEntity::class,

        PlaylistEntity::class,
        PlaylistTrackEntity::class,

        PodcastPlaylistEntity::class,
        PodcastPlaylistTrackEntity::class,

        PodcastPositionEntity::class,

        ImageVersionEntity::class,
        LyricsSyncAdjustmentEntity::class,
        EqualizerPresetEntity::class

    ), version = 16, exportSchema = true
)
@TypeConverters(CustomTypeConverters::class)
internal abstract class AppDatabase : RoomDatabase() {

    abstract fun playingQueueDao(): PlayingQueueDao

    abstract fun folderMostPlayedDao(): FolderMostPlayedDao

    abstract fun playlistMostPlayedDao(): PlaylistMostPlayedDao

    abstract fun genreMostPlayedDao(): GenreMostPlayedDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun recentSearchesDao(): RecentSearchesDao

    abstract fun historyDao(): HistoryDao

    abstract fun lastPlayedAlbumDao(): LastPlayedAlbumDao
    abstract fun lastPlayedArtistDao(): LastPlayedArtistDao
    abstract fun lastPlayedPodcastArtistDao(): LastPlayedPodcastArtistDao
    abstract fun lastPlayedPodcastAlbumDao(): LastPlayedPodcastAlbumDao

    abstract fun lastFmDao(): LastFmDao

    abstract fun offlineLyricsDao(): OfflineLyricsDao

    abstract fun usedImageDao(): UsedImageDao

    abstract fun playlistDao(): PlaylistDao
    abstract fun podcastPlaylistDao(): PodcastPlaylistDao

    abstract fun podcastPositionDao(): PodcastPositionDao

    abstract fun imageVersionDao(): ImageVersionDao
    abstract fun lyricsSyncAdjustmentDao(): LyricsSyncAdjustmentDao
    abstract fun equalizerPresetsDao(): EqualizerPresetsDao
}