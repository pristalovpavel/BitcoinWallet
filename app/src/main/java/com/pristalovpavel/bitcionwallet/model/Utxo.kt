package com.pristalovpavel.bitcionwallet.model

data class Utxo(
    val txid: String,
    val voutIndex: Long,
    val value: Long
)