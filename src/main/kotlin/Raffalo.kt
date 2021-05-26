import dev.kord.core.entity.Member
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import models.Config
import models.Game
import java.io.File

object Raffalo {

    val games = mutableListOf<Game>()

    val membersInChat = mutableListOf<Member>()

    val pickableGames: List<Game>
        get() = games.filter { !it.hasBeenPicked }

    val config = Json.decodeFromString(Config.serializer(), File("config.json").readText())

    fun start() {
        println(config.airTableKey)
    }

    fun update() {

    }

}