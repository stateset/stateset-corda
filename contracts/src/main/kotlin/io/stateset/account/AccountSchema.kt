package io.stateset.account

import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Index
import javax.persistence.Table

/**
 * The family of schemas for [AccountSchema].
 */

object AccountSchema

/**
 * First version of an [AccountSchema] schema.
 */


object AccountSchemaV1 : MappedSchema(AccountSchema.javaClass, 1, listOf(PersistentAccount::class.java)) {
    @Entity
    @Table(name = "account_states", indexes = arrayOf(Index(name = "idx_account_controller", columnList = "controller"),
            Index(name = "idx_account_accountName", columnList = "accountName")))
    class PersistentAccount(
            @Column(name = "accountId")
            var accountId: String,

            @Column(name = "accountName")
            var accountName: String,

            @Column(name = "accountType")
            var accountType: String,

            @Column(name = "industry")
            var industry: String,

            @Column(name = "phone")
            var phone: String,

            @Column(name = "yearStarted")
            var yearStarted: String,

            @Column(name = "annualRevenue")
            var annualRevenue: String,

            @Column(name = "businessAddress")
            var businessAddress: String,

            @Column(name = "businessCity")
            var businessCity: String,

            @Column(name = "businessState")
            var businessState: String,

            @Column(name = "businessZipCode")
            var businessZipCode: String,

            @Column(name = "controller")
            var controller: String,

            @Column(name = "processor")
            var processor: String

    ) : PersistentState() {
        constructor() : this("default-constructor-required-for-hibernate", "", "", "", "", "", "", "", "", "", "", "", "")
    }

}
