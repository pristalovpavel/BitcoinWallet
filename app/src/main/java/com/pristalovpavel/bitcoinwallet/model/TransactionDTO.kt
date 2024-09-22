package com.pristalovpavel.bitcoinwallet.model

import com.google.gson.annotations.SerializedName

data class TransactionDTO(
    @SerializedName("txid")
    val txId: String,
    @SerializedName("vin")
    val vIn: List<In>,
    @SerializedName("vout")
    val vOut: List<Out>,
    val fee: Long,
    val status: Status
)

data class In(
    @SerializedName("txid")
    val txId: String,
    @SerializedName("vout")
    val vOut: Int,
    @SerializedName("prevout")
    val prevOut: PrevOut
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

data class PrevOut(
    val value: Long,
    @SerializedName("scriptpubkey_address")
    val scriptPublicKeyAddress: String
)