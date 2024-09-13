package com.pristalovpavel.bitcionwallet

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import com.pristalovpavel.bitcionwallet.api.BitcoinApi
import com.pristalovpavel.bitcionwallet.repository.BitcoinRepository
import com.pristalovpavel.bitcionwallet.ui.BitcoinWalletApp
import com.pristalovpavel.bitcionwallet.ui.MainScreen
import com.pristalovpavel.bitcionwallet.ui.theme.BitcionWalletTheme
import com.pristalovpavel.bitcionwallet.utils.readPrivateKeyFromFile
import com.pristalovpavel.bitcionwallet.viewmodel.BitcoinViewModel
import com.pristalovpavel.bitcionwallet.viewmodel.BitcoinViewModelFactory
import java.math.BigDecimal

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // should be in DI normally
        val api = BitcoinApi.create()
        val repository = BitcoinRepository(api)
        val viewModelFactory = BitcoinViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(BitcoinViewModel::class.java)

        setContent {
            BitcionWalletTheme {
                BitcoinWalletApp(viewModel = viewModel)
            }
        }
    }
}