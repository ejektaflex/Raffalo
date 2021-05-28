package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Config {

    @SerialName("airtable_user_key")
    var airTableUser: String = "UNKNOWN_KEY"

    @SerialName("airtable_table_key")
    var airTableTable: String = "UNKNOWN_KEY"

    @SerialName("bot_token")
    var discordBotToken: String = "UNKNOWN_TOKEN"

    var owner: Long = 0L

    var channel: Long = 0L

    var guild: Long = 0L

    var roleToPing: Long = 0L

}