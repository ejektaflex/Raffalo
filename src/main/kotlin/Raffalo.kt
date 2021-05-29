import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.DmChannel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import models.Config
import models.Game
import models.Participant
import models.StateData
import services.AirService
import services.DiscService
import java.io.File
import kotlin.math.max
import kotlin.system.exitProcess

object Raffalo {

    var games = listOf<Game>()

    var participants = mutableMapOf<Long, Participant>()

    val pickableGames: List<Game>
        get() = games.filter { !it.hasBeenPicked }

    var pickedGame: Game? = null

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

    suspend fun getParticipant(snowflake: Snowflake): Participant {
        return getParticipant(DiscService.client.getUser(snowflake)!!)
    }

    suspend fun startNewRaffle(days: Int, msgChannel: MessageChannelBehavior) {

        pickedGame = pickableGames.firstOrNull()

        if (pickedGame == null) {
            println("???")
            DiscService.sendMessage("No raffle-able items exist!")
            println("oh no")
            return
        }

        pickedGame?.let {

            StateData.resetParticipants()

            println("Picked: $pickedGame")

            var msg = "Hello! Welcome to today's raffle! It will last for: $days days."

            if (it.isMystery) {
                msg += "\nThis raffle is a mystery!"

                // Show a hint, if one exists
                if (it.text.trim() != "") {
                    msg += " The raffle hint is:"
                    msg += "\n\n`${it.text}`"
                }

            } else {
                msg += "\nThis week's raffle is a known game! The game is named: \n\n`${it.name}`"
                msg += "\n\nhttps://store.steampowered.com/app/${it.steamId}/"
            }

            msg += "\n\nThe more you interact with our server, the better your odds!"

            val sent = msgChannel.createMessage(msg)
            DiscService.replacePinWith(sent)
        }

    }

    suspend fun cancelRaffle(msgChannel: MessageChannelBehavior) {
        StateData.reset()
        msgChannel.createMessage("This raffle has been cancelled!")
    }

    suspend fun sendGift(user: User, game: Game) {
        user.getDmChannelOrNull()?.let {
            it.createMessage("Here's what you've won, ${user.username}!")
            it.createMessage(
                game.keys.joinToString("\n") { key -> "<https://www.humblebundle.com/gift?key=$key>" }
            )
        }
    }

    suspend fun endRaffle(msgChannel: MessageChannelBehavior) {

        DiscService.forceCountVoiceStates() // count players currently in chat

        StateData.save(File(System.currentTimeMillis().toString() + ".bak.json").apply {
            createNewFile()
        })

        val game = pickedGame

        if (game == null) {
            msgChannel.createMessage("There is no raffle to end!")
            return
        }

        msgChannel.createMessage("Hello to <@&${config.roleToPing}>!")
        delay(500)
        var msg = "The raffle has ended! "
        msg += "\nThe possible winners are: ${finalParticipants().joinToString(", ") { it.nickname }}"
        msg += "\nThe winner has been chosen, and it turns out to be:"
        val winner = calcWinner()

        if (winner == null) {
            msgChannel.createMessage("Actually, there are no winners today. Please come back next time. :(")
            return
        }

        msg += "\n\n`${winner.nickname}`!"
        msg += "\n\nCongrats! "

        msgChannel.createMessage(msg)

        val shouldSendDM = game.keys.isNotEmpty()

        println("Should send: ${game.keys.isNotEmpty()}, keys: '${game.keys}'")

        if (game.isMystery) {
            msg += "You won this game:\n\n\nhttps://store.steampowered.com/app/${game.steamId}/"
            msg += if (shouldSendDM) {
                "\n\nAsk ${game.submitterIds.joinToString(" or ") { "<@$it>" }} for your prize!"
            } else {
                "\n\nThe prize will be sent to you via a DM!"
            }
        }

        if (shouldSendDM) {
            val winnerMember = DiscService.guildMembers.find { it.id.value == winner.id }
            if (winnerMember == null) {
                msgChannel.createMessage("I couldn't find the winner's user account to send them the prize... Awkward. Please message my owner :'(")
            } else {
                listOf(
                    DiscService.owner, winnerMember // both owner and winner for now
                ).forEach { user -> sendGift(user, game) }
            }
        }

        StateData.reset()
        StateData.reload()

    }

    suspend fun finalParticipants(): List<Participant> {

        DiscService.forceCountVoiceStates() // :vomit:

        println("PICKED GAME: $pickedGame")
        println("SUBMITTER IDS: ${pickedGame?.submitterIds}")
        return participants.filter {
            it.key != DiscService.client.selfId.value
        }.filter {
            val submitterIds = pickedGame?.submitterIds ?: return@filter true
            println("${it.value.nickname} - ${it.key} - $submitterIds - ${it.key in submitterIds}")
            it.key !in submitterIds
        }.filter {
            !DiscService.homeGuild.getMember(Snowflake(it.key)).isBot
        }.filter {
            it.value.numVoiceSecs != 0L || it.value.numMessages != 0L || it.value.numReactions != 0L
        }.values.toList()
    }

    suspend fun genWeightMap(): Map<Participant, Double>? {
        val peeps = finalParticipants()
        //val peeps = participants.values

        println("Peeps:")
        println(peeps.map { it.nickname })

        if (peeps.isEmpty()) {
            return null
        }

        val totReactions = max(1.0, peeps.maxOf { it.numReactions }.toDouble())
        val totVoiceSecs = max(1.0, peeps.maxOf { it.numVoiceSecs }.toDouble())
        val totMessages = max(1.0, peeps.maxOf { it.numMessages }.toDouble())

        // ((A+B+C)/2+1) * (1+(BOOST*0.2))
        return peeps.associateWith {
            (((it.numMessages / totMessages) + (it.numVoiceSecs / totVoiceSecs) + (it.numReactions / totReactions)) / 2 + 1) * (1 + (it.numBoosts * 0.2))
        }
    }

    private suspend fun calcWinner(): Participant? {
        return genWeightMap()?.weightedRandom()
    }


    suspend fun start() {
        load()
        DiscService.start()
    }

    fun stop() {
        StateData.save()
        exitProcess(0)
    }

    private suspend fun load() {
        StateData.load()
        games = AirService.fetchGames()
        println("Games: ${games.joinToString(", ") { it.name }}")
    }

}