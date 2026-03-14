package com.pristalovpavel.bitcoinwallet.repository

import android.content.Context
import com.pristalovpavel.bitcoinwallet.api.BitcoinApi
import com.pristalovpavel.bitcoinwallet.model.TransactionDTO
import com.pristalovpavel.bitcoinwallet.utils.readDataFromFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class BitcoinRepository @Inject constructor(
    private val api: BitcoinApi,
    @param:ApplicationContext private val context: Context
) {
    suspend fun loadPrivateKey(): String {
        return withContext(Dispatchers.IO) {
            readDataFromFile(context, "private_key.txt")
        }
    }

    suspend fun loadAddresses(): List<String> {
        return withContext(Dispatchers.IO) {
            val addressesData = readDataFromFile(context, "addresses.txt")
            addressesData.split("\r\n", "\n").filter { it.isNotEmpty() }
        }
    }

    suspend fun getBalance(address: String): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getAddressInfo(address)
                val balance =
                    response.chainStats.fundedSum - response.chainStats.spentSum
                Result.success(balance)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getTransactions(address: String): Result<List<TransactionDTO>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getTransactions(address)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun sendTransaction(transactionHex: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.sendTransaction(transactionHex)

                if (response.isSuccessful) {
                    Result.success(response.body() ?: "")
                } else {
                    val errorMessage = response.errorBody()?.string()?.trim()
                    Result.failure(
                        Exception(
                            errorMessage?.takeIf { it.isNotEmpty() }
                                ?: "Failed to send transaction (HTTP ${response.code()})"
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
