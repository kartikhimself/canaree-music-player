package dev.olog.presentation

import android.graphics.Typeface
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Priority
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import dev.olog.core.MediaId
import dev.olog.core.MediaIdCategory
import dev.olog.core.gateway.getImageVersionGateway
import dev.olog.image.provider.CoverUtils
import dev.olog.image.provider.CustomMediaStoreSignature
import dev.olog.image.provider.GlideApp
import dev.olog.image.provider.GlideUtils
import dev.olog.image.provider.model.AudioFileCover
import dev.olog.presentation.model.DisplayableFile
import dev.olog.presentation.ripple.RippleTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BindingsAdapter {

    @JvmStatic
    fun loadFile(view: ImageView, item: DisplayableFile) {
        val context = view.context
        GlideApp.with(context).clear(view)

        GlideApp.with(context)
                .load(AudioFileCover(item.path!!))
                .override(GlideUtils.OVERRIDE_SMALL)
                .placeholder(CoverUtils.getGradient(context, MediaId.songId(item.path.hashCode().toLong())))
                .into(view)
    }

    @JvmStatic
    fun loadDirImage(view: ImageView, item: DisplayableFile) {
        val mediaId = MediaId.createCategoryValue(MediaIdCategory.FOLDERS, item.path ?: "")
        loadImageImpl(
            view,
            mediaId,
            GlideUtils.OVERRIDE_SMALL
        )
    }

    @JvmStatic
    private fun loadImageImpl(
        view: ImageView,
        mediaId: MediaId,
        override: Int,
        priority: Priority = Priority.HIGH
    ) {
        val context = view.context

        GlideApp.with(context).clear(view)

        GlobalScope.launch(Dispatchers.Main) {
            val version = withContext(Dispatchers.Default){
                context.getImageVersionGateway().getCurrentVersion(mediaId)
            }

            val builder = GlideApp.with(context)
                .load(mediaId)
                .override(override)
                .priority(priority)
                .placeholder(CoverUtils.getGradient(context, mediaId))
                .signature(CustomMediaStoreSignature(mediaId, version))
                .transition(DrawableTransitionOptions.withCrossFade())

                if (mediaId.isLeaf) {
                    builder.into(view)
                } else {
                    builder.into(RippleTarget(view))
                }

        }
    }

    @JvmStatic
    fun loadSongImage(view: ImageView, mediaId: MediaId) {
        loadImageImpl(
            view,
            mediaId,
            GlideUtils.OVERRIDE_SMALL
        )
    }

    @JvmStatic
    fun loadAlbumImage(view: ImageView, mediaId: MediaId) {
        loadImageImpl(
            view,
            mediaId,
            GlideUtils.OVERRIDE_MID,
            Priority.HIGH
        )
    }

    @JvmStatic
    fun loadBigAlbumImage(view: ImageView, mediaId: MediaId) {
        val context = view.context

        GlideApp.with(context).clear(view)

        GlobalScope.launch(Dispatchers.Main) {
            val version = withContext(Dispatchers.Default){
                context.getImageVersionGateway().getCurrentVersion(mediaId)
            }

            GlideApp.with(context)
                .load(mediaId)
                .override(GlideUtils.OVERRIDE_BIG)
                .priority(Priority.IMMEDIATE)
                .placeholder(CoverUtils.onlyGradient(context, mediaId))
                .error(CoverUtils.getGradient(context, mediaId))
                .onlyRetrieveFromCache(true)
                .signature(CustomMediaStoreSignature(mediaId, version))
                .into(RippleTarget(view))
        }
    }

    @JvmStatic
    fun setBoldIfTrue(view: TextView, setBold: Boolean) {
        val style = if (setBold) Typeface.BOLD else Typeface.NORMAL
        view.setTypeface(null, style)
    }

}