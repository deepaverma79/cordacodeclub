package com.property.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for FundState.
 */
object FundSchema

/**
 * An FundState schema.
 */
object FundSchemaV1 : MappedSchema(
        schemaFamily = FundSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentFundState::class.java)) {
    @Entity
    @Table(name = "FundState")
    class PersistentFundState(
            @Column(name = "fundManager")
            var fundManager: String,

            @Column(name = "investors")
            @ElementCollection
            var investors: List<String>,

            @Column(name = "value")
            var value: Int,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", listOf(), 0, UUID.randomUUID())
    }
}