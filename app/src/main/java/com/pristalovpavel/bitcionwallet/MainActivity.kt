package com.pristalovpavel.bitcionwallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.pristalovpavel.bitcionwallet.api.BitcoinApi
import com.pristalovpavel.bitcionwallet.repository.BitcoinRepository
import com.pristalovpavel.bitcionwallet.ui.BitcoinWalletApp
import com.pristalovpavel.bitcionwallet.ui.theme.BitcoinWalletTheme
import com.pristalovpavel.bitcionwallet.viewmodel.BitcoinViewModel
import com.pristalovpavel.bitcionwallet.viewmodel.BitcoinViewModelFactory

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
