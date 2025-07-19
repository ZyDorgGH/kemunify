package id.zydorg.kemunify.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.zydorg.kemunify.data.model.User
import id.zydorg.kemunify.data.preference.UserPreferences
import id.zydorg.kemunify.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userPreferences: UserPreferences,
): ViewModel() {

    private val _userUiState: MutableStateFlow<UiState<User>> =
        MutableStateFlow(UiState.Loading)
    val userUiState: StateFlow<UiState<User>>
        get() = _userUiState

    fun fetchUser(){
        viewModelScope.launch {
            userPreferences.getUserSession()
                .catch {e ->
                    _userUiState.value = UiState.Error(e.message.toString())
                }
                .collect{user ->
                    _userUiState.value = UiState.Success(user)
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.logout()
        }
    }
}