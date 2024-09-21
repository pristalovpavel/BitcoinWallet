package com.pristalovpavel.bitcionwallet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pristalovpavel.bitcionwallet.R
import com.pristalovpavel.bitcionwallet.model.TransactionDTO
import com.pristalovpavel.bitcionwallet.model.TransactionType
import com.pristalovpavel.bitcionwallet.model.TransactionType.*
import com.pristalovpavel.bitcionwallet.viewmodel.BitcoinViewModel

data class Transaction(
    val type: String,
    val amount: String,
    val address: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    navController: NavController,
    viewModel: BitcoinViewModel,
    walletAddress: String
) {
    val context = LocalContext.current
    val transactionsState by viewModel.transactions.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTransactions(walletAddress)
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bitcoin Wallet",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.headlineMedium
                            .copy(fontWeight = FontWeight.Medium)
                    )
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        content = { paddingValues ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Spacer(modifier = Modifier.height(32.dp))

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
                
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            navController.navigate("sendScreen")
                        }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Send")
                    }
                    Button(
                        onClick = {
                            val clip = ClipData.newPlainText("Wallet Address", walletAddress)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Rounded.Info, contentDescription = "Copy")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Copy")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                transactionsState.fold(
                    onSuccess = { transactions ->
                        LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                            items(transactions) { transaction ->
                                TransactionRow(transaction, walletAddress)
                                HorizontalDivider()
                            }
                        }
                    },
                    onFailure = {
                        Text(text = "Error loading transactions")
                    }
                )
            }
        }
    )
}


@Composable
fun TransactionRow(transaction: TransactionDTO, walletAddress: String) {
    val isOutgoing = transaction.vIn.any { input ->
        input.prevOut.scriptPublicKeyAddress == walletAddress
    }

    val hasOutputToOthers = transaction.vOut.any { out ->
        out.scriptPublicKeyAddress != walletAddress
    }

    val isIncoming = !isOutgoing && transaction.vOut.any { out ->
        out.scriptPublicKeyAddress == walletAddress
    }

    val transactionType: TransactionType = when {
        isOutgoing && hasOutputToOthers -> EXPENSE
        isIncoming -> INCOME
        isOutgoing && !hasOutputToOthers -> SELF_TRANSFER
        else -> UNKNOWN
    }

    val amount: Long = when (transactionType) {
        EXPENSE -> transaction.vOut
            .filter { it.scriptPublicKeyAddress != walletAddress }
            .sumOf { it.value } + transaction.fee
        INCOME -> transaction.vOut
            .filter { it.scriptPublicKeyAddress == walletAddress }
            .sumOf { it.value }
        SELF_TRANSFER -> 0L
        else -> 0L
    }

    val amountInmBtc = amount / 100_000.0
    val shortTxId = transaction.txId.take(7) + "..." + transaction.txId.takeLast(7)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = if (transactionType == INCOME)
                    R.drawable.ic_arrow_circle_down
                else
                    R.drawable.ic_arrow_circle_up
                ),
                contentDescription = "Transaction Icon",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(text = transactionType.title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "From: $shortTxId",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text(
            text = "$amountInmBtc mBTC",
            color = transactionType.color,
            style = MaterialTheme.typography.titleMedium
        )
    }
}