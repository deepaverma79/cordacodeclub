package com.property.state

import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

class DividendToken(val fundManager: Party, val investor: Party, val amount: Int) : ContractState {
    override val participants = listOf(fundManager, investor)
}