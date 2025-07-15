package id.zydorg.kemunify.ui.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.zydorg.kemunify.data.repository.KemunifyRepository
import id.zydorg.kemunify.ui.common.WasteUiState
import id.zydorg.kemunify.ui.screen.waste.WasteViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

class DetailViewModel(
    private val wasteRepository: KemunifyRepository
): ViewModel() {

    val wasteUiState : StateFlow<WasteUiState> =
        wasteRepository.getAllWaste()
            .map { WasteUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(WasteViewModel.MILLIS),
                initialValue = WasteUiState()
            )

    suspend fun updateWasteWeight(
        wasteName: String,
        customerName: String,
        newWeight: BigDecimal
    ) {
        viewModelScope.launch {
            wasteRepository.updateWasteWeight(wasteName, customerName, newWeight)
        }
    }

    suspend fun deleteCustomer(customer: String){
        viewModelScope.launch {
            wasteRepository.delete(customer)
        }
    }
}