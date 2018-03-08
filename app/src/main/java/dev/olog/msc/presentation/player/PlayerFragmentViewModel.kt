package dev.olog.msc.presentation.player

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import dev.olog.msc.R
import dev.olog.msc.domain.entity.FavoriteEnum
import dev.olog.msc.domain.interactor.favorite.ObserveFavoriteAnimationUseCase
import dev.olog.msc.domain.interactor.prefs.AppPreferencesUseCase
import dev.olog.msc.presentation.model.DisplayableItem
import dev.olog.msc.pro.IBilling
import dev.olog.msc.utils.MediaId
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

class PlayerFragmentViewModel @Inject constructor(
        observeFavoriteAnimationUseCase: ObserveFavoriteAnimationUseCase,
        private val billing: IBilling,
        private val appPrefsUseCase: AppPreferencesUseCase

) : ViewModel() {

    private val miniQueue = MutableLiveData<List<DisplayableItem>>()

    fun observeMiniQueue() : LiveData<List<DisplayableItem>> = miniQueue

    fun updateQueue(list: List<DisplayableItem>){
        miniQueue.postValue(list)
    }

    fun observeMiniQueueVisibility(): Observable<Boolean>{
        return Observables.combineLatest(
                billing.observeIsPremium(),
                appPrefsUseCase.observeMiniQueueVisibility(), { premium, show -> premium && show }
        )
    }

    fun observePlayerControlsVisibility(): Observable<Boolean> {
        return Observables.combineLatest(
                billing.observeIsPremium(),
                appPrefsUseCase.observePlayerControlsVisibility(), { premium, show -> premium && show }
        )
    }

    private val progressPublisher = BehaviorSubject.createDefault(0)

    val observeProgress : Observable<Int> = progressPublisher

    fun updateProgress(progress: Int){
        progressPublisher.onNext(progress)
    }

    val footerLoadMore = DisplayableItem(R.layout.item_playing_queue_load_more, MediaId.headerId("load more"), "")

    val playerControls = DisplayableItem(R.layout.fragment_player_controls,
            MediaId.headerId("player controls id"), "")

    val onFavoriteStateChanged: Observable<FavoriteEnum> = observeFavoriteAnimationUseCase.execute()

}