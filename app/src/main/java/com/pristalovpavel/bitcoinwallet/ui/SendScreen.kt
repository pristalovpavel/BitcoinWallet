package com.pristalovpavel.bitcoinwallet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun SendScreen(
    balanceState: Result<Long>,
    transactionStatus: Result<String>,
    onSendClick: (String, String) -> Unit,
    onSendMoreClick: () -> Unit,
    onDialogDismiss: () -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }
    val transactionId = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf("") }

    val balance = when {
        balanceState.isSuccess -> (balanceState.getOrNull() ?: 0L).toString()
        balanceState.isFailure -> "Error"
        else -> "Loading"
    }

    LaunchedEffect(transactionStatus) {
        if (transactionStatus.isSuccess && transactionStatus.getOrNull()?.isNotEmpty() == true) {
            transactionId.value = transactionStatus.getOrNull() ?: ""
            showDialog.value = true
        } else if (transactionStatus.isFailure) {
            errorMessage.value = transactionStatus.exceptionOrNull()?.message ?: "Transaction Failed"
            showDialog.value = true
        }
    }

    SendScreenContent(balance = balance, onSendClick = onSendClick)

    if (showDialog.value) {
        TransactionResultDialog(
            transactionId = transactionId.value,
            errorMessage = errorMessage.value,
            onDismiss = {
                showDialog.value = false
                onSendMoreClick()
                onDialogDismiss()
            }
        )
    }
}