package dev.olog.core.gateway

import dev.olog.core.entity.track.Song
import io.reactivex.Completable

interface PodcastGateway2 : BaseGateway2<Song, Id> {

    fun deleteSingle(id: Id): Completable

}