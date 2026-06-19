package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class DialerRepository(private val dialerDao: DialerDao) {

    val allContacts: Flow<List<Contact>> = dialerDao.getAllContacts()
    val favoriteContacts: Flow<List<Contact>> = dialerDao.getFavoriteContacts()
    val allCallLogs: Flow<List<CallLog>> = dialerDao.getAllCallLogs()

    fun searchContacts(query: String): Flow<List<Contact>> {
        return dialerDao.searchContacts("%$query%")
    }

    suspend fun insertContact(contact: Contact): Long = withContext(Dispatchers.IO) {
        dialerDao.insertContact(contact)
    }

    suspend fun updateContact(contact: Contact) = withContext(Dispatchers.IO) {
        dialerDao.updateContact(contact)
    }

    suspend fun deleteContact(contact: Contact) = withContext(Dispatchers.IO) {
        dialerDao.deleteContact(contact)
    }

    suspend fun getContactByNumber(number: String): Contact? = withContext(Dispatchers.IO) {
        dialerDao.getContactByNumber(number)
    }

    suspend fun insertCallLog(callLog: CallLog): Long = withContext(Dispatchers.IO) {
        dialerDao.insertCallLog(callLog)
    }

    suspend fun deleteCallLog(callLog: CallLog) = withContext(Dispatchers.IO) {
        dialerDao.deleteCallLog(callLog)
    }

    suspend fun clearAllCallLogs() = withContext(Dispatchers.IO) {
        dialerDao.clearAllCallLogs()
    }

    /**
     * Checks if the contact list is empty, and optionally populates realistic starting contacts
     */
    suspend fun populateInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val currentContacts = dialerDao.getAllContacts().firstOrNull() ?: emptyList()
        if (currentContacts.isEmpty()) {
            val initialContacts = listOf(
                Contact(name = "Alex Rivera", phoneNumber = "+1 (555) 019-2831", email = "alex.rivera@gmail.com", isFavorite = true, avatarColorHex = "#34A853"), // Green
                Contact(name = "Emma Watson", phoneNumber = "+1 (555) 016-8321", email = "emma_w@icloud.com", isFavorite = true, avatarColorHex = "#EA4335"), // Red
                Contact(name = "My Mom", phoneNumber = "+1 (555) 011-2358", email = "mom.home@yahoo.com", isFavorite = true, avatarColorHex = "#F4B400"), // Yellow-orange
                Contact(name = "Dad", phoneNumber = "+1 (555) 011-1347", email = "dad@gmail.com", isFavorite = false, avatarColorHex = "#4285F4"), // Blue
                Contact(name = "Liam Wilson", phoneNumber = "+1 (555) 013-4410", email = "liam.wilson@me.com", isFavorite = false, avatarColorHex = "#A142F4"), // Purple
                Contact(name = "Sophia Martinez", phoneNumber = "+1 (555) 012-7491", email = "sophia_m@outlook.com", isFavorite = false, avatarColorHex = "#F43F5E"), // Pink
                Contact(name = "James Smith", phoneNumber = "+1 (555) 014-9982", email = "james_smith@outlook.com", isFavorite = true, avatarColorHex = "#10B981"), // Emerald
                Contact(name = "Vox Voice Changer Test", phoneNumber = "+1 (800) VOX-TEST", email = "voicetest@voxdialer.com", isFavorite = true, avatarColorHex = "#06B6D4") // Teal
            )

            for (contact in initialContacts) {
                dialerDao.insertContact(contact)
            }

            // Populate some call history as well
            val time = System.currentTimeMillis()
            val initialLogs = listOf(
                CallLog(
                    callerName = "My Mom",
                    phoneNumber = "+1 (555) 011-2358",
                    timestamp = time - (3 * 3600 * 1000), // 3 hours ago
                    durationSeconds = 765, // 12m 45s
                    callType = "OUTGOING",
                    voiceEffectUsed = "Normal"
                ),
                CallLog(
                    callerName = "Sophia Martinez",
                    phoneNumber = "+1 (555) 012-7491",
                    timestamp = time - (24 * 3600 * 1000), // 1 day ago
                    durationSeconds = 134, // 2m 14s
                    callType = "INCOMING",
                    voiceEffectUsed = "Baby Voice"
                ),
                CallLog(
                    callerName = "Unknown Number",
                    phoneNumber = "+1 (555) 010-0012",
                    timestamp = time - (29 * 3600 * 1000), // Yesterday morning
                    durationSeconds = 0,
                    callType = "MISSED",
                    voiceEffectUsed = "Normal"
                ),
                CallLog(
                    callerName = "James Smith",
                    phoneNumber = "+1 (555) 014-9982",
                    timestamp = time - (2 * 24 * 3600 * 1000), // 2 days ago
                    durationSeconds = 34,
                    callType = "OUTGOING",
                    voiceEffectUsed = "Robot"
                ),
                CallLog(
                    callerName = "Emma Watson",
                    phoneNumber = "+1 (555) 016-8321",
                    timestamp = time - (3 * 24 * 3600 * 1000), // 3 days ago
                    durationSeconds = 250, // 4m 10s
                    callType = "INCOMING",
                    voiceEffectUsed = "Deep Voice"
                )
            )

            for (log in initialLogs) {
                dialerDao.insertCallLog(log)
            }
        }
    }
}
