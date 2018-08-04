//package com.property.flow
//
//import com.property.state.DividendState
//import net.corda.core.contracts.TransactionVerificationException
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.node.services.queryBy
//import net.corda.core.utilities.getOrThrow
//import net.corda.testing.core.singleIdentity
//import net.corda.testing.node.MockNetwork
//import net.corda.testing.node.StartedMockNode
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertFailsWith
//
//class DividendFlowTests {
//    lateinit var network: MockNetwork
//    lateinit var a: StartedMockNode
//    lateinit var b: StartedMockNode
//    lateinit var c: StartedMockNode
//
//    @Before
//    fun setup() {
//        network = MockNetwork(listOf("com.property.contract"))
//        a = network.createPartyNode()
//        b = network.createPartyNode()
//        c = network.createPartyNode()
//        // For real nodes this happens automatically, but we have to manually register the flow for tests.
//        listOf(a, b, c).forEach { it.registerInitiatedFlow(DividendFlow.Acceptor::class.java) }
//        network.runNetwork()
//    }
//
//    @After
//    fun tearDown() {
//        network.stopNodes()
//    }
//
//    @Test
//    fun `flow rejects invalid DividendStates`() {
//        val flow = DividendFlow.Initiator(-1, UniqueIdentifier.fromString("42fc9002-97f2-11e8-9eb6-529269fb1459"))
//        val future = a.startFlow(flow)
//        network.runNetwork()
//
//        // The DividendContract specifies that DividendState cannot have negative values.
//        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
//    }
//
//    @Test
//    fun `SignedTransaction returned by the flow is signed by the initiator`() {
//        val flow = DividendFlow.Initiator(1, listOf(b.info.singleIdentity(), c.info.singleIdentity()))
//        val future = a.startFlow(flow)
//        network.runNetwork()
//
//        val signedTx = future.getOrThrow()
//        signedTx.verifySignaturesExcept(b.info.singleIdentity().owningKey)
//    }
//
//    @Test
//    fun `SignedTransaction returned by the flow is signed by the acceptor`() {
//        val flow = DividendFlow.Initiator(1, listOf(b.info.singleIdentity(), c.info.singleIdentity()))
//        val future = a.startFlow(flow)
//        network.runNetwork()
//
//        val signedTx = future.getOrThrow()
//        signedTx.verifySignaturesExcept(a.info.singleIdentity().owningKey)
//    }
//
//    @Test
//    fun `flow records a transaction in both parties' transaction storages`() {
//        val flow = DividendFlow.Initiator(1,listOf(b.info.singleIdentity(), c.info.singleIdentity()))
//        val future = a.startFlow(flow)
//        network.runNetwork()
//        val signedTx = future.getOrThrow()
//
//        // We check the recorded transaction in both transaction storages.
//        for (node in listOf(a, b)) {
//            assertEquals(signedTx, node.services.validatedTransactions.getTransaction(signedTx.id))
//        }
//    }
//
//    @Test
//    fun `recorded transaction has no inputs and a single output, the input DividendState`() {
//        val DividendValue = 1
//        val flow = DividendFlow.Initiator(DividendValue,listOf(b.info.singleIdentity(), c.info.singleIdentity()))
//        val future = a.startFlow(flow)
//        network.runNetwork()
//        val signedTx = future.getOrThrow()
//
//        // We check the recorded transaction in both vaults.
//        for (node in listOf(a, b)) {
//            val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
//            val txOutputs = recordedTx!!.tx.outputs
//            assert(txOutputs.size == 1)
//
//            val recordedState = txOutputs[0].data as DividendState
//            assertEquals(recordedState.value, DividendValue)
//            assertEquals(recordedState.DividendManager, a.info.singleIdentity())
//            assertEquals(recordedState.investors,listOf(b.info.singleIdentity(), c.info.singleIdentity()))
//        }
//    }
//
//    @Test
//    fun `flow records the correct DividendState in both parties' vaults`() {
//        val DividendValue = 1
//        val flow = DividendFlow.Initiator(1, listOf(b.info.singleIdentity(), c.info.singleIdentity()))
//        val future = a.startFlow(flow)
//        network.runNetwork()
//        future.getOrThrow()
//
//        // We check the recorded DividendState in both vaults.
//        for (node in listOf(a, b)) {
//            node.transaction {
//                val DividendStates = node.services.vaultService.queryBy<DividendState>().states
//                assertEquals(1, DividendStates.size)
//                val recordedState = DividendStates.single().state.data
//                assertEquals(recordedState.value, DividendValue)
//                assertEquals(recordedState.DividendManager, a.info.singleIdentity())
//                assertEquals(recordedState.investors, listOf(b.info.singleIdentity(), c.info.singleIdentity()))
//            }
//        }
//    }
//}