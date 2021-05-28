import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import models.Participant
import kotlin.random.Random

fun <T : Any> Map<T, Double>.weightedRandom(): T {
    val sum = values.sum()

    if (sum == 0.0) {
        return keys.random()
    }

    var point = Random.nextDouble(sum)

    for ((item, weight) in this) {
        if (point <= weight) {
            return item
        }
        point -= weight
    }
    return keys.last()
}


suspend fun Message.reply(str: String): Message {
    return channel.createMessage(str)
}

fun Member.asParticipant(): Participant {
    return Raffalo.getParticipant(this)
}