package io.stateset

import co.paralleluniverse.fibers.Suspendable
import io.stateset.account.Account
import io.stateset.account.AccountContract
import io.stateset.account.AccountContract.Companion.ACCOUNT_CONTRACT_ID
import io.stateset.account.TypeOfBusiness
import io.stateset.agreement.Agreement
import io.stateset.agreement.AgreementContract
import io.stateset.agreement.AgreementContract.Companion.AGREEMENT_CONTRACT_ID
import io.stateset.agreement.AgreementStatus
import io.stateset.agreement.AgreementType
import io.stateset.approval.*
import io.stateset.approval.ApprovalContract.Companion.APPROVAL_CONTRACT_ID
import io.stateset.case.*
import io.stateset.case.CaseContract.Companion.CASE_CONTRACT_ID
import io.stateset.chat.Chat
import io.stateset.contact.Contact
import io.stateset.contact.ContactContract
import io.stateset.contact.ContactContract.Companion.CONTACT_CONTRACT_ID
import io.stateset.lead.Lead
import io.stateset.lead.LeadContract
import io.stateset.lead.LeadContract.Companion.LEAD_CONTRACT_ID
import io.stateset.invoice.Invoice
import io.stateset.invoice.InvoiceContract
import io.stateset.invoice.InvoiceContract.Companion.INVOICE_CONTRACT_ID
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*


// *********
// * Activate Agreement Flow *
// *********

object ActivateFlow {
    @InitiatingFlow
    @StartableByRPC
    class ActivateAgreementFlow(val agreementNumber: String) : FlowLogic<SignedTransaction>() {

        override val progressTracker = ProgressTracker()

        @Suspendable
        override fun call(): SignedTransaction {


            // Retrieving the Agreement Input from the Vault
            val agreementStateAndRef = serviceHub.vaultService.queryBy<Agreement>().states.find {
                it.state.data.agreementNumber == agreementNumber
            } ?: throw IllegalArgumentException("No Agreement with ID $agreementNumber found.")


            //   val agreementLineItemStateAndDef = serviceHub.vaultService.queryBy<AgreementLineItem>().states.find {
            //       it.state.data.agreementNumber == agreementNumber
            //    } ?: throw IllegalArgumentException("No Agreement Line Item associated to $agreementNumber found.")


            val agreement = agreementStateAndRef.state.data
            //    val agreementLineItem = agreementLineItemStateAndDef.state.data
            val agreementStatus = AgreementStatus.INEFFECT
            //   val agreementLineItemStatus = AgreementLineItemStatus.ACTIVATED


            // Creating the Activated Agreement output.

            val activatedAgreement = Agreement(
                    agreement.agreementNumber,
                    agreement.agreementName,
                    agreement.agreementHash,
                    agreementStatus,
                    agreement.agreementType,
                    agreement.totalAgreementValue,
                    agreement.party,
                    agreement.counterparty,
                    agreement.agreementStartDate,
                    agreement.agreementEndDate,
                    agreement.active,
                    agreement.createdAt,
                    agreement.lastUpdated,
                    agreement.linearId)


            // Creating the command.
            val requiredSigners = listOf(agreement.party.owningKey, agreement.counterparty.owningKey)
            val command = Command(AgreementContract.Commands.ActivateAgreement(), requiredSigners)

            // Created the Activated Agreement Line Item output.


            // val activatedAgreementLineItem = AgreementLineItem(
            //        agreementLineItem.agreement,
            //       agreementLineItem.agreementNumber,
            //       agreementLineItem.agreementLineItemName,
            //      agreementLineItemStatus,
            //      agreementLineItem.agreementLineItemValue,
            //      agreementLineItem.party,
            //     agreementLineItem.counterparty,
            //     agreementLineItem.lineItem,
            //     agreementLineItem.active,
            //     agreementLineItem.createdAt,
            //     agreementLineItem.lastUpdated,
            //     agreementLineItem.linearId

            //  )

            // Building the transaction.
            val notary = agreementStateAndRef.state.notary
            val txBuilder = TransactionBuilder(notary)
            txBuilder.addInputState(agreementStateAndRef)
            // txBuilder.addInputState((agreementLineItemStateAndDef))
            txBuilder.addOutputState(activatedAgreement, AgreementContract.AGREEMENT_CONTRACT_ID)
            // txBuilder.addOutputState(activatedAgreementLineItem, AgreementLineItemContract.AGREEMENT_LINEITEM_CONTRACT_ID)
            txBuilder.addCommand(command)
            // txBuilder.addCommand(AgreementLineItemContract.Commands.ActivateAgreementLineItem(), ourIdentity.owningKey)


            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Gathering the counterparty's signgature
            val counterparty = if (ourIdentity == agreement.party) agreement.counterparty else agreement.party
            val counterpartySession = initiateFlow(counterparty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, listOf(counterpartySession)))

