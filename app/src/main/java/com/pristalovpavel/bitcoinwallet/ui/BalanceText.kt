package com.pristalovpavel.bitcoinwallet.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun BalanceText(balanceState: Result<Long>) {
    val balance = when {
        balanceState.isSuccess -> {
            val balanceString = (balanceState.getOrNull() ?: 0L).toString()
            "Balance: $balanceString sat"
        }
        else -> "Error loading balance"
    }

    Text(text = balance, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
}