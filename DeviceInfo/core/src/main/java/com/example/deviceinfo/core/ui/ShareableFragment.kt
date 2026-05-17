package com.example.deviceinfo.core.ui

/**
 * Fragments that want to provide real data to the Share button implement this.
 */
interface ShareableFragment {
    /** Return a human-readable plain-text summary of the current tab's data. */
    fun getShareText(): String
}
