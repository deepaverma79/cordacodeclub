package com.property.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.*

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
// TODO : This has to be ManyToManyRelationship
//            @ManyToMany
//            @CollectionTable(name = "Investors", joinColumns = arrayOf(JoinColumn(name = "investor_id")))
//            var investors: List<String>,
            var investors: String,

            @Column(name = "value")
            var value: Int,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", ""/*listOf()*/, 0, UUID.randomUUID())
    }
}