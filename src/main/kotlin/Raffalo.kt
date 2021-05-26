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

    suspend fun startNewRaffle(secs: Int) {

        println("!!!")

        val pickedGame = pickableGames.randomOrNull()

        if (pickedGame == null) {
            println("???")
            DiscService.sendMessage("No raffle-able items exist!")
            println("oh no")
            return
        }

        var msg = "Hello! Welcome to today's raffle! It will last for: $secs seconds."

        if (pickedGame.isMystery) {
            msg += "\nThis raffle is a mystery!"

            // Show a hint, if one exists
            if (pickedGame.text.trim() != "") {
                msg += " The raffle hint is:"
                msg += "\n`${pickedGame.text}`"
            }

        } else {
            msg += "\nThis week's raffle is a known game! The game is named: `${pickedGame.name}`"
        }

        msg += "\n\nThe more you interact with our server, the better your odds!"

        DiscService.sendMessage(msg)

        if (!pickedGame.isMystery) {
            DiscService.sendSteamImage(pickedGame.steamId)
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