package dev.olog.msc.theme.observer

import android.app.Activity
import android.app.Application
import android.content.Context

internal class CurrentActivityObserver(context: Context) :
    ActivityLifecycleCallbacks {

    override var currentActivity: Activity? = null
        private set

    init {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

}