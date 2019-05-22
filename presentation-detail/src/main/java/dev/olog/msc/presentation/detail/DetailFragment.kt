package dev.olog.msc.presentation.detail


import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.jakewharton.rxbinding2.widget.RxTextView
import dev.olog.msc.core.MediaId
import dev.olog.msc.presentation.base.DisplayableItemBindingAdapter
import dev.olog.msc.presentation.base.adapter.BasePagedAdapter
import dev.olog.msc.presentation.base.adapter.SetupNestedList
import dev.olog.msc.presentation.base.drag.OnStartDragListener
import dev.olog.msc.presentation.base.drag.TouchHelperAdapterCallback
import dev.olog.msc.presentation.base.extensions.*
import dev.olog.msc.presentation.base.fragment.BaseFragment
import dev.olog.msc.presentation.base.interfaces.CanChangeStatusBarColor
import dev.olog.msc.presentation.base.interfaces.MediaProvider
import dev.olog.msc.presentation.detail.adapter.*
import dev.olog.msc.presentation.detail.listener.HeaderVisibilityScrollListener
import dev.olog.msc.presentation.navigator.Navigator
import dev.olog.msc.shared.extensions.debounceFirst
import dev.olog.msc.shared.extensions.lazyFast
import dev.olog.msc.shared.ui.extensions.toggleVisibility
import kotlinx.android.synthetic.main.fragment_detail.*
import kotlinx.android.synthetic.main.fragment_detail.view.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

