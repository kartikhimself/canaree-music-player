package dev.olog.presentation.edit.album

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import dev.olog.core.MediaId
import dev.olog.core.Stylizer
import dev.olog.presentation.R
import dev.olog.presentation.edit.BaseEditItemFragment
import dev.olog.presentation.edit.EditItemViewModel
import dev.olog.presentation.edit.UpdateAlbumInfo
import dev.olog.presentation.edit.model.SaveImageType
import dev.olog.presentation.edit.model.UpdateResult
import dev.olog.shared.android.extensions.*
import dev.olog.shared.lazyFast
import kotlinx.android.synthetic.main.fragment_edit_album.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EditAlbumFragment : BaseEditItemFragment() {

    companion object {
        const val TAG = "EditAlbumFragment"
        const val ARGUMENTS_MEDIA_ID = "$TAG.arguments.media_id"

        @JvmStatic
        fun newInstance(mediaId: MediaId): EditAlbumFragment {
            return EditAlbumFragment().withArguments(
                    ARGUMENTS_MEDIA_ID to mediaId.toString())
        }
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel by lazyFast {
        viewModelProvider<EditAlbumFragmentViewModel>(viewModelFactory)
    }
    private val editItemViewModel by lazyFast {
        act.viewModelProvider<EditItemViewModel>(viewModelFactory)
    }
    private val mediaId: MediaId by lazyFast {
        MediaId.fromString(getArgument(ARGUMENTS_MEDIA_ID))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.requestData(mediaId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        launch {
            album.afterTextChange()
                .map { it.isNotBlank() }
                .collect { okButton.isEnabled = it }
        }

        loadImage(mediaId)

        viewModel.observeData().subscribe(viewLifecycleOwner) {
            album.setText(it.title)
            artist.setText(it.artist)
            albumArtist.setText(it.albumArtist)
            year.setText(it.year)
            genre.setText(it.genre)
            val text = resources.getQuantityString(
                R.plurals.edit_item_xx_tracks_will_be_updated, it.songs, it.songs)
            albumsUpdated.text =  text
            podcast.isChecked = it.isPodcast
        }
    }

    override fun onResume() {
        super.onResume()
        okButton.setOnClickListener {
            launch { trySave() }
        }
        cancelButton.setOnClickListener { dismiss() }
        picker.setOnClickListener { changeImage() }
    }

    override fun onPause() {
        super.onPause()
        okButton.setOnClickListener(null)
        cancelButton.setOnClickListener(null)
        picker.setOnClickListener(null)
    }

    private suspend fun trySave(){
        val result = editItemViewModel.updateAlbum(
            UpdateAlbumInfo(
                mediaId,
                album.extractText().trim(),
                artist.extractText().trim(),
                albumArtist.extractText().trim(),
                genre.extractText().trim(),
                year.extractText().trim(),
                viewModel.getNewImage(),
                podcast.isChecked
            )
        )

        when (result){
            UpdateResult.OK -> dismiss()
            UpdateResult.EMPTY_TITLE -> ctx.toast(R.string.edit_song_invalid_title)
            UpdateResult.ILLEGAL_YEAR -> ctx.toast(R.string.edit_song_invalid_year)
            UpdateResult.ILLEGAL_DISC_NUMBER,
            UpdateResult.ILLEGAL_TRACK_NUMBER -> {}
        }
    }

    override fun restoreImage() {
        viewModel.restoreOriginalImage()
        loadOriginalImage(mediaId)
    }

    override fun onLoaderCancelled() {
    }

    override suspend fun stylizeImage(stylizer: Stylizer) {
        withContext(Dispatchers.IO) {
            try {
                getOriginalImageBitmap(mediaId)
            } catch (ex: Exception){
                withContext(Dispatchers.Main){
                    ctx.toast("Can't stylize default cover")
                }
                ex.printStackTrace()
                return@withContext
            }?.let { bitmap ->
                stylizeImageInternal(stylizer, bitmap)
            }
        }
    }

    private suspend fun stylizeImageInternal(stylizer: Stylizer, bitmap: Bitmap){
        val style = withContext(Dispatchers.Main) {
            Stylizer.loadDialog(act)
        }
        if (style != null){
            withContext(Dispatchers.Main){
                showLoader("Stylizing image", dismissable = false)
            }
            val stylizedBitmap = stylizer.stylize(style, bitmap)
            viewModel.updateImage(SaveImageType.Stylized(stylizedBitmap))
            withContext(Dispatchers.Main) {
                hideLoader()
                loadImage(stylizedBitmap, mediaId)
            }
        }
    }

    override fun toggleDownloadModule(show: Boolean) {
        downloadModule.toggleVisibility(show, true)
    }

    override fun provideLayoutId(): Int = R.layout.fragment_edit_album
}