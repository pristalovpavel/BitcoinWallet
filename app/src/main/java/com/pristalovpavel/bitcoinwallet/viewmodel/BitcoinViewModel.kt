package com.pristalovpavel.bitcoinwallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pristalovpavel.bitcoinwallet.model.TransactionDTO
import com.pristalovpavel.bitcoinwallet.model.TransactionDisplayData
import com.pristalovpavel.bitcoinwallet.model.TransactionParams
import com.pristalovpavel.bitcoinwallet.model.TransactionType
import com.pristalovpavel.bitcoinwallet.model.Utxo
import com.pristalovpavel.bitcoinwallet.repository.BitcoinRepository
import com.pristalovpavel.bitcoinwallet.utils.getShortAddress
import com.pristalovpavel.bitcoinwallet.utils.readDataFromFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bitcoinj.base.AddressParser
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.base.Coin
import org.bitcoinj.base.ScriptType
import org.bitcoinj.base.Sha256Hash
import org.bitcoinj.core.Context
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.core.TransactionWitness
import org.bitcoinj.crypto.DumpedPrivateKey
import org.bitcoinj.script.ScriptBuilder

class BitcoinViewModel(private val repository: BitcoinRepository) : ViewModel() {

    private val _balance = MutableStateFlow(Result.success(0L))
    val balance: StateFlow<Result<Long>> = _balance.asStateFlow()

    private val _transactionStatus = MutableStateFlow(Result.success(""))
    val transactionStatus: StateFlow<Result<String>> = _transactionStatus

    private val _transactions =
        MutableStateFlow<Result<List<TransactionDTO>>>(Result.success(emptyList()))
    val transactions: StateFlow<Result<List<TransactionDTO>>> = _transactions

    private val _myAddress = MutableStateFlow("")
    val myAddress: StateFlow<String> = _myAddress.asStateFlow()

    private val _ownAddresses = MutableStateFlow<Set<String>>(emptySet())
    val ownAddresses: StateFlow<Set<String>> = _ownAddresses.asStateFlow()

    private val feeAmount = 250L
    private val dustThreshold = 300L

    fun loadAddressData(context: android.content.Context) {
        viewModelScope.launch {
            val addressesList = readDataFromFile(context = context, "addresses.txt")
                .split("\r\n", "\n")

            if(addressesList.isEmpty()) return@launch

            _myAddress.value = addressesList[0]

            _ownAddresses.value = addressesList.toSet()
        }
    }

    fun loadBalance(address: String) {
        viewModelScope.launch {
            val result = repository.getBalance(address)
            _balance.value = result
        }
    }

    fun sendBitcoinTransaction(
        myAddress: String,
        privateKey: String,
        destinationAddress: String,
        amount: Long
    ) {
        viewModelScope.launch {
            try {
                val transactions = repository.getTransactions(myAddress)
                if (transactions.isFailure) {
                    _transactionStatus.value =
                        Result.failure(Exception("Failed to fetch transactions"))
                    return@launch
                }

                val utxo = findSuitableUtxo(transactions.getOrNull() ?: emptyList(), amount)
                if (utxo != null) {
                    val params = TransactionParams(
                        privateKey = privateKey,
                        destinationAddress = destinationAddress,
                        amount = amount,
                        feeAmount = feeAmount,
                        utxo = utxo
                    )
                    val transactionHex = prepareTransaction(params)
                    val result = repository.sendTransaction(transactionHex)
                    _transactionStatus.value = result
                } else {
                    _transactionStatus.value =
                        Result.failure(Exception("No available UTXO for amount $amount"))
                }
            } catch (e: Exception) {
                _transactionStatus.value = Result.failure(e)
            }
        }
    }

    fun loadTransactions(address: String) {
        viewModelScope.launch {
            val result = repository.getTransactions(address)
            _transactions.value = result
        }
    }

