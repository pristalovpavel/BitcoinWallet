package com.pristalovpavel.bitcoinwallet.utils

fun getShortAddress(fullAddress: String): String {
    if(fullAddress.length < 14) return fullAddress

    return fullAddress.take(7) + "..." + fullAddress.takeLast(7)
}