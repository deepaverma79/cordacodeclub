package com.property.api

//import com.property.flow.ChangeOwnerFlow
import com.property.flow.ChangeOwnerFlow
import com.property.flow.DividendFlow
import com.property.flow.FundFlow
import com.property.flow.PropertyFlow
import com.property.state.DividendState
import com.property.state.FundState
import com.property.state.PropertyState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.IdentityService
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("property")
class PropertyServicesApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<PropertyServicesApi>()
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

    @GET
    @Path("my-properties")
    @Produces(MediaType.APPLICATION_JSON)
    fun getMyProperties() = rpcOps.vaultQueryBy<PropertyState>().states

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
            @QueryParam("fundStateValue") fundStateValue: Int?,
            @QueryParam("partyNames") partyNames: List<String>
    ): Response {
        if (fundStateValue == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'fundStateValue' must be non-negative.\n").build()
        }
        val parties = partyNames.map { partyName ->
            rpcOps.partiesFromName(partyName, false).single()
        }

        return try {
            val signedTx = rpcOps.startTrackedFlow(FundFlow::Initiator, fundStateValue, parties).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @PUT
    @Path("sell-fund-share")
    fun changeFundInvestors(
            @QueryParam("currentInvestor") currentInvestorString: String?,
            @QueryParam("newInvestor") newInvestorString: String?,
            @QueryParam("fundId") fundIdString: String?
    ): Response {
        if (currentInvestorString == null ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'currentInvestor' must be present.\n").build()
        }
        if (newInvestorString == null ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'newInvestor' must be present.\n").build()
        }
        if (fundIdString == null ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'fundId' must be present.\n").build()
        }
        val newInvestor = rpcOps.partiesFromName(newInvestorString, false).single()
        val currentInvestor = rpcOps.partiesFromName(currentInvestorString, false).single()
        val fundId = UniqueIdentifier.fromString(fundIdString)

        return try {
            val signedTx = rpcOps.startTrackedFlow(ChangeOwnerFlow::Initiator, currentInvestor, newInvestor, fundId).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction ${signedTx.id} to sell shares from ${currentInvestorString} to ${newInvestorString} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @PUT
    @Path("pay-dividend")
    fun payDividend(
            @QueryParam("amount") amount: Int?,
            @QueryParam("fundId") fundIdString: String?
    ): Response {
        if (amount == null ) {
            return Response.status(BAD_REQUEST).entity("Specify the amount to be paid as dividend.\n").build()
        }
        if (fundIdString == null ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'fundId' must be present.\n").build()
        }
        val fundId = UniqueIdentifier.fromString(fundIdString)
        return try {
            val signedTx = rpcOps.startTrackedFlow(DividendFlow::Initiator, amount, fundId).returnValue.getOrThrow()
            Response.status(CREATED).entity("Amount ${amount} to be paid as for ${fundId} committed to ledger with id:  ${signedTx.id}.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

    @PUT
    @Path("register-property")
    fun createFund(
            @QueryParam("address") address: String?,
            @QueryParam("propertyManager") propertyManager: String?
    ): Response {
        if (address == null ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'address' must be present.\n").build()
        }
        if (propertyManager == null ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'propertyManager' must be present.\n").build()
        }
        val party = rpcOps.partiesFromName(propertyManager, false).single()
        return try {
            val signedTx = rpcOps.startTrackedFlow(PropertyFlow::Initiator, address, party).returnValue.getOrThrow()
            Response.status(CREATED).entity("Property id ${signedTx.id} committed to ledger.\n").build()

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
        val funds = rpcOps.vaultQueryBy<FundState>().states
        return Response.ok(funds).build()
    }

    @GET
    @Path("my-dividend")
    @Produces(MediaType.APPLICATION_JSON)
    fun issuedDividends(): Response {
        val dividends = rpcOps.vaultQueryBy<DividendState>().states
        return Response.ok(dividends).build()
    }
}