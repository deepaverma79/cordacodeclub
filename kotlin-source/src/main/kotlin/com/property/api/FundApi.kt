package com.property.api

import com.property.flow.FundFlow.Initiator
import com.property.schema.FundSchemaV1
import com.property.state.FundState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueFlow
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("property")
class FundApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<FundApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all FundState states that exist in the node's vault.
     */
    @GET
    @Path("funds")
    @Produces(MediaType.APPLICATION_JSON)
    fun getFunds() = rpcOps.vaultQueryBy<FundState>().states

    /**
     * Initiates a flow to agree an FundState between two parties.
     *
     * Once the flow finishes it will have written the FundState to ledger. Both the fundManager and the investor will be able to
     * see it when calling /api/property/FundStates on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("create-fund")
    fun createFund(
            @QueryParam("fundStateValue") fundStateValue: Int,
            @QueryParam("partyNames") partyNames: List<String>
    ): Response {
        if (fundStateValue <= 0 ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'fundStateValue' must be non-negative.\n").build()
        }
        val parties = partyNames.map { partyName ->
            rpcOps.partiesFromName(partyName, false).single()
        }

        return try {
            val signedTx = rpcOps.startTrackedFlow(::Initiator, fundStateValue, parties).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }
	
	/**
     * Displays all Fund states that are created by Party.
     */
    @GET
    @Path("my-funds")
    @Produces(MediaType.APPLICATION_JSON)
    fun myfunds(): Response {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
        val results = builder {
                var partyType = FundSchemaV1.PersistentFundState::fundManager.equal(rpcOps.nodeInfo().legalIdentities.first().name.toString())
                val customCriteria = QueryCriteria.VaultCustomQueryCriteria(partyType)
                val criteria = generalCriteria.and(customCriteria)
                val results = rpcOps.vaultQueryBy<FundState>(criteria).states
                return Response.ok(results).build()
        }
    }
}