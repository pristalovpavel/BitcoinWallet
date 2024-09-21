package com.pristalovpavel.bitcionwallet.api

import com.pristalovpavel.bitcionwallet.model.AddressInfoResponse
import com.pristalovpavel.bitcionwallet.model.TransactionDTO
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BitcoinApi {
    @GET("address/{address}")
    suspend fun getAddressInfo(@Path("address") address: String) : AddressInfoResponse

    @GET("address/{address}/txs")
    suspend fun getTransactions(@Path("address") address: String) : List<TransactionDTO>

    @POST("tx")
    suspend fun sendTransaction(@Body transactionHex: String): Response<String>

    companion object {
        private const val BASE_URL = "https://mempool.space/signet/api/"

        fun create(): BitcoinApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(createOkHttpClient())
                .addConverterFactory(PlainTextConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(BitcoinApi::class.java)
        }
    }
}

private fun createOkHttpClient(): OkHttpClient {
    val logging = HttpLoggingInterceptor()
    logging.setLevel(HttpLoggingInterceptor.Level.BODY)

    return OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()
}