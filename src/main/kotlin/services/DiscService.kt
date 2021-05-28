package services

import asParticipant
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.DiscordGuild
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.VoiceChannelBehavior
import dev.kord.core.cache.data.GuildData
import dev.kord.core.entity.*
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.DmChannel
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.live.channel.live
import dev.kord.core.live.channel.onShutDown
import dev.kord.core.live.live
import dev.kord.core.on
import dev.kord.gateway.DefaultGateway
import dev.kord.gateway.on
import dev.kord.gateway.start
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.route.Route
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import models.Participant
import reply

object DiscService {

    lateinit var client: Kord
    lateinit var homeChannel: Channel
    lateinit var dmChannel: DmChannel
    lateinit var guildChannels: List<GuildChannel>
    lateinit var owner: User
    lateinit var homeGuild: Guild
    lateinit var guildMembers: List<Member>

    private suspend fun getHomeGuild(): Guild {
        return client.getGuild(homeChannel.data.guildId.value!!) ?: throw Exception("Not a guild!")
    }

    suspend fun start() {
        setupEvents()

        client.login {
            owner = client.getUser(Snowflake(Raffalo.config.owner))!!
            homeChannel = client.getChannel(Snowflake(Raffalo.config.channel))!!
            homeGuild = getHomeGuild()
            dmChannel = owner.getDmChannel()
            guildChannels = homeGuild.channels.toList()
            guildMembers = homeGuild.members.toList()
        }

    }

    suspend fun sendMessage(msg: String): Message {
        return dmChannel.createMessage(msg)
    }

    suspend fun replacePinWith(msg: Message) {
        msg.channel.pinnedMessages.toList().forEach {
            it.unpin()
        }
        msg.pin()
    }

    private fun handleVoiceChatEnter(participant: Participant) {
        participant.lastVoiceEnter = System.currentTimeMillis() / 1000
    }

    private fun handleVoiceChatExit(participant: Participant) {
        participant.apply {
            lastVoiceEnter.let {
                numVoiceSecs += (System.currentTimeMillis() / 1000) - it
            }
        }
    }

