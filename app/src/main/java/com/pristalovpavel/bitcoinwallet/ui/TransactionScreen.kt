package com.pristalovpavel.bitcoinwallet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pristalovpavel.bitcoinwallet.R
import com.pristalovpavel.bitcoinwallet.model.TransactionDTO
import com.pristalovpavel.bitcoinwallet.model.TransactionType.INCOME
import com.pristalovpavel.bitcoinwallet.utils.readDataFromFile
import com.pristalovpavel.bitcoinwallet.viewmodel.BitcoinViewModel

@Composable
fun TransactionScreen(
    navController: NavController,
    viewModel: BitcoinViewModel,
    walletAddress: String
) {
    val context = LocalContext.current
    val transactionsState by viewModel.transactions.collectAsState()
    val balanceState by viewModel.balance.collectAsState()

    val ownAddresses = remember {
        readDataFromFile(context, "change_addresses.txt")
            .split("\r\n", "\n").toSet()
    }

    LaunchedEffect(Unit) {
        viewModel.loadTransactions(walletAddress)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
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

        BalanceText(balanceState = balanceState)

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
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                        clip
                    )
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    items(transactions) { transaction ->
                        TransactionRow(viewModel, transaction, ownAddresses) {
                            val url = "https://mempool.space/signet/tx/${transaction.txId}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
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

@Composable
fun TransactionRow(
    viewModel: BitcoinViewModel,
    transaction: TransactionDTO,
    ownAddresses: Set<String>,
    onClick: () -> Unit
    ) {

    val transactionDisplayData = viewModel.getTransactionDisplayData(transaction, ownAddresses)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(
                    id = if (transactionDisplayData.transactionType == INCOME)
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
                Text(text = transactionDisplayData.transactionType.title, style = MaterialTheme.typography.bodyMedium)
                transactionDisplayData.transactionAddressText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Text(
            text = "${transactionDisplayData.amountInmBtc} mBTC",
            color = transactionDisplayData.transactionType.color,
            style = MaterialTheme.typography.titleMedium
        )
    }
}