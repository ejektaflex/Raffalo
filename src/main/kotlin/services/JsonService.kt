package services

import kotlinx.serialization.json.Json

object JsonService {
    val form = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
}