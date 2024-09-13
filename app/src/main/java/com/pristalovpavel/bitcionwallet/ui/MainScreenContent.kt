package com.pristalovpavel.bitcionwallet.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun MainScreenContent(
    balanceState: Result<Long>,
    transactionStatus: Result<String>,
    onSendClick: (String, String) -> Unit
) {
    val context = LocalContext.current

    val balance = when {
        balanceState.isSuccess -> (balanceState.getOrNull() ?: 0L).toString()
        balanceState.isFailure -> "Error"
        else -> "Loading"
    }

    MainScreen(balance = balance, onSendClick = onSendClick)

    LaunchedEffect(transactionStatus) {
        if (transactionStatus.isSuccess && transactionStatus.getOrNull()?.isNotEmpty() == true) {
            Toast.makeText(context, "Transaction Successful: ${transactionStatus.getOrNull()}", Toast.LENGTH_LONG).show()
        } else if (transactionStatus.isFailure) {
            Toast.makeText(context, "Transaction Failed", Toast.LENGTH_LONG).show()
        }
    }
}