package com.example.paytrackr.data

import android.content.Context
import com.example.paytrackr.auth.AuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class TransactionType { DUE, PAYMENT }

data class Transaction(
    val id: String,
    val customerId: String,
    val amount: Double,
    val description: String,
    val timestamp: Long,
    val type: TransactionType,
)

data class Customer(val id: String, val name: String, val phone: String)

data class CustomerDue(
    val customer: Customer,
    val due: Double,
    val lastActivity: Long = 0L,
    val transactions: List<Transaction> = emptyList(),
)

interface DataRepository {
    val customers: Flow<List<CustomerDue>>

    fun addTransaction(customerId: String, amount: Double) {
        addTransaction(customerId, amount, "Entry", TransactionType.DUE)
    }

    fun addTransaction(customerId: String, amount: Double, description: String) {
        addTransaction(customerId, amount, description, TransactionType.DUE)
    }

    fun addTransaction(customerId: String, amount: Double, description: String, type: TransactionType) {}

    fun deleteTransaction(transactionId: String) {}

    fun settleCustomer(customerId: String) {}

    fun addCustomer(name: String, phone: String) {}

    fun deleteCustomer(customerId: String) {}
}

class DefaultDataRepository(
    private val context: Context,
    private val authRepository: AuthRepository = AuthRepository(context),
) : DataRepository {

    private val prefs = context.getSharedPreferences("paytrackr_data", Context.MODE_PRIVATE)

    private val customersFlow = MutableStateFlow(loadCustomers())
    private val transactionsFlow = MutableStateFlow(loadTransactions())

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var customerListener: ListenerRegistration? = null
    private var transactionListener: ListenerRegistration? = null

    init {
        // Clear any old dummy seed entries from previous versions
        if (prefs.contains("customers_v1")) {
            prefs.edit().remove("customers_v1").remove("transactions_v1").apply()
        }

        authRepository.currentUser.onEach { user ->
            if (user != null) {
                attachFirestoreListeners(user.uid)
            } else {
                customerListener?.remove()
                transactionListener?.remove()
                customerListener = null
                transactionListener = null
            }
        }.launchIn(repoScope)
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun attachFirestoreListeners(userId: String) {
        try {
            val db = getFirestore() ?: return
            customerListener?.remove()
            transactionListener?.remove()

            val custRef = db.collection("users").document(userId).collection("customers")
            customerListener = custRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    if (name.isNotBlank()) Customer(id, name, phone) else null
                }
                if (list.isNotEmpty()) {
                    customersFlow.value = list
                    saveCustomers(list)
                } else if (customersFlow.value.isNotEmpty()) {
                    uploadLocalToFirestore(userId)
                }
            }

            val txnRef = db.collection("users").document(userId).collection("transactions")
            transactionListener = txnRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: doc.id
                        val customerId = doc.getString("customerId") ?: ""
                        val amount = doc.getDouble("amount") ?: 0.0
                        val description = doc.getString("description") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val typeStr = doc.getString("type") ?: TransactionType.DUE.name
                        val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.DUE }
                        Transaction(id, customerId, amount, description, timestamp, type)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (list.isNotEmpty()) {
                    transactionsFlow.value = list
                    saveTransactions(list)
                }
            }
        } catch (e: Exception) {
            // Firestore not ready or offline
        }
    }

    private fun uploadLocalToFirestore(userId: String) {
        try {
            val db = getFirestore() ?: return
            val userDoc = db.collection("users").document(userId)
            customersFlow.value.forEach { c ->
                userDoc.collection("customers").document(c.id).set(
                    mapOf("id" to c.id, "name" to c.name, "phone" to c.phone)
                )
            }
            transactionsFlow.value.forEach { t ->
                userDoc.collection("transactions").document(t.id).set(
                    mapOf(
                        "id" to t.id,
                        "customerId" to t.customerId,
                        "amount" to t.amount,
                        "description" to t.description,
                        "timestamp" to t.timestamp,
                        "type" to t.type.name,
                    )
                )
            }
        } catch (e: Exception) {
            // Silently fallback to local
        }
    }

    override val customers: Flow<List<CustomerDue>> =
        combine(customersFlow, transactionsFlow) { customers, transactions ->
            customers.map { customer ->
                val mine = transactions.filter { it.customerId == customer.id }.sortedByDescending { it.timestamp }
                val due = mine.sumOf { if (it.type == TransactionType.DUE) it.amount else -it.amount }
                val lastActivity = mine.maxOfOrNull { it.timestamp } ?: 0L
                CustomerDue(customer, due, lastActivity, mine)
            }.sortedBy { it.customer.name }
        }

    override fun addTransaction(
        customerId: String,
        amount: Double,
        description: String,
        type: TransactionType,
    ) {
        val txn = Transaction(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            amount = amount,
            description = description.ifBlank { if (type == TransactionType.DUE) "Due Entry" else "Payment" },
            timestamp = System.currentTimeMillis(),
            type = type,
        )
        transactionsFlow.update { it + txn }
        saveTransactions(transactionsFlow.value)

        val userId = authRepository.currentUser.value?.uid
        if (userId != null) {
            try {
                getFirestore()?.collection("users")?.document(userId)
                    ?.collection("transactions")?.document(txn.id)
                    ?.set(
                        mapOf(
                            "id" to txn.id,
                            "customerId" to txn.customerId,
                            "amount" to txn.amount,
                            "description" to txn.description,
                            "timestamp" to txn.timestamp,
                            "type" to txn.type.name,
                        )
                    )
            } catch (e: Exception) {}
        }
    }

    override fun deleteTransaction(transactionId: String) {
        transactionsFlow.update { it.filterNot { t -> t.id == transactionId } }
        saveTransactions(transactionsFlow.value)

        val userId = authRepository.currentUser.value?.uid
        if (userId != null) {
            try {
                getFirestore()?.collection("users")?.document(userId)
                    ?.collection("transactions")?.document(transactionId)
                    ?.delete()
            } catch (e: Exception) {}
        }
    }

    override fun settleCustomer(customerId: String) {
        val current = transactionsFlow.value
        val due = current.filter { it.customerId == customerId }.sumOf { if (it.type == TransactionType.DUE) it.amount else -it.amount }
        if (due <= 0.0) return
        val payment = Transaction(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            amount = due,
            description = "Settlement",
            timestamp = System.currentTimeMillis(),
            type = TransactionType.PAYMENT,
        )
        transactionsFlow.update { it + payment }
        saveTransactions(transactionsFlow.value)

        val userId = authRepository.currentUser.value?.uid
        if (userId != null) {
            try {
                getFirestore()?.collection("users")?.document(userId)
                    ?.collection("transactions")?.document(payment.id)
                    ?.set(
                        mapOf(
                            "id" to payment.id,
                            "customerId" to payment.customerId,
                            "amount" to payment.amount,
                            "description" to payment.description,
                            "timestamp" to payment.timestamp,
                            "type" to payment.type.name,
                        )
                    )
            } catch (e: Exception) {}
        }
    }

    override fun addCustomer(name: String, phone: String) {
        val customer = Customer(UUID.randomUUID().toString(), name.trim(), phone.trim())
        customersFlow.update { it + customer }
        saveCustomers(customersFlow.value)

        val userId = authRepository.currentUser.value?.uid
        if (userId != null) {
            try {
                getFirestore()?.collection("users")?.document(userId)
                    ?.collection("customers")?.document(customer.id)
                    ?.set(mapOf("id" to customer.id, "name" to customer.name, "phone" to customer.phone))
            } catch (e: Exception) {}
        }
    }

    override fun deleteCustomer(customerId: String) {
        val toDeleteTxns = transactionsFlow.value.filter { it.customerId == customerId }
        customersFlow.update { it.filterNot { c -> c.id == customerId } }
        transactionsFlow.update { it.filterNot { t -> t.customerId == customerId } }
        saveCustomers(customersFlow.value)
        saveTransactions(transactionsFlow.value)

        val userId = authRepository.currentUser.value?.uid
        if (userId != null) {
            try {
                val db = getFirestore() ?: return
                db.collection("users").document(userId).collection("customers").document(customerId).delete()
                toDeleteTxns.forEach { t ->
                    db.collection("users").document(userId).collection("transactions").document(t.id).delete()
                }
            } catch (e: Exception) {}
        }
    }

    private fun saveTransactions(transactions: List<Transaction>) {
        val array = JSONArray()
        transactions.forEach { t ->
            array.put(JSONObject().apply {
                put("id", t.id)
                put("customerId", t.customerId)
                put("amount", t.amount)
                put("description", t.description)
                put("timestamp", t.timestamp)
                put("type", t.type.name)
            })
        }
        prefs.edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
    }

    private fun saveCustomers(customers: List<Customer>) {
        val array = JSONArray()
        customers.forEach { c ->
            array.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
            })
        }
        prefs.edit().putString(KEY_CUSTOMERS, array.toString()).apply()
    }

    private fun loadCustomers(): List<Customer> {
        val raw = prefs.getString(KEY_CUSTOMERS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Customer(o.getString("id"), o.getString("name"), o.getString("phone"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadTransactions(): List<Transaction> {
        val raw = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Transaction(
                    id = o.getString("id"),
                    customerId = o.getString("customerId"),
                    amount = o.getDouble("amount"),
                    description = o.getString("description"),
                    timestamp = o.getLong("timestamp"),
                    type = TransactionType.valueOf(o.getString("type")),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_CUSTOMERS = "customers_v2"
        private const val KEY_TRANSACTIONS = "transactions_v2"
    }
}