package models

import kotlinx.serialization.Serializable
import services.JsonService
import java.io.File

@Serializable
data class StateData(
    var participants: List<Participant> = listOf(),
    var pickedGame: Game? = null
) {
    companion object {

        private val saveFile = File("save.json").apply {
            createNewFile()
        }

        fun save(toFile: File = saveFile) {
            val data = StateData(Raffalo.participants.values.toList(), Raffalo.pickedGame)
            val str = JsonService.form.encodeToString(serializer(), data)
            toFile.writeText(str)
        }

        fun reset() {
            resetParticipants()
            resetGame()
        }

        private fun resetParticipants() {
            Raffalo.participants.values.forEach { participant ->
                participant.apply {
                    numBoosts = 0
                    numMessages = 0
                    numReactions = 0
                    numVoiceSecs = 0
                }
            }
        }

        private fun resetGame() {
            Raffalo.pickedGame = null
        }

        fun load() {
            val str = saveFile.readText()
            val data = try {
                JsonService.form.decodeFromString(serializer(), str)
            } catch (e: Exception) {
                StateData()
            }
            Raffalo.participants = data.participants.associateBy { it.id }.toMutableMap()
            Raffalo.pickedGame = data.pickedGame
        }

        fun reload() {
            save()
            load()
        }

    }
}