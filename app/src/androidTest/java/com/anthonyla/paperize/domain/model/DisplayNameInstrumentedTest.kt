package com.anthonyla.paperize.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameInstrumentedTest {
    @Test fun documentNamesDecodeEscapesAndPreserveLiteralPlusSigns() {
        mapOf(
            "primary%3APictures%2FSummer%20%2B%20Sun.jpg" to "Summer + Sun.jpg",
            "primary%3APictures" to "Pictures",
            "Pictures/Summer+Sun.jpg" to "Summer+Sun.jpg",
            "" to ""
        ).forEach { (stored, displayed) ->
            assertEquals(displayed, Folder.empty().copy(name = stored).displayName)
            assertEquals(displayed, Wallpaper.empty().copy(fileName = stored).displayFileName)
        }
    }
}
