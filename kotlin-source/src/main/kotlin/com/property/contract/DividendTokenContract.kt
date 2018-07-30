package com.property.contract

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction


open class DividendTokenContract : Contract {
    companion object {
        @JvmStatic
        val ID = "com.property.contract.DividendTokenContract"
    }

    override fun verify(tx: LedgerTransaction) {
    }

    interface Commands : CommandData {
        class Issue : Commands
    }
}
