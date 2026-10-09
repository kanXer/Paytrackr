package com.example.paytrackr.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paytrackr.auth.AuthRepository
import com.example.paytrackr.auth.BusinessProfile
import com.example.paytrackr.data.CustomerDue
import com.example.paytrackr.data.DataRepository
import com.example.paytrackr.data.TransactionType
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState

    data class Error(val throwable: Throwable) : MainScreenUiState

    data class Success(
        val customers: List<CustomerDue>,
        val totalOutstanding: Double,
        val totalReceivedToday: Double,
        val totalReceivedThisMonth: Double,
        val activeCustomers: Int,
    ) : MainScreenUiState
}

class MainScreenViewModel(
    private val dataRepository: DataRepository,
    val authRepository: AuthRepository? = null,
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> =
        authRepository?.currentUser ?: MutableStateFlow(null).asStateFlow()

    val isGuestMode: StateFlow<Boolean> =
        authRepository?.isGuestMode ?: MutableStateFlow(true).asStateFlow()

    val profile: StateFlow<BusinessProfile> =
        authRepository?.profile ?: MutableStateFlow(BusinessProfile()).asStateFlow()

    fun saveProfile(profile: BusinessProfile) {
        authRepository?.saveProfile(profile)
    }

    fun isFirebaseAvailable(): Boolean = authRepository?.isFirebaseAvailable() == true

    fun signIn(email: String, pass: String, onComplete: (Result<FirebaseUser?>) -> Unit) {
        viewModelScope.launch {
            if (authRepository == null) {
                onComplete(Result.failure(Exception("Auth repository unavailable")))
                return@launch
            }
            val res = authRepository.signIn(email, pass)
            onComplete(res)
        }
    }

    fun signUp(email: String, pass: String, name: String = "", onComplete: (Result<FirebaseUser?>) -> Unit) {
        viewModelScope.launch {
            if (authRepository == null) {
                onComplete(Result.failure(Exception("Auth repository unavailable")))
                return@launch
            }
            val res = authRepository.signUp(email, pass, name)
            onComplete(res)
        }
    }

    fun signInWithGoogle(idToken: String, onComplete: (Result<FirebaseUser?>) -> Unit) {
        viewModelScope.launch {
            if (authRepository == null) {
                onComplete(Result.failure(Exception("Auth repository unavailable")))
                return@launch
            }
            val res = authRepository.signInWithGoogle(idToken)
            onComplete(res)
        }
    }

    fun setOrUpdatePassword(password: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            if (authRepository == null) {
                onComplete(Result.failure(Exception("Auth repository unavailable")))
                return@launch
            }
            val res = authRepository.setOrUpdatePassword(password)
            onComplete(res)
        }
    }

    fun sendPasswordResetEmail(email: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            if (authRepository == null) {
                onComplete(Result.failure(Exception("Auth repository unavailable")))
                return@launch
            }
            val res = authRepository.sendPasswordResetEmail(email)
            onComplete(res)
        }
    }

    fun hasPasswordLinked(): Boolean = authRepository?.hasPasswordLinked() == true

    fun signOut() {
        authRepository?.signOut()
    }

    fun setGuestMode(enabled: Boolean) {
        authRepository?.setGuestMode(enabled)
    }

    val uiState: StateFlow<MainScreenUiState> =
        dataRepository.customers
            .map<List<CustomerDue>, MainScreenUiState> { customers ->
                val totalOutstanding = customers.sumOf { it.due }
                val activeCustomers = customers.count { it.due > 0 }

                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val todayStart = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, 1)
                val monthStart = cal.timeInMillis

                val totalReceivedToday = customers
                    .flatMap { it.transactions }
                    .filter { it.type == TransactionType.PAYMENT && it.timestamp >= todayStart }
                    .sumOf { it.amount }

                val totalReceivedThisMonth = customers
                    .flatMap { it.transactions }
                    .filter { it.type == TransactionType.PAYMENT && it.timestamp >= monthStart }
                    .sumOf { it.amount }

                MainScreenUiState.Success(
                    customers = customers,
                    totalOutstanding = totalOutstanding,
                    totalReceivedToday = totalReceivedToday,
                    totalReceivedThisMonth = totalReceivedThisMonth,
                    activeCustomers = activeCustomers,
                )
            }
            .catch { emit(MainScreenUiState.Error(it)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

    fun addTransaction(customerId: String, amount: Double) {
        dataRepository.addTransaction(customerId, amount)
    }

    fun addTransaction(customerId: String, amount: Double, description: String) {
        dataRepository.addTransaction(customerId, amount, description)
    }

    fun addTransaction(
        customerId: String,
        amount: Double,
        description: String,
        type: TransactionType,
    ) {
        dataRepository.addTransaction(customerId, amount, description, type)
    }

    fun deleteTransaction(transactionId: String) {
        dataRepository.deleteTransaction(transactionId)
    }

    fun settleCustomer(customerId: String) {
        dataRepository.settleCustomer(customerId)
    }

    fun addCustomer(name: String, phone: String, initialDue: Double = 0.0) {
        dataRepository.addCustomer(name, phone)
    }

    fun deleteCustomer(customerId: String) {
        dataRepository.deleteCustomer(customerId)
    }
}