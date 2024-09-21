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
        val viewModelFactory = BitcoinViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[BitcoinViewModel::class.java]

        setContent {
            BitcoinWalletTheme {
                BitcoinWalletApp(viewModel = viewModel)
            }
        }
    }
}
