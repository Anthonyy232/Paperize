package com.anthonyla.paperize.feature.wallpaper.util

sealed class OrderType {
    object Ascending: OrderType()
    object Descending: OrderType()
}