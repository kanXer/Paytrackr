package com.example.paytrackr.ui.main

import com.example.paytrackr.data.Customer
import com.example.paytrackr.data.CustomerDue
import com.example.paytrackr.data.DataRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
  private val repository = FakeDataRepository()
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun uiState_initiallyLoading() = runTest {
    val viewModel = MainScreenViewModel(repository)
    assertEquals(MainScreenUiState.Loading, viewModel.uiState.first())
  }

  @Test
  fun addTransaction_increasesCustomerDue() = runTest {
    val viewModel = MainScreenViewModel(repository)
    viewModel.addTransaction("1", 250.0)
    assertEquals(1250.0, repository.customers.first().first().due, 0.01)
  }

  @Test
  fun signInWithGoogle_failsGracefullyWhenAuthNull() = runTest {
    val viewModel = MainScreenViewModel(repository)
    var failed = false
    viewModel.signInWithGoogle("token") { res ->
      if (res.isFailure) failed = true
    }
    testScheduler.advanceUntilIdle()
    assertEquals(true, failed)
  }

  @Test
  fun setOrUpdatePassword_failsGracefullyWhenAuthNull() = runTest {
    val viewModel = MainScreenViewModel(repository)
    var failed = false
    viewModel.setOrUpdatePassword("newPassword123") { res ->
      if (res.isFailure) failed = true
    }
    testScheduler.advanceUntilIdle()
    assertEquals(true, failed)
  }

  @Test
  fun sendPasswordResetEmail_failsGracefullyWhenAuthNull() = runTest {
    val viewModel = MainScreenViewModel(repository)
    var failed = false
    viewModel.sendPasswordResetEmail("test@example.com") { res ->
      if (res.isFailure) failed = true
    }
    testScheduler.advanceUntilIdle()
    assertEquals(true, failed)
  }
}

private class FakeDataRepository : DataRepository {
  private val state =
    MutableStateFlow(listOf(CustomerDue(Customer("1", "Sample", "+91 12345 67890"), 1000.0)))

  override val customers: Flow<List<CustomerDue>> = state.asStateFlow()

  override fun addTransaction(customerId: String, amount: Double) {
    state.update { list ->
      list.map { if (it.customer.id == customerId) it.copy(due = it.due + amount) else it }
    }
  }
}