            // Finalising the transaction.
            return subFlow(FinalityFlow(fullySignedTx, listOf(counterpartySession)))
        }
    }

    @InitiatedBy(ActivateAgreementFlow::class)
    class Acceptor(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    val counterparty = ledgerTx.inputsOfType<Agreement>().single().counterparty
                    if (counterparty != counterpartySession.counterparty) {
                        throw FlowException("Only the counterparty can activate the Agreement")
                    }
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(counterpartySession, txId))
        }
    }
}


// *********
// * Renew Agreement Flow *
// *********

object RenewFlow {
    @InitiatingFlow
    @StartableByRPC
    class RenewAgreementFlow(val agreementNumber: String) : FlowLogic<SignedTransaction>() {

        override val progressTracker = ProgressTracker()

        @Suspendable
        override fun call(): SignedTransaction {

            // Retrieving the Agreement Input from the Vault
            val agreementStateAndRef = serviceHub.vaultService.queryBy<Agreement>().states.find {
                it.state.data.agreementNumber == agreementNumber
            } ?: throw IllegalArgumentException("No agreement with ID $agreementNumber found.")


            val agreement = agreementStateAndRef.state.data
            val agreementStatus = AgreementStatus.RENEWED


            // Creating the Renewal output.

            val renewedAgreement = Agreement(
                    agreement.agreementNumber,
                    agreement.agreementName,
                    agreement.agreementHash,
                    agreementStatus,
                    agreement.agreementType,
                    agreement.totalAgreementValue,
                    agreement.party,
                    agreement.counterparty,
                    agreement.agreementStartDate,
                    agreement.agreementEndDate,
                    agreement.active,
                    agreement.createdAt,
                    agreement.lastUpdated,
                    agreement.linearId)

            // Creating the command.
            val requiredSigners = listOf(agreement.party.owningKey, agreement.counterparty.owningKey)
            val command = Command(AgreementContract.Commands.RenewAgreement(), requiredSigners)

            // Building the transaction.
            val notary = agreementStateAndRef.state.notary
            val txBuilder = TransactionBuilder(notary)
            txBuilder.addInputState(agreementStateAndRef)
            txBuilder.addOutputState(renewedAgreement, AgreementContract.AGREEMENT_CONTRACT_ID)
            txBuilder.addCommand(command)

            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Gathering the counterparty's signgature
            val counterparty = if (ourIdentity == agreement.party) agreement.counterparty else agreement.party
            val counterpartySession = initiateFlow(counterparty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, listOf(counterpartySession)))

            // Finalising the transaction.
            return subFlow(FinalityFlow(fullySignedTx, listOf(counterpartySession)))
        }
    }

    @InitiatedBy(RenewAgreementFlow::class)
    class Acceptor(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    val counterparty = ledgerTx.inputsOfType<Agreement>().single().counterparty
                    if (counterparty != counterpartySession.counterparty) {
                        throw FlowException("Only the counterparty can Renew the Agreement")
                    }
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(counterpartySession, txId))
        }
    }
}

// *********
// * Amend Agreement Flow *
// *********

