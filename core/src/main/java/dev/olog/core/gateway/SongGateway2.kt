package dev.olog.core.gateway

import android.net.Uri
import dev.olog.core.entity.track.Song
import io.reactivex.Completable

interface SongGateway2 : BaseGateway2<Song, Id> {

    fun deleteSingle(id: Id): Completable
    fun deleteGroup(ids: List<Song>): Completable

    fun getByUri(uri: Uri): Song?

}