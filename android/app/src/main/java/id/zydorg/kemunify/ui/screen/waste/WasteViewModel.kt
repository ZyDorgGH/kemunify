package id.zydorg.kemunify.ui.screen.waste

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.zydorg.kemunify.data.converter.Converters
import id.zydorg.kemunify.data.database.CustomerEntity
import id.zydorg.kemunify.data.database.WasteEntity
import id.zydorg.kemunify.data.repository.KemunifyRepository
import id.zydorg.kemunify.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class WasteViewModel(
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

    fun addCustomerWithWeights(name: String, date: String,  weights: Map<String, BigDecimal>, context: Context) {
        viewModelScope.launch {
            // Insert new customer
            try {
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
            } catch (e: Exception){
                Toast.makeText(
                    context,
                    "Error saat menyimpan data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}