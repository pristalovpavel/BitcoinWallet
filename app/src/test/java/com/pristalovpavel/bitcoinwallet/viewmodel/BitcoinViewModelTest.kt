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
        every { repository.loadAddresses() } returns listOf("myAddress")
        every { repository.loadPrivateKey() } returns "privateKey"

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
        // Set myAddress to empty
        viewModel._myAddress.value = ""

        viewModel.loadBalance()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify getBalance is not called
        coVerify(exactly = 0) { repository.getBalance(any()) }
    }

    @Test
    fun `loadBalance updates with failure when repository returns error`() = runTest {
        val errorResult = Result.failure<Long>(Exception("Network error"))
        coEvery { repository.getBalance("myAddress") } returns errorResult

        viewModel.loadBalance()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorResult, viewModel.balance.value)
        coVerify { repository.getBalance("myAddress") }
    }

    @Test
    fun `sendBitcoinTransaction successful`() = runTest {
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