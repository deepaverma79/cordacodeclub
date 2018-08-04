package com.property.contract

import com.property.state.PropertyState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey


open class PropertyContract : Contract {
    companion object {
        @JvmStatic
        val PROPERTY_CONTRACT_ID = "com.property.contract.PropertyContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<PropertyContract.Commands>()
        command.value.verify(tx, command.signers)
        requireThat {
            // Generic constraints around the Property transaction.
            val out = tx.outputsOfType<PropertyState>().single()
            "The Property address cannot be empty." using (out.address != "" )
            "The Property must have an associated fund manager" using (out.participants.isNotEmpty())
        }
    }

    interface Commands : CommandData {
        fun verify(tx: LedgerTransaction, signers: List<PublicKey>)

        class Register : Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                "No inputs should be consumed registering a Property." using (tx.inputs.isEmpty())
                "Only one output should be created when registering a property." using (tx.outputs.size == 1)
                // State Rules
                val outputState = tx.outputsOfType<PropertyState>().single()
                "All participants are required to sign when registering a property." using (signers.containsAll(outputState.participants.map { it.owningKey }))
            }
        }

        class ChangePropertyManager : Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                "At least one input should be consumed when changing ownership of a property." using (tx.inputs.isNotEmpty())
                "At least one output should be created when changing ownership of a property." using (tx.outputs.isNotEmpty())

                // State Rules
                val keys = tx.outputsOfType<PropertyState>()
                        .flatMap { it.participants }
                        .map { it.owningKey }
                        .distinct()

                "All participants are required to sign when changing ownership of a property." using signers.containsAll(keys)
            }
        }

        class Deregister : Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                "Only one input should be consumed when cancelling a property registration." using (tx.inputs.size == 1)
                "Zero outputs should be created when cancelling  a property registration." using (tx.outputs.isEmpty())
            }
        }
    }
}