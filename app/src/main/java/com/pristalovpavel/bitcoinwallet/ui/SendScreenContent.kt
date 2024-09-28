package com.pristalovpavel.bitcoinwallet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pristalovpavel.bitcoinwallet.R

@Composable
fun SendScreenContent(
    balanceState: Result<Long>,
    onSendClick: (String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var amountError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }

    val isFormValid = amount.isNotBlank() && address.isNotBlank() && amountError == null && addressError == null

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Image(
            painter = painterResource(id = R.drawable.ic_currency_bitcoin),
            contentDescription = "Bitcoin Logo",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        BalanceText(balanceState = balanceState)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = {
                amount = it
                amountError = when {
                    it.isBlank() -> "Amount is required"
                    it.toLongOrNull() == null || it.toLong() <= 0 -> "Enter a valid amount"
                    else -> null
                }
            },
            label = { Text(text = "Amount to send") },
            modifier = Modifier.fillMaxWidth(),
            isError = amountError != null,
            supportingText = {
                if (amountError != null) {
                    Text(text = amountError!!)
                }
            }
        )

        OutlinedTextField(
            value = address,
            onValueChange = {
                address = it
                addressError = if (it.isBlank()) {
                    "Address is required"
                } else {
                    null
                }
            },
            label = { Text(text = "Address to send") },
            modifier = Modifier.fillMaxWidth(),
            isError = addressError != null,
            supportingText = {
                if (addressError != null) {
                    Text(text = addressError!!)
                }
            }
        )

        Button(
            onClick = { onSendClick(amount.trim(), address.trim()) },
            enabled = isFormValid
        ) {
            Text(text = "Send")
        }
    }
}