object AmendFlow {
    @InitiatingFlow
    @StartableByRPC
    class AmendAgreementFlow(val agreementNumber: String) : FlowLogic<SignedTransaction>() {

        override val progressTracker = ProgressTracker()

        @Suspendable
        override fun call(): SignedTransaction {


            // Retrieving the Agreement Input from the Vault
            val agreementStateAndRef = serviceHub.vaultService.queryBy<Agreement>().states.find {
                it.state.data.agreementNumber == agreementNumber
            } ?: throw IllegalArgumentException("No agreement with ID $agreementNumber found.")


            val agreement = agreementStateAndRef.state.data
            val agreementStatus = AgreementStatus.AMENDED


            // Creating the Amended Agreement output.


            val amendedAgreement = Agreement(
                    agreement.agreementNumber,
                    agreement.agreementName,
                    agreement.agreementHash,
                    agreementStatus,
                    agreement.agreementType,
                    agreement.totalAgreementValue,
                    agreement.party,
                    agreement.counterparty,
                    agreement.agreementStartDate,
                    agreement.agreementEndDate,
                    agreement.active,
                    agreement.createdAt,
                    agreement.lastUpdated,
                    agreement.linearId)

            // Creating the command.
            val requiredSigners = listOf(agreement.party.owningKey, agreement.counterparty.owningKey)
            val command = Command(AgreementContract.Commands.AmendAgreement(), requiredSigners)

            // Building the transaction.
            val notary = agreementStateAndRef.state.notary
            val txBuilder = TransactionBuilder(notary)
            txBuilder.addInputState(agreementStateAndRef)
            txBuilder.addOutputState(amendedAgreement, AgreementContract.AGREEMENT_CONTRACT_ID)
            txBuilder.addCommand(command)


            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Gathering the counterparty's signgature
            val counterparty = if (ourIdentity == agreement.party) agreement.counterparty else agreement.party
            val counterpartySession = initiateFlow(counterparty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, listOf(counterpartySession)))

            // Finalising the transaction.
            return subFlow(FinalityFlow(fullySignedTx, listOf(counterpartySession)))
        }
    }

    @InitiatedBy(AmendAgreementFlow::class)
    class Acceptor(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    val counterparty = ledgerTx.inputsOfType<Agreement>().single().counterparty
                    if (counterparty != counterpartySession.counterparty) {
                        throw FlowException("Only the counterparty can Amend the Agreement")
                    }
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(counterpartySession, txId))
        }
    }
}


// *********
// * Terminate Agreement Flow *
// *********

object TerminateFlow {
    @InitiatingFlow
    @StartableByRPC
    class TerminateAgreementFlow(val agreementNumber: String) : FlowLogic<SignedTransaction>() {

        override val progressTracker = ProgressTracker()

        @Suspendable
        override fun call(): SignedTransaction {

            // Retrieving the Agreement Input from the Vault
            val agreementStateAndRef = serviceHub.vaultService.queryBy<Agreement>().states.find {
                it.state.data.agreementNumber == agreementNumber
            } ?: throw IllegalArgumentException("No agreement with ID $agreementNumber found.")


            val agreement = agreementStateAndRef.state.data
            val agreementStatus = AgreementStatus.TERMINATED


            // Creating the output.
            val terminatedAgreement = Agreement(
                    agreement.agreementNumber,
                    agreement.agreementName,
                    agreement.agreementHash,
                    agreementStatus,
                    agreement.agreementType,
                    agreement.totalAgreementValue,
                    agreement.party,
                    agreement.counterparty,
                    agreement.agreementStartDate,
                    agreement.agreementEndDate,
                    agreement.active,
                    agreement.createdAt,
                    agreement.lastUpdated,
                    agreement.linearId)

            // Creating the command.
            val requiredSigners = listOf(agreement.party.owningKey, agreement.counterparty.owningKey)
            val command = Command(AgreementContract.Commands.TerminateAgreement(), requiredSigners)

            // Building the transaction.
            val notary = agreementStateAndRef.state.notary
            val txBuilder = TransactionBuilder(notary)
            txBuilder.addInputState(agreementStateAndRef)
            txBuilder.addOutputState(terminatedAgreement, AgreementContract.AGREEMENT_CONTRACT_ID)
            txBuilder.addCommand(command)


            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Gathering the counterparty's signgature
            val counterparty = if (ourIdentity == agreement.party) agreement.counterparty else agreement.party
            val counterpartySession = initiateFlow(counterparty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, listOf(counterpartySession)))

