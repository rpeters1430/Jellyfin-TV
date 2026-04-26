package com.rpeters1430.jellyfintv

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.rpeters1430.jellyfintv.fragments.DetailsFragment
import com.rpeters1430.jellyfintv.models.BaseItemDto

/**
 * Host activity for the media-item details screen.
 * Receives the selected [BaseItemDto] via Intent extras.
 */
class DetailsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        if (savedInstanceState == null) {
            val fragment = DetailsFragment()
            fragment.arguments = intent.extras
            supportFragmentManager.beginTransaction()
                .replace(R.id.details_fragment, fragment)
                .commit()
        }
    }

    companion object {
        const val EXTRA_ITEM = "extra_item"

        fun createIntent(activity: FragmentActivity, item: BaseItemDto): Intent =
            Intent(activity, DetailsActivity::class.java).apply {
                putExtra(EXTRA_ITEM, com.google.gson.Gson().toJson(item))
            }
    }
}
