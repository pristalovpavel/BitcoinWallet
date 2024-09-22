package com.pristalovpavel.bitcoinwallet.model

import com.google.gson.annotations.SerializedName

data class Utxo(
    @SerializedName("txid")
    val txId: String, // transaction to spend from
    @SerializedName("voutIndex")
    val vOutIndex: Long, // index of output in transaction
    val value: Long // UTXO amount (in satoshis)
)