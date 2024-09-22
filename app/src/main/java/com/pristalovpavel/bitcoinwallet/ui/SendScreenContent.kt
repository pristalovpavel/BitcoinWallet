package com.pristalovpavel.bitcoinwallet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SendScreenContent(balance: String, onSendClick: (String, String) -> Unit) {
    var amount by remember { mutableStateOf("1900") }
    var address by remember { mutableStateOf("tb1q9uxwr8qny8scmqw6n6t5ll9dfpms0yz606fd50") }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text = "Bitcoin wallet", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Balance: $balance ssats", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text(text = "Amount to send") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(text = "Address to send") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSendClick(amount, address) }
        ) {
            Text(text = "Send")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    SendScreenContent(balance = "0.0") { _, _ -> }
}