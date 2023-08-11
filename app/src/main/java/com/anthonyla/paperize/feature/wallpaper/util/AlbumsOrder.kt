package com.anthonyla.paperize.feature.wallpaper.util

sealed class AlbumsOrder(
    val orderType: OrderType
) {
    class Date(orderType: OrderType): AlbumsOrder(orderType)
    fun copyOrder(orderType: OrderType): AlbumsOrder {
        return when(this) {
            is Date -> Date(orderType)
        }
    }
}
