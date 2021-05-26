package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Config {

    @SerialName("airtable")
    var airTableKey: String = "UNKNOWN_KEY"

    @SerialName("bot_token")
    var discordBotToken: String = "UNKNOWN_TOKEN"

}