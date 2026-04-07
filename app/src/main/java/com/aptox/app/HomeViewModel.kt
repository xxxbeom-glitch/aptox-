package com.aptox.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.util.Calendar

data class HomeGreeting(
    val title: String,
    val subtext: String,
)

class HomeViewModel : ViewModel() {

    private val _greeting = MutableStateFlow(computeGreeting())
    val greeting: StateFlow<HomeGreeting> = _greeting.asStateFlow()

    init {
        _greeting.value = computeGreeting()
    }

    fun refreshGreeting() {
        _greeting.value = computeGreeting()
    }

    private fun computeGreeting(): HomeGreeting {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val today = LocalDate.now()
        return HomeGreetingMents.pick(today, hour)
    }

    class Factory(
        @Suppress("UNUSED_PARAMETER") private val context: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel() as T
        }
    }
}
