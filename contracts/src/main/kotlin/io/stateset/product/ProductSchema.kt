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

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Index
import javax.persistence.Table


object ProductSchema

/**
 * First version of an [ProductSchema] schema.
 */


object ProductSchemaV1 : MappedSchema(
        schemaFamily = ProductSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentProduct::class.java)) {
    @Entity
    @Table(name = "product_states", indexes = arrayOf(Index(name = "idx_product_party", columnList = "party"),
            Index(name = "idx_product_name", columnList = "name")))
    class PersistentProduct(
            @Column(name = "id")
            var id: String,

            @Column(name = "name")
            var name: String,

            @Column(name = "description")
            var description: String,

            @Column(name = "product_url")
            var product_url: String,

            @Column(name = "image_url")
            var image_url: String,

            @Column(name = "breadcrumbs")
            var breadcrumbs: String,

            @Column(name = "inventory")
            var inventory: String,

            @Column(name = "price")
            var price: String,

            @Column(name = "custom_color")
            var custom_color: String,

            @Column(name = "barcode")
            var barcode: String,

            @Column(name = "inventory_status")
            var inventoryStatus: String,

            @Column(name = "status")
            var status: String,

            @Column(name = "custom_size")
            var custom_size: String,

            @Column(name = "custom_gender")
            var custom_gender: String,

            @Column(name = "group_id")
            var group_id: String,

            @Column(name = "custom_brand")
            var custom_brand: String,

            @Column(name = "active")
            var active: String,

            @Column(name = "created_at")
            var createdAt: String,

            @Column(name = "last_updated")
            var lastUpdated: String,

            @Column(name = "party")
            var party: String,

            @Column(name = "counterparty")
            var counterparty: String,

            @Column(name = "linear_id")
            var linearId: String,

            @Column(name = "external_Id")
            var externalId: String
    ) : PersistentState() {
        constructor() : this("default-constructor-required-for-hibernate", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
    }
}