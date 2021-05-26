package services

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.on
import kotlinx.coroutines.delay

object DiscService {

    lateinit var client: Kord

    suspend fun start() {
        setupEvents()
        client.login()
    }

    private suspend fun setupEvents() {
        client = Kord(Raffalo.config.discordBotToken)

        client.on<VoiceStateUpdateEvent> {

            if (old == null || old?.channelId == null) {
                Raffalo.getParticipant(state.getMember()).lastVoiceEnter = System.currentTimeMillis() / 1000
            } else if (state.data.channelId == null) {
                Raffalo.getParticipant(state.getMember()).apply {
                    numVoiceSecs += (System.currentTimeMillis() / 1000) - lastVoiceEnter
                }
            }

        }

        // Numerical reaction handling

        client.on<ReactionAddEvent> {
            Raffalo.getParticipant(getUser()).apply {
                numReactions++
            }
        }

        client.on<ReactionRemoveEvent> {
            Raffalo.getParticipant(getUser()).apply {
                numReactions--
            }
        }



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