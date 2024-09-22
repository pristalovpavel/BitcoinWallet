package com.pristalovpavel.bitcoinwallet.model

import com.google.gson.annotations.SerializedName

data class AddressInfoDTO (
    @SerializedName("chain_stats")
    val chainStats: ChainStats
)

data class ChainStats (
    @SerializedName("funded_txo_sum")
    val fundedSum: Long,
    @SerializedName("spent_txo_sum")
    val spentSum: Long
)