package com.rpeters1430.jellyfintv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.rpeters1430.jellyfintv.fragments.BrowseFragment

/**
 * Host activity for the main content-browsing screen.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, BrowseFragment())
                .commit()
        }
    }
}
