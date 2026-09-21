package com.campussync.app.models

/**
 * Represents the different item types displayed in the marks list.
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