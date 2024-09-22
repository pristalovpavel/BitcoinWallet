package com.pristalovpavel.bitcoinwallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.pristalovpavel.bitcoinwallet.api.BitcoinApi
import com.pristalovpavel.bitcoinwallet.repository.BitcoinRepository
import com.pristalovpavel.bitcoinwallet.ui.BitcoinWalletApp
import com.pristalovpavel.bitcoinwallet.ui.theme.BitcoinWalletTheme
import com.pristalovpavel.bitcoinwallet.viewmodel.BitcoinViewModel
import com.pristalovpavel.bitcoinwallet.viewmodel.BitcoinViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // should be in DI normally
        val api = BitcoinApi.create()
        val repository = BitcoinRepository(api)

        val bitcoinViewModelFactory = BitcoinViewModelFactory(repository)
        val bitcoinViewModel = ViewModelProvider(this, bitcoinViewModelFactory)[BitcoinViewModel::class.java]

        setContent {
            BitcoinWalletTheme {
                BitcoinWalletApp(bitcoinViewModel = bitcoinViewModel)
            }
        }
    }
}
