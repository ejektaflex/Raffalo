package services

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import models.Game

object AirService {

    private val client = HttpClient()

    private val form = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun fetchGames(): List<Game> {

        // TODO review: Max records is set to 3 in URL!
        val response = client.get<String>("https://api.airtable.com/v0/") {
            // Configure request parameters exposed by HttpRequestBuilder
            header("Authorization", "Bearer " + "")
        }

        val asObject = form.decodeFromString<JsonObject>(response)

        return asObject["records"]!!.jsonArray.map {
            val game = form.decodeFromJsonElement(Game.serializer(), it.jsonObject["fields"]!!)
            game.id = it.jsonObject["id"]!!.jsonPrimitive.content
            game
        }
    }


}