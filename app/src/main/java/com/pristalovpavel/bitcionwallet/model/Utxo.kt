package com.pristalovpavel.bitcionwallet.model

import com.google.gson.annotations.SerializedName

data class Utxo(
    @SerializedName("txid")
    val txId: String,
    @SerializedName("voutIndex")
    val vOutIndex: Long,
    val value: Long
)