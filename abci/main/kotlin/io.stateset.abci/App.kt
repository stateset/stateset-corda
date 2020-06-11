package io.stateset.abci

import jetbrains.exodus.env.Environments

fun main() {
    Environments.newInstance("tmp/storage").use { env ->
        val app = StatesetNetworkCommunication(env)
        val server = StatesetGrpcServer(app, 26658)
        server.start()
        server.blockUntilShutdown()
    }
}