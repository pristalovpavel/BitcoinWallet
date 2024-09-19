package com.pristalovpavel.bitcionwallet.model

data class TransactionResponse(
    val txid: String,
    val vin: List<Vin>,
    val vout: List<Vout>,
    val status: Status
)

data class Vin(
    val txid: String,
    val vout: Int
)

data class Vout(
    val value: Long,
    val scriptpubkey: String,
    val scriptpubkey_address: String
)

data class Status(
    val confirmed: Boolean
)