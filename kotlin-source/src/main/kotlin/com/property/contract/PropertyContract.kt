package com.property.contract

import com.property.state.FundState
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
        val command = tx.commands.requireSingleCommand<Commands.Register>()
        requireThat {
            // Generic constraints around the Fund transaction.
            "No inputs should be consumed registering a Property." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)
            val out = tx.outputsOfType<PropertyState>().single()
            "Property Manager must be the signer." using (command.signers.containsAll(out.participants.map { it.owningKey }))

            // Fund-specific constraints.
            "The Property address cannot be empty." using (out.address != "" )
            "The Property must have an associated fund manager" using (out.participants.isNotEmpty())
        }
    }

    interface Commands : CommandData {
        fun verify(tx: LedgerTransaction, signers: List<PublicKey>)

        class Register : Commands {
            companion object {
                val CONTRACT_RULE_INPUTS = "Zero inputs should be consumed when registering a property."
                val CONTRACT_RULE_OUTPUTS = "Only one output should be created when registering a property."
                val CONTRACT_RULE_SIGNERS = "All participants are required to sign when registering a property."
            }

            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                CONTRACT_RULE_INPUTS using (tx.inputs.isEmpty())
                CONTRACT_RULE_OUTPUTS using (tx.outputs.size == 1)

                // State Rules
                val outputState = tx.outputsOfType<FundState>().single()
                CONTRACT_RULE_SIGNERS using (signers.containsAll(outputState.participants.map { it.owningKey }))
            }
        }

        class ChangePropertyManager : Commands {
            companion object {
                val CONTRACT_RULE_INPUTS = "At least one input should be consumed when changing ownership of a property."
                val CONTRACT_RULE_OUTPUTS = "At least one output should be created when changing ownership of a property."
                val CONTRACT_RULE_SIGNERS = "All participants are required to sign when changing ownership of a property."
            }

            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                CONTRACT_RULE_INPUTS using (tx.inputs.isNotEmpty())
                CONTRACT_RULE_OUTPUTS using (tx.outputs.isNotEmpty())

                // State Rules
                val keys = tx.outputsOfType<PropertyState>()
                        .flatMap { it.participants }
                        .map { it.owningKey }
                        .distinct()

                CONTRACT_RULE_SIGNERS using signers.containsAll(keys)
            }
        }

        class Deregister : Commands {
            companion object {
                val CONTRACT_RULE_INPUTS = "Only one input should be consumed when cancelling a property registration."
                val CONTRACT_RULE_OUTPUTS = "Zero outputs should be created when cancelling  a property registration."
            }

            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                CONTRACT_RULE_INPUTS using (tx.inputs.size == 1)
                CONTRACT_RULE_OUTPUTS using (tx.outputs.isEmpty())
            }
        }
    }
}