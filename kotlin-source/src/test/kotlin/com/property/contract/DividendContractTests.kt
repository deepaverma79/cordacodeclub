
package com.property.contract

import com.property.contract.DividendContract.Companion.DIVIDEND_CONTRACT_ID
import com.property.state.DividendState
import com.property.state.FundState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class DividendContractTests {
    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))
    private val tinyCorp = TestIdentity(CordaX500Name("TinyCorp", "Tokyo", "JP"))

    @Test
    fun `transaction must include MakePayment command`() {
        ledgerServices.ledger {
            transaction {
                output(DIVIDEND_CONTRACT_ID, DividendState(100, UniqueIdentifier.fromString("42fc9002-97f2-11e8-9eb6-529269fb1458"),
                        listOf(megaCorp.party)))
                fails()
                command(listOf(megaCorp.publicKey), DividendContract.Commands.MakePayment())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                input(DIVIDEND_CONTRACT_ID, DividendState(100, UniqueIdentifier.fromString("42fc9002-97f2-11e8-9eb6-529269fb1459"),
                        listOf(megaCorp.party)))
                output(DIVIDEND_CONTRACT_ID, DividendState(100, UniqueIdentifier.fromString("42fc9002-97f2-11e8-9eb6-529269fb1459"),
                        listOf(megaCorp.party)))
                command(listOf(megaCorp.publicKey), DividendContract.Commands.MakePayment())
                `fails with`("No inputs should be consumed when issuing dividend.")
            }
        }
    }

    @Test
    fun `transaction must have same number of outputs as the investors`() {
        ledgerServices.ledger {
            transaction {
                output(DIVIDEND_CONTRACT_ID, DividendState(100, UniqueIdentifier.fromString("42fc9002-97f2-11e8-9eb6-529269fb1459"),
                        listOf(megaCorp.party)))
                output(DIVIDEND_CONTRACT_ID, DividendState(100, UniqueIdentifier.fromString("42fc9002-97f2-11e8-9eb6-529269fb1459"),
                        listOf(megaCorp.party)))
                output(DIVIDEND_CONTRACT_ID, DividendState(100, UniqueIdentifier.fromString("42fc9002-97f2-11e8-9eb6-529269fb1459"),
                        listOf(miniCorp.party)))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), DividendContract.Commands.MakePayment())
                `fails with`("All signers must be issued dividend.")
            }
        }
    }

    @Test
    fun `All participants are required to sign when issuing dividend`() {
        ledgerServices.ledger {
            transaction {
                output(DIVIDEND_CONTRACT_ID, DividendState(100, UniqueIdentifier.fromString("42fc9002-97f2-11e8-9eb6-529269fb1459"),
                        listOf(megaCorp.party, miniCorp.party)))
                command(listOf(megaCorp.publicKey), DividendContract.Commands.MakePayment())
                `fails with`("All participants are required to sign when issuing dividend.")
            }
        }
    }

    @Test
    fun `All investors must be the signer`() {
        ledgerServices.ledger {
            transaction {
                output(DIVIDEND_CONTRACT_ID, DividendState(100, UniqueIdentifier.fromString("42fc9002-97f2-11e8-9eb6-529269fb1459"),  listOf(miniCorp.party)))
                command(listOf(megaCorp.publicKey, tinyCorp.publicKey), DividendContract.Commands.MakePayment())
                `fails with`("All signers must be issued dividend.")
            }
        }
    }
}