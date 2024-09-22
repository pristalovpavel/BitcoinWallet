package com.pristalovpavel.bitcoinwallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pristalovpavel.bitcoinwallet.model.TransactionDTO
import com.pristalovpavel.bitcoinwallet.model.Utxo
import com.pristalovpavel.bitcoinwallet.repository.BitcoinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BitcoinViewModel (private val repository: BitcoinRepository) : ViewModel() {

    private val _balance = MutableStateFlow(Result.success(0L))
    val balance: StateFlow<Result<Long>> = _balance.asStateFlow()

    private val _transactionStatus = MutableStateFlow(Result.success(""))
    val transactionStatus: StateFlow<Result<String>> = _transactionStatus

    private val _transactions = MutableStateFlow<Result<List<TransactionDTO>>>(Result.success(emptyList()))
    val transactions: StateFlow<Result<List<TransactionDTO>>> = _transactions

    private val feeAmount = 250L
    private val dustThreshold = 300L

    fun loadBalance(address: String) {
        viewModelScope.launch {
            val result = repository.getBalance(address)
            _balance.value = result
        }
    }

    fun sendBitcoinTransaction(
        myAddress: String,
        privateKey: String,
        destinationAddress: String,
        amount: Long
    ) {
        viewModelScope.launch {
            try {
                val transactions = repository.getTransactions(myAddress)
                if (transactions.isFailure) {
                    _transactionStatus.value = Result.failure(Exception("Failed to fetch transactions"))
                    return@launch
                }

                val utxo = findSuitableUtxo(transactions.getOrNull() ?: emptyList(), amount)
                if (utxo != null) {
                    val result = repository.sendTransaction(
                        privateKey = privateKey,
                        destinationAddress = destinationAddress,
                        amount = amount,
                        feeAmount = feeAmount,
                        utxoTxId = utxo.txId,
                        outputIndex = utxo.vOutIndex,
                        valueIn = utxo.value
                    )
                    _transactionStatus.value = result
                } else {
                    _transactionStatus.value =
                        Result.failure(Exception("No available UTXO for amount $amount"))
                }
            } catch (e: Exception) {
                _transactionStatus.value = Result.failure(e)
            }
        }
    }

    fun loadTransactions(address: String) {
            viewModelScope.launch {
                val result = repository.getTransactions(address)
                _transactions.value = result
            }
        }

    private fun findSuitableUtxo(transactions: List<TransactionDTO>, amount: Long): Utxo? {
        for (tx in transactions) {
            if (tx.status.confirmed) {
                tx.vOut.forEachIndexed { index, vout ->
                    if (vout.value >= (amount + feeAmount + dustThreshold)) {
                        // Check that this output has not been used as an input (UTXO)
                        val isUsed = transactions.any { transaction ->
                            transaction.vIn.any { vin -> vin.txId == tx.txId && vin.vOut == index }
                        }

                        // If UTXO was not used, return it
                        if (!isUsed) {
                            return Utxo(tx.txId, index.toLong(), vout.value)
                        }
                    }
                }
            }
        }
        return null
    }
}