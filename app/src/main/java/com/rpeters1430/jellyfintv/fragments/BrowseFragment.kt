package com.rpeters1430.jellyfintv.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.rpeters1430.jellyfintv.App
import com.rpeters1430.jellyfintv.DetailsActivity
import com.rpeters1430.jellyfintv.R
import com.rpeters1430.jellyfintv.models.BaseItemDto
import com.rpeters1430.jellyfintv.presenter.CardPresenter
import com.rpeters1430.jellyfintv.utils.ImageUrlBuilder
import com.rpeters1430.jellyfintv.viewmodel.BrowseViewModel

/**
 * Main content-browsing screen using the Leanback [BrowseSupportFragment].
 * Displays categorised rows: Continue Watching, Latest Movies, Latest Shows, and Libraries.
 */
class BrowseFragment : BrowseSupportFragment() {

    private lateinit var viewModel: BrowseViewModel
    private lateinit var backgroundManager: BackgroundManager

    private val prefs by lazy { (requireActivity().application as App).prefs }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = requireActivity().application as App
        viewModel = BrowseViewModel(app.repository)

        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        title = prefs.username?.let { "Hi, $it" } ?: "Jellyfin TV"
        badgeDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.app_banner)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backgroundManager = BackgroundManager.getInstance(requireActivity()).apply {
            attach(requireActivity().window)
        }

        setupEventListeners()
        observeViewModel()
        viewModel.loadContent()
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BrowseViewModel.BrowseState.Loading -> {
                    // Leanback spinner is shown by default while adapter is empty
                }

                is BrowseViewModel.BrowseState.Ready -> {
                    buildRows(state)
                }

                is BrowseViewModel.BrowseState.Error -> {
                    // Show a simple error row
                    val errorAdapter = ArrayObjectAdapter(ListRowPresenter())
                    adapter = errorAdapter
                }
            }
        }
    }

    private fun buildRows(state: BrowseViewModel.BrowseState.Ready) {
        val serverUrl = prefs.serverUrl ?: return
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = CardPresenter(serverUrl)

        if (state.continueWatching.isNotEmpty()) {
            rowsAdapter.add(buildRow("Continue Watching", state.continueWatching, cardPresenter))
        }

        if (state.latestMovies.isNotEmpty()) {
            rowsAdapter.add(buildRow("Latest Movies", state.latestMovies, cardPresenter))
        }

        if (state.latestShows.isNotEmpty()) {
            rowsAdapter.add(buildRow("Latest Shows", state.latestShows, cardPresenter))
        }

        state.libraries.forEach { library ->
            rowsAdapter.add(buildRow(library.name, listOf(library), cardPresenter))
        }

        adapter = rowsAdapter
    }

    private fun buildRow(
        title: String,
        items: List<BaseItemDto>,
        cardPresenter: CardPresenter
    ): ListRow {
        val listRowAdapter = ArrayObjectAdapter(cardPresenter)
        items.forEach { listRowAdapter.add(it) }
        val header = HeaderItem(title)
        return ListRow(header, listRowAdapter)
    }

    private fun setupEventListeners() {
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is BaseItemDto) {
                startActivity(DetailsActivity.createIntent(requireActivity(), item))
            }
        }

        onItemViewSelectedListener = OnItemViewSelectedListener { _, item, _, _ ->
            if (item is BaseItemDto) {
                updateBackground(item)
            }
        }
    }

    private fun updateBackground(item: BaseItemDto) {
        val serverUrl = prefs.serverUrl ?: return
        val url = ImageUrlBuilder.backdropImage(serverUrl, item) ?: return

        Glide.with(requireContext())
            .load(url)
            .centerCrop()
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    backgroundManager.drawable = resource
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    backgroundManager.clearDrawable()
                }
            })
    }
}
