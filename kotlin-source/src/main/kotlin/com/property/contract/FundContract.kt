package com.property.contract

import com.property.state.FundState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [FundState], which in turn encapsulates a [FundState].
 *
 * For a new [FundState] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [FundState].
 * - An Create() command with the public keys of both the fundManager and the investor.
 *
 * All contracts must sub-class the [Contract] interface.
 */
open class FundContract : Contract {
    companion object {
        @JvmStatic
        val FUND_CONTRACT_ID = "com.property.contract.FundContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Issue>()
        requireThat {
            // Generic constraints around the Fund transaction.
            "No inputs should be consumed when issuing an Fund." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)
            val out = tx.outputsOfType<FundState>().single()
            "The fundManager and the investor cannot be the same entity." using (out.fundManager != out.investors[0])
            "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

            // Fund-specific constraints.
            "The Fund's value must be non-negative." using (out.value > 0)
            "The Fund's value must not be greater than a 10 million." using (out.value < 10000000)
        }
    }

    interface Commands : CommandData {
        fun verify(tx: LedgerTransaction, signers: List<PublicKey>)

        class Issue : Commands {
            companion object {
                val CONTRACT_RULE_INPUTS = "Zero inputs should be consumed when issuing an invoice."
                val CONTRACT_RULE_OUTPUTS = "Only one output should be created when issuing an invoice."
                val CONTRACT_RULE_SIGNERS = "All participants are required to sign when issuing an invoice."
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

        class ChangeOwner : Commands {
            companion object {
                val CONTRACT_RULE_INPUTS = "At least one input should be consumed when changing ownership."
                val CONTRACT_RULE_OUTPUTS = "At least one output should be created when changing ownership."
                val CONTRACT_RULE_SIGNERS = "All participants are required to sign when changing ownership."
            }

            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                CONTRACT_RULE_INPUTS using (tx.inputs.isNotEmpty())
                CONTRACT_RULE_OUTPUTS using (tx.outputs.isNotEmpty())

                // State Rules
                val keys = tx.outputsOfType<FundState>()
                        .flatMap { it.participants }
                        .map { it.owningKey }
                        .distinct()

                CONTRACT_RULE_SIGNERS using signers.containsAll(keys)
            }
        }

        class Amend : Commands {
            companion object {
                val CONTRACT_RULE_INPUTS = "Only one input should be consumed when amending an invoice."
                val CONTRACT_RULE_OUTPUTS = "Only one output should be created when amending an invoice."
            }

            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                CONTRACT_RULE_INPUTS using (tx.inputs.size == 1)
                CONTRACT_RULE_OUTPUTS using (tx.outputs.size == 1)
            }
        }

        class Cancel : Commands {
            companion object {
                val CONTRACT_RULE_INPUTS = "Only one input should be consumed when cancelling an invoice."
                val CONTRACT_RULE_OUTPUTS = "Zero outputs should be created when cancelling an invoice."
            }

            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                CONTRACT_RULE_INPUTS using (tx.inputs.size == 1)
                CONTRACT_RULE_OUTPUTS using (tx.outputs.isEmpty())
            }
        }
    }
}