    @OptIn(KordPreview::class)
    private suspend fun setupEvents() {
        client = Kord(Raffalo.config.discordBotToken)


        client.slashCommands.createGuildApplicationCommands(Snowflake(295925849243254784)) {
            command("raff", "Raffalo related commands") {
                println("Hallo!")
                boolean("hi", "hello")
            }

        }

        client.on<ReadyEvent> {
            guildChannels.filterIsInstance<VoiceChannel>().forEach {
                println("Name: ${it.name}, States: ${it.voiceStates.toList()}")
                val chatParts = it.voiceStates.map { vs -> vs.getMember().asParticipant() }.toList()
                chatParts.forEach { participant ->
                    handleVoiceChatEnter(participant)
                }
            }

            println("BOT READY!")
        }

        // Voice chat leave/join handling

        client.on<VoiceStateUpdateEvent> {
            if (old == null || old?.channelId == null) {
                handleVoiceChatEnter(state.getMember().asParticipant())
            } else if (state.data.channelId == null) {
                handleVoiceChatExit(state.getMember().asParticipant())
            }
        }

        // Reaction handling

        client.on<ReactionAddEvent> {
            val msg = getMessageOrNull() ?: return@on
            val guild = msg.getGuildOrNull()
            val numReacted = msg.reactions.map { it.emoji }.filter { getUser() in msg.getReactors(it).toList() }.size
            // Only add stat if they've never reacted to the message before (therefore, reactions now = 1)
            if (numReacted <= 1) {
                println("${msg.getAuthorAsMember()?.nickname} has added a reaction (${emoji.name})")
                Raffalo.getParticipant(guild?.getMemberOrNull(userId) ?: getUser()).apply {
                    numReactions++
                }
            }
        }

        client.on<ReactionRemoveEvent> {
            val guild = getMessageOrNull()?.getGuildOrNull()
            println("${getMessageOrNull()?.getAuthorAsMember()?.nickname} has removed a reaction (${emoji.name})")
            Raffalo.getParticipant(guild?.getMemberOrNull(userId) ?: getUser()).apply {
                numReactions--
            }
        }

        // Message handling

        val pingPong = ReactionEmoji.Unicode("\uD83C\uDFD3")

        client.on<MessageCreateEvent> {

            /*
            println("mseget")
            println(message.author)
            println("mseoki")
            */

            if (message.getGuildOrNull()?.id?.value == Raffalo.config.guild) {
                println("${message.getAuthorAsMember()?.nickname}:\t\t${message.content}")
                Raffalo.getParticipant(message.author ?: return@on).apply {
                    numMessages++
                }
            }

            when (message.content) {
                "!ping" -> {
                    val response = message.reply("Pong!")
                    response.addReaction(pingPong)
                    delay(1000)
                    message.delete()
                    response.delete()
                }

                "!stats" -> {
                    Raffalo.getParticipant(message.author ?: return@on).apply {
                        message.reply("You have $numTickets ticket${if (numTickets > 1) "s" else ""} and have used $numBoosts/3 boosts for this raffle.")
                    }
                }

                "!vc" -> {
                    guildChannels.filterIsInstance<VoiceChannel>().forEach {
                        println("Name: ${it.name}, States: ${it.voiceStates.toList()}")
                    }
                }

                "!spend" -> {
                    Raffalo.getParticipant(message.author ?: return@on).apply {
                        if (numTickets > 0) {
                            if (numBoosts < 3) {
                                numTickets--
                                numBoosts++
                                message.reply("You've boosted yourself by +20%!")
                            } else {
                                message.reply("You've already used the maximum number of boosts!")
                            }
                        } else {
                            message.reply("You don't have any tickets to spend!")
                        }
                    }
                }
            }

            // Owner-only commands
            if (getHomeGuild().getOwner().id == message.author?.id) {
                val text = message.content
                when  {

                    text.startsWith("!start") -> {
                        println("Attempting to start a new raffle")
                        val input = text.split(" ").drop(1).map { it.trim() }
                        if (input.isEmpty()) {
                            message.reply("You need to specify a raffle time!")
                        } else {
                            Raffalo.startNewRaffle(1 * input[0].toInt(), message.channel)
                        }

                    }

                    text.startsWith("!giveticket") -> {
                        println("TICKET!: $text")
                        val input = text.split(" ").drop(1).map { it.trim() }
                        if (input.isEmpty()) {
                            message.reply("To who?")
                        } else {
                            var snowStr = input[0]
                            snowStr = snowStr.substringAfter("<@!").substringBefore(">")
                            val snowflake = Snowflake(snowStr)
                            Raffalo.getParticipant(snowflake).apply {
                                numTickets++
                                message.reply("Gave $nickname a ticket!")
                            }

                        }
                    }

                    else -> {
                        when (text) {

                            "!end" -> {
                                println("Ending raffle")
                                Raffalo.endRaffle(message.channel)
                            }

                            "!bedug" -> {
                                message.reply("ok sir")
                                println(Raffalo.finalParticipants())
                                println("DEBUGGING")
                            }

                            "!finals" -> {
                                message.reply(Raffalo.finalParticipants().map { it.nickname }.toString())
                            }

                            "!weights" -> {
                                message.reply("Calculating weights!")
                                message.reply(
                                    Raffalo.genWeightMap()?.map { "${it.key.nickname} - ${it.key.numReactions} - ${it.key.numVoiceSecs} - ${it.key.numMessages} - ${it.value}" }?.joinToString("\n") ?: "No?"
                                )
                            }

                            "!clear" -> {
                                Raffalo.participants.clear()
                            }

                            "!shutdown" -> {
                                message.reply("Raffalo is shutting down!")

                                // Manually "exit" everybody out of voice chat seconds-count wise
                                guildChannels.filterIsInstance<VoiceChannel>().forEach {
                                    println("Name: ${it.name}, States: ${it.voiceStates.toList()}")
                                    val chatParts = it.voiceStates.map { vs -> vs.getMember().asParticipant() }.toList()
                                    chatParts.forEach { participant ->
                                        handleVoiceChatExit(participant)
                                    }
                                }
                                Raffalo.stop()
                            }

                            "!games" -> {
                                println(Raffalo.games.map { it.name })
                                val response = message.reply(
                                    Raffalo.pickableGames.joinToString("\n") { it.name }
                                )
                            }

                            "!show" -> {
                                val response = message.reply(
                                    Raffalo.getParticipant(message.author!!).toString()
                                )
                                delay(1000)
                                response.delete()
                            }
                        }
                    }

                }
            }

        }

    }



}