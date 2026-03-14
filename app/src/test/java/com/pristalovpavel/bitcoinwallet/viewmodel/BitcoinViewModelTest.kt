package com.pristalovpavel.bitcoinwallet.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.pristalovpavel.bitcoinwallet.model.In
import com.pristalovpavel.bitcoinwallet.model.Out
import com.pristalovpavel.bitcoinwallet.model.PrevOut
import com.pristalovpavel.bitcoinwallet.model.Status
import com.pristalovpavel.bitcoinwallet.model.TransactionDTO
import com.pristalovpavel.bitcoinwallet.model.TransactionParams
import com.pristalovpavel.bitcoinwallet.model.TransactionType
import com.pristalovpavel.bitcoinwallet.model.Utxo
import com.pristalovpavel.bitcoinwallet.repository.BitcoinRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class BitcoinViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: BitcoinRepository
    private lateinit var viewModel: BitcoinViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // Set Main dispatcher to a test dispatcher
        Dispatchers.setMain(testDispatcher)

        // Mock the repository
        repository = mockk()

        // Mock repository methods
        coEvery { repository.loadAddresses() } returns listOf("myAddress")
        coEvery { repository.loadPrivateKey() } returns "privateKey"

        // Initialize ViewModel
        viewModel = BitcoinViewModel(repository)
    }

    @After
    fun tearDown() {
        // Reset the Main dispatcher
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads address data`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that myAddress and ownAddresses are initialized
        assertEquals("myAddress", viewModel.myAddress.value)
        assertEquals(setOf("myAddress"), viewModel.ownAddresses.value)
    }

    @Test
    fun `loadBalance updates balance when address is not empty`() = runTest {
        val expectedBalance = Result.success(1000L)
        coEvery { repository.getBalance("myAddress") } returns expectedBalance

        viewModel.loadBalance()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedBalance, viewModel.balance.value)
        coVerify { repository.getBalance("myAddress") }
    }

    @Test
    fun `loadBalance does not update when address is empty`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // Set myAddress to empty
        viewModel._myAddress.value = ""

        viewModel.loadBalance()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify getBalance is not called
        coVerify(exactly = 0) { repository.getBalance(any()) }
    }

    @Test
    fun `loadBalance updates with failure when repository returns error`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val errorResult = Result.failure<Long>(Exception("Network error"))
        coEvery { repository.getBalance("myAddress") } returns errorResult

        viewModel.loadBalance()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorResult, viewModel.balance.value)
        coVerify { repository.getBalance("myAddress") }
    }

    @Test
    fun `sendBitcoinTransaction successful`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val transactionsResult = Result.success(emptyList<TransactionDTO>())
        coEvery { repository.getTransactions("myAddress") } returns transactionsResult

        // Mock findSuitableUtxo
        val utxo = Utxo("txid", 0L, 100000L)
        val findUtxoMock = spyk(viewModel, recordPrivateCalls = true)
        every { findUtxoMock["findSuitableUtxo"](any<List<TransactionDTO>>(), any<Long>()) } returns utxo

        // Mock prepareTransaction
        every { findUtxoMock["prepareTransaction"](any<TransactionParams>()) } returns "transactionHex"

        // Mock sendTransaction
        val sendResult = Result.success("transactionId")
        coEvery { repository.sendTransaction("transactionHex") } returns sendResult

        // Replace viewModel with the spied one
        viewModel = findUtxoMock

        viewModel.sendBitcoinTransaction("destinationAddress", 50000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(sendResult, viewModel.transactionStatus.value)
        coVerify { repository.getTransactions("myAddress") }
        coVerify { repository.sendTransaction("transactionHex") }
    }

    @Test
    fun `sendBitcoinTransaction fails when no UTXO found`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val transactionsResult = Result.success(emptyList<TransactionDTO>())
        coEvery { repository.getTransactions("myAddress") } returns transactionsResult

        // Mock findSuitableUtxo to return null
        val findUtxoMock = spyk(viewModel, recordPrivateCalls = true)
        every { findUtxoMock["findSuitableUtxo"](any<List<TransactionDTO>>(), any<Long>()) } returns null

        // Replace viewModel with the spied one
        viewModel = findUtxoMock

        viewModel.sendBitcoinTransaction("destinationAddress", 50000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.transactionStatus.value.isFailure)
        assertEquals(
            "No available UTXO for amount 50000",
            viewModel.transactionStatus.value.exceptionOrNull()?.message
        )
    }

    @Test
    fun `loadTransactions updates transactions when address is not empty`() = runTest {
        val expectedTransactions = Result.success(emptyList<TransactionDTO>())
        coEvery { repository.getTransactions("myAddress") } returns expectedTransactions

        viewModel.loadTransactions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedTransactions, viewModel.transactions.value)
        coVerify { repository.getTransactions("myAddress") }
    }


    @Test
    fun `loadTransactions updates with failure when repository returns error`() = runTest {
        val errorResult = Result.failure<List<TransactionDTO>>(Exception("API error"))
        coEvery { repository.getTransactions("myAddress") } returns errorResult

        viewModel.loadTransactions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorResult, viewModel.transactions.value)
        coVerify { repository.getTransactions("myAddress") }
    }

    @Test
    fun `getTransactionDisplayData returns correct data for INCOME transaction`() {
        val transaction = createTransactionDTO(
            vInAddresses = listOf("1KFHE7w8BhaENAswwryaoccDb6qcT6DbYY"),
            vOutAddresses = listOf("myAddress"),
            values = listOf(100000L),
            fee = 1000L,
            confirmed = true
        )
        val ownAddresses = setOf("myAddress")

        val displayData = viewModel.getTransactionDisplayData(transaction, ownAddresses)

        assertEquals(TransactionType.INCOME, displayData.transactionType)
        assertEquals(1.0, displayData.amountInmBtc, 0.001)
        assertEquals("From: 1KFHE7w...cT6DbYY", displayData.transactionAddressText)
    }

    @Test
    fun `getTransactionDisplayData returns correct data for EXPENSE transaction`() {
        val transaction = createTransactionDTO(
            vInAddresses = listOf("myAddress"),
            vOutAddresses = listOf("1KFHE7w8BhaENAswwryaoccDb6qcT6DbYY"),
            values = listOf(50000L),
            fee = 1000L,
            confirmed = true
        )
        val ownAddresses = setOf("myAddress")

        val displayData = viewModel.getTransactionDisplayData(transaction, ownAddresses)

        assertEquals(TransactionType.EXPENSE, displayData.transactionType)
        assertEquals(0.51, displayData.amountInmBtc, 0.001)
        assertEquals("To: 1KFHE7w...cT6DbYY", displayData.transactionAddressText)
    }

    @Test
    fun `getTransactionDisplayData returns correct data for SELF_TRANSFER transaction`() {
        val transaction = createTransactionDTO(
            vInAddresses = listOf("myAddress"),
            vOutAddresses = listOf("myAddress"),
            values = listOf(100000L),
            fee = 1000L,
            confirmed = true
        )
        val ownAddresses = setOf("myAddress")

        val displayData = viewModel.getTransactionDisplayData(transaction, ownAddresses)

        assertEquals(TransactionType.SELF_TRANSFER, displayData.transactionType)
        assertEquals(0.0, displayData.amountInmBtc, 0.001)
        assertNull(displayData.transactionAddressText)
    }

    @Test
    fun `getTransactionDisplayData handles coinbase and OP_RETURN without crash`() {
        val transaction = TransactionDTO(
            txId = "coinbaseTxId",
            fee = 0L,
            vIn = listOf(
                In(
                    txId = null,
                    vOut = 0,
                    prevOut = null
                )
            ),
            vOut = listOf(
                Out(
                    value = 0L,
                    scriptPublicKey = "6a",
                    scriptPublicKeyAddress = null
                ),
                Out(
                    value = 100000L,
                    scriptPublicKey = "scriptPubKey",
                    scriptPublicKeyAddress = "myAddress"
                )
            ),
            status = Status(confirmed = true)
        )

        val displayData = viewModel.getTransactionDisplayData(transaction, setOf("myAddress"))

        assertEquals(TransactionType.INCOME, displayData.transactionType)
        assertEquals(1.0, displayData.amountInmBtc, 0.001)
        assertNull(displayData.transactionAddressText)
    }

    @Test
    fun `sendBitcoinTransaction ignores foreign UTXO`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val transactionsResult = Result.success(
            listOf(
                TransactionDTO(
                    txId = "txid",
                    fee = 1000L,
                    vIn = emptyList(),
                    vOut = listOf(
                        Out(
                            value = 100000L,
                            scriptPublicKey = "scriptPubKey",
                            scriptPublicKeyAddress = "foreignAddress"
                        )
                    ),
                    status = Status(confirmed = true)
                )
            )
        )
        coEvery { repository.getTransactions("myAddress") } returns transactionsResult

        viewModel.sendBitcoinTransaction("destinationAddress", 50000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.transactionStatus.value.isFailure)
        assertEquals(
            "No available UTXO for amount 50000",
            viewModel.transactionStatus.value.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.sendTransaction(any()) }
    }

    // --- sendBitcoinTransaction edge cases ---

    @Test
    fun `sendBitcoinTransaction fails when address is empty`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel._myAddress.value = ""

        viewModel.sendBitcoinTransaction("destinationAddress", 50000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.transactionStatus.value.isFailure)
        assertEquals(
            "Sender's address hasn't loaded",
            viewModel.transactionStatus.value.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.getTransactions(any()) }
    }

    @Test
    fun `sendBitcoinTransaction fails when private key is not loaded`() = runTest {
        // Create a ViewModel with blank private key
        coEvery { repository.loadPrivateKey() } returns ""
        val vmNoKey = BitcoinViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getTransactions("myAddress") } returns Result.success(emptyList())

        vmNoKey.sendBitcoinTransaction("destinationAddress", 50000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vmNoKey.transactionStatus.value.isFailure)
        assertEquals(
            "Private key hasn't loaded",
            vmNoKey.transactionStatus.value.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.sendTransaction(any()) }
    }

    @Test
    fun `sendBitcoinTransaction fails when getTransactions returns failure`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getTransactions("myAddress") } returns
                Result.failure(Exception("Network error"))

        viewModel.sendBitcoinTransaction("destinationAddress", 50000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.transactionStatus.value.isFailure)
        assertEquals(
            "Failed to fetch transactions",
            viewModel.transactionStatus.value.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.sendTransaction(any()) }
    }

    // --- loadTransactions edge case ---

    @Test
    fun `loadTransactions does not call API when address is empty`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel._myAddress.value = ""

        viewModel.loadTransactions()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.getTransactions(any()) }
    }

    // --- findSuitableUtxo edge cases (tested through sendBitcoinTransaction) ---

    @Test
    fun `sendBitcoinTransaction skips unconfirmed UTXO`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val transactionsResult = Result.success(
            listOf(
                TransactionDTO(
                    txId = "txid",
                    fee = 1000L,
                    vIn = emptyList(),
                    vOut = listOf(
                        Out(
                            value = 100000L,
                            scriptPublicKey = "scriptPubKey",
                            scriptPublicKeyAddress = "myAddress"
                        )
                    ),
                    status = Status(confirmed = false)
                )
            )
        )
        coEvery { repository.getTransactions("myAddress") } returns transactionsResult

        viewModel.sendBitcoinTransaction("destinationAddress", 50000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.transactionStatus.value.isFailure)
        assertEquals(
            "No available UTXO for amount 50000",
            viewModel.transactionStatus.value.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.sendTransaction(any()) }
    }

    @Test
    fun `sendBitcoinTransaction skips already spent UTXO`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val transactionsResult = Result.success(
            listOf(
                // Transaction with a suitable output
                TransactionDTO(
                    txId = "sourceTxId",
                    fee = 1000L,
                    vIn = emptyList(),
                    vOut = listOf(
                        Out(
                            value = 100000L,
                            scriptPublicKey = "scriptPubKey",
                            scriptPublicKeyAddress = "myAddress"
                        )
                    ),
                    status = Status(confirmed = true)
                ),
                // Transaction that spends the above output
                TransactionDTO(
                    txId = "spendingTxId",
                    fee = 250L,
                    vIn = listOf(
                        In(
                            txId = "sourceTxId",
                            vOut = 0,
                            prevOut = PrevOut(
                                value = 100000L,
                                scriptPublicKeyAddress = "myAddress"
                            )
                        )
                    ),
                    vOut = listOf(
                        Out(
                            value = 99750L,
                            scriptPublicKey = "scriptPubKey",
                            scriptPublicKeyAddress = "otherAddress"
                        )
                    ),
                    status = Status(confirmed = true)
                )
            )
        )
        coEvery { repository.getTransactions("myAddress") } returns transactionsResult

        viewModel.sendBitcoinTransaction("destinationAddress", 50000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.transactionStatus.value.isFailure)
        assertEquals(
            "No available UTXO for amount 50000",
            viewModel.transactionStatus.value.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.sendTransaction(any()) }
    }

    @Test
    fun `sendBitcoinTransaction skips OP_RETURN outputs with null address`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val transactionsResult = Result.success(
            listOf(
                TransactionDTO(
                    txId = "txid",
                    fee = 1000L,
                    vIn = emptyList(),
                    vOut = listOf(
                        Out(
                            value = 100000L,
                            scriptPublicKey = "6a",
                            scriptPublicKeyAddress = null
                        )
                    ),
                    status = Status(confirmed = true)
                )
            )
        )
        coEvery { repository.getTransactions("myAddress") } returns transactionsResult

        viewModel.sendBitcoinTransaction("destinationAddress", 50000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.transactionStatus.value.isFailure)
        assertEquals(
            "No available UTXO for amount 50000",
            viewModel.transactionStatus.value.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { repository.sendTransaction(any()) }
    }

    // --- init edge cases ---

    @Test
    fun `init with empty addresses keeps defaults`() = runTest {
        coEvery { repository.loadAddresses() } returns emptyList()
        coEvery { repository.loadPrivateKey() } returns "privateKey"

        val vm = BitcoinViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", vm.myAddress.value)
        assertEquals(emptySet<String>(), vm.ownAddresses.value)
    }

    @Test
    fun `init with blank private key sets null`() = runTest {
        coEvery { repository.loadAddresses() } returns listOf("myAddress")
        coEvery { repository.loadPrivateKey() } returns "   "

        val vm = BitcoinViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // privateKey is private, but we can verify behavior through sendBitcoinTransaction
        coEvery { repository.getTransactions("myAddress") } returns Result.success(emptyList())

        vm.sendBitcoinTransaction("dest", 1000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.transactionStatus.value.isFailure)
        assertEquals(
            "Private key hasn't loaded",
            vm.transactionStatus.value.exceptionOrNull()?.message
        )
    }

    // --- getTransactionDisplayData edge cases ---

    @Test
    fun `getTransactionDisplayData returns UNKNOWN when no inputs or outputs match`() {
        val transaction = TransactionDTO(
            txId = "txid",
            fee = 500L,
            vIn = listOf(
                In(
                    txId = "otherTxId",
                    vOut = 0,
                    prevOut = PrevOut(
                        value = 50000L,
                        scriptPublicKeyAddress = "foreignAddress1"
                    )
                )
            ),
            vOut = listOf(
                Out(
                    value = 49500L,
                    scriptPublicKey = "scriptPubKey",
                    scriptPublicKeyAddress = "foreignAddress2"
                )
            ),
            status = Status(confirmed = true)
        )

        val displayData = viewModel.getTransactionDisplayData(transaction, setOf("myAddress"))

        assertEquals(TransactionType.UNKNOWN, displayData.transactionType)
        assertEquals(0.0, displayData.amountInmBtc, 0.001)
        assertNull(displayData.transactionAddressText)
    }

    @Test
    fun `getTransactionDisplayData EXPENSE includes fee in amount`() {
        val transaction = createTransactionDTO(
            vInAddresses = listOf("myAddress"),
            vOutAddresses = listOf("recipientAddress", "myAddress"),
            values = listOf(30000L, 69000L),
            fee = 1000L,
            confirmed = true
        )

        val displayData = viewModel.getTransactionDisplayData(transaction, setOf("myAddress"))

        assertEquals(TransactionType.EXPENSE, displayData.transactionType)
        // amount = 30000 (to recipient) + 1000 (fee) = 31000 sat = 0.31 mBTC
        assertEquals(0.31, displayData.amountInmBtc, 0.001)
    }

    @Test
    fun `getTransactionDisplayData INCOME with multiple own outputs sums correctly`() {
        val transaction = TransactionDTO(
            txId = "txid",
            fee = 500L,
            vIn = listOf(
                In(
                    txId = "otherTxId",
                    vOut = 0,
                    prevOut = PrevOut(
                        value = 200000L,
                        scriptPublicKeyAddress = "senderAddress"
                    )
                )
            ),
            vOut = listOf(
                Out(
                    value = 60000L,
                    scriptPublicKey = "scriptPubKey",
                    scriptPublicKeyAddress = "myAddress"
                ),
                Out(
                    value = 40000L,
                    scriptPublicKey = "scriptPubKey",
                    scriptPublicKeyAddress = "myAddress"
                )
            ),
            status = Status(confirmed = true)
        )

        val displayData = viewModel.getTransactionDisplayData(transaction, setOf("myAddress"))

        assertEquals(TransactionType.INCOME, displayData.transactionType)
        // 60000 + 40000 = 100000 sat = 1.0 mBTC
        assertEquals(1.0, displayData.amountInmBtc, 0.001)
    }

    @Test
    fun `resetTransactionStatus resets the transaction status`() {
        viewModel._transactionStatus.value = Result.failure(Exception("Some error"))
        viewModel.resetTransactionStatus()
        assertEquals(Result.success(""), viewModel.transactionStatus.value)
    }

    private fun createTransactionDTO(
        vInAddresses: List<String>,
        vOutAddresses: List<String>,
        values: List<Long>,
        fee: Long,
        confirmed: Boolean
    ): TransactionDTO {
        val vIn = vInAddresses.map { address ->
            In(
                txId = "vinTxId",
                vOut = 0,
                prevOut = PrevOut(
                    value = 0L,
                    scriptPublicKeyAddress = address
                )
            )
        }

        val vOut = vOutAddresses.mapIndexed { index, address ->
            Out(
                value = values.getOrElse(index) { 0L },
                scriptPublicKey = "scriptPubKey",
                scriptPublicKeyAddress = address
            )
        }

        return TransactionDTO(
            txId = "txid",
            fee = fee,
            vIn = vIn,
            vOut = vOut,
            status = Status(
                confirmed = confirmed
            )
        )
    }
}
