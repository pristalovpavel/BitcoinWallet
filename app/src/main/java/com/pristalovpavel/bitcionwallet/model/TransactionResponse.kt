package com.pristalovpavel.bitcionwallet.model

import com.google.gson.annotations.SerializedName

data class TransactionResponse(
    @SerializedName("txid")
    val txId: String,
    @SerializedName("vin")
    val vIn: List<In>,
    @SerializedName("vout")
    val vOut: List<Out>,
    val status: Status
)

data class In(
    @SerializedName("txid")
    val txId: String,
    @SerializedName("vout")
    val vOut: Int
)

data class Out(
    val value: Long,
    @SerializedName("scriptpubkey")
    val scriptPublicKey: String,
    @SerializedName("scriptpubkey_address")
    val scriptPublicKeyAddress: String
)

data class Status(
    val confirmed: Boolean
)