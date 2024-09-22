package com.pristalovpavel.bitcoinwallet.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

@Composable
fun TransactionResultDialog(
    transactionId: String,
    errorMessage: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (errorMessage.isEmpty()) "Your funds have been sent!" else "Transaction Failed",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (errorMessage.isEmpty()) {
                val annotatedString = buildAnnotatedString {
                    append("Your transaction ID is ")

                    val startIndex = length
                    withStyle(
                        style = SpanStyle(color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline)) {
                            append(transactionId.take(12) + "..." + transactionId.takeLast(12))
                    }

                    val endIndex = length
                    addStringAnnotation(
                        tag = "transaction_id",
                        annotation = transactionId,
                        start = startIndex,
                        end = endIndex
                    )
                    append(".")
                }

                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures { offsetPosition ->
                            textLayoutResult.value?.let { layoutResult ->
                                val position = layoutResult.getOffsetForPosition(offsetPosition)
                                annotatedString.getStringAnnotations("transaction_id", position, position)
                                    .firstOrNull()?.let { _ ->
                                        val url = "https://mempool.space/signet/tx/${transactionId}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                            }
                        }
                    },
                    onTextLayout = { textLayoutResult.value = it }
                )
            } else {
                Text(text = errorMessage)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Send more")
            }
        }
    )
}