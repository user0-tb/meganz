package mega.privacy.android.data.facade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.ScheduledMeetingUpdate
import mega.privacy.android.data.model.meeting.ChatCallUpdate
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatCallListenerInterface
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatListenerInterface
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatPresenceConfig
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatRoomListenerInterface
import nz.mega.sdk.MegaChatScheduledFlags
import nz.mega.sdk.MegaChatScheduledMeeting
import nz.mega.sdk.MegaChatScheduledMeetingListenerInterface
import nz.mega.sdk.MegaChatScheduledRules
import nz.mega.sdk.MegaChatSession
import nz.mega.sdk.MegaHandleList
import javax.inject.Inject

/**
 * Mega chat api facade implementation of the [MegaChatApiGateway]
 *
 * @property chatApi      [MegaChatApiAndroid]
 * @property sharingScope [CoroutineScope]
 */
internal class MegaChatApiFacade @Inject constructor(
    private val chatApi: MegaChatApiAndroid,
    @ApplicationScope private val sharingScope: CoroutineScope,
) : MegaChatApiGateway {

    override val initState: Int
        get() = chatApi.initState

    override fun init(session: String?): Int =
        chatApi.init(session)

    override fun logout(listener: MegaChatRequestListenerInterface?) = chatApi.logout(listener)

    override fun setLogger(logger: MegaChatLoggerInterface) =
        MegaChatApiAndroid.setLoggerObject(logger)

    override fun setLogLevel(logLevel: Int) = MegaChatApiAndroid.setLogLevel(logLevel)

    override fun addChatRequestListener(listener: MegaChatRequestListenerInterface) =
        chatApi.addChatRequestListener(listener)

    override fun removeChatRequestListener(listener: MegaChatRequestListenerInterface) =
        chatApi.removeChatRequestListener(listener)

    override fun pushReceived(
        beep: Boolean,
        chatId: Long,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.pushReceived(beep, chatId, listener)

    override fun retryPendingConnections(
        disconnect: Boolean,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.retryPendingConnections(disconnect, listener)

    override val chatUpdates: Flow<ChatUpdate>
        get() = callbackFlow {
            val listener = object : MegaChatListenerInterface {
                override fun onChatListItemUpdate(api: MegaChatApiJava?, item: MegaChatListItem?) {
                    trySend(ChatUpdate.OnChatListItemUpdate(item))
                }

                override fun onChatInitStateUpdate(api: MegaChatApiJava?, newState: Int) {
                    trySend(ChatUpdate.OnChatInitStateUpdate(newState))
                }

                override fun onChatOnlineStatusUpdate(
                    api: MegaChatApiJava?,
                    userhandle: Long,
                    status: Int,
                    inProgress: Boolean,
                ) {
                    trySend(ChatUpdate.OnChatOnlineStatusUpdate(userhandle, status, inProgress))
                }

                override fun onChatPresenceConfigUpdate(
                    api: MegaChatApiJava?,
                    config: MegaChatPresenceConfig?,
                ) {
                    trySend(ChatUpdate.OnChatPresenceConfigUpdate(config))
                }

                override fun onChatConnectionStateUpdate(
                    api: MegaChatApiJava?,
                    chatid: Long,
                    newState: Int,
                ) {
                    trySend(ChatUpdate.OnChatConnectionStateUpdate(chatid, newState))
                }

                override fun onChatPresenceLastGreen(
                    api: MegaChatApiJava?,
                    userhandle: Long,
                    lastGreen: Int,
                ) {
                    trySend(ChatUpdate.OnChatPresenceLastGreen(userhandle, lastGreen))
                }

                override fun onDbError(api: MegaChatApiJava?, error: Int, msg: String?) {
                    trySend(ChatUpdate.OnDbError(error, msg))
                }
            }

            chatApi.addChatListener(listener)
            awaitClose { chatApi.removeChatListener(listener) }
        }.shareIn(sharingScope, SharingStarted.WhileSubscribed())

    override val chatCallUpdates: Flow<ChatCallUpdate>
        get() = callbackFlow {
            val listener = object : MegaChatCallListenerInterface {
                override fun onChatCallUpdate(api: MegaChatApiJava?, chatCall: MegaChatCall?) {
                    trySend(ChatCallUpdate.OnChatCallUpdate(chatCall))
                }

                override fun onChatSessionUpdate(
                    api: MegaChatApiJava?,
                    chatId: Long,
                    callId: Long,
                    session: MegaChatSession?,
                ) {
                    trySend(ChatCallUpdate.OnChatSessionUpdate(chatId, callId, session))
                }
            }

            chatApi.addChatCallListener(listener)
            awaitClose { chatApi.removeChatCallListener(listener) }
        }.shareIn(sharingScope, SharingStarted.WhileSubscribed())

    override fun getChatRoomUpdates(chatId: Long): Flow<ChatRoomUpdate> = callbackFlow {
        val listener = object : MegaChatRoomListenerInterface {
            override fun onChatRoomUpdate(api: MegaChatApiJava?, chat: MegaChatRoom?) {
                trySend(ChatRoomUpdate.OnChatRoomUpdate(chat))
            }

            override fun onMessageLoaded(api: MegaChatApiJava?, msg: MegaChatMessage?) {
                trySend(ChatRoomUpdate.OnMessageLoaded(msg))
            }

            override fun onMessageReceived(api: MegaChatApiJava?, msg: MegaChatMessage?) {
                trySend(ChatRoomUpdate.OnMessageReceived(msg))
            }

            override fun onMessageUpdate(api: MegaChatApiJava?, msg: MegaChatMessage?) {
                trySend(ChatRoomUpdate.OnMessageUpdate(msg))
            }

            override fun onHistoryReloaded(api: MegaChatApiJava?, chat: MegaChatRoom?) {
                trySend(ChatRoomUpdate.OnHistoryReloaded(chat))
            }

            override fun onReactionUpdate(
                api: MegaChatApiJava?,
                msgId: Long,
                reaction: String,
                count: Int,
            ) {
                trySend(ChatRoomUpdate.OnReactionUpdate(msgId, reaction, count))
            }

            override fun onHistoryTruncatedByRetentionTime(
                api: MegaChatApiJava?,
                msg: MegaChatMessage,
            ) {
                trySend(ChatRoomUpdate.OnHistoryTruncatedByRetentionTime(msg))
            }
        }

        chatApi.closeChatRoom(chatId, listener)
        chatApi.openChatRoom(chatId, listener)

        awaitClose {
            chatApi.closeChatRoom(chatId, listener)
        }
    }.shareIn(sharingScope, SharingStarted.WhileSubscribed())

    override suspend fun requestLastGreen(userHandle: Long) =
        chatApi.requestLastGreen(userHandle, null)

    override fun createChat(
        isGroup: Boolean,
        peers: MegaChatPeerList,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.createChat(isGroup, peers, listener)

    override fun leaveChat(
        chatId: Long,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.leaveChat(chatId, listener)

    override fun setOpenInvite(
        chatId: Long,
        enabled: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.setOpenInvite(chatId, enabled, listener)

    override fun getChatRooms(): List<MegaChatRoom> =
        chatApi.chatRooms

    override fun getMeetingChatRooms(): List<MegaChatRoom>? =
        chatApi.getChatRoomsByType(MegaChatApi.CHAT_TYPE_MEETING_ROOM)

    override fun getGroupChatRooms(): List<MegaChatRoom>? =
        chatApi.getChatRoomsByType(MegaChatApi.CHAT_TYPE_GROUP)

    override fun getIndividualChatRooms(): List<MegaChatRoom>? =
        chatApi.getChatRoomsByType(MegaChatApi.CHAT_TYPE_INDIVIDUAL)

    override fun getChatRoomByUser(userHandle: Long): MegaChatRoom? =
        chatApi.getChatRoomByUser(userHandle)

    override fun loadUserAttributes(
        chatId: Long,
        userList: MegaHandleList,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.loadUserAttributes(chatId, userList, listener)

    override fun getUserEmailFromCache(userHandle: Long): String? =
        chatApi.getUserEmailFromCache(userHandle)

    override fun getUserAliasFromCache(userHandle: Long): String? =
        chatApi.getUserAliasFromCache(userHandle)

    override fun getUserFirstnameFromCache(userHandle: Long): String? =
        chatApi.getUserFirstnameFromCache(userHandle)

    override fun getUserLastnameFromCache(userHandle: Long): String? =
        chatApi.getUserLastnameFromCache(userHandle)

    override fun getUserFullNameFromCache(userHandle: Long): String? =
        chatApi.getUserFullnameFromCache(userHandle)

    override fun getUserOnlineStatus(userHandle: Long): Int =
        chatApi.getUserOnlineStatus(userHandle)

    override fun getChatRoom(chatId: Long): MegaChatRoom? =
        chatApi.getChatRoom(chatId)

    override fun getChatListItem(chatId: Long): MegaChatListItem? =
        chatApi.getChatListItem(chatId)

    override fun getChatListItems(mask: Int, filter: Int): List<MegaChatListItem>? =
        chatApi.getChatListItems(mask, filter)

    override fun getChatCall(chatId: Long): MegaChatCall? =
        chatApi.getChatCall(chatId)

    override fun getChatCallByCallId(callId: Long): MegaChatCall? =
        chatApi.getChatCallByCallId(callId)

    override fun getChatCalls(state: Int): MegaHandleList? =
        chatApi.getChatCalls(state)

    override fun getChatCallIds(): MegaHandleList? =
        chatApi.chatCallsIds

    override fun startChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.startChatCall(chatId, enabledVideo, enabledAudio, listener)

    override fun startChatCallNoRinging(
        chatId: Long,
        schedId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.startChatCallNoRinging(chatId, schedId, enabledVideo, enabledAudio, listener)

    override fun answerChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.answerChatCall(chatId, enabledVideo, enabledAudio, listener)

    override fun hangChatCall(
        callId: Long,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.hangChatCall(callId, listener)

    override fun holdChatCall(
        chatId: Long,
        setOnHold: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.setCallOnHold(chatId, setOnHold, listener)

    override fun setChatVideoInDevice(
        device: String,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.setChatVideoInDevice(device, listener)

    override val scheduledMeetingUpdates: Flow<ScheduledMeetingUpdate>
        get() = callbackFlow {
            val listener = object : MegaChatScheduledMeetingListenerInterface {
                override fun onChatSchedMeetingUpdate(
                    api: MegaChatApiJava?,
                    scheduledMeeting: MegaChatScheduledMeeting?,
                ) {
                    trySend(ScheduledMeetingUpdate.OnChatSchedMeetingUpdate(scheduledMeeting))
                }

                override fun onSchedMeetingOccurrencesUpdate(
                    api: MegaChatApiJava?,
                    chatId: Long,
                    append: Boolean,
                ) {
                    trySend(ScheduledMeetingUpdate.OnSchedMeetingOccurrencesUpdate(chatId, append))
                }
            }

            chatApi.addSchedMeetingListener(listener)
            awaitClose {
                chatApi.removeSchedMeetingListener(listener)
            }
        }.shareIn(sharingScope, SharingStarted.WhileSubscribed())

    override fun getAllScheduledMeetings(): List<MegaChatScheduledMeeting>? =
        chatApi.allScheduledMeetings

    override fun getScheduledMeeting(
        chatId: Long,
        schedId: Long,
    ): MegaChatScheduledMeeting? =
        chatApi.getScheduledMeeting(chatId, schedId)

    override fun getScheduledMeetingsByChat(chatId: Long): List<MegaChatScheduledMeeting>? =
        chatApi.getScheduledMeetingsByChat(chatId)

    override fun fetchScheduledMeetingOccurrencesByChat(
        chatId: Long,
        since: Long,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.fetchScheduledMeetingOccurrencesByChat(chatId, since, listener)

    override fun inviteToChat(
        chatId: Long,
        userHandle: Long,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.inviteToChat(chatId, userHandle, MegaChatPeerList.PRIV_STANDARD, listener)

    override fun checkChatLink(
        link: String,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.checkChatLink(link, listener)

    override fun setPublicChatToPrivate(
        chatId: Long,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.setPublicChatToPrivate(chatId, listener)

    override fun queryChatLink(
        chatId: Long,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.queryChatLink(chatId, listener)

    override fun removeChatLink(
        chatId: Long,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.removeChatLink(chatId, listener)

    override fun createChatLink(
        chatId: Long,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.createChatLink(chatId, listener)

    override fun getOnlineStatus(): Int =
        chatApi.onlineStatus

    override fun getMyUserHandle(): Long = chatApi.myUserHandle

    override fun getMyFullname(): String? = chatApi.myFullname

    override fun getMyEmail(): String? = chatApi.myEmail

    override fun removeFromChat(
        chatId: Long,
        handle: Long,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.removeFromChat(chatId, handle, listener)

    override fun updateChatPermissions(
        chatId: Long,
        handle: Long,
        privilege: Int,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.updateChatPermissions(chatId, handle, privilege, listener)

    override fun getMessage(chatId: Long, messageId: Long): MegaChatMessage? =
        chatApi.getMessage(chatId, messageId)

    override fun getMessageFromNodeHistory(chatId: Long, messageId: Long): MegaChatMessage? =
        chatApi.getMessageFromNodeHistory(chatId, messageId)

    override fun getChatInvalidHandle(): Long = MegaChatApiAndroid.MEGACHAT_INVALID_HANDLE

    override fun removeRequestListener(listener: MegaChatRequestListenerInterface) =
        chatApi.removeChatRequestListener(listener)

    override fun signalPresenceActivity(listener: MegaChatRequestListenerInterface) =
        chatApi.signalPresenceActivity(listener)

    override fun clearChatHistory(
        chatId: Long,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.clearChatHistory(chatId, listener)

    override fun archiveChat(
        chatId: Long,
        archive: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.archiveChat(chatId, archive, listener)

    override suspend fun refreshUrl() = chatApi.refreshUrl()

    override fun createChatroomAndSchedMeeting(
        peerList: MegaChatPeerList,
        isMeeting: Boolean,
        publicChat: Boolean,
        title: String,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
        timezone: String,
        startDate: Long,
        endDate: Long,
        description: String,
        flags: MegaChatScheduledFlags?,
        rules: MegaChatScheduledRules?,
        attributes: String?,
        listener: MegaChatRequestListenerInterface,
    ) {
        chatApi.createChatroomAndSchedMeeting(
            peerList, isMeeting,
            publicChat,
            title,
            speakRequest,
            waitingRoom,
            openInvite,
            timezone,
            startDate,
            endDate,
            description,
            flags,
            rules,
            attributes,
            listener
        )
    }

    override fun getConnectedState() = chatApi.connectionState

    override fun getChatConnectionState(chatId: Long) = chatApi.getChatConnectionState(chatId)

    override suspend fun getNumUnreadChats() = chatApi.unreadChats

    override fun setOnlineStatus(status: Int, listener: MegaChatRequestListenerInterface) =
        chatApi.setOnlineStatus(status, listener)
}
