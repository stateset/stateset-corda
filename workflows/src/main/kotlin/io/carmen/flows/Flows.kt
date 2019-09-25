package io.carmen

import co.paralleluniverse.fibers.Suspendable
import io.carmen.account.Account
import io.carmen.account.AccountContract
import io.carmen.account.AccountContract.Companion.ACCOUNT_CONTRACT_ID
import io.carmen.application.Application
import io.carmen.application.ApplicationContract
import io.carmen.application.ApplicationContract.Companion.APPLICATION_CONTRACT_ID
import io.carmen.application.ApplicationStatus
import io.carmen.case.*
import io.carmen.case.CaseContract.Companion.CASE_CONTRACT_ID
import io.carmen.chat.Chat
import io.carmen.contact.Contact
import io.carmen.contact.ContactContract
import io.carmen.contact.ContactContract.Companion.CONTACT_CONTRACT_ID
import io.carmen.lead.Lead
import io.carmen.lead.LeadContract
import io.carmen.lead.LeadContract.Companion.LEAD_CONTRACT_ID
import io.carmen.chat.Chat.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.identity.PartyAndCertificate
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.queryBy
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


// *********
// * Create Account Flow *
// *********


object CreateAccountFlow {
    @InitiatingFlow
    @StartableByRPC
    class Controller(val accountId: String,
                     val accountName: String,
                     val accountType: String,
                     val industry: String,
                     val phone: String,
                     val processor: Party) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Trade.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
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

        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION

            // Generate an unsigned transaction.
            val accountState = Account(accountId, accountName, accountType, industry, phone, serviceHub.myInfo.legalIdentities.first(), processor)
            val txCommand = Command(AccountContract.Commands.CreateAccount(), accountState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary = notary)
                    txBuilder.addOutputState(accountState, ACCOUNT_CONTRACT_ID)
                    txBuilder.addCommand(txCommand)

            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)


            val otherPartyFlow = initiateFlow(processor)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))

            // Finalising the transaction.
            return subFlow(FinalityFlow(fullySignedTx, listOf(otherPartyFlow), FINALISING_TRANSACTION.childProgressTracker()))
        }
    }


    @InitiatedBy(Controller::class)
    class AccountProcessor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Account transaction." using (output is Account)
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}




// *********
// * Create Contact Flow *
// *********

object CreateContactFlow {
    @InitiatingFlow
    @StartableByRPC
    class Controller(val contactId: String,
                     val firstName: String,
                     val lastName: String,
                     val email: String,
                     val phone: String,
                     val processor: Party) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Trade.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
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

        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION

            // Generate an unsigned transaction.
            val contactState = Contact(contactId, firstName, lastName, email, phone, serviceHub.myInfo.legalIdentities.first(), processor)
            val txCommand = Command(ContactContract.Commands.CreateContact(), contactState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary = notary)
                    txBuilder.addOutputState(contactState, CONTACT_CONTRACT_ID)
                    txBuilder.addCommand(txCommand)

            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)


            val otherPartyFlow = initiateFlow(processor)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))

            // Finalising the transaction.
            return subFlow(FinalityFlow(fullySignedTx, listOf(otherPartyFlow)))
        }
    }


    @InitiatedBy(Controller::class)
    class Processor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Contact transaction." using (output is Contact)
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}




object CreateLeadFlow {
    @InitiatingFlow
    @StartableByRPC
    class Controller(val leadId: String,
                     val firstName: String,
                     val lastName: String,
                     val company: String,
                     val title: String,
                     val email: String,
                     val phone: String,
                     val country: String,
                     val processor: Party) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Trade.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
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

        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION

            // Generate an unsigned transaction.
            val leadState = Lead(leadId, firstName, lastName, company, title, email, phone, country, serviceHub.myInfo.legalIdentities.first(), processor)
            val txCommand = Command(LeadContract.Commands.CreateLead(), leadState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary = notary)
                    txBuilder.addOutputState(leadState, LEAD_CONTRACT_ID)
                    txBuilder.addCommand(txCommand)

            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)


            val otherPartyFlow = initiateFlow(processor)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))

            // Finalising the transaction.
            return subFlow(FinalityFlow(fullySignedTx, listOf(otherPartyFlow)))
        }
    }


    @InitiatedBy(Controller::class)
    class Processor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Contact transaction." using (output is Lead)
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}


