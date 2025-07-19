package id.zydorg.kemunify.ui.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.zydorg.kemunify.data.database.WasteEntity
import id.zydorg.kemunify.data.repository.KemunifyRepository
import id.zydorg.kemunify.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.math.BigDecimal

class DetailViewModel(
    private val wasteRepository: KemunifyRepository
): ViewModel() {


    private val _wasteUiState: MutableStateFlow<UiState<List<WasteEntity>>> =
        MutableStateFlow(UiState.Loading)
    val wasteUiState: StateFlow<UiState<List<WasteEntity>>>
        get() = _wasteUiState

    fun fetchWaste(){
        viewModelScope.launch {
            wasteRepository.getAllWaste()
                .catch {e ->
                    _wasteUiState.value = UiState.Error(e.message.toString())
                }
                .collect{customer ->
                    _wasteUiState.value = UiState.Success(customer)
                }
        }
    }

    fun updateWasteWeight(
        wasteName: String,
        customerName: String,
        newWeight: BigDecimal
    ) {
        viewModelScope.launch {
            wasteRepository.updateWasteWeight(wasteName, customerName, newWeight)
        }
    }

    fun deleteCustomer(customer: String){
        viewModelScope.launch {
            wasteRepository.delete(customer)
        }
    }
}