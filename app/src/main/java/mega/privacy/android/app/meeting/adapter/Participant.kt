package mega.privacy.android.app.meeting.adapter

import java.io.File
import java.io.Serializable

data class Participant(
    val name: String,
    val avatar: File?,
    val avatarBackground: String,
    val isMe: Boolean,
    val isModerator: Boolean,
    val isAudioOn: Boolean,
    val isVideoOn: Boolean,
    val isContact: Boolean = false
) : Serializable