// *********
// * Create Case  Flow *
// *********

object CreateCaseFlow {
    @InitiatingFlow
    @StartableByRPC
    @CordaSerializable
    class Initiator(val caseId: String,
                    val caseName: String,
                    val caseNumber: String,
                    val description: String,
                    val caseStatus: CaseStatus,
                    val casePriority: CasePriority,
                    val resolver: Party) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Trade.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
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

        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION

            // Generate an unsigned transaction.
            val caseState = Case(caseId, caseName, caseNumber, description, caseStatus, casePriority, serviceHub.myInfo.legalIdentities.first(), resolver)
            val txCommand = Command(CaseContract.Commands.CreateCase(), caseState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary = notary)
                    txBuilder.addOutputState(caseState, CASE_CONTRACT_ID)
                    txBuilder.addCommand(txCommand)

            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)


            val otherPartyFlow = initiateFlow(resolver)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))

            // Finalising the transaction.
            return subFlow(FinalityFlow(fullySignedTx, listOf(otherPartyFlow)))
        }
    }

    @InitiatedBy(Initiator::class)
    class
    Resolver(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Contact transaction." using (output is Case)
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}

// *********
// * Close Case Flow *
// *********

@InitiatingFlow
@StartableByRPC
class CloseCaseFlow(val caseId: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val caseStateAndRef = serviceHub.vaultService.queryBy<Case>().states.find {
            it.state.data.caseId == caseId
        } ?: throw IllegalArgumentException("No Case with ID $caseId found.")


        val case = caseStateAndRef.state.data
        val caseStatus = CaseStatus.CLOSED


        // Creating the output.
        val closedCase = Case(
                case.caseId,
                case.caseName,
                case.caseNumber,
                case.description,
                caseStatus,
                case.casePriority,
                case.submitter,
                case.resolver,
                case.linearId)

        // Building the transaction.
        val notary = caseStateAndRef.state.notary
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(caseStateAndRef)
        txBuilder.addOutputState(closedCase, CaseContract.CASE_CONTRACT_ID)
        txBuilder.addCommand(CaseContract.Commands.CloseCase(), ourIdentity.owningKey)
        txBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(txBuilder)
    }

    @InitiatedBy(CloseCaseFlow::class)
    class CaseCloser(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a Case transaction." using (output is Case)
                    val case= output as Case
                    val caseStatus = CaseStatus.CLOSED
                }
            }

            val signedTransaction = subFlow(signTransactionFlow)
            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = signedTransaction.id))
        }
    }
}



// *********
// * Close Case Flow *
// *********

@InitiatingFlow
@StartableByRPC
class ResolveCaseFlow(val caseId: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val caseStateAndRef = serviceHub.vaultService.queryBy<Case>().states.find {
            it.state.data.caseId == caseId
        } ?: throw IllegalArgumentException("No Case with ID $caseId found.")


        val case = caseStateAndRef.state.data
        val caseStatus = CaseStatus.RESOLVED


        // Creating the output.
        val resolvedCase = Case(
                case.caseId,
                case.caseName,
                case.caseNumber,
                case.description,
                caseStatus,
                case.casePriority,
                case.submitter,
                case.resolver,
                case.linearId)

        // Building the transaction.
        val notary = caseStateAndRef.state.notary
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(caseStateAndRef)
        txBuilder.addOutputState(resolvedCase, CaseContract.CASE_CONTRACT_ID)
        txBuilder.addCommand(CaseContract.Commands.ResolveCase(), ourIdentity.owningKey)
        txBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(txBuilder)
    }

    @InitiatedBy(ResolveCaseFlow::class)
    class CaseResolver(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a Case transaction." using (output is Case)
                    val case= output as Case
                    val caseStatus = CaseStatus.RESOLVED
                }
            }

            val signedTransaction = subFlow(signTransactionFlow)
            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = signedTransaction.id))
        }
    }
}


// *********
// * Escalate Case Flow *
// *********