            // Finalising the transaction.
            return subFlow(FinalityFlow(fullySignedTx, listOf(counterpartySession)))
        }
    }

    @InitiatedBy(TerminateAgreementFlow::class)
    class Acceptor(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    val counterparty = ledgerTx.inputsOfType<Agreement>().single().counterparty
                    if (counterparty != counterpartySession.counterparty) {
                        throw FlowException("Only the counterparty can Terminate the Agreement")
                    }
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(counterpartySession, txId))
        }
    }
}





// *********
// * Create Agreement Flow *
// *********



object CreateAgreementFlow {
    @StartableByRPC
    @InitiatingFlow
    @Suspendable
    class Initiator(val agreementNumber: String,
                    val agreementName: String,
                    val agreementHash: String,
                    val agreementStatus: AgreementStatus,
                    val agreementType: AgreementType,
                    val totalAgreementValue: Int,
                    val agreementStartDate: String,
                    val agreementEndDate: String,
                    val otherParty: Party) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Agreement.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty signature.") {
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

            // Generate an unsigned transaction.
            val me = ourIdentityAndCert.party
            val active = false
            val time = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            val formatted = time.format(formatter)
            val createdAt = formatted
            val lastUpdated = formatted
            // val contactReference = serviceHub.vaultService.queryBy<Contract>(contact_id).state.single()
            // val reference = contactReference.referenced()
            // val agreementState = Agreement(agreementNumber, agreementName, agreementStatus, agreementType, totalAgreementValue, serviceHub.myInfo.legalIdentities.first(), otherParty, agreementStartDate, agreementEndDate, agreementLineItem, attachmentId, active, createdAt, lastUpdated )
            val agreementState = Agreement(agreementNumber, agreementName, agreementHash, agreementStatus, agreementType, totalAgreementValue, me,  otherParty, agreementStartDate, agreementEndDate, active, createdAt, lastUpdated)
            val txCommand = Command(AgreementContract.Commands.CreateAgreement(), agreementState.participants.map { it.owningKey })
            progressTracker.currentStep = VERIFYING_TRANSACTION
            val txBuilder = TransactionBuilder(notary)
                    //        .addReferenceState(reference)
                    .addOutputState(agreementState, AGREEMENT_CONTRACT_ID)
                    .addCommand(txCommand)
            // .addOutputState(AttachmentContract.Attachment(attachmentId), ATTACHMENT_ID)
            //  .addCommand(AttachmentContract.Command, ourIdentity.owningKey)
            //  .addAttachment(attachmentId)

            txBuilder.verify(serviceHub)
            // Sign the transaction.
            progressTracker.currentStep = SIGNING_TRANSACTION
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)


            val otherPartyFlow = initiateFlow(otherParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))

            // Finalising the transaction.
            return subFlow(FinalityFlow(fullySignedTx, listOf(otherPartyFlow), FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Agreement transaction." using (output is Agreement)
                    val agreement = output as Agreement
                    "I won't accept Agreements with a value under 100." using (agreement.totalAgreementValue >= 100)
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = txId))
        }
    }

}


// *********
// * Create Account Flow *
// *********


