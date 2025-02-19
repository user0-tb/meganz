package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.UserStatus
import java.io.File

/**
 * The repository interface regarding Chat participants.
 */
interface ChatParticipantsRepository {

    /**
     * Get chat participants
     */
    suspend fun getAllChatParticipants(chatId: Long): List<ChatParticipant>

    /**
     * Get chat participants handles
     */
    suspend fun getChatParticipantsHandles(chatId: Long): List<Long>

    /**
     * Get status
     *
     * @param participant [ChatParticipant]
     * @return [UserStatus]
     */
    suspend fun getStatus(participant: ChatParticipant): UserStatus

    /**
     * Get current user status
     *
     * @return  [UserStatus]
     */
    suspend fun getCurrentStatus(): UserStatus

    /**
     * Get alias
     *
     * @param participant [ChatParticipant].
     * @return Participant alias.
     */
    suspend fun getAlias(participant: ChatParticipant): String?

    /**
     * Check credentials
     *
     * @param participant [ChatParticipant].
     * @return  True, if credentials are verified. False, if not.
     */
    suspend fun areCredentialsVerified(participant: ChatParticipant): Boolean

    /**
     * Get avatar
     *
     * @param participant [ChatParticipant].
     * @param skipCache
     * @return Participant avatar.
     */
    suspend fun getAvatarUri(participant: ChatParticipant, skipCache: Boolean = false): File?

    /**
     * Get avatar color
     *
     * @param participant [ChatParticipant].
     * @return Participant avatar color.
     */
    suspend fun getAvatarColor(participant: ChatParticipant): Int

    /**
     * Get permission
     *
     * @param chatId        Chat Id.
     * @param participant   [ChatParticipant].
     * @return Participant permissions.
     */
    suspend fun getPermissions(chatId: Long, participant: ChatParticipant): ChatRoomPermission

    /**
     * Request user attributes
     *
     * This function is useful to get the email address, first name, last name and full name
     * from chat link participants that they are not loaded
     *
     * After request is finished, you can call to MegaChatApi::getUserFirstnameFromCache,
     * MegaChatApi::getUserLastnameFromCache, MegaChatApi::getUserFullnameFromCache,
     * MegaChatApi::getUserEmailFromCache (email will not available in anonymous mode)
     *
     * @param chatId Handle of the chat whose member attributes requested
     * @param usersHandles List of user handles whose attributes have to be requested
     */
    suspend fun loadUserAttributes(chatId: Long, usersHandles: List<Long>)

    /**
     * Returns the current fullname of the user
     *
     * Returns NULL if data is not cached yet.
     *
     * @param userHandle Handle of the user whose fullname is requested.
     * @return The full name from user
     */
    suspend fun getUserFullNameFromCache(userHandle: Long): String?

    /**
     * Returns the current email address of the user
     *
     * Returns NULL if data is not cached yet or it's not possible to get
     *
     * @param userHandle Handle of the user whose email is requested.
     * @return The email from user
     */
    suspend fun getUserEmailFromCache(userHandle: Long): String?

    /**
     * Set online status
     *
     * @param status
     */
    suspend fun setOnlineStatus(status: UserStatus)
}