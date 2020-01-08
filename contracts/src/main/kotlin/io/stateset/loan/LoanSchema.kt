package io.stateset.loan

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

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Index
import javax.persistence.Table


object LoanSchema

/**
 * First version of an [LoanSchema] schema.
 */


object LoanSchemaV1 : MappedSchema(
        schemaFamily = LoanSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentLoan::class.java)) {
    @Entity
    @Table(name = "loan_states", indexes = arrayOf(Index(name = "idx_loan_party", columnList = "party"),
            Index(name = "idx_loan_loanName", columnList = "loanName")))
    class PersistentLoan(
            @Column(name = "loanNumber")
            var loanNumber: String,

            @Column(name = "loanName")
            var loanName: String,

            @Column(name = "loanReason")
            var loanReason: String,

            @Column(name = "amountDue")
            var amountDue: String,

            @Column(name = "amountPaid")
            var amountPaid: String,

            @Column(name = "amountRemaining")
            var amountRemaining: String,

            @Column(name = "subtotal")
            var subtotal: String,

            @Column(name = "total")
            var total: String,

            @Column(name = "party")
            var party: String,

            @Column(name = "counterparty")
            var counterparty: String,

            @Column(name = "dueDate")
            var dueDate: String,

            @Column(name = "periodStartDate")
            var periodStartDate: String,

            @Column(name = "periodEndDate")
            var periodEndDate: String,

            @Column(name = "paid")
            var paid: String,

            @Column(name = "active")
            var active: String,

            @Column(name = "createdAt")
            var createdAt: String,

            @Column(name = "lastUpdated")
            var lastUpdated: String,

            @Column(name = "linear_id")
            var linearId: String,

            @Column(name = "external_Id")
            var externalId: String
    ) : PersistentState() {
        constructor() : this("default-constructor-required-for-hibernate", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
    }
}