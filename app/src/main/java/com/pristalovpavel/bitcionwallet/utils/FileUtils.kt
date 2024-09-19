package com.pristalovpavel.bitcionwallet.utils

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

fun readDataFromFile(context: Context, fileName: String): String {
    return try {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val privateKey = reader.readLine()
        reader.close()

        privateKey
    } catch (e: Exception) {
        e.printStackTrace()

        ""
    }
}