@InitiatingFlow
@StartableByRPC
class EscalateCaseFlow(val caseId: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val caseStateAndRef = serviceHub.vaultService.queryBy<Case>().states.find {
            it.state.data.caseId == caseId
        } ?: throw IllegalArgumentException("No Case with ID $caseId found.")


        val case = caseStateAndRef.state.data
        val caseStatus = CaseStatus.ESCALATED


        // Creating the output.
        val escalatedCase = Case(
                case.caseId,
                case.caseName,
                case.caseNumber,
                case.description,
                caseStatus,
                case.casePriority,
                case.submitter,
                case.resolver,
                case.linearId)

        // Building the transaction.
        val notary = caseStateAndRef.state.notary
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(caseStateAndRef)
        txBuilder.addOutputState(escalatedCase, CaseContract.CASE_CONTRACT_ID)
        txBuilder.addCommand(CaseContract.Commands.ResolveCase(), ourIdentity.owningKey)
        txBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(txBuilder)
    }

    @InitiatedBy(EscalateCaseFlow::class)
    class Escalator(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a Case transaction." using (output is Case)
                    val case = output as Case
                    val caseStatus = CaseStatus.ESCALATED
                }
            }

            val signedTransaction = subFlow(signTransactionFlow)
            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = signedTransaction.id))
        }
    }
}


// *********
// * Send Message Flows *
// *********


@InitiatingFlow
@StartableByRPC
class SendMessage(private val to: Party, private val userId: String, private val body: String) : FlowLogic<Unit>() {

    companion object {
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Message.")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
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

    @Suspendable
    override fun call() {
        val stx: SignedTransaction = createMessageStx()
        val otherPartySession = initiateFlow(to)
        progressTracker.nextStep()
        subFlow(FinalityFlow(stx, setOf(otherPartySession), FINALISING_TRANSACTION.childProgressTracker()))
    }

    private fun createMessageStx(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txb = TransactionBuilder(notary)
        val me = ourIdentityAndCert.party
        val fromUserId = 21039231.toString()
        val sent = true
        val delivered = false
        val fromMe = true
        val time = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        val formatted = time.format(formatter)
        val messageNumber = 100.toString()
        txb.addOutputState(Chat.Message(UniqueIdentifier(), body, fromUserId, to, me, userId, sent, delivered, fromMe, formatted, messageNumber), Chat::class.qualifiedName!!)
        txb.addCommand(Chat.SendMessageCommand, me.owningKey)
        return serviceHub.signInitialTransaction(txb)
    }

    @InitiatedBy(SendMessage::class)
    class SendChatResponder(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val stx = subFlow(object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    val message = stx.coreTransaction.outputsOfType<Chat.Message>().single()
                    require(message.from != ourIdentity) {
                        "The sender of the new message cannot have my identity when I am not the creator of the transaction"
                    }
                    require(message.from == otherPartySession.counterparty) {
                        "The sender of the reply must must be the party creating this transaction"
                    }
                }
            })
            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = stx.id))
        }
    }

}





// *********
// * Create Application Flow *
// *********



object CreateApplicationFlow {
    @StartableByRPC
    @InitiatingFlow
    @Suspendable
    class Initiator(val applicationId: String,
                    val applicationName: String,
                    val industry: String,
                    val applicationStatus: ApplicationStatus,
                    val otherParty: Party) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Agreement.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
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
            progressTracker.currentStep = GENERATING_TRANSACTION

            val applicationState = Application(applicationId, applicationName, industry, applicationStatus, serviceHub.myInfo.legalIdentities.first(), otherParty)
            val txCommand = Command(ApplicationContract.Commands.CreateApplication(), applicationState.participants.map { it.owningKey })
            progressTracker.currentStep = VERIFYING_TRANSACTION
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(applicationState, APPLICATION_CONTRACT_ID)
                    .addCommand(txCommand)

            txBuilder.verify(serviceHub)
            // Sign the transaction.
            progressTracker.currentStep = SIGNING_TRANSACTION
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            val otherPartySession = initiateFlow(otherParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession), GATHERING_SIGS.childProgressTracker()))

            // Finalising the transaction.
            return subFlow(FinalityFlow(fullySignedTx, listOf(otherPartySession)))
        }
    }


    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Application transaction." using (output is Application)
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}





// *********
// * Approve Application Flow *
// *********

