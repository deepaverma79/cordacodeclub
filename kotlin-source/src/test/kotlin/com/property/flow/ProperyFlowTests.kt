package com.property.flow

import com.property.state.FundState
import com.property.state.PropertyState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ProperyFlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.property.contract"))
        a = network.createPartyNode()
        b = network.createPartyNode()
        c = network.createPartyNode()
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b, c).forEach { it.registerInitiatedFlow(PropertyFlow.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `flow rejects property registration which do not have an address`() {
        val flow = PropertyFlow.Initiator("", b.info.singleIdentity())
        val future = b.startFlow(flow)
        network.runNetwork()

        // The PropertyContract specifies that PropertyState must have an addess.
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the property manager`() {
        val flow = PropertyFlow.Initiator("Santa Carla",  b.info.singleIdentity())
        val future = b.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(b.info.singleIdentity().owningKey)
    }

    @Test
    fun `flow records a transaction in property managers' transaction storage`() {
        val flow = PropertyFlow.Initiator("Santa Monaca", c.info.singleIdentity())
        val future = c.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()
        assertEquals(signedTx, c.services.validatedTransactions.getTransaction(signedTx.id))
    }

    @Test
    fun `recorded transaction has no inputs and a single output, the input PropertyState`() {
        val address = "California Sun"
        val flow = PropertyFlow.Initiator(address,b.info.singleIdentity())
        val future = b.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both vaults.
        val recordedTx = b.services.validatedTransactions.getTransaction(signedTx.id)
        val txOutputs = recordedTx!!.tx.outputs
        assert(txOutputs.size == 1)

        val recordedState = txOutputs[0].data as PropertyState
        assertEquals(recordedState.address, address)
        assertEquals(recordedState.owner, b.info.singleIdentity())
    }
}