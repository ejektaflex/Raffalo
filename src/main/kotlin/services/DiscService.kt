package services

import dev.kord.common.entity.DiscordEmbed
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.on
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import java.net.URI
import java.nio.file.Path

object DiscService {

    lateinit var client: Kord
    lateinit var homeChannel: Channel

    suspend fun start() {
        setupEvents()
        client.login {
            homeChannel = client.getChannel(Snowflake(Raffalo.config.channel))!!
            println(homeChannel)
        }
        //homeChannel = client.getChannel(Snowflake(Raffalo.config.channel))
    }

    suspend fun sendMessage(msg: String) {
        (homeChannel as MessageChannelBehavior).createMessage(msg)
    }

    suspend fun sendSteamImage(appId: Int) {
        (homeChannel as MessageChannelBehavior).createMessage("https://steamcdn-a.akamaihd.net/steam/apps/${appId}/header.jpg")
    }

    private suspend fun setupEvents() {
        client = Kord(Raffalo.config.discordBotToken)

        // Voice chat leave/join handling

        client.on<VoiceStateUpdateEvent> {

            if (old == null || old?.channelId == null) {
                Raffalo.getParticipant(state.getMember()).lastVoiceEnter = System.currentTimeMillis() / 1000
            } else if (state.data.channelId == null) {
                Raffalo.getParticipant(state.getMember()).apply {
                    numVoiceSecs += (System.currentTimeMillis() / 1000) - lastVoiceEnter
                }
            }

        }

        // Reaction handling

        client.on<ReactionAddEvent> {
            val msg = getMessage()
            val numReacted = msg.reactions.map { it.emoji }.filter { getUser() in msg.getReactors(it).toList() }.size
            // Only add stat if they've only reacted to the message once
            if (numReacted > 1) {
                Raffalo.getParticipant(getUser()).apply {
                    numReactions++
                }
            }
        }

        client.on<ReactionRemoveEvent> {
            Raffalo.getParticipant(getUser()).apply {
                numReactions--
            }
        }

        // Message handling

        val pingPong = ReactionEmoji.Unicode("\uD83C\uDFD3")

        client.on<MessageCreateEvent> {

            Raffalo.getParticipant(member ?: return@on).apply {
                numMessages++
            }

            when (message.content) {
                "!ping" -> {
                    val response = message.channel.createMessage("Pong!")
                    response.addReaction(pingPong)
                    delay(1000)
                    message.delete()
                    response.delete()
                }
            }

            // Owner-only commands
            if (message.getGuild().getOwner().id == message.author?.id) {
                val text = message.content
                when  {

                    text.startsWith("!start") -> {
                        println("Attempting to start a new raffle")
                        val input = text.split(" ").drop(1)
                        if (input.isEmpty()) {
                            message.channel.createMessage("You need to specify a raffle time!")
                        } else {
                            Raffalo.startNewRaffle(86400 * input[0].toInt())
                        }

                    }

                    else -> {
                        when (text) {

                            "!games" -> {
                                println(Raffalo.games.map { it.name })
                                val response = message.channel.createMessage(
                                    Raffalo.pickableGames.joinToString("\n") { it.name }
                                )
                            }

                            "!show" -> {
                                val response = message.channel.createMessage(
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