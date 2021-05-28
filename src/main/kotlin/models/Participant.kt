package models

import kotlinx.serialization.Serializable

@Serializable
data class Participant(
    var id: Long = 0L,
    var nickname: String = "NO_NICK",
    var lastVoiceEnter: Long = 0,
    var numReactions: Long = 0,
    var numVoiceSecs: Long = 0,
    var numMessages: Long = 0,
    var numTickets: Int = 0,
    var numBoosts: Int = 0
) {
}