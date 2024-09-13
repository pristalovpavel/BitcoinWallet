package com.pristalovpavel.bitcionwallet.model

data class AddressInfoResponse(
    val chain_stats: ChainStats
)

data class ChainStats (
    val funded_txo_sum: Long,
    val spent_txo_sum: Long
)