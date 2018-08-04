package com.property.flow

import co.paralleluniverse.fibers.Suspendable
import com.property.contract.FundContract
import com.property.contract.FundContract.Companion.FUND_CONTRACT_ID
import com.property.state.FundState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step


object ChangeOwnerFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val currentOwner: Party,
                    val newOwner: Party,
                    val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction based on new Owner.")
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

            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(linearId.id))
            val existingStateAndRef = serviceHub.vaultService.queryBy<FundState>(queryCriteria).states.single()
            val existingState = existingStateAndRef.state.data

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val fundState = FundState(existingState.value, existingState.fundManager, existingState.investors.filter { party -> party != currentOwner } + newOwner)
            val txCommand = Command(FundContract.Commands.ChangeOwner(), listOf(existingState.fundManager.owningKey, currentOwner.owningKey, newOwner.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(existingStateAndRef)
                    .addOutputState(fundState, FUND_CONTRACT_ID)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            val otherParties = listOf(existingState.fundManager, currentOwner, newOwner).filter { it != ourIdentity }
            val otherPartyFlows = otherParties.map { initiateFlow(it) }
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, otherPartyFlows, GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                }
            }
            return subFlow(signTransactionFlow)
        }
    }
}
