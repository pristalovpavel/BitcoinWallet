package com.pristalovpavel.bitcoinwallet.model

data class TransactionDisplayData (
    val transactionType: TransactionType,
    val amountInmBtc: Double,
    val transactionAddressText: String?
)