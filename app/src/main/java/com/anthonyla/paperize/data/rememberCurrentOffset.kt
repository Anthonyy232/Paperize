package com.anthonyla.paperize.data

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember

/**
 * Remember the current offset of a [LazyListState].
 * Source:
 * https://stackoverflow.com/a/75677163
 * https://stackoverflow.com/a/70558512
 */
@Composable
fun rememberCurrentOffset(state: LazyListState): State<Int> {
    val position = remember { derivedStateOf { state.firstVisibleItemIndex } }
    val itemOffset = remember { derivedStateOf { state.firstVisibleItemScrollOffset } }
    val lastPosition = rememberPrevious(position.value)
    val lastItemOffset = rememberPrevious(itemOffset.value)
    val currentOffset = remember { mutableIntStateOf(0) }

    LaunchedEffect(position.value, itemOffset.value) {
        currentOffset.intValue = when {
            lastPosition == null || position.value == 0 -> itemOffset.value
            lastPosition == position.value -> currentOffset.intValue + (itemOffset.value - (lastItemOffset ?: 0))
            lastPosition > position.value -> currentOffset.intValue - (lastItemOffset ?: 0)
            else -> currentOffset.intValue + itemOffset.value
        }
    }

    return currentOffset
}

@Composable
fun <T> rememberPrevious(
    current: T,
    shouldUpdate: (prev: T?, curr: T) -> Boolean = { prev, curr -> prev != curr },
): T? {
    val ref = rememberRef<T>()
    SideEffect {
        if (shouldUpdate(ref.value, current)) {
            ref.value = current
        }
    }
    return ref.value
}

@Composable
fun <T> rememberRef(): MutableState<T?> {
    return remember {
        object : MutableState<T?> {
            override var value: T? = null
            override fun component1(): T? = value
            override fun component2(): (T?) -> Unit = { value = it }
        }
    }
}