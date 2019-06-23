package dev.olog.msc.presentation.main.widget

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import dev.olog.msc.presentation.library.categories.CategoriesFragment
import dev.olog.msc.presentation.playing.queue.PlayingQueueFragment
import dev.olog.msc.presentation.search.SearchFragment
import dev.olog.msc.utils.k.extension.fragmentTransaction
import dev.olog.msc.R
import dev.olog.presentation.main.BottomNavigationPage
import dev.olog.presentation.main.LibraryPage

internal class BottomNavigator {

    private val tags = listOf(
            CategoriesFragment.TAG_TRACK,
            CategoriesFragment.TAG_PODCAST,
            SearchFragment.TAG,
            PlayingQueueFragment.TAG
    )

    fun navigate(activity: FragmentActivity, page: BottomNavigationPage, libraryPage: LibraryPage){
        val fragmentTag = page.toFragmentTag(libraryPage)

        if (!tags.contains(fragmentTag)) {
            throw IllegalArgumentException("invalid fragment tag $fragmentTag")
        }

        for (index in 0..activity.supportFragmentManager.backStackEntryCount) {
            // clear the backstack
            activity.supportFragmentManager.popBackStack()
        }

        activity.fragmentTransaction {
            disallowAddToBackStack()
            setReorderingAllowed(true)
            // hide other categories fragment
            hidesAllBottomNavigationFragments(activity)

            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)

            val fragment = activity.supportFragmentManager.findFragmentByTag(fragmentTag)
            if (fragment == null) {
                add(R.id.fragmentContainer, tagToInstance(fragmentTag), fragmentTag)
            } else {
                show(fragment)
            }
        }
    }

    private fun FragmentTransaction.hidesAllBottomNavigationFragments(activity: FragmentActivity){
        activity.supportFragmentManager.fragments
                .asSequence()
                .filter { tags.contains(it.tag) }
                .forEach { hide(it) }
    }

    private fun BottomNavigationPage.toFragmentTag(libraryPage: LibraryPage): String {
        return when (this){
            BottomNavigationPage.LIBRARY -> {
                when(libraryPage){
                    LibraryPage.TRACKS -> CategoriesFragment.TAG_TRACK
                    LibraryPage.PODCASTS -> CategoriesFragment.TAG_PODCAST
                }
            }
            BottomNavigationPage.SEARCH -> SearchFragment.TAG
            BottomNavigationPage.QUEUE -> PlayingQueueFragment.TAG
        }
    }

    private fun tagToInstance(tag: String): Fragment = when (tag) {
        CategoriesFragment.TAG_TRACK -> CategoriesFragment.newInstance(false)
        CategoriesFragment.TAG_PODCAST -> CategoriesFragment.newInstance(true)
        SearchFragment.TAG -> SearchFragment()
        PlayingQueueFragment.TAG -> PlayingQueueFragment()
        else -> throw IllegalArgumentException("invalid fragment tag $tag")
    }
}