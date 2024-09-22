package com.pristalovpavel.bitcoinwallet.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pristalovpavel.bitcoinwallet.ui.theme.BitcoinWalletTheme
import com.pristalovpavel.bitcoinwallet.utils.readDataFromFile
import com.pristalovpavel.bitcoinwallet.viewmodel.BitcoinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitcoinWalletApp(bitcoinViewModel: BitcoinViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val privateKey = remember { readDataFromFile(context = context, "private_key.txt") }
    val myAddress = remember { readDataFromFile(context = context, "my_address.txt") }

    val balanceState by bitcoinViewModel.balance.collectAsState()
    val transactionStatus by bitcoinViewModel.transactionStatus.collectAsState()

    LaunchedEffect(Unit) {
        bitcoinViewModel.loadBalance(myAddress)
    }

    BitcoinWalletTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
                    val showBackButton = currentBackStackEntry?.destination?.route == "sendScreen"

                    TopAppBar(
                        title = {
                            Text(
                                text = "Bitcoin Wallet",
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.headlineMedium
                                    .copy(fontWeight = FontWeight.Medium)
                            )
                        },
                        navigationIcon = {
                            if (showBackButton) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.mediumTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                content = { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "transactionScreen",
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable("transactionScreen") {
                            TransactionScreen(
                                navController = navController,
                                viewModel = bitcoinViewModel,
                                walletAddress = myAddress
                            )
                        }
                        composable("sendScreen") {
                            SendScreen(
                                balanceState = balanceState,
                                transactionStatus = transactionStatus,
                                onSendClick = { amount, address ->
                                    bitcoinViewModel.sendBitcoinTransaction(
                                        myAddress,
                                        privateKey,
                                        address,
                                        amount.trim().toLong()
                                    )
                                },
                                onSendMoreClick = {
                                    bitcoinViewModel.loadBalance(myAddress)
                                },
                                onDialogDismiss = {
                                    bitcoinViewModel.resetTransactionStatus()
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}
