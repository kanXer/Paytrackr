package com.example.paytrackr.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class BusinessProfile(
    val name: String = "",
    val shopName: String = "",
    val upiId: String = "",
    val phone: String = "",
)

class AuthRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("paytrackr_auth", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isGuestMode = MutableStateFlow(prefs.getBoolean(KEY_GUEST_MODE, false))
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _profile = MutableStateFlow(loadLocalProfile())
    val profile: StateFlow<BusinessProfile> = _profile.asStateFlow()

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val auth = FirebaseAuth.getInstance()
                _currentUser.value = auth.currentUser
                auth.addAuthStateListener { firebaseAuth ->
                    _currentUser.value = firebaseAuth.currentUser
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        fetchCloudProfile(user.uid)
                    }
                }
                auth.currentUser?.let { fetchCloudProfile(it.uid) }
            }
        } catch (e: Exception) {
            // Firebase not yet initialized
        }
    }

    private fun loadLocalProfile(): BusinessProfile {
        return BusinessProfile(
            name = prefs.getString(KEY_PROFILE_NAME, "") ?: "",
            shopName = prefs.getString(KEY_PROFILE_SHOP, "") ?: "",
            upiId = prefs.getString(KEY_PROFILE_UPI, "") ?: "",
            phone = prefs.getString(KEY_PROFILE_PHONE, "") ?: "",
        )
    }

    fun saveProfile(profile: BusinessProfile) {
        _profile.value = profile
        prefs.edit()
            .putString(KEY_PROFILE_NAME, profile.name)
            .putString(KEY_PROFILE_SHOP, profile.shopName)
            .putString(KEY_PROFILE_UPI, profile.upiId)
            .putString(KEY_PROFILE_PHONE, profile.phone)
            .apply()

        val user = _currentUser.value
        if (user != null && isFirebaseAvailable()) {
            scope.launch {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(user.uid)
                        .collection("profile").document("business")
                        .set(
                            mapOf(
                                "name" to profile.name,
                                "shopName" to profile.shopName,
                                "upiId" to profile.upiId,
                                "phone" to profile.phone,
                            )
                        )
                } catch (e: Exception) {
                    // Silently ignore if offline
                }
            }
        }
    }

    private fun fetchCloudProfile(userId: String) {
        scope.launch {
            try {
                if (!isFirebaseAvailable()) return@launch
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(userId)
                    .collection("profile").document("business").get().await()
                if (doc.exists()) {
                    val p = BusinessProfile(
                        name = doc.getString("name") ?: _profile.value.name,
                        shopName = doc.getString("shopName") ?: _profile.value.shopName,
                        upiId = doc.getString("upiId") ?: _profile.value.upiId,
                        phone = doc.getString("phone") ?: _profile.value.phone,
                    )
                    _profile.value = p
                    prefs.edit()
                        .putString(KEY_PROFILE_NAME, p.name)
                        .putString(KEY_PROFILE_SHOP, p.shopName)
                        .putString(KEY_PROFILE_UPI, p.upiId)
                        .putString(KEY_PROFILE_PHONE, p.phone)
                        .apply()
                } else if (_profile.value.shopName.isNotBlank() || _profile.value.upiId.isNotBlank()) {
                    saveProfile(_profile.value)
                }
            } catch (e: Exception) {
                // Offline fallback
            }
        }
    }

    fun isFirebaseAvailable(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signIn(email: String, pass: String): Result<FirebaseUser?> {
        return try {
            val auth = FirebaseAuth.getInstance()
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            _isGuestMode.value = false
            prefs.edit().putBoolean(KEY_GUEST_MODE, false).apply()
            _currentUser.value = result.user
            result.user?.let { fetchCloudProfile(it.uid) }
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, pass: String, name: String = ""): Result<FirebaseUser?> {
        return try {
            val auth = FirebaseAuth.getInstance()
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            _isGuestMode.value = false
            prefs.edit().putBoolean(KEY_GUEST_MODE, false).apply()
            if (name.isNotBlank()) {
                try {
                    val update = UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
                    result.user?.updateProfile(update)?.await()
                } catch (e: Exception) {
                    // Ignore profile update error
                }
                saveProfile(_profile.value.copy(name = name.trim()))
            }
            _currentUser.value = result.user
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser?> {
        return try {
            val auth = FirebaseAuth.getInstance()
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            _isGuestMode.value = false
            prefs.edit().putBoolean(KEY_GUEST_MODE, false).apply()
            _currentUser.value = result.user
            result.user?.let { user ->
                if (_profile.value.name.isBlank() && !user.displayName.isNullOrBlank()) {
                    saveProfile(_profile.value.copy(name = user.displayName!!))
                }
                fetchCloudProfile(user.uid)
            }
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun hasPasswordLinked(): Boolean {
        val user = _currentUser.value ?: return false
        return user.providerData.any { it.providerId == com.google.firebase.auth.EmailAuthProvider.PROVIDER_ID }
    }

    suspend fun setOrUpdatePassword(password: String): Result<Unit> {
        return try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            val email = user.email ?: return Result.failure(Exception("No email associated with account"))

            val hasPassword = user.providerData.any { it.providerId == com.google.firebase.auth.EmailAuthProvider.PROVIDER_ID }
            if (!hasPassword) {
                try {
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
                    user.linkWithCredential(credential).await()
                } catch (linkError: Exception) {
                    user.updatePassword(password).await()
                }
            } else {
                user.updatePassword(password).await()
            }
            _currentUser.value = auth.currentUser
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            val auth = FirebaseAuth.getInstance()
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setGuestMode(enabled: Boolean) {
        _isGuestMode.value = enabled
        prefs.edit().putBoolean(KEY_GUEST_MODE, enabled).apply()
    }

    fun signOut() {
        try {
            if (isFirebaseAvailable()) {
                FirebaseAuth.getInstance().signOut()
            }
        } catch (e: Exception) {
            // Ignore
        }
        _currentUser.value = null
        setGuestMode(false)
    }

    companion object {
        private const val KEY_GUEST_MODE = "is_guest_mode_v2"
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_PROFILE_SHOP = "profile_shop"
        private const val KEY_PROFILE_UPI = "profile_upi"
        private const val KEY_PROFILE_PHONE = "profile_phone"
    }
}
