package com.pristalovpavel.bitcoinwallet.model

import androidx.compose.ui.graphics.Color

enum class TransactionType (val title: String, val color: Color) {
    INCOME("Received", Color(0xFF77DD77)),
    EXPENSE("Sent", Color(0xFFE57373)),
    SELF_TRANSFER("Self Transfer", Color.Blue),
    UNKNOWN("Unknown", Color.Gray)
}