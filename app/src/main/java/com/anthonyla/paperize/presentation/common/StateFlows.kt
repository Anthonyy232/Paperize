package com.anthonyla.paperize.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Share while the UI collects, keeping the upstream alive across configuration changes. */
fun <T> Flow<T>.stateWhileSubscribed(scope: CoroutineScope, initialValue: T): StateFlow<T> =
    stateIn(scope, SharingStarted.WhileSubscribed(5_000), initialValue)

/** Runs [block] with [busy] raised, lowering it however the block ends. */
fun ViewModel.launchBusy(busy: MutableStateFlow<Boolean>, block: suspend () -> Unit) {
    busy.value = true
    viewModelScope.launch {
        try {
            block()
        } finally {
            busy.value = false
        }
    }
}
