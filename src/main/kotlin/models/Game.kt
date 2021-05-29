package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Game {
    var id: String = "UNKNOWN_ID"

    @SerialName("Discord Ids")
    var submitterIds: List<Long> = listOf()

    @SerialName("Game Name")
    var name: String = "UNKNOWN_NAME"

    @SerialName("Is Mystery?")
    var isMystery: Boolean = false

    @SerialName("Humble Gift Keys")
    var keysRaw: String = "UNKNOWN_KEYS"

    @SerialName("Picked")
    var hasBeenPicked: Boolean = false

    @SerialName("Raffle Text")
    var text: String = ""

    @SerialName("SteamId")
    var steamId: Int = 0

    val keys: List<String>
        get() = keysRaw.split('\n').map { it.trim() }.filter { it != "" }

    override fun toString(): String {
        return "$id, $name, picked=$hasBeenPicked"
    }

}