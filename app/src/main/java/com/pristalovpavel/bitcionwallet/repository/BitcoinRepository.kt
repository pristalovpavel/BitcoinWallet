package com.pristalovpavel.bitcionwallet.repository

import com.pristalovpavel.bitcionwallet.api.BitcoinApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.bitcoinj.base.Address
import org.bitcoinj.base.Coin
import org.bitcoinj.core.Context
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.ECKey
import org.bitcoinj.params.SigNetParams
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import java.math.BigDecimal


class BitcoinRepository (private val api: BitcoinApi) {
    suspend fun getBalance(address: String): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getAddressInfo(address)
                val balance = response.chain_stats.funded_txo_sum - response.chain_stats.spent_txo_sum
                Result.success(balance)
            } catch (e: Exception){
                Result.failure(e)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun sendTransaction(privateKey: String, destinationAddress: String, amount: BigDecimal) : Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val params: NetworkParameters = SigNetParams.get()
                val context = Context()
                Context.propagate(context)

                val key = ECKey.fromPrivate(privateKey.toBigInteger(16))

                val toAddress = Address.fromString(params, destinationAddress)

                val wallet = Wallet.createBasic(params)
                wallet.importKey(key)

                val sendAmount = Coin.valueOf(amount.multiply(BigDecimal(100_000_000)).toLong())

                val request = SendRequest.to(toAddress, sendAmount)
                request.changeAddress = wallet.currentReceiveAddress()

                wallet.completeTx(request)

                wallet.commitTx(request.tx)

                val transactionHex = request.tx.bitcoinSerialize().toHexString()

                val body = RequestBody.create("text/plain".toMediaTypeOrNull(), transactionHex)
                val response = api.sendTransaction(body)

                if (response.isSuccessful) {
                    Result.success("Transaction sent successfully: ${request.tx.txId}")
                } else {
                    Result.failure(Exception("Failed to send transaction"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}