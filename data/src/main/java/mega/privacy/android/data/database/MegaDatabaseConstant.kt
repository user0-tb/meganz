package mega.privacy.android.data.database

/**
 * Database constant
 * Share constant between legacy SQLite and new Room database
 */
object MegaDatabaseConstant {
    /**
     * Database Version
     */
    const val DATABASE_VERSION = 70

    /**
     * Database Name
     */
    const val DATABASE_NAME = "megapreferences"

    /**
     * Table Contacts
     */
    const val TABLE_CONTACTS = "contacts"

    /**
     * Table Completed Transfers
     */
    const val TABLE_COMPLETED_TRANSFERS = "completedtransfers"

    /**
     * Table Active Transfers
     */
    const val TABLE_ACTIVE_TRANSFERS = "active_transfers"
}
