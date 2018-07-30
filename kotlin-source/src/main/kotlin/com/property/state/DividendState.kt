package com.property.state

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

data class DividendState(val amount: Int,
                         val fundId: UniqueIdentifier,
                         val investors: List<Party>,
                         override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
    override val participants = investors
}