object CreateAccountFlow {
    @InitiatingFlow
    @StartableByRPC
    class Controller(val accountId: String,
                     val accountName: String,
                     val accountType: TypeOfBusiness,
                     val industry: String,
                     val phone: String,
                     val yearStarted: Int,
                     val annualRevenue: Double,
                     val businessAddress: String,
                     val businessCity: String,
                     val businessState: String,
                     val businessZipCode: String,
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
            val accountState = Account(accountId, accountName, accountType, industry, phone, yearStarted, annualRevenue, businessAddress, businessCity, businessState, businessZipCode, serviceHub.myInfo.legalIdentities.first(), processor)
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
// * Create Approval Flow *
// *********



object CreateApprovalFlow {
    @StartableByRPC
    @InitiatingFlow
    @Suspendable
    class Initiator(val approvalId: String,
                    val approvalName: String,
                    val industry: String,
                    val approvalStatus: ApprovalStatus,
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

            val approvalState = Approval(approvalId, approvalName, industry, approvalStatus, serviceHub.myInfo.legalIdentities.first(), otherParty)
            val txCommand = Command(ApprovalContract.Commands.CreateApproval(), approvalState.participants.map { it.owningKey })
            progressTracker.currentStep = VERIFYING_TRANSACTION
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(approvalState, APPROVAL_CONTRACT_ID)
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
                    "This must be an Application transaction." using (output is Approval)
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}





// *********
// * Approve Flow *
// *********

@InitiatingFlow
@StartableByRPC
class ApproveFlow(val approvalId: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val approvalStateAndRef = serviceHub.vaultService.queryBy<Approval>().states.find {
            it.state.data.approvalId == approvalId
        } ?: throw IllegalArgumentException("No agreement with ID $approvalId found.")


        val approval = approvalStateAndRef.state.data
        val approvalStatus = ApprovalStatus.APPROVED


        // Creating the output.
        val approvedApproval = Approval(
                approval.approvalId,
                approval.approvalName,
                approval.industry,
                approvalStatus,
                approval.submitter,
                approval.approver,
                approval.linearId)

        // Building the transaction.
        val notary = approvalStateAndRef.state.notary
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(approvalStateAndRef)
        txBuilder.addOutputState(approvedApproval, ApprovalContract.APPROVAL_CONTRACT_ID)
        txBuilder.addCommand(ApprovalContract.Commands.Approve(), ourIdentity.owningKey)
        txBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(txBuilder)
    }

    @InitiatedBy(ApproveFlow::class)
    class Approver(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Agreement transaction." using (output is Approval)
                    val approval = output as Approval
                    val approvalStatus = ApprovalStatus.APPROVED
                }
            }

            val signedTransaction = subFlow(signTransactionFlow)
            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = signedTransaction.id))
        }
    }
}




// *********
// * Reject Approval Flow *
// *********


@InitiatingFlow
@StartableByRPC
class RejectFlow(val approvalId: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val approvalStateAndRef = serviceHub.vaultService.queryBy<Approval>().states.find {
            it.state.data.approvalId == approvalId
        } ?: throw IllegalArgumentException("No agreement with ID $approvalId found.")


        val approval = approvalStateAndRef.state.data
        val approvalStatus = ApprovalStatus.REJECTED

        // Creating the output.
        val rejectedApproval = Approval(
                approval.approvalId,
                approval.approvalName,
                approval.industry,
                approvalStatus,
                approval.submitter,
                approval.approver,
                approval.linearId)

        // Building the transaction.
        val notary = approvalStateAndRef.state.notary
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(approvalStateAndRef)
        txBuilder.addOutputState(rejectedApproval, ApprovalContract.APPROVAL_CONTRACT_ID)
        txBuilder.addCommand(ApprovalContract.Commands.Reject(), ourIdentity.owningKey)
        txBuilder.verify(serviceHub)

        val stx = serviceHub.signInitialTransaction(txBuilder)
        return serviceHub.signInitialTransaction(txBuilder)
    }

    @InitiatedBy(RejectFlow::class)
    class Rejecter(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Agreement transaction." using (output is Approval)
                    val approval = output as Approval
                    val approvalStatus = ApprovalStatus.REJECTED
                }
            }

            val signedTransaction = subFlow(signTransactionFlow)
            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = signedTransaction.id))
        }
    }
}


// *********
// * Create Invoice Flow *
// *********

