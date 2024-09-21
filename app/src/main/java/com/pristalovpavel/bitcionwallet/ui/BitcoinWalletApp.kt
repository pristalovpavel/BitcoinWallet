package com.pristalovpavel.bitcionwallet.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pristalovpavel.bitcionwallet.ui.theme.BitcoinWalletTheme
import com.pristalovpavel.bitcionwallet.utils.readDataFromFile
import com.pristalovpavel.bitcionwallet.viewmodel.BitcoinViewModel

@Composable
fun BitcoinWalletApp (viewModel: BitcoinViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val privateKey = remember { readDataFromFile(context = context, "private_key.txt") }
    val myAddress = remember { readDataFromFile(context = context, "my_address.txt") }

    val balanceState by viewModel.balance.collectAsState()
    val transactionStatus by viewModel.transactionStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadBalance(myAddress)
    }

    BitcoinWalletTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "transactionScreen") {
                composable("transactionScreen") {
                    TransactionScreen(
                        navController = navController,
                        viewModel = viewModel,
                        walletAddress = myAddress
                    )
                }
                composable("sendScreen") {
                    SendScreenContent(
                        balanceState = balanceState,
                        transactionStatus = transactionStatus,
                        onSendClick = { amount, address ->
                            viewModel.sendBitcoinTransaction(myAddress, privateKey, address, amount.trim().toLong())
                        },
                        onSendMoreClick = {
                            viewModel.loadBalance(myAddress)
                        },
                    )
                }
            }
        }
    }
}
