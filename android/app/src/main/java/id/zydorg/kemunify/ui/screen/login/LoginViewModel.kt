package id.zydorg.kemunify.ui.screen.login

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import id.zydorg.kemunify.data.model.User
import id.zydorg.kemunify.data.preference.UserPreferences
import id.zydorg.kemunify.data.repository.KemunifyRepository
import id.zydorg.kemunify.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userPreferences: UserPreferences,
    private val kemunifyRepository: KemunifyRepository
) :ViewModel() {

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

    fun signInWithGoogle(context: Context, credentialManager: CredentialManager){
        viewModelScope.launch {
            kemunifyRepository.signInWithGoogle(context, credentialManager)
                .catch { e ->
                    _userUiState.value = UiState.Error(e.message.toString())
                }
                .collect{user ->
                    _userUiState.value = UiState.Success(user)
                }
        }
    }

    fun requestDriveAuthorization(
        context: Context,
        navigateToHome: () -> Unit,
        authorizationLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val requestedScopes = listOf(Scope(DriveScopes.DRIVE_FILE))
        val authorizationRequest =
            AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes)
                .build()

        Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest)
            .addOnSuccessListener{
                Log.d("DriveAuth", "Authorization success. Has resolution? ${it.hasResolution()}")
                if(it.hasResolution()){
                    val pendingIntent = it.pendingIntent
                    val intentSenderRequest = pendingIntent?.intentSender?.let { it1 ->
                        IntentSenderRequest.Builder(
                            it1
                        ).build()
                    }
                    intentSenderRequest?.let { it1 -> authorizationLauncher.launch(it1) }

                } else {
                    Toast.makeText(
                        context,
                        "Accses already granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                navigateToHome()
            }.addOnFailureListener { Toast.makeText(context, "Failure: ${it.message}", Toast.LENGTH_SHORT).show() }
    }
}