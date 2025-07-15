package id.zydorg.kemunify.ui.screen.waste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.zydorg.kemunify.data.converter.Converters
import id.zydorg.kemunify.data.database.CustomerEntity
import id.zydorg.kemunify.data.database.WasteEntity
import id.zydorg.kemunify.data.repository.KemunifyRepository
import id.zydorg.kemunify.ui.common.WasteUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class WasteViewModel(
    private val wasteRepository: KemunifyRepository
): ViewModel() {
    companion object {
        internal const val MILLIS = 5_000L
    }

    val wasteUiState : StateFlow<WasteUiState> =
        wasteRepository.getAllWaste()
            .map { WasteUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(WasteViewModel.MILLIS),
                initialValue = WasteUiState()
            )

    suspend fun addCustomerWithWeights(name: String, date: String,  weights: Map<String, BigDecimal>) {
        viewModelScope.launch {
            // Insert new customer
            wasteRepository.insertCustomer(CustomerEntity(name, date))

            val allWaste = wasteRepository.getAllWaste().first()

            if (allWaste.isEmpty()) {
                // Create new waste entries if none exist
                weights.forEach { (wasteName, weightValue) ->
                    val newWeights = mutableMapOf(name to weightValue.setScale(2, RoundingMode.HALF_UP))
                    val newWaste = WasteEntity(
                        wasteName = wasteName,
                        weightsJson = Converters().fromMap(newWeights)
                    )
                    wasteRepository.insertWaste(newWaste)
                }
            } else {
                // Update existing waste entries
                allWaste.forEach { wasteEntity ->
                    val currentWeights = Gson().fromJson<MutableMap<String, BigDecimal>>(
                        wasteEntity.weightsJson,
                        object : TypeToken<MutableMap<String, BigDecimal>>() {}.type
                    ) ?: mutableMapOf()

                    // Get weight for this waste type (default to 0.00)
                    val weightForThisWaste = weights[wasteEntity.wasteName]?.setScale(2, RoundingMode.HALF_UP)
                        ?: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

                    currentWeights[name] = weightForThisWaste

                    // Update entity
                    val updatedEntity = wasteEntity.copy(
                        weightsJson = Gson().toJson(currentWeights)
                    )
                    wasteRepository.updateWaste(updatedEntity)
                }
            }
        }
    }


    suspend fun deleteWaste(id: Int){
        viewModelScope.launch {
            wasteRepository.deleteWaste(id)
        }
    }

}