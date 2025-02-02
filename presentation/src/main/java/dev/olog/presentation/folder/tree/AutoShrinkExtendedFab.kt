package dev.olog.presentation.folder.tree

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.*

class AutoShrinkExtendedFab(
    context: Context,
    attrs: AttributeSet
) : ExtendedFloatingActionButton(context, attrs), CoroutineScope by MainScope() {

    private var job: Job? = null

    override fun extend() {
        super.extend()
        job?.cancel()
        job = launch {
            delay(5000)
            shrink()
        }
    }

    override fun shrink() {
        super.shrink()
        job?.cancel()
    }

    override fun shrink(animate: Boolean) {
        super.shrink(animate)
        job?.cancel()
    }

    override fun hide() {
        super.hide()
        fastShrink()
    }

    override fun hide(animate: Boolean) {
        super.hide(animate)
        fastShrink()
    }

    override fun show() {
        super.show()
        job?.cancel()
    }

    private fun fastShrink(){
        job?.cancel()
        if (isExtended){
            shrink()
        }
    }

}