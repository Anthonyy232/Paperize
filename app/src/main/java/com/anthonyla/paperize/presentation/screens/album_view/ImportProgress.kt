package com.anthonyla.paperize.presentation.screens.album_view

sealed interface ImportProgress {
    data object Idle : ImportProgress

    sealed interface Active : ImportProgress

    /** Walking the folder tree; the total is not yet known. */
    data class Scanning(val found: Int) : Active

    data class Saving(val saved: Int, val total: Int) : Active
}
