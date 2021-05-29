package doot

import Raffalo

suspend fun main(args: Array<String>) {

    Raffalo.start()

    println(Raffalo.games.map { it.name })

    return
}
