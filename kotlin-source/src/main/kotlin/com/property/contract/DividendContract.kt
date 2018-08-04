package com.property.contract

import com.property.state.FundState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey


open class DividendContract : Contract {
    companion object {
        @JvmStatic
        val DIVIDEND_CONTRACT_ID = "com.property.contract.DividendContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<DividendContract.Commands>()
        command.value.verify(tx, command.signers)
    }

    interface Commands : CommandData {
        fun verify(tx: LedgerTransaction, signers: List<PublicKey>)

        class MakePayment : Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                "Zero inputs should be consumed when issuing dividend." using (tx.inputs.isEmpty())
                "All signers must be issued dividend." using (tx.outputs.size == signers.size)

                // State Rules
                val outputState = tx.outputsOfType<FundState>().single()
                "All participants are required to sign when issuing dividend." using (signers.containsAll(outputState.participants.map { it.owningKey }))
            }
        }
    }
}
