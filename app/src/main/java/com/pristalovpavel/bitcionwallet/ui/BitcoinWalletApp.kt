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
import com.pristalovpavel.bitcionwallet.ui.theme.BitcionWalletTheme
import com.pristalovpavel.bitcionwallet.utils.readPrivateKeyFromFile
import com.pristalovpavel.bitcionwallet.viewmodel.BitcoinViewModel
import java.math.BigDecimal

@Composable
fun BitcoinWalletApp (viewModel: BitcoinViewModel) {
    val context = LocalContext.current

    val privateKey = remember { readPrivateKeyFromFile(context = context, "private_key.txt") }
    val myAddress = remember { readPrivateKeyFromFile(context = context, "my_address.txt") }

    val balanceState by viewModel.balance.collectAsState()
    val transactionStatus by viewModel.transactionStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadBalance(myAddress)
    }

    BitcionWalletTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MainScreenContent(
                balanceState = balanceState,
                transactionStatus = transactionStatus,
                onSendClick = { amount, address ->
                    viewModel.sendTransaction(privateKey, address, BigDecimal(amount))
                }
            )
        }
    }
}
