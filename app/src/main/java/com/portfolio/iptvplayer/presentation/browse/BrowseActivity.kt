package com.portfolio.iptvplayer.presentation.browse

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.portfolio.iptvplayer.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrowseActivity : FragmentActivity(R.layout.activity_browse) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.browse_fragment_container, ChannelGridFragment())
                .commitNow()
        }
    }
}
