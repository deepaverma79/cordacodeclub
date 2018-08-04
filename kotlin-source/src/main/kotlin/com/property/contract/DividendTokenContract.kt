package com.property.contract

import com.property.state.DividendState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

open class DividendTokenContract : Contract {
    companion object {
        @JvmStatic
        val DIVIDEND_TOKEN_CONTRACT_ID = "com.property.contract.DividendTokenContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<DividendTokenContract.Commands>()
        command.value.verify(tx, command.signers)
    }

    interface Commands : CommandData {
        fun verify(tx: LedgerTransaction, signers: List<PublicKey>)

        class Issue : Commands {
            override fun verify(tx: LedgerTransaction, signers: List<PublicKey>) {
                // Transaction Rules
                "No inputs should be consumed when issuing dividend." using (tx.inputs.isEmpty())
                "One output should be generated when receiving Dividend Token." using (tx.outputs.size == 1)
            }
        }
    }
}
