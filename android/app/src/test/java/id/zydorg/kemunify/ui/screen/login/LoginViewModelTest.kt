package id.zydorg.kemunify.ui.screen.login

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import id.zydorg.kemunify.data.model.User
import id.zydorg.kemunify.data.preference.UserPreferences
import id.zydorg.kemunify.data.repository.KemunifyRepository
import id.zydorg.kemunify.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest{

    private val userPreferences: UserPreferences = mock()
    private val kemunifyRepository: KemunifyRepository = mock()
    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mock()
    private val credentialManager: CredentialManager = mock()
    private lateinit var viewModel: LoginViewModel


    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(userPreferences, kemunifyRepository)
    }

    // 1. Test fetchUser success
    @Test
    fun `fetchUser should update state to Success with user data`() = runTest {
        // Given
        val user = User(fullName = "Test User", email = "test@example.com", profile = "photoUrl", isLogin = true)
        whenever(userPreferences.getUserSession()).thenReturn(flowOf(user))

        // When
        viewModel.fetchUser()
        advanceUntilIdle()

        // Then
        val state = viewModel.userUiState.value
        assertTrue(state is UiState.Success)
        assertEquals(user, (state as UiState.Success).data)
    }

    // 2. Test fetchUser error
    @Test
    fun `fetchUser should update state to Error on exception`() = runTest {
        // Given
        val errorMessage = "Preferences error"
        whenever(userPreferences.getUserSession()).thenReturn(
            flow { throw RuntimeException(errorMessage) }
        )

        // When
        viewModel.fetchUser()
        advanceUntilIdle()

        // Then
        val state = viewModel.userUiState.value
        assertTrue(state is UiState.Error)
        assertEquals(errorMessage, (state as UiState.Error).errorMessage)
    }

    // 3. Test signInWithGoogle success
    @Test
    fun `signInWithGoogle should update state to Success on success`() = runTest {
        // Given
        val user = User(fullName = "Google User", email = "google@example.com", profile = "photoUrl", isLogin = true)

        whenever(kemunifyRepository.signInWithGoogle(context, credentialManager))
            .thenReturn(flowOf(user))

        // When
        viewModel.signInWithGoogle(context, credentialManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.userUiState.value
        assertTrue(state is UiState.Success)
        assertEquals(user, (state as UiState.Success).data)
    }

    // 4. Test signInWithGoogle failure
    @Test
    fun `signInWithGoogle should update state to Error on failure`() = runTest {
        // Given
        val context = mock<Context>()
        val credentialManager = mock<CredentialManager>()
        val errorMessage = "Google sign-in failed"

        // Membuat Status object
        val status = Status(CommonStatusCodes.INTERNAL_ERROR, errorMessage, null)
        val apiException = ApiException(status)

        whenever(kemunifyRepository.signInWithGoogle(context, credentialManager))
            .thenReturn(
                flow { throw RuntimeException(apiException.message) }
            )

        // When
        viewModel.signInWithGoogle(context, credentialManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.userUiState.value
        assertTrue(state is UiState.Error)

        // Pesan error akan berisi kode status dan pesan
        val expectedMessage = "8: $errorMessage"
        assertEquals(expectedMessage, (state as UiState.Error).errorMessage)
    }

    // 5. Test signInWithGoogle exception handling
    @Test
    fun `signInWithGoogle should handle unexpected exceptions`() = runTest {
        // Given
        val errorMessage = "Unexpected error"

        whenever(kemunifyRepository.signInWithGoogle(context, credentialManager))
            .thenReturn(
                flow { throw RuntimeException(errorMessage) }
            )

        // When
        viewModel.signInWithGoogle(context, credentialManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.userUiState.value
        assertTrue(state is UiState.Error)
        assertEquals(errorMessage, (state as UiState.Error).errorMessage)
    }

    // 6. Test initial state
    @Test
    fun `initial state should be Loading`() = runTest {
        assertTrue(viewModel.userUiState.value is UiState.Loading)
    }
}