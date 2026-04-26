package com.rpeters1430.jellyfintv.fragments

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.rpeters1430.jellyfintv.App
import com.rpeters1430.jellyfintv.DetailsActivity
import com.rpeters1430.jellyfintv.PlaybackActivity
import com.rpeters1430.jellyfintv.R
import com.rpeters1430.jellyfintv.models.BaseItemDto
import com.rpeters1430.jellyfintv.presenter.DetailsDescriptionPresenter
import com.rpeters1430.jellyfintv.utils.ImageUrlBuilder
import com.rpeters1430.jellyfintv.viewmodel.DetailsViewModel

/**
 * Shows detailed information about a media item and provides a Play action.
 */
class DetailsFragment : DetailsSupportFragment() {

    private lateinit var viewModel: DetailsViewModel
    private lateinit var backgroundController: DetailsSupportFragmentBackgroundController
    private lateinit var item: BaseItemDto

    private val prefs by lazy { (requireActivity().application as App).prefs }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = requireActivity().application as App
        viewModel = DetailsViewModel(app.repository)

        // Deserialise item from arguments
        val json = arguments?.getString(DetailsActivity.EXTRA_ITEM)
            ?: throw IllegalArgumentException("DetailsFragment requires EXTRA_ITEM")
        item = Gson().fromJson(json, BaseItemDto::class.java)

        backgroundController = DetailsSupportFragmentBackgroundController(this)
        backgroundController.enableParallax()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildDetailsAdapter()
        loadBackdrop()
        observeViewModel()
    }

    private fun buildDetailsAdapter() {
        val descriptionRow = DetailsOverviewRow(item).apply {
            actionsAdapter = SparseArrayObjectAdapter().also { actions ->
                actions.set(ACTION_PLAY, Action(ACTION_PLAY.toLong(), "Play"))
            }
        }

        val serverUrl = prefs.serverUrl
        if (serverUrl != null) {
            val imageUrl = ImageUrlBuilder.primaryImage(serverUrl, item, maxWidth = 400)
            if (imageUrl != null) {
                Glide.with(requireContext())
                    .load(imageUrl)
                    .centerCrop()
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            descriptionRow.imageDrawable = resource
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }

        val fullWidthDetailsPresenter = FullWidthDetailsOverviewRowPresenter(
            DetailsDescriptionPresenter()
        )

        val selector = ClassPresenterSelector().apply {
            addClassPresenter(DetailsOverviewRow::class.java, fullWidthDetailsPresenter)
            addClassPresenter(ListRow::class.java, ListRowPresenter())
        }

        val rowsAdapter = ArrayObjectAdapter(selector)
        rowsAdapter.add(descriptionRow)

        onActionClickedListener = OnActionClickedListener { action ->
            when (action.id.toInt()) {
                ACTION_PLAY -> viewModel.requestPlayback(item)
            }
        }

        adapter = rowsAdapter
    }

    private fun loadBackdrop() {
        val serverUrl = prefs.serverUrl ?: return
        val url = ImageUrlBuilder.backdropImage(serverUrl, item) ?: return
        Glide.with(requireContext())
            .load(url)
            .centerCrop()
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    backgroundController.coverBitmap =
                        (resource as? android.graphics.drawable.BitmapDrawable)?.bitmap
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun observeViewModel() {
        viewModel.playbackState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DetailsViewModel.PlaybackState.Ready -> {
                    val info = state.playbackInfo
                    val serverUrl = prefs.serverUrl ?: return@observe
                    val token = prefs.accessToken ?: return@observe
                    val userId = prefs.userId ?: return@observe

                    // Use the first available direct-stream source, or fall back to our own URL builder
                    val streamUrl = info.mediaSources
                        ?.firstOrNull { it.supportsDirectStream == true }
                        ?.let { src ->
                            val path = src.directStreamUrl
                            if (!path.isNullOrBlank()) {
                                val base = serverUrl.trimEnd('/')
                                if (path.startsWith("http")) {
                                    path
                                } else {
                                    // Append api_key as a query parameter, respecting existing '?'
                                    val separator = if (path.contains('?')) '&' else '?'
                                    "$base$path${separator}api_key=$token"
                                }
                            } else null
                        }
                        ?: ImageUrlBuilder.videoStreamUrl(serverUrl, item.id, userId, token)

                    val startPositionMs = item.userData?.playbackPositionTicks
                        ?.div(10_000L) ?: 0L

                    val playIntent = Intent(requireContext(), PlaybackActivity::class.java).apply {
                        putExtra(PlaybackActivity.EXTRA_STREAM_URL, streamUrl)
                        putExtra(PlaybackActivity.EXTRA_ITEM_ID, item.id)
                        putExtra(PlaybackActivity.EXTRA_PLAY_SESSION_ID, info.playSessionId)
                        putExtra(PlaybackActivity.EXTRA_START_POSITION_MS, startPositionMs)
                    }
                    startActivity(playIntent)
                    viewModel.resetPlaybackState()
                }

                is DetailsViewModel.PlaybackState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetPlaybackState()
                }

                else -> { /* Idle / Loading – no action needed */ }
            }
        }
    }

    companion object {
        private const val ACTION_PLAY = 1
    }
}
