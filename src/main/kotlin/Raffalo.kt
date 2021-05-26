import dev.kord.core.entity.Member
import models.Game

object Raffalo {

    val games = mutableListOf<Game>()

    val membersInChat = mutableListOf<Member>()

    val pickableGames: List<Game>
        get() = games.filter { !it.hasBeenPicked }

    fun update() {

    }

}