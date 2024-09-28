package com.pristalovpavel.bitcoinwallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.pristalovpavel.bitcoinwallet.ui.BitcoinWalletApp
import com.pristalovpavel.bitcoinwallet.ui.theme.BitcoinWalletTheme
import com.pristalovpavel.bitcoinwallet.viewmodel.BitcoinViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val bitcoinViewModel: BitcoinViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BitcoinWalletTheme {
                BitcoinWalletApp(bitcoinViewModel = bitcoinViewModel)
            }
        }
    }
}
