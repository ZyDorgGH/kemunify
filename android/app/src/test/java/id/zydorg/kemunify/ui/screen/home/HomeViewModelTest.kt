package id.zydorg.kemunify.ui.screen.home

import id.zydorg.kemunify.data.database.CustomerEntity
import id.zydorg.kemunify.data.model.User
import id.zydorg.kemunify.data.preference.UserPreferences
import id.zydorg.kemunify.data.repository.KemunifyRepository
import id.zydorg.kemunify.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val wasteRepository: KemunifyRepository = mock()
    private val userPreferences: UserPreferences = mock()

    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // Setup default mock behaviors
        Dispatchers.setMain(testDispatcher)

        whenever(wasteRepository.getAllCustomer()).thenReturn(flowOf(emptyList()))
        whenever(userPreferences.getUserSession()).thenReturn(flowOf(User("", "","", isLogin = false)))

        viewModel = HomeViewModel(wasteRepository, userPreferences)
    }


    // 1. Test StateFlow untuk customer data
    @Test
    fun `customerUiState should emit CustomerUiState with data`() = runTest {
        // Given
        val customers = listOf(
            CustomerEntity(customerName = "Customer 1", "now"),
            CustomerEntity(customerName = "Customer 2", "now")
        )
        whenever(wasteRepository.getAllCustomer()).thenReturn(flowOf(customers))

        // When
        viewModel.fetchCustomer()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.customerUiState.value
        assertTrue(state is UiState.Success)
        assertEquals(customers, (state as UiState.Success).data)
        println(state.data)

    }

    // 2. Test customerUiState error flow
    @Test
    fun `customerUiState should update to Error on repository failure`() = runTest {
        val errorMessage = "Database error"
        whenever(wasteRepository.getAllCustomer()).thenReturn(
            flow { throw RuntimeException(errorMessage) }
        )

        // Initial state should be Loading
        assertTrue(viewModel.customerUiState.value is UiState.Loading)

        // When
        viewModel.fetchCustomer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.customerUiState.value
        assertTrue(state is UiState.Error)
        assertEquals(errorMessage, (state as UiState.Error).errorMessage)

    }

    // 4. Test userUiState error flow
    @Test
    fun `userUiState should update to Error on preferences failure`() = runTest {
        val errorMessage = "Preferences error"
        whenever(userPreferences.getUserSession()).thenReturn(
            flow { throw RuntimeException(errorMessage) }
        )

        assertTrue(viewModel.userUiState.value is UiState.Loading)
        viewModel.fetchUser()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.userUiState.value
        assertTrue(state is UiState.Error)
        assertEquals(errorMessage, (state as UiState.Error).errorMessage)

    }

    // 5. Test delete customer
    @Test
    fun `deleteCustomer should call repository delete`() = runTest {
        // Given
        val customerName = "Test Customer"
        doAnswer {
            // Simulate coroutine execution
            testDispatcher.scheduler.advanceUntilIdle()
            Unit
        }.whenever(wasteRepository).delete(customerName)

        // When
        viewModel.deleteCustomer(customerName)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(wasteRepository).delete(customerName)
    }

    // 6. Test delete all customers
    @Test
    fun `deleteAllCustomers should call repository deleteAll`() = runTest {
        // Given
        doAnswer {
            testDispatcher.scheduler.advanceUntilIdle()
            Unit
        }.whenever(wasteRepository).deleteAllCustomers()

        // When
        viewModel.deleteAllCustomers()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(wasteRepository).deleteAllCustomers()
    }

    // 7. Test logout functionality
    @Test
    fun `logout should call userPreferences logout`() = runTest {
        // Given
        doAnswer {
            testDispatcher.scheduler.advanceUntilIdle()
            Unit
        }.whenever(userPreferences).logout()

        // When
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(userPreferences).logout()
    }

    // 8. Test state updates for multiple collectors
    @Test
    fun `multiple collectors should get same state updates`() = runTest {
        // Given
        val customers1 = listOf(CustomerEntity(customerName = "Customer A", date = "now"))
        val customers2 = listOf(CustomerEntity(customerName = "Customer B", date = "now"))

        // First fetch returns customers1
        whenever(wasteRepository.getAllCustomer()).thenReturn(flowOf(customers1))
        viewModel.fetchCustomer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify first state
        var state = viewModel.customerUiState.value
        assertTrue(state is UiState.Success)
        assertEquals(customers1, (state as UiState.Success).data)

        // Second fetch returns customers2
        whenever(wasteRepository.getAllCustomer()).thenReturn(flowOf(customers2))
        viewModel.fetchCustomer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify second state
        state = viewModel.customerUiState.value
        assertTrue(state is UiState.Success)
        assertEquals(customers2, (state as UiState.Success).data)
    }
}