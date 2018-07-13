package com.property.state

import com.property.schema.FundSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param value the value of the IOU.
 * @param fundManager the party issuing the IOU.
 * @param investor the party receiving and approving the IOU.
 */
data class FundState(val value: Int,
                     val fundManager: Party,
                     val investor: Party,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(fundManager, investor)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is FundSchemaV1 -> FundSchemaV1.PersistentFundState(
                    this.fundManager.name.toString(),
                    this.investor.name.toString(),
                    this.value,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(FundSchemaV1)
}
