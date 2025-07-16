package id.zydorg.kemunify.ui.common

import id.zydorg.kemunify.data.database.CustomerEntity
import id.zydorg.kemunify.data.database.WasteEntity

data class WasteUiState(val wastes: List<WasteEntity> = listOf()){}

data class CustomerUiState(val customer: List<CustomerEntity> = listOf()){}


data class GoogleSignInState(
    val isSignedIn: Boolean = false,
    val email: String? = null,
    val displayName: String? = null
)