@InitiatingFlow
@StartableByRPC
class ApproveApplicationFlow(val applicationId: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val applicationStateAndRef = serviceHub.vaultService.queryBy<Application>().states.find {
            it.state.data.applicationId == applicationId
        } ?: throw IllegalArgumentException("No agreement with ID $applicationId found.")


        val application = applicationStateAndRef.state.data
        val applicationStatus = ApplicationStatus.APPROVED


        // Creating the output.
        val approvedApplication = Application(
                application.applicationId,
                application.applicationName,
                application.industry,
                applicationStatus,
                application.agent,
                application.provider,
                application.linearId)

        // Building the transaction.
        val notary = applicationStateAndRef.state.notary
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(applicationStateAndRef)
        txBuilder.addOutputState(approvedApplication, ApplicationContract.APPLICATION_CONTRACT_ID)
        txBuilder.addCommand(ApplicationContract.Commands.ApproveApplication(), ourIdentity.owningKey)
        txBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(txBuilder)
    }

    @InitiatedBy(ApproveApplicationFlow::class)
    class Approver(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Agreement transaction." using (output is Application)
                    val application = output as Application
                    val applicationStatus = ApplicationStatus.APPROVED
                }
            }

            val signedTransaction = subFlow(signTransactionFlow)
            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = signedTransaction.id))
        }
    }
}




// *********
// * Reject Application Flow *
// *********


@InitiatingFlow
@StartableByRPC
class RejectApplicationFlow(val applicationId: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val applicationStateAndRef = serviceHub.vaultService.queryBy<Application>().states.find {
            it.state.data.applicationId == applicationId
        } ?: throw IllegalArgumentException("No agreement with ID $applicationId found.")


        val application = applicationStateAndRef.state.data
        val applicationStatus = ApplicationStatus.REJECTED

        // Creating the output.
        val rejectedApplication = Application(
                application.applicationId,
                application.applicationName,
                application.industry,
                applicationStatus,
                application.agent,
                application.provider,
                application.linearId)

        // Building the transaction.
        val notary = applicationStateAndRef.state.notary
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(applicationStateAndRef)
        txBuilder.addOutputState(rejectedApplication, ApplicationContract.APPLICATION_CONTRACT_ID)
        txBuilder.addCommand(ApplicationContract.Commands.RejectApplication(), ourIdentity.owningKey)
        txBuilder.verify(serviceHub)

        val stx = serviceHub.signInitialTransaction(txBuilder)
        return serviceHub.signInitialTransaction(txBuilder)
    }

    @InitiatedBy(RejectApplicationFlow::class)
    class Rejecter(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Agreement transaction." using (output is Application)
                    val application = output as Application
                    val applicationStatus = ApplicationStatus.REJECTED
                }
            }

            val signedTransaction = subFlow(signTransactionFlow)
            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = signedTransaction.id))
        }
    }
}




// ****************
// * Review Application Flow *
// ****************



@InitiatingFlow
@StartableByRPC
class ReviewApplicationFlow(val applicationId: String): FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val applicationStateAndRef = serviceHub.vaultService.queryBy<Application>().states.find {
            it.state.data.applicationId == applicationId
        } ?: throw IllegalArgumentException("No agreement with ID $applicationId found.")


        val application = applicationStateAndRef.state.data
        val applicationStatus = ApplicationStatus.INREVIEW

        // Creating the output.
        val reviewedApplication = Application(
                application.applicationId,
                application.applicationName,
                application.industry,
                applicationStatus,
                application.agent,
                application.provider,
                application.linearId)

        // Building the transaction.
        val notary = applicationStateAndRef.state.notary
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(applicationStateAndRef)
        txBuilder.addOutputState(reviewedApplication, ApplicationContract.APPLICATION_CONTRACT_ID)
        txBuilder.addCommand(ApplicationContract.Commands.RejectApplication(), ourIdentity.owningKey)
        txBuilder.verify(serviceHub)

        val stx = serviceHub.signInitialTransaction(txBuilder)
        return serviceHub.signInitialTransaction(txBuilder)
    }

    @InitiatedBy(ReviewApplicationFlow::class)
    class Reviewer(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Agreement transaction." using (output is Application)
                    val application = output as Application
                    val applicationStatus =  ApplicationStatus.INREVIEW
                }
            }

            val signedTransaction = subFlow(signTransactionFlow)
            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = signedTransaction.id))
        }
    }
}