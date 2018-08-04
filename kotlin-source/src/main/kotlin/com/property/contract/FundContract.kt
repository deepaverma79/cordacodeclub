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
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                "Zero inputs should be consumed when issuing an Fund." using (tx.inputs.isEmpty())
                "Only one output state should be created." using (tx.outputs.size == 1)

                // State Rules
                val outputState = tx.outputsOfType<FundState>().single()
                "All participants are required to sign when issuing a fund." using (signers.containsAll(outputState.participants.map { it.owningKey }))

                // Fund-specific constraints.
                "The Fund's value must be non-negative." using (outputState.value > 0)
                "The Fund's value must not be greater than a 10 million." using (outputState.value < 10000000)
            }
        }

        class ChangeOwner : Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                "At least one input should be consumed when changing ownership." using (tx.inputs.isNotEmpty())
                "At least one output should be created when changing ownership." using (tx.outputs.isNotEmpty())

                // State Rules
                val input = tx.inputsOfType<FundState>().single()
                val output = tx.outputsOfType<FundState>().single()
                val newInvestor = output.investors.find { investor -> investor !in input.investors }!!
                val oldInvestor = input.investors.find { investor -> investor !in output.investors }!!

                val requiredSigners = listOf(input.fundManager, newInvestor, oldInvestor)
                val keys = requiredSigners.map { it.owningKey }

                "Fund manager, input owner and output owner are required to sign when changing ownership." using signers.containsAll(keys)
            }
        }

        //When a fund is cancelled the investors are then paid
        class Cancel : Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                "Only one input should be consumed when cancelling a fund." using (tx.inputs.size == 1)
                "Zero outputs should be created when cancelling a fund." using (tx.outputs.isEmpty())
            }
        }
    }
}
