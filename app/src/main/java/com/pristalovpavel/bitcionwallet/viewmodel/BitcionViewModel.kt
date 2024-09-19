package com.pristalovpavel.bitcionwallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pristalovpavel.bitcionwallet.model.TransactionResponse
import com.pristalovpavel.bitcionwallet.model.Utxo
import com.pristalovpavel.bitcionwallet.repository.BitcoinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun sendBitcoinTransaction(
        myAddress: String,
        privateKey: String,
        destinationAddress: String,
        amount: Long
    ) {
        viewModelScope.launch {
            try {
                val transactions = repository.getTransactions(myAddress)
                if(transactions.isFailure) return@launch

                val utxo = findSuitableUtxo(transactions.getOrNull() ?: emptyList(), amount)
                if (utxo != null) {
                    val result = repository.sendTransaction(
                        privateKey = privateKey,
                        destinationAddress = destinationAddress,
                        amount = amount,
                        utxoTxId = utxo.txid,
                        outputIndex = utxo.voutIndex,
                        valueIn = utxo.value
                    )

                    if (result.isSuccess) {
                        println("Trx success: ${result.getOrNull()}")
                    } else {
                        println("Trx error: ${result.exceptionOrNull()}")
                    }
                } else {
                    println("No available UTXO for amount $amount")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun findSuitableUtxo(transactions: List<TransactionResponse>, amount: Long): Utxo? {
        for (tx in transactions) {
            if (tx.status.confirmed) {
                tx.vout.forEachIndexed { index, vout ->
                    if (vout.value >= amount) {
                        // Check that this output has not been used as an input (UTXO)
                        val isUsed = transactions.any { transaction ->
                            transaction.vin.any { vin -> vin.txid == tx.txid && vin.vout == index }
                        }

                        // If UTXO was not used, return it
                        if (!isUsed) {
                            return Utxo(tx.txid, index.toLong(), vout.value)
                        }
                    }
                }
            }
        }
        return null
    }
}