    fun getTransactionDisplayData(transaction: TransactionDTO, ownAddresses: Set<String>): TransactionDisplayData {
        val isOutgoing = transaction.vIn.any { input ->
            input.prevOut.scriptPublicKeyAddress in ownAddresses
        }

        val hasOutputToOthers = transaction.vOut.any { out ->
            out.scriptPublicKeyAddress !in ownAddresses
        }

        val isIncoming = !isOutgoing && transaction.vOut.any { out ->
            out.scriptPublicKeyAddress in ownAddresses
        }

        val transactionType: TransactionType = when {
            isOutgoing && hasOutputToOthers -> TransactionType.EXPENSE
            isIncoming -> TransactionType.INCOME
            isOutgoing && !hasOutputToOthers -> TransactionType.SELF_TRANSFER
            else -> TransactionType.UNKNOWN
        }

        val amount: Long = when (transactionType) {
            TransactionType.EXPENSE -> transaction.vOut
                .filter { it.scriptPublicKeyAddress !in ownAddresses }
                .sumOf { it.value } + transaction.fee

            TransactionType.INCOME -> transaction.vOut
                .filter { it.scriptPublicKeyAddress in ownAddresses }
                .sumOf { it.value }

            TransactionType.SELF_TRANSFER -> 0L
            else -> 0L
        }

        val amountInmBtc = amount / 100_000.0

        val transactionAddressText = when (transactionType) {
            TransactionType.INCOME -> {
                val senderAddress = transaction.vIn.firstOrNull { input ->
                    input.prevOut.scriptPublicKeyAddress !in ownAddresses
                }?.prevOut?.scriptPublicKeyAddress

                if (senderAddress != null) "From: ${getShortAddress(senderAddress)}" else null
            }
            TransactionType.EXPENSE -> {
                val receiverAddress = transaction.vOut.firstOrNull { out ->
                    out.scriptPublicKeyAddress !in ownAddresses
                }?.scriptPublicKeyAddress

                if (receiverAddress != null) "To: ${getShortAddress(receiverAddress)}" else null
            }
            else -> null
        }

        return TransactionDisplayData(
            transactionType = transactionType,
            amountInmBtc = amountInmBtc,
            transactionAddressText = transactionAddressText
        )
    }

    fun resetTransactionStatus() {
        _transactionStatus.value = Result.success("")
    }

    private fun findSuitableUtxo(transactions: List<TransactionDTO>, amount: Long): Utxo? {
        for (tx in transactions) {
            if (tx.status.confirmed) {
                tx.vOut.forEachIndexed { index, vout ->
                    if (vout.value >= (amount + feeAmount + dustThreshold)) {
                        // Check that this output has not been used as an input (UTXO)
                        val isUsed = transactions.any { transaction ->
                            transaction.vIn.any { vin -> vin.txId == tx.txId && vin.vOut == index }
                        }

                        // If UTXO was not used, return it
                        if (!isUsed) {
                            return Utxo(tx.txId, index.toLong(), vout.value)
                        }
                    }
                }
            }
        }
        return null
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun prepareTransaction(params: TransactionParams): String {
        Context.propagate(Context())

        // Base network settings
        val scriptType = ScriptType.P2WPKH
        val network = BitcoinNetwork.SIGNET

        // Get dumped private key from P2WPKH
        val cleanKey = params.privateKey.substringAfter(':')
        val key = DumpedPrivateKey.fromBase58(network, cleanKey).key

        val addressParser = AddressParser.getDefault()
        val toAddress = addressParser.parseAddress(params.destinationAddress)

        val sendAmount = Coin.valueOf(params.amount)

        // Total sum of outputs (UTXO)
        val totalInput = Coin.valueOf(params.utxo.value)
        // Commission to miners.
        // Min 220 satoshi, otherwise you will get "min relay fee not met, 1 < 220" from backend
        val fee = Coin.valueOf(params.feeAmount)

        // We check if there are enough funds to send, taking into account the commission
        if (totalInput.subtract(sendAmount) < fee) {
            throw IllegalArgumentException("Not enough funds to send transaction with fee")
        }

        val transaction = Transaction()
        // Add output - recipient address and amount
        transaction.addOutput(sendAmount, toAddress)

        // Calculate change (if any)
        val change = totalInput.subtract(sendAmount).subtract(fee)
        if (change.isPositive) {
            // Important: add change to the sender's address
            transaction.addOutput(change, key.toAddress(scriptType, network))
        }

        // UTXO - transaction from which we spend
        val utxo = Sha256Hash.wrap(params.utxo.txId)
        val outPoint = TransactionOutPoint(params.utxo.vOutIndex, utxo)
        val input = TransactionInput(transaction, byteArrayOf(), outPoint, Coin.valueOf(params.utxo.value))

        // Add input. Important: need to add it after adding of outputs
        transaction.addInput(input)

        // Get scriptPubKey for signing from previous output (UTXO)
        val scriptCode = ScriptBuilder.createP2PKHOutputScript(key.pubKeyHash)

        // Sign inputs after adding of all outputs
        for (i in 0 until transaction.inputs.size) {
            val txIn = transaction.getInput(i.toLong())
            val signature = transaction.calculateWitnessSignature(
                i,
                key,
                scriptCode,
                Coin.valueOf(params.utxo.value),
                Transaction.SigHash.ALL,
                false
            )
            txIn.witness = TransactionWitness.of(listOf(signature.encodeToBitcoin(), key.pubKey))
        }

        // Convert the transaction to HEX for sending.
        // We need to send only HEX as a plain text.
        return transaction.serialize().toHexString()
    }
}