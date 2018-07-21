package com.property.contract

import com.property.contract.FundContract.Companion.FUND_CONTRACT_ID
import com.property.state.FundState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class FundContractTests {
    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))

    @Test
    fun `transaction must include Create command`() {
        val fundValue = 1
        ledgerServices.ledger {
            transaction {
                output(FUND_CONTRACT_ID, FundState(fundValue, miniCorp.party, megaCorp.party))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), FundContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        val fundValue = 1
        ledgerServices.ledger {
            transaction {
                input(FUND_CONTRACT_ID, FundState(fundValue, miniCorp.party, megaCorp.party))
                output(FUND_CONTRACT_ID, FundState(fundValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), FundContract.Commands.Issue())
                `fails with`("No inputs should be consumed when issuing an Fund.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        val fundValue = 1
        ledgerServices.ledger {
            transaction {
                output(FUND_CONTRACT_ID, FundState(fundValue, miniCorp.party, megaCorp.party))
                output(FUND_CONTRACT_ID, FundState(fundValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), FundContract.Commands.Issue())
                `fails with`("Only one output state should be created.")
            }
        }
    }

//    @Test
//    fun `lender must sign transaction`() {
//        val fundValue = 1
//        ledgerServices.ledger {
//            transaction {
//                output(FUND_CONTRACT_ID, FundState(fundValue, miniCorp.party, megaCorp.party))
//                command(miniCorp.publicKey, FundContract.Commands.Create())
//                `fails with`("All of the participants must be signers.")
//            }
//        }
//    }
//
//    @Test
//    fun `borrower must sign transaction`() {
//        val fundValue = 1
//        ledgerServices.ledger {
//            transaction {
//                output(FUND_CONTRACT_ID, FundState(fundValue, miniCorp.party, megaCorp.party))
//                command(megaCorp.publicKey, FundContract.Commands.Create())
//                `fails with`("All of the participants must be signers.")
//            }
//        }
//    }
//
//    @Test
//    fun `lender is not borrower`() {
//        val fundValue = 1
//        ledgerServices.ledger {
//            transaction {
//                output(FUND_CONTRACT_ID, FundState(fundValue, megaCorp.party, megaCorp.party))
//                command(listOf(megaCorp.publicKey, miniCorp.publicKey), FundContract.Commands.Create())
//                `fails with`("The fundManager and the investor cannot be the same entity.")
//            }
//        }
//    }
//
//    @Test
//    fun `cannot create negative-value Fund`() {
//        val fundValue = -1
//        ledgerServices.ledger {
//            transaction {
//                output(FUND_CONTRACT_ID, FundState(fundValue, miniCorp.party, megaCorp.party))
//                command(listOf(megaCorp.publicKey, miniCorp.publicKey), FundContract.Commands.Create())
//                `fails with`("The Fund's value must be non-negative.")
//            }
//        }
//    }
//
//    @Test
//    fun `cannot create more than 10 million Fund`() {
//        val fundValue = 10000000
//        ledgerServices.ledger {
//            transaction {
//                output(FUND_CONTRACT_ID, FundState(fundValue, miniCorp.party, megaCorp.party))
//                command(listOf(megaCorp.publicKey, miniCorp.publicKey), FundContract.Commands.Create())
//                `fails with`("The Fund's value must not be greater than a 10 million.")
//            }
//        }
//    }
}