class DetailFragment : BaseFragment(),
        CanChangeStatusBarColor,
        SetupNestedList,
        OnStartDragListener {

    companion object {
        const val TAG = "DetailFragment"
        const val ARGUMENTS_MEDIA_ID = "$TAG.arguments.media_id"
        const val ARGUMENTS_SHARED_ELEMENT = "$TAG.arguments.shared_element"

        @JvmStatic
        fun newInstance(mediaId: MediaId): DetailFragment {
            return DetailFragment().withArguments(
                    ARGUMENTS_MEDIA_ID to mediaId.toString()
            )
        }
        @JvmStatic
        fun newInstance(mediaId: MediaId, transitionName: String): DetailFragment {
            return DetailFragment().withArguments(
                    ARGUMENTS_MEDIA_ID to mediaId.toString(),
                    ARGUMENTS_SHARED_ELEMENT to transitionName
            )
        }
    }

    @Inject
    lateinit var navigator: Navigator
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel by lazyFast { viewModelProvider<DetailFragmentViewModel>(viewModelFactory) }

    private val recyclerOnScrollListener by lazyFast { HeaderVisibilityScrollListener(this) }

    private val mediaId by lazyFast { MediaId.fromString(arguments!!.getString(ARGUMENTS_MEDIA_ID)!!) }

    private val mostPlayedAdapter by lazyFast { DetailMostPlayedAdapter(navigator) }
    private val recentlyAddedAdapter by lazyFast { DetailRecentlyAddedAdapter(navigator) }
    private val relatedArtistAdapter by lazyFast { DetailRelatedArtistsAdapter(navigator) }
    private val albumsAdapter by lazyFast { DetailAlbumsAdapter(navigator) }

    private val adapter by lazyFast {
        DetailFragmentAdapter(mediaId, act as MediaProvider, this, navigator, viewModel, this)
    }

    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper?.startDrag(viewHolder)
    }

    internal var hasLightStatusBarColor by Delegates.observable(false) { _, _, new ->
        adjustStatusBarColor(new)
    }

    override fun onViewBound(view: View, savedInstanceState: Bundle?) {
        loadImage(view)

        view.list.layoutManager = LinearLayoutManager(ctx)
        view.list.adapter = adapter
        view.list.setHasFixedSize(true)

        var swipeDirections = ItemTouchHelper.LEFT
        if (adapter.canSwipeRight) {
            swipeDirections = swipeDirections or ItemTouchHelper.RIGHT
        }
        val callback = TouchHelperAdapterCallback(adapter, swipeDirections)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper!!.attachToRecyclerView(view.list)

        view.fastScroller.attachRecyclerView(view.list)
        view.fastScroller.showBubble(false)

        viewModel.data
                .subscribe(viewLifecycleOwner) {
                    adapter.submitList(it)
                    //                        if (ctx.isLandscape){
//                            // header in list is not used in landscape
//                            copy[DetailFragmentDataType.HEADER]!!.clear()
//                        }
                }

        viewModel.mostPlayed
                .subscribe(viewLifecycleOwner, mostPlayedAdapter::submitList)

        viewModel.recentlyAdded
                .subscribe(viewLifecycleOwner, recentlyAddedAdapter::submitList)
//
        viewModel.relatedArtists
                .subscribe(viewLifecycleOwner, relatedArtistAdapter::submitList)
//
        viewModel.siblings
                .subscribe(viewLifecycleOwner, albumsAdapter::submitList)

//        viewModel.itemLiveData.subscribe(viewLifecycleOwner) { item ->
//            if (item.isNotEmpty()){
//                headerText.text = item[0].title
//                val cover = view.findViewById<View>(R.id.cover)
//                if (!isPortrait() && cover is ShapeImageView){
//                    DisplayableItemBindingAdapter.loadBigAlbumImage(cover, item[0])
//                }
//            }
//        }

        RxTextView.afterTextChangeEvents(view.editText)
                .map { it.view().text.toString() }
                .filter { it.isBlank() || it.trim().length >= 2 }
                .debounceFirst(250, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .asLiveData()
                .subscribe(viewLifecycleOwner) { text ->
                    val isEmpty = text.isEmpty()
                    view.clear.toggleVisibility(!isEmpty, true)
                    viewModel.updateFilter(text)
                }
    }

    override fun onResume() {
        super.onResume()
        list.addOnScrollListener(recyclerOnScrollListener)
        back.setOnClickListener { act.onBackPressed() }
        more.setOnClickListener { navigator.toDialog(viewModel.mediaId, more) }
        filter.setOnClickListener {
            searchWrapper.toggleVisibility(!searchWrapper.isVisible, true)
        }
        clear.setOnClickListener { editText.setText("") }
    }

    override fun onPause() {
        super.onPause()
        list.removeOnScrollListener(recyclerOnScrollListener)
        back.setOnClickListener(null)
        more.setOnClickListener(null)
        filter.setOnClickListener(null)
        clear.setOnClickListener(null)
    }

    private fun loadImage(view: View){
        setSharedElementEnterTransition(TransitionInflater.from(context).inflateTransition(android.R.transition.move))
        arguments!!.getString(ARGUMENTS_SHARED_ELEMENT)?.let {
            view.cover.transitionName = it
        }
        postponeEnterTransition()
        DisplayableItemBindingAdapter.loadBigAlbumImage(view.cover, mediaId)
        view.cover.doOnPreDraw { startPostponedEnterTransition() }
    }

    override fun setupNestedList(layoutId: Int, recyclerView: RecyclerView) {
        when (layoutId) {
            R.layout.item_detail_most_played_list -> {
                setupHorizontalListAsGrid(recyclerView, mostPlayedAdapter)
            }
            R.layout.item_detail_recently_added_list -> {
                setupHorizontalListAsGrid(recyclerView, recentlyAddedAdapter)
            }
            R.layout.item_detail_related_artists_list -> {
                setupHorizontalListAsList(recyclerView, relatedArtistAdapter)
            }
            R.layout.item_detail_albums_list -> {
                setupHorizontalListAsList(recyclerView, albumsAdapter)
            }
        }
    }

    private fun setupHorizontalListAsGrid(list: RecyclerView, adapter: BasePagedAdapter<*>) {
        val layoutManager = GridLayoutManager(
                list.context, DetailFragmentViewModel.NESTED_SPAN_COUNT,
                GridLayoutManager.HORIZONTAL, false
        )
        list.layoutManager = layoutManager
        list.adapter = adapter
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(list)
    }

    private fun setupHorizontalListAsList(list: RecyclerView, adapter: BasePagedAdapter<*>) {
        val layoutManager = LinearLayoutManager(list.context, LinearLayoutManager.HORIZONTAL, false)
        list.layoutManager = layoutManager
        list.adapter = adapter
    }

    override fun adjustStatusBarColor() {
        adjustStatusBarColor(hasLightStatusBarColor)
    }

    override fun adjustStatusBarColor(lightStatusBar: Boolean) {
        if (lightStatusBar) {
            setLightStatusBar()
        } else {
            removeLightStatusBar()
        }
    }

    private fun removeLightStatusBar() {
        act.window.removeLightStatusBar()
        val color = ContextCompat.getColor(ctx, R.color.detail_button_color_light)
        view?.back?.setColorFilter(color)
        more?.setColorFilter(color)
        filter?.setColorFilter(color)
    }

    private fun setLightStatusBar() {
        val isDarkMode = resources.getBoolean(R.bool.is_dark_mode)
        if (isDarkMode){
            return
        }

        act.window.setLightStatusBar()
        val color = ContextCompat.getColor(ctx, R.color.detail_button_color_dark)
        view?.back?.setColorFilter(color)
        more?.setColorFilter(color)
        filter?.setColorFilter(color)
    }

    override fun provideLayoutId(): Int = R.layout.fragment_detail
}
