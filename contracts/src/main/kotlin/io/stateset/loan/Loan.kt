/**
 *   Copyright 2020, Stateset.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.stateset.loan

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.toBase58String
import java.lang.Boolean.TRUE
import java.util.*


// *****************
// * Loan State *
// *****************

@BelongsToContract(LoanContract::class)
data class Loan(val loanNumber: String,
                val loanName: String,
                val loanReason: String,
                val amountDue: Amount<Currency>,
                val amountPaid: Amount<Currency> = Amount(0, amountDue.token),
                val amountRemaining: Amount<Currency> = Amount(0, amountPaid.token),
                val subtotal: Amount<Currency> = Amount(0, amountDue.token),
                val total: Amount<Currency> = Amount(0, subtotal.token),
                val party: Party,
                val counterparty: Party,
                val dueDate: String,
                val periodStartDate: String,
                val periodEndDate: String,
                val paid: Boolean?,
                val active: Boolean?,
                val createdAt: String?,
                val lastUpdated: String?,
                override val linearId: UniqueIdentifier = UniqueIdentifier()) : ContractState, LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = listOf(party, counterparty)
    fun pay(amountToPay: Amount<Currency>) = copy(amountDue = amountDue + amountToPay)

    override fun toString(): String {
        val partyString = (party as? Party)?.name?.organisation ?: party.owningKey.toBase58String()
        val counterpartyString = (counterparty as? Party)?.name?.organisation ?: counterparty.owningKey.toBase58String()
        return "Invoice ($linearId): $counterpartyString has an invoice with $partyString for $total and the current paid status is $paid."
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is LoanSchemaV1 -> LoanSchemaV1.PersistentLoan(
                    loanNumber = this.loanNumber,
                    loanName = this.loanName,
                    loanReason = this.loanReason,
                    amountDue = this.amountDue.toString(),
                    amountPaid = this.amountPaid.toString(),
                    amountRemaining = this.amountRemaining.toString(),
                    subtotal= this.subtotal.toString(),
                    total = this.total.toString(),
                    party = this.party.name.toString(),
                    counterparty = this.counterparty.name.toString(),
                    dueDate = this.dueDate,
                    periodStartDate = this.periodStartDate,
                    periodEndDate = this.periodEndDate,
                    paid = this.paid.toString(),
                    active = this.active.toString(),
                    createdAt = this.createdAt.toString(),
                    lastUpdated = this.lastUpdated.toString(),
                    linearId = this.linearId.id.toString(),
                    externalId = this.linearId.id.toString()

            )
            else -> throw IllegalArgumentException("Unrecognized schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(LoanSchemaV1)
}


// **********************
// * Loan Contract *
// **********************

class LoanContract : Contract {
    // This is used to identify our contract when building a transaction
    companion object {
        val LOAN_CONTRACT_ID = LoanContract::class.java.canonicalName
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {

        class CreateLoan : TypeOnlyCommandData(), Commands
        class PayLoan : TypeOnlyCommandData(), Commands


    }


    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val loanInputs = tx.inputsOfType<Loan>()
        val loanOutputs = tx.outputsOfType<Loan>()
        val loanCommand = tx.commandsOfType<Commands>().single()

        when(loanCommand.value) {
            is Commands.CreateLoan-> requireThat {
                "no inputs should be consumed" using (loanInputs.isEmpty())
                // TODO we might allow several jobs to be proposed at once later
                "one output should be produced" using (loanOutputs.size == 1)

                val invoiceOutput = loanOutputs.single()
                "the party should be different to the counterparty" using (invoiceOutput.party != invoiceOutput.counterparty)
                //"the total should be greater than 0" using (invoiceOutput.total)

                "the party and counterparty are required signers" using
                        (loanCommand.signers.containsAll(listOf(invoiceOutput.party.owningKey, invoiceOutput.counterparty.owningKey)))
            }


            is Commands.PayLoan -> requireThat {
                "one input should be produced" using (loanInputs.size == 1)
                "one output should be produced" using (loanOutputs.size == 1)

                val loanOutput = loanOutputs.single()

                "the output paid should be TRUE" using (loanOutput.paid == TRUE)
            }

            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

}