package com.example.bankingapp


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bankingapp.data.BankRepository
import com.example.bankingapp.data.model.AccountHolder
import com.example.bankingapp.screens.AccountDetailsScreen
import com.example.bankingapp.screens.AccountListScreen
import com.example.bankingapp.screens.AddAccountDialog
import com.example.bankingapp.viewmodel.BankViewModel
import com.example.bankingapp.viewmodel.SimpleVMFactory


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = BankRepository(filesDir.resolve("bankdata.json"))

        setContent {
            val vm: BankViewModel = viewModel(factory = SimpleVMFactory { BankViewModel(repository) })
            var showAdd by remember { mutableStateOf(false) }
            var showResetConfirm by remember { mutableStateOf(false) }
            var selectedAccountNumber by remember { mutableStateOf<String?>(null) }

            // Always derive the selected account fresh from vm.accounts
            // This is the key fix — stale selectedAccount won't update after deposit/withdraw
            val selectedAccount = selectedAccountNumber?.let { accNo ->
                vm.accounts.find { it.accountNumber == accNo }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("🏦 Overseas Bank App") },
                        actions = {
                            IconButton(onClick = { showResetConfirm = true }) { Text("🔄") }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { showAdd = true }) { Text("+") }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    if (selectedAccount == null) {
                        AccountListScreen(vm.accounts) { selectedAccountNumber = it.accountNumber }
                    } else {
                        AccountDetailsScreen(
                            account = selectedAccount,
                            onBack = { selectedAccountNumber = null },
                            vm = vm
                        )
                    }
                }
            }

            if (showAdd) {
                AddAccountDialog(onDismiss = { showAdd = false }) { name, accNo ->
                    vm.addAccount(name, accNo)
                    showAdd = false
                }
            }

            if (showResetConfirm) {
                AlertDialog(
                    onDismissRequest = { showResetConfirm = false },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.resetData()
                            selectedAccountNumber = null  // clear selection on reset
                            showResetConfirm = false
                        }) { Text("Confirm") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
                    },
                    title = { Text("Reset Data?") },
                    text = { Text("This will delete all accounts and transactions.") }
                )
            }
        }
    }
}