package com.pristalovpavel.bitcoinwallet.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun BalanceText(balanceState: Result<Long>) {
    val balance = when {
        balanceState.isSuccess -> {
            val balanceString = (balanceState.getOrNull() ?: 0L).toString()
            "Balance: $balanceString ssats"
        }
        else -> "Error loading balance"
    }

    Text(text = balance, style = MaterialTheme.typography.bodyLarge)
}