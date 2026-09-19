package com.campussync.app.models

/**
 * Sealed class to represent different types of items in the Marks list.
 */
sealed class MarkListItem {
    data class Header(
        val moduleCode: String,
        val moduleName: String,
        val average: Double
    ) : MarkListItem()

    data class Item(
        val markEntry: MarkEntry
    ) : MarkListItem()
}
