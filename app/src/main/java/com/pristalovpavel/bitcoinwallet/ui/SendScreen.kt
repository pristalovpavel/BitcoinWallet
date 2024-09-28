package com.pristalovpavel.bitcoinwallet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

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

    val currentTransactionStatus by rememberUpdatedState(newValue = transactionStatus)

    LaunchedEffect(currentTransactionStatus) {
        if (currentTransactionStatus.isSuccess && currentTransactionStatus.getOrNull()?.isNotEmpty() == true) {
            transactionId.value = currentTransactionStatus.getOrNull() ?: ""
            showDialog.value = true
        } else if (currentTransactionStatus.isFailure) {
            errorMessage.value = currentTransactionStatus.exceptionOrNull()?.message ?: "Transaction Failed"
            showDialog.value = true
        }
    }

    SendScreenContent(balanceState = balanceState, onSendClick = onSendClick)

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