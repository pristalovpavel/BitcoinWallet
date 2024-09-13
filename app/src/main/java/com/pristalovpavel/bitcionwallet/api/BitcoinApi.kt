package com.pristalovpavel.bitcionwallet.api

import com.pristalovpavel.bitcionwallet.model.AddressInfoResponse
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BitcoinApi {
    @GET("address/{address}")
    suspend fun getAddressInfo(@Path("address") address: String) : AddressInfoResponse

    @POST("tx")
    suspend fun sendTransaction(@Body transactionHex: RequestBody): retrofit2.Response<Unit>

    companion object {
        private const val BASE_URL = "https://mempool.space/signet/api/"

        fun create(): BitcoinApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(createOkHttpClient())
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