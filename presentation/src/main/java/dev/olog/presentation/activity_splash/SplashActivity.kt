package dev.olog.presentation.activity_splash

import android.Manifest
import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import com.tbruyelle.rxpermissions2.RxPermissions
import dagger.Lazy
import dev.olog.presentation.R
import dev.olog.presentation._base.BaseActivity
import dev.olog.presentation.navigation.Navigator
import dev.olog.presentation.utils.extension.asLiveData
import dev.olog.presentation.utils.extension.hasPermission
import dev.olog.presentation.utils.extension.requestStoragePemission
import dev.olog.presentation.utils.extension.subscribe
import dev.olog.shared.unsubscribe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_splash.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SplashActivity : BaseActivity() {

    @Inject lateinit var presenter: SplashActivityPresenter
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var adapter : Lazy<SplashActivityViewPagerAdapter>
    @Inject lateinit var rxPermissions: RxPermissions

    private var timeDisposable : Disposable? = null
    private var prefetchImageDisposable : Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hasStoragePermission = checkStoragePermission()

        if (presenter.isFirstAccess(hasStoragePermission)){
            setContentView(R.layout.activity_splash)
            viewPager.adapter = adapter.get()
            inkIndicator.setViewPager(viewPager)
            setupStorageRequestListener()
        } else {
            navigator.toMainActivity()
        }

    }

    override fun onStop() {
        super.onStop()
        timeDisposable.unsubscribe()
        prefetchImageDisposable.unsubscribe()
    }

    private fun setupStorageRequestListener(){
        RxView.clicks(next)
                .flatMap { if (viewPager.currentItem != 0) {
                    rxPermissions.requestStoragePemission()
                } else {
                    Observable.just(false)
                }}.asLiveData()
                .subscribe(this, { success ->
                    if (success){
                        timeDisposable = Observable.timer(4, TimeUnit.SECONDS)
                                .doOnSubscribe { showLoader() }
                                .doOnSubscribe { startLoadingImages() }
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    loader.pauseAnimation()
                                    navigator.toMainActivity()
                                }, Throwable::printStackTrace)
                    } else if (viewPager.currentItem == 0){
                        viewPager.setCurrentItem(1, true)
                    }
                })
    }

    private fun showLoader(){
        viewPager.visibility = View.GONE
        inkIndicator.visibility = View.GONE
        next.visibility = View.GONE

        val messages = resources.getStringArray(R.array.splash_loading_messages)
        val randomMessage = messages[Random().nextInt(messages.size)]

        loader.visibility = View.VISIBLE
        message.visibility = View.VISIBLE
        message.text = randomMessage
        loader.playAnimation()
    }

    private fun startLoadingImages(){
        prefetchImageDisposable = presenter.prefetchImages().subscribe({}, Throwable::printStackTrace)
    }

    private fun checkStoragePermission() : Boolean {
        return hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

}