import dev.kord.core.Kord
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.channel.VoiceChannelUpdateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.user.UserUpdateEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.on
import kotlinx.coroutines.delay

suspend fun main() {

    Raffalo.start()

    return

    val client = Kord("")
    val pingPong = ReactionEmoji.Unicode("\uD83C\uDFD3")

    client.on<MessageCreateEvent> {
        if (message.content != "!ping") return@on

        val response = message.channel.createMessage("Pong!")
        response.addReaction(pingPong)

        delay(1000)
        message.delete()
        response.delete()
    }

    client.on<VoiceStateUpdateEvent> {

        if (old == null) {
            println("We entered! $old")
        } else if (state.data.channelId == null) {
            println("We left? $state")
        }

        println(this.old)
        println(this.state)
    }

    client.login()



}
