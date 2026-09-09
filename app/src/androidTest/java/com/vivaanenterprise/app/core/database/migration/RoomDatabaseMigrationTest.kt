package com.vivaanenterprise.app.core.database.migration

import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseMigrationTest {

    @Test
    fun migrate1To2_preservesExistingData_addsNullableDeliveryFactoryAddress() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("migration-test-db")

        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-test-db")
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `business_profiles` (
                            `id` TEXT NOT NULL, `businessName` TEXT NOT NULL, `addressLine1` TEXT NOT NULL, `addressLine2` TEXT NOT NULL,
                            `cityStatePincode` TEXT NOT NULL, `gstin` TEXT NOT NULL, `mobile` TEXT NOT NULL, `pan` TEXT NOT NULL,
                            `bankAccountName` TEXT NOT NULL, `bankName` TEXT NOT NULL, `bankAccountNumber` TEXT NOT NULL,
                            `bankIfsc` TEXT NOT NULL, `bankBranch` TEXT NOT NULL, `declaration` TEXT NOT NULL,
                            `authorisedSignatory` TEXT NOT NULL, `state` TEXT NOT NULL, `stateCode` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `clients` (
                            `id` TEXT NOT NULL, `companyName` TEXT NOT NULL, `address` TEXT NOT NULL, `gstin` TEXT,
                            `state` TEXT, `stateCode` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                            `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `products` (
                            `id` TEXT NOT NULL, `name` TEXT NOT NULL, `hsnSac` TEXT NOT NULL, `defaultGstRateBasisPoints` INTEGER NOT NULL,
                            `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                            `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `business_documents` (
                            `id` TEXT NOT NULL, `documentType` TEXT NOT NULL, `documentNumber` TEXT NOT NULL, `documentDate` INTEGER NOT NULL,
                            `status` TEXT NOT NULL, `clientId` TEXT NOT NULL, `placeOfSupply` TEXT, `paymentTerms` TEXT,
                            `deliveryNote` TEXT, `supplierReference` TEXT, `otherReferences` TEXT, `buyerOrderNumber` TEXT,
                            `buyerOrderDate` INTEGER, `dispatchDocumentNumber` TEXT, `deliveryNoteDate` INTEGER,
                            `dispatchThrough` TEXT, `destination` TEXT, `termsOfDelivery` TEXT, `taxableAmountPaise` INTEGER NOT NULL,
                            `cgstAmountPaise` INTEGER NOT NULL, `sgstAmountPaise` INTEGER NOT NULL, `igstAmountPaise` INTEGER NOT NULL,
                            `totalTaxAmountPaise` INTEGER NOT NULL, `grandTotalPaise` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        var db = factory.create(config).writableDatabase

        // Populate v1 rows
        db.execSQL(
            """
            INSERT INTO business_profiles (id, businessName, addressLine1, addressLine2, cityStatePincode, gstin, mobile, pan, bankAccountName, bankName, bankAccountNumber, bankIfsc, bankBranch, declaration, authorisedSignatory, state, stateCode, createdAt, updatedAt, syncStatus)
            VALUES ('profile-1', 'VIVAAN ENTERPRISE', 'Street 4', 'Jorawar Nagar', 'Surendranagar, Gujarat', '24CHWPG0910J1ZB', '9737178061', 'CHWPG0910J', 'SHETH JANVI', 'HDFC BANK', '50100419622062', 'HDFC0000299', 'Paldi', 'Declaration', 'For VIVAAN', 'Gujarat', '24', 1000, 1000, 'SYNCED')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO clients (id, companyName, address, gstin, state, stateCode, createdAt, updatedAt, isDeleted, syncStatus)
            VALUES ('client-1', 'Acme Corp', 'Ahmedabad, Gujarat', '24AAAAC1234A1Z1', 'Gujarat', '24', 1000, 1000, 0, 'SYNCED')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO products (id, name, hsnSac, defaultGstRateBasisPoints, isActive, createdAt, updatedAt, isDeleted, syncStatus)
            VALUES ('prod-1', 'Scotch Tape', '3919', 1800, 1, 1000, 1000, 0, 'SYNCED')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO business_documents (id, documentType, documentNumber, documentDate, status, clientId, deliveryNote, destination, taxableAmountPaise, cgstAmountPaise, sgstAmountPaise, igstAmountPaise, totalTaxAmountPaise, grandTotalPaise, createdAt, updatedAt, isDeleted, syncStatus)
            VALUES ('doc-1', 'TAX_INVOICE', 'VE/01/2026-27', 1700000000000, 'FINALIZED', 'client-1', 'Delivery Note 101', 'Pune', 100000, 9000, 9000, 0, 18000, 118000, 1000, 1000, 0, 'SYNCED')
            """.trimIndent()
        )
        db.close()

        // Reopen and apply MIGRATION_1_2
        val openHelper = factory.create(config)
        db = openHelper.writableDatabase
        MIGRATION_1_2.migrate(db)

        // Verify v2 database row state
        val profileCursor = db.query("SELECT id FROM business_profiles WHERE id = 'profile-1'")
        assertTrue(profileCursor.moveToFirst())
        profileCursor.close()

        val clientCursor = db.query("SELECT id FROM clients WHERE id = 'client-1'")
        assertTrue(clientCursor.moveToFirst())
        clientCursor.close()

        val productCursor = db.query("SELECT id FROM products WHERE id = 'prod-1'")
        assertTrue(productCursor.moveToFirst())
        productCursor.close()

        val cursor = db.query("SELECT id, documentNumber, deliveryNote, destination, deliveryFactoryAddress, grandTotalPaise FROM business_documents WHERE id = 'doc-1'")
        assertTrue(cursor.moveToFirst())

        assertEquals("doc-1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals("VE/01/2026-27", cursor.getString(cursor.getColumnIndexOrThrow("documentNumber")))
        assertEquals("Delivery Note 101", cursor.getString(cursor.getColumnIndexOrThrow("deliveryNote")))
        assertEquals("Pune", cursor.getString(cursor.getColumnIndexOrThrow("destination")))
        assertNull(cursor.getString(cursor.getColumnIndexOrThrow("deliveryFactoryAddress")))
        assertEquals(118000L, cursor.getLong(cursor.getColumnIndexOrThrow("grandTotalPaise")))

        cursor.close()
        db.close()
    }

    @Test
    fun migrate2To3_preservesExistingData_addsNullableTaxTreatment() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("migration-test-db-2-3")

        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-test-db-2-3")
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `business_documents` (
                            `id` TEXT NOT NULL, `documentType` TEXT NOT NULL, `documentNumber` TEXT NOT NULL, `documentDate` INTEGER NOT NULL,
                            `status` TEXT NOT NULL, `clientId` TEXT NOT NULL, `placeOfSupply` TEXT, `paymentTerms` TEXT,
                            `deliveryNote` TEXT, `deliveryFactoryAddress` TEXT, `supplierReference` TEXT, `otherReferences` TEXT, `buyerOrderNumber` TEXT,
                            `buyerOrderDate` INTEGER, `dispatchDocumentNumber` TEXT, `deliveryNoteDate` INTEGER,
                            `dispatchThrough` TEXT, `destination` TEXT, `termsOfDelivery` TEXT, `taxableAmountPaise` INTEGER NOT NULL,
                            `cgstAmountPaise` INTEGER NOT NULL, `sgstAmountPaise` INTEGER NOT NULL, `igstAmountPaise` INTEGER NOT NULL,
                            `totalTaxAmountPaise` INTEGER NOT NULL, `grandTotalPaise` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        var db = factory.create(config).writableDatabase

        db.execSQL(
            """
            INSERT INTO business_documents (id, documentType, documentNumber, documentDate, status, clientId, deliveryNote, deliveryFactoryAddress, destination, taxableAmountPaise, cgstAmountPaise, sgstAmountPaise, igstAmountPaise, totalTaxAmountPaise, grandTotalPaise, createdAt, updatedAt, isDeleted, syncStatus)
            VALUES ('doc-v2', 'TAX_INVOICE', 'VE/02/2026-27', 1700000000000, 'FINALIZED', 'client-1', 'Note 1', 'Factory 1', 'Pune', 100000, 9000, 9000, 0, 18000, 118000, 1000, 1000, 0, 'SYNCED')
            """.trimIndent()
        )
        db.close()

        val openHelper = factory.create(config)
        db = openHelper.writableDatabase
        MIGRATION_2_3.migrate(db)

        val cursor = db.query("SELECT id, documentNumber, deliveryFactoryAddress, taxTreatment, grandTotalPaise FROM business_documents WHERE id = 'doc-v2'")
        assertTrue(cursor.moveToFirst())

        assertEquals("doc-v2", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals("VE/02/2026-27", cursor.getString(cursor.getColumnIndexOrThrow("documentNumber")))
        assertEquals("Factory 1", cursor.getString(cursor.getColumnIndexOrThrow("deliveryFactoryAddress")))
        assertNull(cursor.getString(cursor.getColumnIndexOrThrow("taxTreatment")))
        assertEquals(118000L, cursor.getLong(cursor.getColumnIndexOrThrow("grandTotalPaise")))

        cursor.close()
        db.close()
    }

    @Test
    fun migrate1To2To3_fullChainMigration_preservesAllData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("migration-test-db-1-2-3")

        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-test-db-1-2-3")
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `business_profiles` (
                            `id` TEXT NOT NULL, `businessName` TEXT NOT NULL, `addressLine1` TEXT NOT NULL, `addressLine2` TEXT NOT NULL,
                            `cityStatePincode` TEXT NOT NULL, `gstin` TEXT NOT NULL, `mobile` TEXT NOT NULL, `pan` TEXT NOT NULL,
                            `bankAccountName` TEXT NOT NULL, `bankName` TEXT NOT NULL, `bankAccountNumber` TEXT NOT NULL,
                            `bankIfsc` TEXT NOT NULL, `bankBranch` TEXT NOT NULL, `declaration` TEXT NOT NULL,
                            `authorisedSignatory` TEXT NOT NULL, `state` TEXT NOT NULL, `stateCode` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `clients` (
                            `id` TEXT NOT NULL, `companyName` TEXT NOT NULL, `address` TEXT NOT NULL, `gstin` TEXT,
                            `state` TEXT, `stateCode` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                            `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `products` (
                            `id` TEXT NOT NULL, `name` TEXT NOT NULL, `hsnSac` TEXT NOT NULL, `defaultGstRateBasisPoints` INTEGER NOT NULL,
                            `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                            `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `business_documents` (
                            `id` TEXT NOT NULL, `documentType` TEXT NOT NULL, `documentNumber` TEXT NOT NULL, `documentDate` INTEGER NOT NULL,
                            `status` TEXT NOT NULL, `clientId` TEXT NOT NULL, `placeOfSupply` TEXT, `paymentTerms` TEXT,
                            `deliveryNote` TEXT, `supplierReference` TEXT, `otherReferences` TEXT, `buyerOrderNumber` TEXT,
                            `buyerOrderDate` INTEGER, `dispatchDocumentNumber` TEXT, `deliveryNoteDate` INTEGER,
                            `dispatchThrough` TEXT, `destination` TEXT, `termsOfDelivery` TEXT, `taxableAmountPaise` INTEGER NOT NULL,
                            `cgstAmountPaise` INTEGER NOT NULL, `sgstAmountPaise` INTEGER NOT NULL, `igstAmountPaise` INTEGER NOT NULL,
                            `totalTaxAmountPaise` INTEGER NOT NULL, `grandTotalPaise` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        var db = factory.create(config).writableDatabase

        // Populate v1 data
        db.execSQL(
            """
            INSERT INTO business_profiles (id, businessName, addressLine1, addressLine2, cityStatePincode, gstin, mobile, pan, bankAccountName, bankName, bankAccountNumber, bankIfsc, bankBranch, declaration, authorisedSignatory, state, stateCode, createdAt, updatedAt, syncStatus)
            VALUES ('profile-1', 'VIVAAN ENTERPRISE', 'Street 4', 'Jorawar Nagar', 'Surendranagar', '24CHWPG0910J1ZB', '9737178061', 'CHWPG0910J', 'SHETH JANVI', 'HDFC BANK', '50100419622062', 'HDFC0000299', 'Paldi', 'Declaration', 'For VIVAAN', 'Gujarat', '24', 1000, 1000, 'SYNCED')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO clients (id, companyName, address, gstin, state, stateCode, createdAt, updatedAt, isDeleted, syncStatus)
            VALUES ('client-1', 'Acme Corp', 'Ahmedabad', '24AAAAC1234A1Z1', 'Gujarat', '24', 1000, 1000, 0, 'SYNCED')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO products (id, name, hsnSac, defaultGstRateBasisPoints, isActive, createdAt, updatedAt, isDeleted, syncStatus)
            VALUES ('prod-1', 'Scotch Tape', '3919', 1800, 1, 1000, 1000, 0, 'SYNCED')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO business_documents (id, documentType, documentNumber, documentDate, status, clientId, deliveryNote, destination, taxableAmountPaise, cgstAmountPaise, sgstAmountPaise, igstAmountPaise, totalTaxAmountPaise, grandTotalPaise, createdAt, updatedAt, isDeleted, syncStatus)
            VALUES ('doc-v1', 'TAX_INVOICE', 'VE/01/2026-27', 1700000000000, 'FINALIZED', 'client-1', 'Note 1', 'Pune', 100000, 9000, 9000, 0, 18000, 118000, 1000, 1000, 0, 'SYNCED')
            """.trimIndent()
        )
        db.close()

        val openHelper = factory.create(config)
        db = openHelper.writableDatabase
        MIGRATION_1_2.migrate(db)
        MIGRATION_2_3.migrate(db)

        // Verify all entities survive through v1 -> v2 -> v3 chain
        val profileCursor = db.query("SELECT id, businessName FROM business_profiles WHERE id = 'profile-1'")
        assertTrue(profileCursor.moveToFirst())
        assertEquals("VIVAAN ENTERPRISE", profileCursor.getString(profileCursor.getColumnIndexOrThrow("businessName")))
        profileCursor.close()

        val clientCursor = db.query("SELECT id, companyName FROM clients WHERE id = 'client-1'")
        assertTrue(clientCursor.moveToFirst())
        assertEquals("Acme Corp", clientCursor.getString(clientCursor.getColumnIndexOrThrow("companyName")))
        clientCursor.close()

        val productCursor = db.query("SELECT id, name FROM products WHERE id = 'prod-1'")
        assertTrue(productCursor.moveToFirst())
        assertEquals("Scotch Tape", productCursor.getString(productCursor.getColumnIndexOrThrow("name")))
        productCursor.close()

        val docCursor = db.query("SELECT id, documentNumber, deliveryNote, destination, deliveryFactoryAddress, taxTreatment, grandTotalPaise FROM business_documents WHERE id = 'doc-v1'")
        assertTrue(docCursor.moveToFirst())
        assertEquals("doc-v1", docCursor.getString(docCursor.getColumnIndexOrThrow("id")))
        assertEquals("VE/01/2026-27", docCursor.getString(docCursor.getColumnIndexOrThrow("documentNumber")))
        assertEquals("Note 1", docCursor.getString(docCursor.getColumnIndexOrThrow("deliveryNote")))
        assertEquals("Pune", docCursor.getString(docCursor.getColumnIndexOrThrow("destination")))
        assertNull(docCursor.getString(docCursor.getColumnIndexOrThrow("deliveryFactoryAddress")))
        assertNull(docCursor.getString(docCursor.getColumnIndexOrThrow("taxTreatment")))
        assertEquals(118000L, docCursor.getLong(docCursor.getColumnIndexOrThrow("grandTotalPaise")))

        docCursor.close()
        db.close()
    }
}
