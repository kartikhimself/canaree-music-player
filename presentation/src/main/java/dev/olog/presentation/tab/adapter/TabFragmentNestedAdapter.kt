package dev.olog.presentation.tab.adapter

import androidx.lifecycle.Lifecycle
import dev.olog.presentation.BindingsAdapter
import dev.olog.presentation.base.adapter.*
import dev.olog.presentation.dagger.FragmentLifecycle
import dev.olog.presentation.model.DisplayableAlbum
import dev.olog.presentation.model.DisplayableItem
import dev.olog.presentation.navigator.Navigator
import kotlinx.android.synthetic.main.item_tab_album_last_played.view.*

internal class TabFragmentNestedAdapter(
    @FragmentLifecycle lifecycle: Lifecycle,
    private val navigator: Navigator

) : ObservableAdapter<DisplayableItem>(
    lifecycle,
    DiffCallbackDisplayableItem
) {

    override fun initViewHolderListeners(viewHolder: DataBoundViewHolder, viewType: Int) {
        viewHolder.setOnClickListener(this) { item, _, _ ->
            navigator.toDetailFragment(item.mediaId)
        }
        viewHolder.setOnLongClickListener(this) { item, _, _ ->
            navigator.toDialog(item.mediaId, viewHolder.itemView)
        }
        viewHolder.elevateAlbumOnTouch()
    }

    override fun bind(holder: DataBoundViewHolder, item: DisplayableItem, position: Int) {
        require(item is DisplayableAlbum)

        holder.view.apply {
            BindingsAdapter.loadAlbumImage(holder.imageView!!, item.mediaId)
            quickAction.setId(item.mediaId)
            firstText.text = item.title
            secondText.text = item.subtitle
        }
    }

}