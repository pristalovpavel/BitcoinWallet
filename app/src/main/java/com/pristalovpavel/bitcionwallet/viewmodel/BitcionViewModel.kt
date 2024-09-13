package com.pristalovpavel.bitcionwallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pristalovpavel.bitcionwallet.repository.BitcoinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class BitcoinViewModel (private val repository: BitcoinRepository) : ViewModel() {

    private val _balance = MutableStateFlow(Result.success(0L))
    val balance: StateFlow<Result<Long>> = _balance.asStateFlow()

    private val _transactionStatus = MutableStateFlow(Result.success(""))
    val transactionStatus: StateFlow<Result<String>> = _transactionStatus

    fun loadBalance(address: String) {
        viewModelScope.launch {
            val result = repository.getBalance(address)
            _balance.value = result
        }
    }

    fun sendTransaction(privateKey: String, destinationAddress: String, amount: BigDecimal) {
        viewModelScope.launch {
            val result = repository.sendTransaction(privateKey, destinationAddress, amount)
            _transactionStatus.value = result
        }
    }
}