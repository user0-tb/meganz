package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.UserAlert

/**
 * Notification repository.
 */
interface NotificationsRepository {

    /**
     * Monitor user alerts
     *
     * @return a flow of all global user alerts
     */
    fun monitorUserAlerts(): Flow<List<UserAlert>>

    /**
     * Monitor events
     *
     * @return a flow of global [Event]
     */
    fun monitorEvent(): Flow<Event>

    /**
     * Get user alerts
     *
     * @return list of current user alerts
     */
    suspend fun getUserAlerts(): List<UserAlert>

    /**
     * Acknowledge user alerts have been seen
     */
    suspend fun acknowledgeUserAlerts()

    /**
     * Monitor home badge count.
     *
     * @return Flow of the number of pending actions the current logged in account has.
     */
    fun monitorHomeBadgeCount(): Flow<Int>

    /**
     * Broadcast home badge count.
     *
     * @param badgeCount Number of pending actions the current logged in account has.
     */
    suspend fun broadcastHomeBadgeCount(badgeCount: Int)
}