import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import kotlinx.serialization.json.Json
import models.Config
import models.Game
import models.Participant
import services.AirService
import services.DiscService
import java.io.File

object Raffalo {

    var games = listOf<Game>()

    var participants = mutableMapOf<Long, Participant>()

    val pickableGames: List<Game>
        get() = games.filter { !it.hasBeenPicked }

    val config = Json.decodeFromString(Config.serializer(), File("config.json").readText())

    fun getParticipant(member: Member): Participant {
        return participants.getOrPut(member.id.value) {
            Participant(member.id.value, member.nickname ?: member.username)
        }
    }

    fun getParticipant(user: User): Participant {
        return participants.getOrPut(user.id.value) {
            Participant(user.id.value, user.username)
        }
    }

    suspend fun start() {
        update()
        DiscService.start()
    }

    private suspend fun update() {
        games = AirService.fetchGames()
    }

}