package com.property.flow

import co.paralleluniverse.fibers.Suspendable
import com.property.contract.DividendContract
import com.property.contract.DividendTokenContract
import com.property.state.DividendState
import com.property.state.DividendToken
import com.property.state.FundState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

object DividendFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val amount: Int,
                    val fundId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction for dividend flow")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(fundId.id))
            val existingStateAndRef = serviceHub.vaultService.queryBy<FundState>(queryCriteria).states.single()
            val existingState = existingStateAndRef.state.data

            // Stage 1.
            progressTracker.currentStep = DividendFlow.Initiator.Companion.GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val dividendState = DividendState(amount, fundId, existingState.investors)
            val txCommand = Command(DividendContract.Commands.MakePayment(), existingState.investors.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(dividendState, DividendContract.DIVIDEND_CONTRACT_ID)
//                    .addOutputState(DividendToken(fundManager, investor1), DividendTokenContract.ID)
//                    .addOutputState(DividendToken(fundManager, investor2), DividendTokenContract.ID)
//                    .addOutputState(DividendToken(fundManager, investor3), DividendTokenContract.ID)
                    .addCommand(txCommand)
            //TODO: Add DividendTokenContract.Commands.Issue command.

            // Stage 2.
            progressTracker.currentStep = DividendFlow.Initiator.Companion.VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = DividendFlow.Initiator.Companion.SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = DividendFlow.Initiator.Companion.GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            val otherPartyFlows = existingState.investors.map { party -> initiateFlow(party) }
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, otherPartyFlows, DividendFlow.Initiator.Companion.GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = DividendFlow.Initiator.Companion.FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, DividendFlow.Initiator.Companion.FINALISING_TRANSACTION.childProgressTracker()))
        }
    }
}
