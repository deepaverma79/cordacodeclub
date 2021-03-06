package com.property.state

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

data class PropertyState(val address: String,val owner: Party,
                         override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
    override val participants = listOf(owner)
}