package com.pristalovpavel.bitcoinwallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pristalovpavel.bitcoinwallet.repository.BitcoinRepository

class BitcoinViewModelFactory(private val repository: BitcoinRepository): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(BitcoinViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BitcoinViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}