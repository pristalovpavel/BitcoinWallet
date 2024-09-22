package com.pristalovpavel.bitcoinwallet.model

import androidx.compose.ui.graphics.Color

enum class TransactionType (val title: String, val color: Color) {
    INCOME("Received", Color.Green),
    EXPENSE("Sent", Color.Red),
    SELF_TRANSFER("Self Transfer", Color.Blue),
    UNKNOWN("Unknown", Color.Gray)
}