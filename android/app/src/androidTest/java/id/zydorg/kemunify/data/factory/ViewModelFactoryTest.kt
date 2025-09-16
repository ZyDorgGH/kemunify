package id.zydorg.kemunify.data.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.zydorg.kemunify.MainActivityViewModel
import id.zydorg.kemunify.data.di.InjectionTest
import id.zydorg.kemunify.ui.screen.detail.DetailViewModel
import id.zydorg.kemunify.ui.screen.home.HomeViewModel
import id.zydorg.kemunify.ui.screen.login.LoginViewModel
import id.zydorg.kemunify.ui.screen.profile.ProfileViewModel
import id.zydorg.kemunify.ui.screen.waste.WasteViewModel

class ViewModelFactoryTest(
    private val injection: InjectionTest
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {

            modelClass.isAssignableFrom(WasteViewModel::class.java) -> WasteViewModel(
                injection.userRepository
            )

            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(
                injection.userRepository,
                injection.userPreferences
            )

            modelClass.isAssignableFrom(DetailViewModel::class.java) -> DetailViewModel(
                injection.userRepository
            )

            modelClass.isAssignableFrom(MainActivityViewModel::class.java) -> MainActivityViewModel(
                injection.userPreferences
            )
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(
                injection.userPreferences,
                injection.userRepository
            )
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(
                injection.userPreferences
            )

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }

}