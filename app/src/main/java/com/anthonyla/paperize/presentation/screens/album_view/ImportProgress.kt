package com.anthonyla.paperize.presentation.screens.album_view

/**
 * Progress of an ongoing add-folder / add-wallpapers import, surfaced to the UI so it can
 * show a live count instead of a bare spinner.
 */
sealed interface ImportProgress {
    /** No import running. */
    data object Idle : ImportProgress

    /** Walking the folder tree; the total is not yet known. */
    data class Scanning(val found: Int) : ImportProgress

    /** Writing the discovered images to the database. */
    data class Saving(val saved: Int, val total: Int) : ImportProgress
}
