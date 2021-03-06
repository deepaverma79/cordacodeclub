package com.property.contract

import com.property.state.DividendState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
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
                "No inputs should be consumed when issuing dividend." using (tx.inputs.isEmpty())
                "All signers must be issued dividend." using (tx.outputs.size == signers.size)

                // State Rules
                tx.outputsOfType<DividendState>().map {  "Dividend payable value must be non-negative." using (it.amount > 0) }
            }
        }
    }
}
