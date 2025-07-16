package id.zydorg.kemunify.data.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.zydorg.kemunify.data.di.Injection
import id.zydorg.kemunify.ui.screen.detail.DetailViewModel
import id.zydorg.kemunify.ui.screen.home.HomeViewModel
import id.zydorg.kemunify.ui.screen.waste.WasteViewModel

class ViewModelFactory(
    private val injection: Injection
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {

            modelClass.isAssignableFrom(WasteViewModel::class.java) -> WasteViewModel(
                injection.userRepository
            )

            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(
                injection.userRepository,
                injection.userPreferencesDataStore
            )

            modelClass.isAssignableFrom(DetailViewModel::class.java) -> DetailViewModel(
                injection.userRepository
            )

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }

}