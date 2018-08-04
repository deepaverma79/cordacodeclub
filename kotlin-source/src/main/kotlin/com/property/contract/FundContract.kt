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
        val command = tx.commands.requireSingleCommand<Commands>()
        command.value.verify(tx, command.signers)
    }

    interface Commands : CommandData {
        fun verify(tx: LedgerTransaction, signers: List<PublicKey>)

        class Issue : Commands {
            companion object {
                val CONTRACT_RULE_INPUTS = "Zero inputs should be consumed when issuing an Fund."
                val CONTRACT_RULE_OUTPUTS = "Only one output state should be created."
                val CONTRACT_RULE_SIGNERS = "All participants are required to sign when issuing a fund."
            }

            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                CONTRACT_RULE_INPUTS using (tx.inputs.isEmpty())
                CONTRACT_RULE_OUTPUTS using (tx.outputs.size == 1)

                // State Rules
                val outputState = tx.outputsOfType<FundState>().single()
                CONTRACT_RULE_SIGNERS using (signers.containsAll(outputState.participants.map { it.owningKey }))

                // Fund-specific constraints.
                "The Fund's value must be non-negative." using (outputState.value > 0)
                "The Fund's value must not be greater than a 10 million." using (outputState.value < 10000000)

            }
        }

        class ChangeOwner : Commands {
            companion object {
                val CONTRACT_RULE_INPUTS = "At least one input should be consumed when changing ownership."
                val CONTRACT_RULE_OUTPUTS = "At least one output should be created when changing ownership."
                val CONTRACT_RULE_SIGNERS = "Fund manager, input owner and output owner are required to sign when changing ownership."
            }

            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                CONTRACT_RULE_INPUTS using (tx.inputs.isNotEmpty())
                CONTRACT_RULE_OUTPUTS using (tx.outputs.isNotEmpty())

                // State Rules
                val input = tx.inputsOfType<FundState>().single()
                val output = tx.outputsOfType<FundState>().single()
                val newInvestor = output.investors.find { investor -> investor !in input.investors }!!
                val oldInvestor = input.investors.find { investor -> investor !in output.investors }!!

                val requiredSigners = listOf(input.fundManager, newInvestor, oldInvestor)
                val keys = requiredSigners.map { it.owningKey }

                CONTRACT_RULE_SIGNERS using signers.containsAll(keys)
            }
        }

        //When a fund is cancelled the investors are then paid
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
