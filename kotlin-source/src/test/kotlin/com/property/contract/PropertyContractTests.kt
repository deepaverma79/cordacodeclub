
package com.property.contract

import com.property.contract.PropertyContract.Companion.PROPERTY_CONTRACT_ID
import com.property.state.PropertyState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class PropertyContractTests {
    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "London", "GB"))

    @Test
    fun `Property registration must include Register command`() {
        ledgerServices.ledger {
            transaction {
                output(PROPERTY_CONTRACT_ID, PropertyState("Santa Carla", megaCorp.party))
                fails()
                command(listOf(megaCorp.publicKey), PropertyContract.Commands.Register())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        val address = "Glasgow"
        ledgerServices.ledger {
            transaction {
                input(PROPERTY_CONTRACT_ID, PropertyState(address, megaCorp.party))
                output(PROPERTY_CONTRACT_ID, PropertyState(address, megaCorp.party))
                command(listOf(megaCorp.publicKey), PropertyContract.Commands.Register())
                `fails with`("No inputs should be consumed registering a Property.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        val address = "Glasgow"
        ledgerServices.ledger {
            transaction {
                output(PROPERTY_CONTRACT_ID, PropertyState(address, megaCorp.party))
                output(PROPERTY_CONTRACT_ID, PropertyState(address, megaCorp.party))
                command(listOf(megaCorp.publicKey), PropertyContract.Commands.Register())
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `Property Manager must be the signer`() {
        val address = "Barcelona"
        ledgerServices.ledger {
            transaction {
                output(PROPERTY_CONTRACT_ID, PropertyState(address,  megaCorp.party))
                command(listOf(miniCorp.publicKey), PropertyContract.Commands.Register())
                `fails with`("Property Manager must be the signer.")
            }
        }
    }

    @Test
    fun `cannot register property with no address`() {
        val address = ""
        ledgerServices.ledger {
            transaction {
                output(PROPERTY_CONTRACT_ID, PropertyState(address, megaCorp.party))
                command(listOf(megaCorp.publicKey), PropertyContract.Commands.Register())
                `fails with`("The Property address cannot be empty.")
            }
        }
    }
}