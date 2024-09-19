package com.pristalovpavel.bitcionwallet.repository

import com.pristalovpavel.bitcionwallet.api.BitcoinApi
import com.pristalovpavel.bitcionwallet.model.TransactionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.bitcoinj.base.AddressParser
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.base.Coin
import org.bitcoinj.base.ScriptType
import org.bitcoinj.base.Sha256Hash
import org.bitcoinj.core.Context
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.core.TransactionWitness
import org.bitcoinj.crypto.DumpedPrivateKey
import org.bitcoinj.script.ScriptBuilder


class BitcoinRepository(private val api: BitcoinApi) {
    suspend fun getBalance(address: String): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getAddressInfo(address)
                val balance =
                    response.chain_stats.funded_txo_sum - response.chain_stats.spent_txo_sum
                Result.success(balance)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getTransactions(address: String): Result<List<TransactionResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getTransactions(address)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun sendTransaction(
        privateKey: String,
        destinationAddress: String,
        amount: Long,
        utxoTxId: String,  // transaction to spend from
        outputIndex: Long,  // index of output in transaction
        valueIn: Long  // UTXO amount (in satoshis)
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Context.propagate(Context())

                // Base settings for our network
                val scriptType = ScriptType.P2WPKH
                val network = BitcoinNetwork.SIGNET

                // Get dumped private key from P2WPKH
                val cleanKey = privateKey.substringAfter(':')
                val key = DumpedPrivateKey.fromBase58(network, cleanKey).key

                val addressParser = AddressParser.getDefault()
                val toAddress = addressParser.parseAddress(destinationAddress)
                //val toAddress = Address.fromString(params, destinationAddress)

                val sendAmount = Coin.valueOf(amount)

                // Total sum of outputs (UTXO)
                val totalInput = Coin.valueOf(valueIn)
                // Commission to miners.
                // Min 220 satoshi, otherwise you will get "min relay fee not met, 1 < 220" from backend
                val fee = Coin.valueOf(221)

                // We check if there are enough funds to send, taking into account the commission
                if (totalInput.subtract(sendAmount) < fee) {
                    return@withContext Result.failure(Exception("Not enough funds to send transaction with fee"))
                }

                val transaction = Transaction()
                // Add output - recipient address and amount
                transaction.addOutput(sendAmount, toAddress)

                // Calculate change (if any)
                val change = totalInput.subtract(sendAmount).subtract(fee)
                if (change.isPositive) {
                    // Important: add change to the sender's address
                    transaction.addOutput(change, key.toAddress(scriptType, network))
                }

                // UTXO - transaction from which we spend
                val utxo = Sha256Hash.wrap(utxoTxId)
                val outPoint = TransactionOutPoint(outputIndex, utxo)
                val input = TransactionInput(transaction, byteArrayOf(), outPoint, Coin.valueOf(valueIn))

                // Add input. Important: need to add it after adding of outputs
                transaction.addInput(input)

                // Get scriptPubKey for signing from previous output (UTXO)
                val scriptCode = ScriptBuilder.createP2PKHOutputScript(key.pubKeyHash)

                // Sign inputs after adding of all outputs
                for (i in 0 until transaction.inputs.size) {
                    val txIn = transaction.getInput(i.toLong())
                    val signature = transaction.calculateWitnessSignature(
                        i,
                        key,
                        scriptCode,
                        Coin.valueOf(valueIn),
                        Transaction.SigHash.ALL,
                        false
                    )
                    txIn.witness = TransactionWitness.of(listOf(signature.encodeToBitcoin(), key.pubKey))
                }

                // Convert the transaction to HEX for sending.
                // We need to send only HEX as a plain text.
                val transactionHex = transaction.serialize().toHexString()
                val body = transactionHex.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.sendTransaction(body)

                if (response.isSuccessful) {
                    Result.success("Transaction sent successfully: ${transaction.txId}")
                } else {
                    Result.failure(Exception("Failed to send transaction"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getBalanceFromTransactions(
        transactions: List<TransactionResponse>,
        address: String
    ): Long {
        var balance = 0L

        transactions.forEach { tx ->
            tx.vout.forEach { vout ->
                // Check if this vout is for my address
                if (vout.scriptpubkey_address == address) {
                    // Check if it was spent
                    val isSpent = transactions.any { txIn ->
                        txIn.vin.any { vin ->
                            vin.txid == tx.txid && vin.vout == tx.vout.indexOf(vout)
                        }
                    }

                    // if trx haven't spent, add it
                    if (!isSpent) {
                        balance += vout.value
                    }
                }
            }
        }

        return balance
    }
}