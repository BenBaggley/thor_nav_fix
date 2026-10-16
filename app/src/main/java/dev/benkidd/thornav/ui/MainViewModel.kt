package dev.benkidd.thornav.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.benkidd.thornav.data.UserDefaults
import dev.benkidd.thornav.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    val isScreenOnEnabled: StateFlow<Boolean> = userPreferences.isScreenOnEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserDefaults.SCREEN_ON,
        )
    val isUserUnlockEnabled: StateFlow<Boolean> = userPreferences.isUserUnlockEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserDefaults.USER_UNLOCK,
        )
    val isBootEnabled: StateFlow<Boolean> = userPreferences.isBootEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserDefaults.BOOT,
        )

    fun setScreenOnEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferences.setScreenOnEnabled(enabled)
    }

    fun setUserUnlockEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferences.setUserUnlockEnabled(enabled)
    }

    fun setBootEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferences.setBootEnabled(enabled)
    }
}
