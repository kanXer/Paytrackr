package com.example.paytrackr.ui.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paytrackr.auth.AuthRepository
import com.example.paytrackr.auth.ForgotPasswordDialog
import com.example.paytrackr.auth.GoogleAuthHelper
import com.example.paytrackr.auth.GoogleLogoIcon
import com.example.paytrackr.auth.SetOrChangePasswordDialog
import com.example.paytrackr.auth.WelcomeAuthScreen
import com.example.paytrackr.data.Customer
import com.example.paytrackr.data.CustomerDue
import com.example.paytrackr.data.DefaultDataRepository
import com.example.paytrackr.data.Transaction
import com.example.paytrackr.data.TransactionType
import com.example.paytrackr.theme.PayTrackrTheme
import com.example.paytrackr.util.formatAmount
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

enum class CustomerFilter {
    ALL, DUE_ONLY, SETTLED, RECENT
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel? = null,
) {
    val context = LocalContext.current.applicationContext
    val vm: MainScreenViewModel = viewModel ?: viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val authRepo = AuthRepository(context)
                val dataRepo = DefaultDataRepository(context, authRepo)
                return MainScreenViewModel(dataRepo, authRepo) as T
            }
        }
    )

    val state by vm.uiState.collectAsStateWithLifecycle()
    val currentUser by vm.currentUser.collectAsStateWithLifecycle()
    val isGuestMode by vm.isGuestMode.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()

    val isAuthenticatedOrGuest = currentUser != null || isGuestMode

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (!isAuthenticatedOrGuest) {
            WelcomeAuthScreen(
                onSignInWithGoogle = { idToken, onRes -> vm.signInWithGoogle(idToken, onRes) },
                onSignIn = { email, pass, onRes -> vm.signIn(email, pass, onRes) },
                onSignUp = { email, pass, name, onRes -> vm.signUp(email, pass, name, onRes) },
                onContinueAsGuest = { guestName, guestShop ->
                    val finalShop = if (guestShop.isNotBlank()) guestShop else guestName
                    vm.saveProfile(profile.copy(name = guestName, shopName = finalShop))
                    vm.setGuestMode(true)
                },
                onSetOrUpdatePassword = { pass, onRes -> vm.setOrUpdatePassword(pass, onRes) },
                onResetPassword = { email, onRes -> vm.sendPasswordResetEmail(email, onRes) },
            )
        } else {
            when (val uiState = state) {
                MainScreenUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is MainScreenUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Unable to load data: ${uiState.throwable.localizedMessage}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                is MainScreenUiState.Success -> {
                    CustomerListScreen(
                        customers = uiState.customers,
                        totalOutstanding = uiState.totalOutstanding,
                        totalReceivedToday = uiState.totalReceivedToday,
                        totalReceivedThisMonth = uiState.totalReceivedThisMonth,
                        activeCustomers = uiState.activeCustomers,
                        onAddTransaction = { customerId, amount, description, type ->
                            vm.addTransaction(customerId, amount, description, type)
                        },
                        onAddCustomer = { name, phone ->
                            vm.addCustomer(name, phone)
                        },
                        onSettleCustomer = { customerId ->
                            vm.settleCustomer(customerId)
                        },
                        onDeleteTransaction = { txnId ->
                            vm.deleteTransaction(txnId)
                        },
                        onDeleteCustomer = { customerId ->
                            vm.deleteCustomer(customerId)
                        },
                        currentUser = currentUser,
                        profile = profile,
                        hasPasswordLinked = currentUser?.providerData?.any { it.providerId == "password" } == true,
                        onSaveProfile = { vm.saveProfile(it) },
                        onSignIn = { email, pass, onRes -> vm.signIn(email, pass, onRes) },
                        onSignUp = { email, pass, name, onRes -> vm.signUp(email, pass, name, onRes) },
                        onSignInWithGoogle = { idToken, onRes -> vm.signInWithGoogle(idToken, onRes) },
                        onSetOrUpdatePassword = { pass, onRes -> vm.setOrUpdatePassword(pass, onRes) },
                        onResetPassword = { email, onRes -> vm.sendPasswordResetEmail(email, onRes) },
                        onSignOut = { vm.signOut() },
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerListScreen(
    customers: List<CustomerDue>,
    onAddTransaction: (customerId: String, amount: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    CustomerListScreen(
        customers = customers,
        totalOutstanding = customers.sumOf { it.due },
        totalReceivedToday = 0.0,
        totalReceivedThisMonth = 0.0,
        activeCustomers = customers.count { it.due > 0 },
        onAddTransaction = { id, amount, _, _ -> onAddTransaction(id, amount) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    customers: List<CustomerDue>,
    totalOutstanding: Double = customers.sumOf { it.due },
    totalReceivedToday: Double = 0.0,
    totalReceivedThisMonth: Double = 0.0,
    activeCustomers: Int = customers.count { it.due > 0 },
    onAddTransaction: (customerId: String, amount: Double, description: String, type: TransactionType) -> Unit = { _, _, _, _ -> },
    onAddCustomer: (name: String, phone: String) -> Unit = { _, _ -> },
    onSettleCustomer: (customerId: String) -> Unit = {},
    onDeleteTransaction: (txnId: String) -> Unit = {},
    onDeleteCustomer: (customerId: String) -> Unit = {},
    currentUser: FirebaseUser? = null,
    profile: com.example.paytrackr.auth.BusinessProfile = com.example.paytrackr.auth.BusinessProfile(),
    hasPasswordLinked: Boolean = false,
    onSaveProfile: (com.example.paytrackr.auth.BusinessProfile) -> Unit = {},
    onSignIn: (String, String, (Result<FirebaseUser?>) -> Unit) -> Unit = { _, _, _ -> },
    onSignUp: (String, String, String, (Result<FirebaseUser?>) -> Unit) -> Unit = { _, _, _, _ -> },
    onSignInWithGoogle: (String, (Result<FirebaseUser?>) -> Unit) -> Unit = { _, _ -> },
    onSetOrUpdatePassword: (String, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onResetPassword: (String, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CustomerFilter.ALL) }

    var customerForTransaction by remember { mutableStateOf<CustomerDue?>(null) }
    var initialTransactionType by remember { mutableStateOf(TransactionType.DUE) }

    var selectedCustomerForSheet by remember { mutableStateOf<CustomerDue?>(null) }
    var transactionToDelete by remember { mutableStateOf<Pair<CustomerDue, Transaction>?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerDue?>(null) }
    var customerToSettle by remember { mutableStateOf<CustomerDue?>(null) }
    var customerForReminder by remember { mutableStateOf<CustomerDue?>(null) }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAuthBottomSheet by remember { mutableStateOf(false) }
    var initialCustomerName by remember { mutableStateOf("") }
    var initialCustomerPhone by remember { mutableStateOf("") }

    // Contact Picker Launcher for WhatsApp & Phone Contacts
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            try {
                val cursor = context.contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ),
                    null,
                    null,
                    null,
                )
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val phoneIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val pickedName = if (nameIdx != -1) c.getString(nameIdx) else ""
                        val pickedPhone = if (phoneIdx != -1) c.getString(phoneIdx) else ""
                        if (!pickedName.isNullOrBlank()) {
                            initialCustomerName = pickedName
                            initialCustomerPhone = pickedPhone ?: ""
                            showAddCustomerDialog = true
                            Toast.makeText(context, "Selected contact: $pickedName", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load contact details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        }
        try {
            contactPickerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Contact picker not available. Enter manually.", Toast.LENGTH_SHORT).show()
            initialCustomerName = ""
            initialCustomerPhone = ""
            showAddCustomerDialog = true
        }
    }

    // Keep sheet's customer synchronized with reactive updates
    val activeSheetCustomer = remember(customers, selectedCustomerForSheet) {
        selectedCustomerForSheet?.let { cur ->
            customers.find { it.customer.id == cur.customer.id } ?: cur
        }
    }

    // Filter and search customers
    val filteredCustomers = remember(customers, searchQuery, selectedFilter) {
        var result = customers
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            result = result.filter {
                it.customer.name.lowercase().contains(q) || it.customer.phone.contains(q)
            }
        }
        when (selectedFilter) {
            CustomerFilter.ALL -> result
            CustomerFilter.DUE_ONLY -> result.filter { it.due > 0 }
            CustomerFilter.SETTLED -> result.filter { it.due <= 0 }
            CustomerFilter.RECENT -> result.sortedByDescending { it.lastActivity }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Sleek & Premium Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: App Logo & Shop/Khata Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Spacer(modifier = Modifier.width(9.dp))
                Column {
                    Text(
                        text = "PayTrackr",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                    Text(
                        text = profile.shopName.ifBlank { "Khata & Dues" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            // Right: Profile & Cloud Sync Button
            Surface(
                onClick = { showAuthBottomSheet = true },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (currentUser != null) Color(0xFF00E599).copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile & Account",
                        modifier = Modifier.size(18.dp),
                        tint = if (currentUser != null) Color(0xFF00E599) else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = profile.name.ifBlank { profile.shopName.ifBlank { "Profile" } }.take(12),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    if (currentUser != null) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E599)),
                        )
                    }
                }
            }
        }

        // Nav ke Neeche: "Add from Contact" & "+ Add New"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Add from Contact Button
            Surface(
                onClick = { launchContactPicker() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 11.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Add from Contact",
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add from Contact",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }

            // + Add New Customer Button (Electric Crimson)
            Surface(
                onClick = {
                    initialCustomerName = ""
                    initialCustomerPhone = ""
                    showAddCustomerDialog = true
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 11.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add New",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Search Input with Clear Button
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search by customer name or phone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                    singleLine = true,
                )
            }

            // Financial Hero Metric Card
            item {
                HeroDashboardCard(
                    totalOutstanding = totalOutstanding,
                    totalReceivedToday = totalReceivedToday,
                    activeCustomers = activeCustomers,
                )
            }

            // Filter Chips Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == CustomerFilter.ALL,
                            onClick = { selectedFilter = CustomerFilter.ALL },
                            label = { Text("All (${customers.size})") },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == CustomerFilter.ALL,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == CustomerFilter.DUE_ONLY,
                            onClick = { selectedFilter = CustomerFilter.DUE_ONLY },
                            label = { Text("Due Only ($activeCustomers)") },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.tertiary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == CustomerFilter.DUE_ONLY,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.tertiary,
                            ),
                        )
                    }
                    item {
                        val settledCount = customers.count { it.due <= 0 }
                        FilterChip(
                            selected = selectedFilter == CustomerFilter.SETTLED,
                            onClick = { selectedFilter = CustomerFilter.SETTLED },
                            label = { Text("Settled ($settledCount)") },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == CustomerFilter.SETTLED,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == CustomerFilter.RECENT,
                            onClick = { selectedFilter = CustomerFilter.RECENT },
                            label = { Text("Recent Activity") },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == CustomerFilter.RECENT,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            ),
                        )
                    }
                }
            }

            // Customer List
            if (filteredCustomers.isEmpty()) {
                item {
                    EmptyCustomerPlaceholder(
                        isSearching = searchQuery.isNotEmpty(),
                        onClearSearch = { searchQuery = "" },
                        onPickContact = { launchContactPicker() },
                        onAddCustom = {
                            initialCustomerName = ""
                            initialCustomerPhone = ""
                            showAddCustomerDialog = true
                        },
                    )
                }
            } else {
                items(
                    items = filteredCustomers,
                    key = { it.customer.id },
                ) { customerDue ->
                    CustomerCard(
                        customerDue = customerDue,
                        onAddEntryClick = {
                            customerForTransaction = customerDue
                            initialTransactionType = TransactionType.DUE
                        },
                        onRemindClick = {
                            customerForReminder = customerDue
                        },
                        onSettleClick = {
                            customerToSettle = customerDue
                        },
                        onCardClick = {
                            selectedCustomerForSheet = customerDue
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    // Modal BottomSheet for Customer Details & Entry History
    activeSheetCustomer?.let { customerDue ->
        CustomerDetailBottomSheet(
            customerDue = customerDue,
            shopName = profile.shopName,
            upiId = profile.upiId,
            onDismiss = { selectedCustomerForSheet = null },
            onAddDueClick = {
                customerForTransaction = customerDue
                initialTransactionType = TransactionType.DUE
            },
            onAddPaymentClick = {
                customerForTransaction = customerDue
                initialTransactionType = TransactionType.PAYMENT
            },
            onSettleClick = {
                customerToSettle = customerDue
            },
            onDeleteEntryClick = { txn ->
                transactionToDelete = customerDue to txn
            },
            onDeleteCustomerClick = {
                customerToDelete = customerDue
            },
            onRemindClick = {
                customerForReminder = customerDue
            },
        )
    }

    // Modal BottomSheet for Business Profile, UPI & Cloud Sync
    if (showAuthBottomSheet) {
        ProfileAndAuthBottomSheet(
            currentUser = currentUser,
            profile = profile,
            hasPasswordLinked = hasPasswordLinked,
            onSaveProfile = onSaveProfile,
            onDismiss = { showAuthBottomSheet = false },
            onSignIn = onSignIn,
            onSignUp = onSignUp,
            onSignInWithGoogle = onSignInWithGoogle,
            onSetOrUpdatePassword = onSetOrUpdatePassword,
            onResetPassword = onResetPassword,
            onSignOut = onSignOut,
        )
    }

    // Add Transaction Dialog (Compatible with tests asserting 'New transaction', 'Amount', 'Add')
    customerForTransaction?.let { customerDue ->
        AddTransactionDialog(
            customerDue = customerDue,
            initialType = initialTransactionType,
            onDismiss = { customerForTransaction = null },
            onConfirm = { amount, desc, type ->
                onAddTransaction(customerDue.customer.id, amount, desc, type)
                customerForTransaction = null
                Toast.makeText(context, "Entry of ${formatAmount(amount)} added", Toast.LENGTH_SHORT).show()
            },
        )
    }

    // Payment Reminder Options Dialog (WhatsApp, WhatsApp Business, SMS)
    customerForReminder?.let { customerDue ->
        ReminderOptionsDialog(
            customerDue = customerDue,
            shopName = profile.shopName,
            upiId = profile.upiId,
            onDismiss = { customerForReminder = null },
        )
    }

    // Add Customer Dialog with Contact Picker and Custom Entry
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            initialName = initialCustomerName,
            initialPhone = initialCustomerPhone,
            onPickContact = { launchContactPicker() },
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone ->
                onAddCustomer(name, phone)
                showAddCustomerDialog = false
                Toast.makeText(context, "Customer $name added", Toast.LENGTH_SHORT).show()
            },
        )
    }

    // Delete Entry Confirmation Dialog
    transactionToDelete?.let { (customer, txn) ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Text(
                    text = "Delete Entry?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this ${if (txn.type == TransactionType.DUE) "Due" else "Payment"} entry of ${formatAmount(txn.amount)} (${txn.description.ifBlank { "Entry" }}) for ${customer.customer.name}?\n\nThe outstanding balance will be automatically recalculated.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(txn.id)
                        transactionToDelete = null
                        Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Delete Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    // Delete Customer Confirmation Dialog
    customerToDelete?.let { customerDue ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = {
                Text(
                    text = "Delete Customer?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ${customerDue.customer.name} and all their ${customerDue.transactions.size} entries? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomer(customerDue.customer.id)
                        customerToDelete = null
                        selectedCustomerForSheet = null
                        Toast.makeText(context, "${customerDue.customer.name} deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Delete Customer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    // Settle Customer Confirmation Dialog
    customerToSettle?.let { customerDue ->
        AlertDialog(
            onDismissRequest = { customerToSettle = null },
            title = {
                Text(
                    text = "Settle All Dues",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Clear all outstanding dues for ${customerDue.customer.name}? This will record a settlement payment of ${formatAmount(customerDue.due)} and set the remaining balance to ₹0.00.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSettleCustomer(customerDue.customer.id)
                        customerToSettle = null
                        Toast.makeText(context, "Account settled for ${customerDue.customer.name}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Confirm Settle", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToSettle = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun HeroDashboardCard(
    totalOutstanding: Double,
    totalReceivedToday: Double,
    activeCustomers: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = "TOTAL OUTSTANDING (YOU WILL GET)",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatAmount(totalOutstanding),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                ),
                color = if (totalOutstanding > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp,
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Today's Collection
                Column {
                    Text(
                        text = "Received Today",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatAmount(totalReceivedToday),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color(0xFF00E599),
                    )
                }

                // Active Accounts Count
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Active Customers",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$activeCustomers with dues",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerCard(
    customerDue: CustomerDue,
    onAddEntryClick: () -> Unit,
    onRemindClick: () -> Unit,
    onSettleClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDue = customerDue.due > 0
    val avatarGradient = remember(customerDue.customer.name) {
        getAvatarGradient(customerDue.customer.name)
    }
    val initials = remember(customerDue.customer.name) {
        val parts = customerDue.customer.name.trim().split(" ")
        if (parts.size >= 2) {
            "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        } else {
            customerDue.customer.name.take(2).uppercase()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDue) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
        ) {
            // Header: Avatar, Name, Phone, and Reminder Bell
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Gradient Initials Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(avatarGradient)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Name & Phone
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customerDue.customer.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = customerDue.customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Direct 1-tap WhatsApp Reminder on Front of Card
                if (isDue) {
                    Surface(
                        onClick = onRemindClick,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.45f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Send WhatsApp Reminder",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remind",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF25D366),
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF00E599).copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = "Settled ✓",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E599),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount status & Action buttons (Settle + Entry)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = if (isDue) "You will get" else "Account Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDue) formatAmount(customerDue.due) else "Settled ✓",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp,
                        ),
                        color = if (isDue) MaterialTheme.colorScheme.tertiary else Color(0xFF00E599),
                    )
                }

                // Action buttons: Settle (if due) + Entry
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isDue) {
                        Surface(
                            onClick = onSettleClick,
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00E599).copy(alpha = 0.14f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFF00E599).copy(alpha = 0.5f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Settle Due",
                                    tint = Color(0xFF00E599),
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Settle",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E599),
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onAddEntryClick,
                        modifier = Modifier.semantics {
                            text = AnnotatedString("Add transaction")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = "+ Entry",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailBottomSheet(
    customerDue: CustomerDue,
    shopName: String = "",
    upiId: String = "",
    onDismiss: () -> Unit,
    onAddDueClick: () -> Unit,
    onAddPaymentClick: () -> Unit,
    onSettleClick: () -> Unit,
    onDeleteEntryClick: (Transaction) -> Unit,
    onDeleteCustomerClick: () -> Unit,
    onRemindClick: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val isDue = customerDue.due > 0
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            // Customer Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = customerDue.customer.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = customerDue.customer.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = onDeleteCustomerClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete customer",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Balance Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDue) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                else Color(0xFF00E599).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDue) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                    else Color(0xFF00E599).copy(alpha = 0.4f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = if (isDue) "PENDING DUE (YOU WILL GET)" else "ALL SETTLED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = if (isDue) MaterialTheme.colorScheme.tertiary else Color(0xFF00E599),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatAmount(customerDue.due),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                            ),
                            color = if (isDue) MaterialTheme.colorScheme.tertiary else Color(0xFF00E599),
                        )
                    }

                    // Direct Call / WhatsApp Shortcuts
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (customerDue.customer.phone.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customerDue.customer.phone}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.onSurface)
                            }

                            IconButton(
                                onClick = onRemindClick,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Remind", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Premium Fast Transaction Hub: + Gave (Due), + Received (Payment), and Settle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 1. + GAVE (Udhar Diya / Due +)
                Surface(
                    onClick = onAddDueClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2B1015),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFFF2E4D).copy(alpha = 0.5f),
                    ),
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF2E4D).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Gave Due",
                                tint = Color(0xFFFF2E4D),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "+ Gave",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF2E4D),
                        )
                        Text(
                            text = "Udhar (+)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 2. + RECEIVED (Jama Mila / Got -)
                Surface(
                    onClick = onAddPaymentClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF092419),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF00E599).copy(alpha = 0.5f),
                    ),
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E599).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Received Payment",
                                tint = Color(0xFF00E599),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "+ Got",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E599),
                        )
                        Text(
                            text = "Jama (-)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 3. SETTLE (Pura Hisab Clear ✓)
                Surface(
                    onClick = onSettleClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDue) Color(0xFF132238) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDue) Color(0xFF38BDF8).copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    ),
                    shadowElevation = if (isDue) 2.dp else 0.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDue) Color(0xFF38BDF8).copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surface,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Settle Account",
                                tint = if (isDue) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Settle",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDue) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Text(
                            text = if (isDue) "Clear Due" else "Settled ✓",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Entries List Header
            Text(
                text = "Entries History (${customerDue.transactions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Transactions History List
            if (customerDue.transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No entries recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(customerDue.transactions, key = { it.id }) { txn ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = txn.description.ifBlank {
                                            if (txn.type == TransactionType.DUE) "Due Entry" else "Payment Received"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateFormat.format(Date(txn.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = if (txn.type == TransactionType.DUE) "+${formatAmount(txn.amount)}"
                                        else "-${formatAmount(txn.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (txn.type == TransactionType.DUE) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.primary,
                                    )

                                    // Clear Delete Entry Button
                                    IconButton(
                                        onClick = { onDeleteEntryClick(txn) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete entry",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AddTransactionDialog(
    customerDue: CustomerDue,
    initialType: TransactionType = TransactionType.DUE,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, description: String, type: TransactionType) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf(initialType) }

    val amountValue = amountText.toDoubleOrNull() ?: 0.0
    val isAmountValid = amountValue > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New transaction",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Customer: ${customerDue.customer.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Type Toggle: Gave (Due) vs Got (Payment)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = transactionType == TransactionType.DUE,
                        onClick = { transactionType = TransactionType.DUE },
                        label = { Text("Gave (Due +)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                            selectedLabelColor = MaterialTheme.colorScheme.tertiary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = transactionType == TransactionType.DUE,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    )

                    FilterChip(
                        selected = transactionType == TransactionType.PAYMENT,
                        onClick = { transactionType = TransactionType.PAYMENT },
                        label = { Text("Got (Paid -)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = transactionType == TransactionType.PAYMENT,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }

                // Amount Field (Label 'Amount' strictly maintained for UI test compatibility)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            amountText = input
                        }
                    },
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            "₹",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Quick Amount Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("100", "500", "1000", "2000").forEach { preset ->
                        Surface(
                            onClick = { amountText = preset },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "+$preset",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                    }
                }

                // Description Field
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description (e.g. Groceries)") },
                    placeholder = { Text("Note / Item name") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Quick Tag Suggestions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("Groceries", "Cash", "Recharge", "Milk").forEach { tag ->
                        Surface(
                            onClick = { descriptionText = tag },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isAmountValid) {
                        onConfirm(amountValue, descriptionText.trim(), transactionType)
                    }
                },
                enabled = isAmountValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
fun AddCustomerDialog(
    initialName: String = "",
    initialPhone: String = "",
    onPickContact: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String) -> Unit,
) {
    var nameText by remember(initialName) { mutableStateOf(initialName) }
    var phoneText by remember(initialPhone) { mutableStateOf(initialPhone) }

    val isValid = nameText.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Customer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Prominent WhatsApp & Phone Contacts Picker Button
                Surface(
                    onClick = onPickContact,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Select from Contacts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = "Browse ➔",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Divider: Or Enter Custom Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        text = "  OR  ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Customer Name") },
                    placeholder = { Text("e.g. Ramesh Patel") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = phoneText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }) {
                            phoneText = input
                        }
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+91 98765 43210") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(nameText.trim(), phoneText.trim())
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Save Customer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
fun EmptyCustomerPlaceholder(
    isSearching: Boolean,
    onClearSearch: () -> Unit,
    onPickContact: () -> Unit,
    onAddCustom: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearching) "No matching customers found" else "No customers yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isSearching) {
                "Try searching with another name or phone number."
            } else {
                "Track customer credit (Udhar) & payments with ease."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (isSearching) {
            OutlinedButton(
                onClick = onClearSearch,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Clear Search")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPickContact,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Contact")
                }

                OutlinedButton(
                    onClick = onAddCustom,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Custom")
                }
            }
        }
    }
}

fun getAvatarGradient(name: String): List<Color> {
    val gradients = listOf(
        listOf(Color(0xFFFF2E4D), Color(0xFFC70039)), // Electric Crimson -> Deep Ruby
        listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)), // Sunset Scarlet -> Coral Flame
        listOf(Color(0xFFE11D48), Color(0xFF881337)), // Rose -> Dark Burgundy
        listOf(Color(0xFFFF3366), Color(0xFFBA135D)), // Neon Red -> Maroon
        listOf(Color(0xFFE50914), Color(0xFF990000)), // True Red -> Crimson Wine
        listOf(Color(0xFFFF5E62), Color(0xFFFF9966)), // Red Flame -> Warm Coral
    )
    val index = (name.hashCode().absoluteValue) % gradients.size
    return gradients[index]
}

@Preview(showBackground = true)
@Composable
fun CustomerListScreenPreview() {
    PayTrackrTheme {
        CustomerListScreen(
            customers = listOf(
                CustomerDue(Customer("1", "Ravi Kumar", "+91 98765 43210"), 1500.0),
                CustomerDue(Customer("2", "Priya Sharma", "+91 98123 45678"), 850.5),
                CustomerDue(Customer("3", "Ahmed Khan", "+91 99001 12233"), 220.0),
            ),
            totalOutstanding = 2570.5,
            totalReceivedToday = 0.0,
            activeCustomers = 3,
            onAddTransaction = { _, _, _, _ -> },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndAuthBottomSheet(
    currentUser: FirebaseUser?,
    profile: com.example.paytrackr.auth.BusinessProfile,
    hasPasswordLinked: Boolean = false,
    onSaveProfile: (com.example.paytrackr.auth.BusinessProfile) -> Unit,
    onDismiss: () -> Unit,
    onSignIn: (String, String, (Result<FirebaseUser?>) -> Unit) -> Unit,
    onSignUp: (String, String, String, (Result<FirebaseUser?>) -> Unit) -> Unit,
    onSignInWithGoogle: (String, (Result<FirebaseUser?>) -> Unit) -> Unit = { _, _ -> },
    onSetOrUpdatePassword: (String, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onResetPassword: (String, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onSignOut: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Shop & UPI, 1: Cloud Sync

    // Profile form state
    var shopName by remember(profile) { mutableStateOf(profile.shopName) }
    var ownerName by remember(profile) { mutableStateOf(profile.name) }
    var upiId by remember(profile) { mutableStateOf(profile.upiId) }

    // Auth form state
    var isSignUp by remember { mutableStateOf(false) }
    var regName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showPasswordDialog by remember { mutableStateOf(false) }
    var isPasswordInitialSetup by remember { mutableStateOf(false) }
    var targetEmailForPasswordDialog by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                isGoogleLoading = true
                errorMessage = null
                onSignInWithGoogle(idToken) { authRes ->
                    isGoogleLoading = false
                    authRes.onSuccess { user ->
                        Toast.makeText(context, "Google Sign-In successful!", Toast.LENGTH_SHORT).show()
                        val hasPass = user?.providerData?.any { it.providerId == "password" } == true
                        if (!hasPass && user != null) {
                            targetEmailForPasswordDialog = user.email ?: ""
                            isPasswordInitialSetup = true
                            showPasswordDialog = true
                        } else {
                            onDismiss()
                        }
                    }.onFailure { err ->
                        errorMessage = err.localizedMessage ?: "Google sign-in failed"
                    }
                }
            } else {
                isGoogleLoading = false
                errorMessage = "Google token not found. Please verify Google Play Services."
            }
        } catch (e: ApiException) {
            isGoogleLoading = false
            if (e.statusCode == 12501 || e.statusCode == CommonStatusCodes.CANCELED) {
                // User cancelled sign in
            } else if (e.statusCode == 10) {
                errorMessage = "Google Sign-In configuration error (Code 10: DEVELOPER_ERROR). Please ensure SHA-1 fingerprint is added in Firebase Console."
            } else {
                errorMessage = "Google Sign-In error (${e.statusCode}): ${e.localizedMessage}"
            }
        }
    }

    if (showPasswordDialog) {
        SetOrChangePasswordDialog(
            userEmail = targetEmailForPasswordDialog.ifBlank { currentUser?.email ?: "" },
            isInitialSetup = isPasswordInitialSetup,
            onDismiss = {
                showPasswordDialog = false
                if (isPasswordInitialSetup) {
                    onDismiss()
                }
            },
            onSubmit = { newPass, callback ->
                onSetOrUpdatePassword(newPass) { res ->
                    callback(res)
                    if (res.isSuccess) {
                        showPasswordDialog = false
                        if (isPasswordInitialSetup) {
                            onDismiss()
                        }
                    }
                }
            },
        )
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            onDismiss = { showForgotPasswordDialog = false },
            onSubmit = { resetEmail, callback ->
                onResetPassword(resetEmail, callback)
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (selectedTab == 0) "Profile & UPI" else "Cloud Sync & Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two Tabs: Shop & UPI Profile | Cloud Sync
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
            ) {
                Surface(
                    onClick = { selectedTab = 0 },
                    shape = RoundedCornerShape(11.dp),
                    color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "🏪 Shop & UPI",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                Surface(
                    onClick = { selectedTab = 1 },
                    shape = RoundedCornerShape(11.dp),
                    color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "☁️ Account & Sync",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            if (currentUser != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E599)),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (selectedTab == 0) {
                // TAB 0: Shop & UPI Profile Details
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop / Business Name") },
                    placeholder = { Text("e.g. Ramesh Kirana Store") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner / Your Name") },
                    placeholder = { Text("e.g. Ramesh Sharma") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it.trim() },
                    label = { Text("UPI ID (VPA)") },
                    placeholder = { Text("e.g. 9876543210@paytm") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val updated = profile.copy(
                            shopName = shopName.trim(),
                            name = ownerName.trim(),
                            upiId = upiId.trim(),
                        )
                        onSaveProfile(updated)
                        Toast.makeText(context, "Business & UPI details saved!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Save Profile & UPI", fontWeight = FontWeight.Bold)
                }

                if (currentUser != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            targetEmailForPasswordDialog = currentUser.email ?: ""
                            isPasswordInitialSetup = !hasPasswordLinked
                            showPasswordDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (hasPasswordLinked) "Change Password" else "Set Password for Email Login",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            } else {
                // TAB 1: Cloud Sync & Authentication
                if (currentUser != null) {
                    // Logged in State
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E599)),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cloud Sync Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E599),
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentUser.email ?: "Logged in",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (profile.name.isNotBlank() || profile.shopName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${profile.name.ifBlank { "" }} • ${profile.shopName.ifBlank { "" }}".trim().removePrefix("•").removeSuffix("•"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All data is backed up in real-time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Security & Password Management Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (hasPasswordLinked) "Password Protection" else "Email Login Password",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = if (hasPasswordLinked) "Password is set for email login" else "No password set yet (Google only)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    targetEmailForPasswordDialog = currentUser.email ?: ""
                                    isPasswordInitialSetup = !hasPasswordLinked
                                    showPasswordDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (hasPasswordLinked) "Change Password" else "Set Password for Email Login",
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onSignOut()
                            onDismiss()
                            Toast.makeText(context, "Signed out of cloud account", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text("Sign Out", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                } else {
                    // Not Logged In State:
                    // 1. Google Sign-In Button
                    Surface(
                        onClick = {
                            if (!isGoogleLoading) {
                                val webClientId = GoogleAuthHelper.getGoogleWebClientId(context)
                                if (webClientId.isBlank() || webClientId.contains("placeholder")) {
                                    Toast.makeText(
                                        context,
                                        "Note: Configure Google Web Client ID and SHA-1 in Firebase Console.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                                try {
                                    val client = GoogleAuthHelper.getGoogleSignInClient(context)
                                    client.signOut().addOnCompleteListener {
                                        googleSignInLauncher.launch(client.signInIntent)
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Google Sign-In launch failed: ${e.localizedMessage}"
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Connecting to Google...", fontWeight = FontWeight.SemiBold)
                            } else {
                                GoogleLogoIcon(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Continue with Google",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "  OR WITH EMAIL  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sign In / Register toggle tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                    ) {
                        Surface(
                            onClick = {
                                isSignUp = false
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isSignUp) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (!isSignUp) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text("Sign In", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Surface(
                            onClick = {
                                isSignUp = true
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSignUp) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSignUp) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text("Register", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // In Register mode, take Name as well!
                    if (isSignUp) {
                        OutlinedTextField(
                            value = regName,
                            onValueChange = {
                                regName = it
                                errorMessage = null
                            },
                            label = { Text("Your Name / Owner Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password (min 6 chars)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (!isSignUp) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = { showForgotPasswordDialog = true },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    "Forgot Password?",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || password.length < 6) {
                                errorMessage = "Please enter a valid email and 6+ character password"
                                return@Button
                            }
                            if (isSignUp && regName.isBlank()) {
                                errorMessage = "Please enter your name"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            if (isSignUp) {
                                onSignUp(email.trim(), password, regName.trim()) { res ->
                                    isLoading = false
                                    res.onSuccess {
                                        Toast.makeText(context, "Account created & synced!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }.onFailure { e ->
                                        errorMessage = e.localizedMessage ?: "Sign up failed"
                                    }
                                }
                            } else {
                                onSignIn(email.trim(), password) { res ->
                                    isLoading = false
                                    res.onSuccess {
                                        Toast.makeText(context, "Welcome back! Data synced.", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }.onFailure { e ->
                                        errorMessage = e.localizedMessage ?: "Sign in failed"
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && email.isNotBlank() && password.length >= 6 && (!isSignUp || regName.isNotBlank()),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(if (isSignUp) "Create Account & Sync" else "Sign In & Sync", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Continue as Guest")
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}