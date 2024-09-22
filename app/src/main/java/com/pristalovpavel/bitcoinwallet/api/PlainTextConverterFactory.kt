package com.pristalovpavel.bitcoinwallet.api

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class PlainTextConverterFactory private constructor() : Converter.Factory() {

    companion object {
        fun create(): PlainTextConverterFactory = PlainTextConverterFactory()
    }

    private val mediaType = "text/plain".toMediaTypeOrNull()

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<String, RequestBody>? {
        return if (type == String::class.java) {
            Converter { value -> value.toRequestBody(mediaType) }
        } else {
            null
        }
    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, String>? {
        return if (type == String::class.java) {
            Converter { value -> value.string() }
        } else {
            null
        }
    }
}