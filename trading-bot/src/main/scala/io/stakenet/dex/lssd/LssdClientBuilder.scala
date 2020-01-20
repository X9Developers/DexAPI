package io.stakenet.dex.lssd

import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder
import lssdrpc.lssdrpc.{currenciesGrpc, ordersGrpc, swapsGrpc, tradingPairsGrpc}

object LssdClientBuilder {
  def currenciesClient(host: String, port: Int): currenciesGrpc.currenciesBlockingClient = {
    currenciesGrpc.blockingStub(createChannel(host, port))
  }

  def ordersClient(host: String, port: Int): ordersGrpc.ordersBlockingClient = {
    ordersGrpc.blockingStub(createChannel(host, port))
  }

  def swapsClient(host: String, port: Int): swapsGrpc.swapsBlockingClient = {
    swapsGrpc.blockingStub(createChannel(host, port))
  }

  def tradingPairsClient(host: String, port: Int): tradingPairsGrpc.tradingPairsBlockingClient = {
    tradingPairsGrpc.blockingStub(createChannel(host, port))
  }


  private def createChannel (host: String, port: Int): ManagedChannel   = {
    val channel = NettyChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build()

    sys.addShutdownHook {
      channel.shutdown()
    }
    channel
  }
}