object CreateInvoiceFlow {
    @StartableByRPC
    @InitiatingFlow
    @Suspendable
    class Invoicer(val invoiceNumber: String,
                   val invoiceName: String,
                   val billingReason: String,
                   val amountDue: Amount<Currency>,
                   val amountPaid: Amount<Currency>,
                   val amountRemaining: Amount<Currency>,
                   val subtotal: Amount<Currency>,
                   val total: Amount<Currency>,
                   val dueDate: String,
                   val periodStartDate: String,
                   val periodEndDate: String,
                   val paid: Boolean?,
                   val active: Boolean?,
                   val createdAt: String?,
                   val lastUpdated: String?,
                   val otherParty: Party) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Agreement.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty signature.") {
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

            // Generate an unsigned transaction.
            val me = ourIdentityAndCert.party
            val active = false
            val paid = false
            val time = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            val formatted = time.format(formatter)
            val createdAt = formatted
            val lastUpdated = formatted
            val agreementReference = serviceHub.vaultService.queryBy<Agreement>().states.single()
            val reference = agreementReference.referenced()
            val invoiceState = Invoice(invoiceNumber, invoiceName, billingReason, amountDue, amountPaid, amountRemaining, subtotal, total, me, otherParty, dueDate, periodStartDate, periodEndDate, paid, active, createdAt, lastUpdated)
            val txCommand = Command(InvoiceContract.Commands.CreateInvoice(), invoiceState.participants.map { it.owningKey })
            progressTracker.currentStep = VERIFYING_TRANSACTION
            val txBuilder = TransactionBuilder(notary)
                    .addReferenceState(reference)
                    .addOutputState(invoiceState, INVOICE_CONTRACT_ID)
                    .addCommand(txCommand)

            txBuilder.verify(serviceHub)
            // Sign the transaction.
            progressTracker.currentStep = SIGNING_TRANSACTION
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)


            val otherPartyFlow = initiateFlow(otherParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))

            // Finalising the transaction.
            return subFlow(FinalityFlow(fullySignedTx, listOf(otherPartyFlow), FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Invoicer::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Invoice transaction." using (output is Invoice)
                    val invoice = output as Invoice
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = txId))
        }
    }

}


