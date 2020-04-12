package io.stateset.abci

import io.grpc.BindableService
import io.grpc.ServerBuilder

class StatesetGrpcServer(
    private val service: BindableService,
    private val port: Int
) {
    private val server = ServerBuilder
        .forPort(port)
        .addService(service)
        .build()

    fun start() {
        server.start()
        println("stateset gRPC server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                println("shutting down stateset gRPC server since JVM is shutting down")
                this@StatesetGrpcServer.stop()
                println("stateset gRPC server shut down")
            }
        })
    }

    fun stop() {
        server.shutdown()
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    fun blockUntilShutdown() {
        server.awaitTermination()
    }

}