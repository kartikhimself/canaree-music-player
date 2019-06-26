package dev.olog.data.queries

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore.Audio.Genres.*
import dev.olog.contentresolversql.querySql
import dev.olog.core.gateway.Id
import dev.olog.core.prefs.BlacklistPreferences
import dev.olog.core.prefs.SortPreferences

internal class GenreQueries(
    private val contentResolver: ContentResolver,
    blacklistPrefs: BlacklistPreferences,
    sortPrefs: SortPreferences
) : BaseQueries(blacklistPrefs, sortPrefs, false) {

    fun getAll(): Cursor {

        val query = """
            SELECT $_ID, $NAME
            FROM $EXTERNAL_CONTENT_URI
            ORDER BY $DEFAULT_SORT_ORDER
        """

        return contentResolver.querySql(query)
    }

    fun getById(id: Id): Cursor {

        val query = """
            SELECT $_ID, $NAME
            FROM $EXTERNAL_CONTENT_URI
            WHERE $_ID = ?
            ORDER BY $DEFAULT_SORT_ORDER
        """

        return contentResolver.querySql(query)
    }

    fun countGenreSize(genreId: Id): Cursor {
        // TODO remove playlist with 0 tracks if possibile
        val query = """
            SELECT ${Members._ID}, ${Members.AUDIO_ID}
            FROM ${Members.getContentUri("external", genreId)}
            WHERE ${defaultSelection()}
        """
        return contentResolver.querySql(query)
    }

    fun getRelatedArtists(genreId: Id): Cursor {
        val query = """
             SELECT
                ${Members.ARTIST_ID},
                ${Members.ARTIST},
                ${Columns.ALBUM_ARTIST},
                ${Members.IS_PODCAST}
            FROM ${Members.getContentUri("external", genreId)}
            WHERE ${defaultSelection()}

            ORDER BY lower(${Members.ARTIST}) COLLATE UNICODE ASC
        """

        return contentResolver.querySql(query, arrayOf(genreId.toString()))
    }

    private fun defaultSelection(): String {
        return "${isPodcast()} AND ${notBlacklisted()}"
    }

}