/*

// *********
// * Pay Invoice Flow *
// *********



object PayInvoice {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val linearId: UniqueIdentifier,
                    private val amount: Amount<Currency>) : FlowLogic<SignedTransaction>() {

        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object PREPARATION : ProgressTracker.Step("Obtaining Obligation from vault.")
            object BUILDING : ProgressTracker.Step("Building and verifying transaction.")
            object SIGNING : ProgressTracker.Step("signing transaction.")
            object COLLECTING : ProgressTracker.Step("Collecting counterparty signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING : ProgressTracker.Step("Finalising transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(PREPARATION, BUILDING, SIGNING, COLLECTING, FINALISING)


            fun getInvoiceByLinearId(linearId: UniqueIdentifier): StateAndRef<Invoice> {
                val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                        null,
                        ImmutableList.of(linearId),
                        Vault.StateStatus.UNCONSUMED, null)

                return getService(nodeName).proxy().vaultService.queryBy<Invoice>(queryCriteria).states.singleOrNull()
                        ?: throw FlowException("Invoice with id $linearId not found.")
            }

            fun resolveIdentity(abstractParty: AbstractParty): Party {
                return getService(nodeName).proxy().identityService.requireWellKnownPartyFromAnonymous(abstractParty)
            }
        }

        @Suspendable
        override fun call(): SignedTransaction {

            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            // Stage 1. Retrieve obligation specified by linearId from the vault.
            progressTracker.currentStep = Initiator.Companion.PREPARATION
            val invoiceToPay = getInvoiceByLinearId(linearId)
            val inputInvoice = invoiceToPay.state.data

            val partyIdentity = resolveIdentity(inputInvoice.party)
            val counterpartyIdentity = resolveIdentity(inputInvoice.counterparty)

            // Stage 3. This flow can only be initiated by the current recipient.
            check(partyIdentity == ourIdentity) {
                throw FlowException("Pay Invoice flow must be initiated by the counterparty.")
            }

            // Stage 4. Check we have enough cash to settle the requested amount.
            val cashBalance = serviceHub.getCashBalance(amount.token)
            val amountLeftToPay = inputInvoice.amountRemaining
            check(cashBalance.quantity > 0L) {
                throw FlowException("Counterpary has no ${amount.token} to pay the invoice.")
            }
            check(cashBalance >= amount) {
                throw FlowException("Borrower has only $cashBalance but needs $amount to pay the invoice.")
            }
            check(amountLeftToPay >= amount) {
                throw FlowException("There's only $amountLeftToPay left to pay but you pledged $amount.")
            }

            // Stage 5. Create a pay command.
            val payCommand = Command(
                    InvoiceContract.Commands.PayInvoice(),
                    inputInvoice.participants.map { it.owningKey })

            // Stage 6. Create a transaction builder. Add the settle command and input obligation.
            progressTracker.currentStep = BUILDING
            val builder = TransactionBuilder(notary)
                    .addInputState(invoiceToPay)
                    .addCommand(payCommand)

            // Stage 7. Get some cash from the vault and add a spend to our transaction builder.
            // We pay cash to the lenders obligation key.
            val lenderPaymentKey = inputInvoice.party
            val (_, cashSigningKeys) = Cash.generateSpend(serviceHub, builder, amount, lenderPaymentKey)

            // Stage 8. Only add an output obligation state if the obligation has not been fully settled.
            val amountRemaining = amountLeftToPay - amount
            if (amountRemaining > Amount.zero(amount.token)) {
                val outputObligation = inputInvoice.pay(amount)
                builder.addOutputState(outputObligation, INVOICE_CONTRACT_ID)
            }

            // Stage 9. Verify and sign the transaction.
            progressTracker.currentStep = SIGNING
            builder.verify(serviceHub)
            val ptx = serviceHub.signInitialTransaction(builder, cashSigningKeys + inputInvoice.counterparty.owningKey)

            // Stage 10. Get counterparty signature.
            progressTracker.currentStep = COLLECTING
            val session = initiateFlow(partyIdentity)
            subFlow(IdentitySyncFlow.Send(session, ptx.tx))
            val stx = subFlow(CollectSignaturesFlow(
                    ptx,
                    setOf(session),
                    cashSigningKeys + inputInvoice.counterparty.owningKey,
                    COLLECTING.childProgressTracker())
            )

            // Stage 11. Finalize the transaction.
            progressTracker.currentStep = FINALISING

            // Finalising the transaction.
            return subFlow(FinalityFlow(stx, listOf(otherPartySession), CreateInvoiceFlow.Invoicer.Companion.FINALISING_TRANSACTION.childProgressTracker()))
        }
    }


    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Invoice transaction." using (output is Invoice)
                    val invoice = output as Invoice
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = txId))
        }
    }

}


// *********
// * Factor Invoice Flow *
// *********



object FactorInvoice {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val linearId: UniqueIdentifier,
                    private val amount: Amount<Currency>,
                    private val borrower: Party,
                    private val lender: Party) : FlowLogic<SignedTransaction>() {

        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object PREPARATION : ProgressTracker.Step("Obtaining Obligation from vault.")
            object BUILDING : ProgressTracker.Step("Building and verifying transaction.")
            object SIGNING : ProgressTracker.Step("signing transaction.")
            object COLLECTING : ProgressTracker.Step("Collecting counterparty signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING : ProgressTracker.Step("Finalising transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(PREPARATION, BUILDING, SIGNING, COLLECTING, FINALISING)


            fun getInvoiceByLinearId(linearId: UniqueIdentifier): StateAndRef<Invoice> {
                val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                        null,
                        ImmutableList.of(linearId),
                        Vault.StateStatus.UNCONSUMED, null)

                return serviceHub.vaultService.queryBy<Invoice>(queryCriteria).states.singleOrNull()
                        ?: throw FlowException("Invoice with id $linearId not found.")
            }

            fun resolveIdentity(abstractParty: AbstractParty): Party {
                return serviceHub.identityService.requireWellKnownPartyFromAnonymous(abstractParty)
            }
        }

        @Suspendable
        override fun call(): SignedTransaction {

            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            // Stage 1. Retrieve obligation specified by linearId from the vault.
            progressTracker.currentStep = Initiator.Companion.PREPARATION
            val invoiceToFactor = getInvoiceByLinearId(linearId)
            val inputInvoice = invoiceToFactor.state.data

            val borrowerIdentity = resolveIdentity(borrower)
            val lenderIdentity = resolveIdentity(lender)
            val invoiceReference = serviceHub.vaultService.queryBy<Invoice>().states.single()
            val reference = invoiceReference.referenced()

            // Stage 3. This flow can only be initiated by the current recipient.
            check(borrowerIdentity == ourIdentity) {
                throw FlowException("Factor Invoice flow must be initiated by the party.")
            }

            // Stage 4. Check we have enought to issue the loan based on the requested loan amount.
            val cashBalance = serviceHub.getCashBalance(amount.token)
            val amountLeftToPay = inputInvoice.amountRemaining
            check(cashBalance.quantity > 0) {
                throw FlowException("Lender has no ${amount.token} to factor the invoice.")
            }
            check(cashBalance >= amount) {
                throw FlowException("Borrower has only $cashBalance but needs $amount to pay the invoice.")
            }
            check(amountLeftToPay >= amount) {
                throw FlowException("There's only $amountLeftToPay left to pay but you pledged $amount.")
            }

            // Stage 5. Create a pay command.
            val factorCommand = Command(
                    InvoiceContract.Commands.FactorInvoice(),
                    inputInvoice.participants.map { it.owningKey })

            // Stage 6. Create a transaction builder. Add the settle command and input obligation.
            progressTracker.currentStep = BUILDING
            val builder = TransactionBuilder(notary)
                    .addReferenceState(reference)
                    .addInputState(invoiceToFactor)
                    .addCommand(factorCommand)

            // Stage 7. Get some cash from the vault and add a spend to our transaction builder.
            // We pay cash to the lenders obligation key.
            val borrowerPaymentKey = borrower
            val (_, cashSigningKeys) = Cash.generateSpend(serviceHub, builder, amount, borrowerPaymentKey)

            // Stage 8. Add a Loan Output State with a Reference State to the Invoice
            val amountRemaining = amountLeftToPay - amount
            if (amountRemaining > Amount.zero(amount.token)) {
                val outputLoan = inputInvoice.pay(amount)
                builder.addOutputState(outputLoan, INVOICE_CONTRACT_ID)
            }

            // Stage 9. Verify and sign the transaction.
            progressTracker.currentStep = SIGNING
            builder.verify(serviceHub)
            val ptx = serviceHub.signInitialTransaction(builder, cashSigningKeys + inputInvoice.party.owningKey)

            // Stage 10. Get the Lender's signature.
            progressTracker.currentStep = COLLECTING
            val session = initiateFlow(lenderIdentity)
            subFlow(IdentitySyncFlow.Send(session, ptx.tx))
            val stx = subFlow(CollectSignaturesFlow(
                    ptx,
                    setOf(session),
                    cashSigningKeys + lender.owningKey,
                    COLLECTING.childProgressTracker())
            )

            // Stage 11. Finalize the transaction.
            progressTracker.currentStep = FINALISING

            // Finalising the transaction.
            return subFlow(FinalityFlow(stx, listOf(otherPartySession), CreateInvoiceFlow.Invoicer.Companion.FINALISING_TRANSACTION.childProgressTracker()))
        }
    }


    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Invoice transaction." using (output is Invoice)
                    val invoice = output as Invoice
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherSideSession = otherPartySession, expectedTxId = txId))
        }
    }

      */

