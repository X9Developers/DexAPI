package io.stakenet.dex.lnd

import io.grpc.ManagedChannel
import io.grpc.netty.{GrpcSslContexts, NettyChannelBuilder}
import io.netty.handler.ssl.SslContext
import lnrpc.rpc.LightningGrpc

object LightningClientBuilder {
  def lndLtcA: LightningGrpc.LightningBlockingClient = {
    val host = "localhost"
    val port = 10001
    val cert = "/exchange-a-tls.cert"
    val sslContext = gRPCSSLContext(cert)
    val channel = managedChannel(sslContext, host, port)
    lightningStub(channel)
  }

  def lndXsnA: LightningGrpc.LightningBlockingClient = {
    val host = "localhost"
    val port = 10003
    val cert = "/exchange-a-tls.cert"
    val sslContext = gRPCSSLContext(cert)
    val channel = managedChannel(sslContext, host, port)
    lightningStub(channel)
  }

  def lndLtcB: LightningGrpc.LightningBlockingClient = {
    val host = "localhost"
    val port = 20001
    val cert = "/exchange-b-tls.cert"
    val sslContext = gRPCSSLContext(cert)
    val channel = managedChannel(sslContext, host, port)
    lightningStub(channel)
  }

  def lndXsnB: LightningGrpc.LightningBlockingClient = {
    val host = "localhost"
    val port = 20003
    val cert = "/exchange-b-tls.cert"
    val sslContext = gRPCSSLContext(cert)
    val channel = managedChannel(sslContext, host, port)
    lightningStub(channel)
  }

  private def gRPCSSLContext(filename: String): SslContext = {
    val trustedServerCertificate = getClass.getResourceAsStream(filename)
    GrpcSslContexts
      .forClient()
      .trustManager(trustedServerCertificate)
      .build()
  }

  private def managedChannel(sslContext: SslContext,
                             host: String,
                             port: Int): ManagedChannel = {
    NettyChannelBuilder
      .forAddress(host, port)
      .sslContext(sslContext)
      .build()
  }

  private def lightningStub(
    managedChannel: ManagedChannel
  ): LightningGrpc.LightningBlockingClient = {
    sys.addShutdownHook {
      managedChannel.shutdown()
    }
    LightningGrpc.blockingStub(managedChannel)
  }
}
