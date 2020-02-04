package io.stateset.product

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


import com.sun.org.apache.xpath.internal.operations.Bool
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.toBase58String
import java.lang.Boolean.TRUE
import java.lang.IllegalArgumentException
import java.util.*

// *****************
// * Product State *
// *****************

@BelongsToContract(ProductContract::class)
data class Product(val id: String,
                   val name: String,
                   val description: String,
                   val product_url: String,
                   val image_url: String,
                   val breadcrumbs: String,
                   val inventory: Int,
                   val price: Int,
                   val custom_color: String,
                   val barcode: String,
                   val inventoryStatus: Boolean,
                   val status: Boolean,
                   val custom_size: String,
                   val custom_brand: String,
                   val custom_gender: String,
                   val group_id: String,
                   val active: Boolean,
                   val createdAt: String,
                   val lastUpdated: String,
                   val party: Party,
                   val counterparty: Party,
                   override val linearId: UniqueIdentifier = UniqueIdentifier()) : ContractState, LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = listOf(party, counterparty)

    override fun toString(): String {
        val partyString = (party as? Party)?.name?.organisation ?: party.owningKey.toBase58String()
        val counterpartyString = (counterparty as? Party)?.name?.organisation ?: counterparty.owningKey.toBase58String()
        return "Product Feed ($linearId): $counterpartyString is using product data from $partyString"
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is ProductSchemaV1 -> ProductSchemaV1.PersistentProduct(
                    id = this.id,
                    name = this.name,
                    description = this.description,
                    product_url = this.product_url,
                    image_url = this.image_url,
                    breadcrumbs = this.breadcrumbs,
                    inventory = this.inventory.toString(),
                    price = this.price.toString(),
                    custom_color = this.custom_color,
                    barcode = this.barcode,
                    inventoryStatus = this.inventoryStatus.toString(),
                    status = this.status.toString(),
                    custom_size = this.custom_size,
                    group_id = this.group_id,
                    custom_brand = this.custom_brand,
                    custom_gender = this.custom_gender,
                    active = this.active.toString(),
                    createdAt = this.createdAt,
                    lastUpdated = this.lastUpdated,
                    party = this.party.toString(),
                    counterparty = this.counterparty.toString(),
                    linearId = this.linearId.id.toString(),
                    externalId = this.linearId.id.toString()

            )
            else -> throw IllegalArgumentException("Unrecognized schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ProductSchemaV1)
}


// **********************
// * Product Contract *
// **********************

class ProductContract : Contract {
    // This is used to identify our contract when building a transaction
    companion object {
        val PRODUCT_CONTRACT_ID = ProductContract::class.java.canonicalName
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {

        class CreateProductFeed : TypeOnlyCommandData(), Commands

    }


    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val productInputs = tx.inputsOfType<Product>()
        val productOutputs = tx.outputsOfType<Product>()
        val productCommand = tx.commandsOfType<ProductContract.Commands>().single()

        when(productCommand.value) {
            is Commands.CreateProductFeed-> requireThat {
                "no inputs should be consumed" using (productInputs.isEmpty())
                // TODO we might allow several jobs to be proposed at once later
                "one output should be produced" using (productOutputs.size == 1)

                val productOutput = productOutputs.single()
                "the party should be different to the counterparty" using (productOutput.party != productOutput.counterparty)

                "the party and counterparty are required signers" using
                        (productCommand.signers.containsAll(listOf(productOutput.party.owningKey, productOutput.counterparty.owningKey)))
            }

            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

}