package services

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import models.Game
import services.JsonService.form

object AirService {

    private val client = HttpClient()



    suspend fun fetchGames(): List<Game> {
        val response = client.get<String>("https://api.airtable.com/v0/${Raffalo.config.airTableTable}/Games?view=Grid%20view") {
            // Configure request parameters exposed by HttpRequestBuilder
            header("Authorization", "Bearer " + Raffalo.config.airTableUser)
        }

        val asObject = form.decodeFromString<JsonObject>(response)

        return asObject["records"]!!.jsonArray.map {
            val obj = it.jsonObject
            //println("OBJ: $obj")
            val game = form.decodeFromJsonElement(Game.serializer(), obj["fields"]!!)
            game.id = obj["id"]!!.jsonPrimitive.content
            game
        }
    }


}