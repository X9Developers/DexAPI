package io.stakenet.dex

import io.stakenet.dex.lssd.LssdClientBuilder
import lssdrpc.lssdrpc.AddCurrencyRequest.TlsCert
import lssdrpc.lssdrpc.SwapResult.Value
import lssdrpc.lssdrpc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class LssdRpcConfig(host: String, port: Int)
case class LndRpcConfig(host: String, port: Int, tlsCert: String) {
  def string: String = s"$host:$port"
}

case class CurrencyData(currency: String, lndRpcConfig: LndRpcConfig)

class Exchange(pair: String,
               currencies: (CurrencyData, CurrencyData),
               currenciesLssd: currenciesGrpc.currenciesBlockingClient,
               ordersLssd: ordersGrpc.ordersBlockingClient,
               tradingPairLssd: tradingPairsGrpc.tradingPairsBlockingClient,
               swapLssd: swapsGrpc.swapsBlockingClient) {

  private var myOpenOrders: List[Order] = List.empty

  private def unsafeUpdateOpenOrders(newOpenOrders: List[Order]): Unit = {
    myOpenOrders = newOpenOrders
  }

  def placeOrder(request: PlaceOrderRequest): Unit = {
    ordersLssd.placeOrder(request)
  }

  def placeOrder(order: Order): Unit = {
    ordersLssd.placeOrder(
      PlaceOrderRequest(
        pairId = order.pairId,
        side = order.side,
        funds = order.funds,
        price = order.price,
      )
    )
  }

  private def handleOrderPlaced(order: Order): Unit = synchronized {
    myOpenOrders = order :: myOpenOrders
  }

  private def handleOrderRemoved(removedOrderId: String): Unit = synchronized {
    val orderMaybe = myOpenOrders.find(_.orderId == removedOrderId)
    val newOpenOrders = myOpenOrders.filter(_.orderId == removedOrderId)
    unsafeUpdateOpenOrders(newOpenOrders)

    orderMaybe.foreach { removedOrder =>
      placeOrder(removedOrder)
    }
  }

  def init(): Unit = {
    List(currencies._1, currencies._2).foreach { current =>
      val tlsCert = TlsCert.RawCert(current.lndRpcConfig.tlsCert)
      val request = AddCurrencyRequest(
        currency = current.currency,
        lndChannel = current.lndRpcConfig.string,
        tlsCert = tlsCert
      )
      currenciesLssd.addCurrency(request)
    }
    tradingPairLssd.enableTradingPair(EnableTradingPairRequest(pair))

    // After enableTradingPair, lssd runs a command to get the current orders which runs in another thread,
    // this sleep tries to ensure that the response for that command has been received.
    Thread.sleep(3000)
    subscribe(
      onOrderPlaced = handleOrderPlaced,
      onOrderRemoved = handleOrderRemoved
    )
  }

  private def subscribe(onOrderPlaced: Order => Unit,
                        onOrderRemoved: String => Unit) = {
    val a = Future {
      log("Subscribe to swaps")
      swapLssd.subscribeSwaps(SubscribeSwapsRequest()).foreach { f =>
        f.value match {
          case Value.Success(value) =>
            log(s"swap success: $value")
            val order = myOpenOrders.find(value.orderId == _.orderId).map(_.copy(orderId = ""))
            order.foreach(placeOrder)
          case Value.Failure(value) =>
            log(s"swap failure: $value")
            val order = myOpenOrders.find(value.orderId == _.orderId).map(_.copy(orderId = ""))
            order.foreach(placeOrder)
          case Value.Empty => println("unknown message")
        }
      }
    }

    val b = Future {
      log("Subscribe to orders")
      ordersLssd.subscribeOrders(SubscribeOrdersRequest()).foreach {
        orderUpdate =>
          orderUpdate.update match {
            case OrderUpdate.Update.OrderRemoval(value) if value.isOwnOrder =>
              log(s"my order removed = ${value.orderId}")
              onOrderRemoved(value.orderId)
            case OrderUpdate.Update.OrderRemoval(value) =>
              log(s"order removed = ${value.orderId}")
              ()
            case OrderUpdate.Update.Order(value) if value.isOwnOrder =>
              log(s"my order placed: $value")
              onOrderPlaced(value)

            case OrderUpdate.Update.Order(value) =>
              log(s"order placed: $value")
              ()

            case _ => ()
          }
      }
    }

    Future.sequence(List(a, b)).map(_ => ())
  }

  private def log(msg: String): Unit = {
    println(s"$Exchange pair: $msg")
  }
}

object Exchange {
  def apply(lssdConfig: LssdRpcConfig,
            lndLtcConfig: LndRpcConfig,
            lndXsnConfig: LndRpcConfig): Exchange = {
    val currenciesLssd =
      LssdClientBuilder.currenciesClient(lssdConfig.host, lssdConfig.port)
    val ordersLssd =
      LssdClientBuilder.ordersClient(lssdConfig.host, lssdConfig.port)
    val tradingPairLssd =
      LssdClientBuilder.tradingPairsClient(lssdConfig.host, lssdConfig.port)
    val swapsLssd =
      LssdClientBuilder.swapsClient(lssdConfig.host, lssdConfig.port)
    val currencies = (
      CurrencyData(currency = "XSN", lndRpcConfig = lndXsnConfig),
      CurrencyData(currency = "LTC", lndRpcConfig = lndLtcConfig)
    )

    new Exchange(
      "XSN_LTC",
      currencies,
      currenciesLssd,
      ordersLssd,
      tradingPairLssd,
      swapsLssd
    )
  }
}

object Bot {

  def main(args: Array[String]): Unit = {
    actualBotFlow()
    Await.result(Future.never, Duration.Inf)
  }

  def actualBotFlow() = Future {
    println("Exchange Flow")
    val exchange = exchangeA()
    exchange.init()

    println("Exchange - Place buy orders")
    (90000 to 95000 by 200).foreach { price =>
      exchange.placeOrder(
        PlaceOrderRequest(
          pairId = "XSN_LTC",
          side = OrderSide.buy,
          funds = Some(BigInteger(price.toString)),
          price = Some(BigInteger(price.toString)),
        )
      )
    }
    println("Exchange - Buy orders placed")

    println("Exchange - Place sell orders")
    (96000 to 99000 by 200).foreach { price =>
      exchange.placeOrder(
        PlaceOrderRequest(
          pairId = "XSN_LTC",
          side = OrderSide.sell,
          price = Some(BigInteger(price.toString)),
          funds = Some(BigInteger(price.toString))
        )
      )
    }
    println("Exchange - Sell orders placed")
  }

  def exchangeAFlow() = Future {
    println("Exchange A Flow")
    val exchange = exchangeA()
    exchange.init()

    println("Exchange A - Place order")
    exchange.placeOrder(
      PlaceOrderRequest(
        pairId = "XSN_LTC",
        side = OrderSide.buy,
        funds = Some(BigInteger("10000")),
        price = Some(BigInteger("10000")),
      )
    )
    println("Exchange A - Order placed")
  }

  def exchangeBFlow() = Future {
    println("Exchange B Flow")
    val exchange = exchangeB()
    exchange.init()

    println("Exchange B - Place order")
    exchange.placeOrder(
      PlaceOrderRequest(
        pairId = "XSN_LTC",
        side = OrderSide.sell,
        funds = Some(BigInteger("10000")),
        price = Some(BigInteger("10000")),
      )
    )
    println("Exchange B - Order placed")
  }

  def exchangeA(): Exchange = {
    val tlsCertRaw = readFile("exchange-a-tls.cert")
    Exchange.apply(
      lssdConfig = LssdRpcConfig("localhost", 50051),
      lndLtcConfig =
        LndRpcConfig(host = "localhost", port = 10001, tlsCert = tlsCertRaw),
      lndXsnConfig =
        LndRpcConfig(host = "localhost", port = 10003, tlsCert = tlsCertRaw)
    )
  }

  def exchangeB(): Exchange = {
    val tlsCertRaw = readFile("exchange-b-tls.cert")
    println(tlsCertRaw)
    Exchange.apply(
      lssdConfig = LssdRpcConfig("localhost", 50002),
      lndLtcConfig =
        LndRpcConfig(host = "localhost", port = 20001, tlsCert = tlsCertRaw),
      lndXsnConfig =
        LndRpcConfig(host = "localhost", port = 20003, tlsCert = tlsCertRaw)
    )
  }

  private def readFile(name: String): String = {
    val url = getClass.getResource("/" + name)
    val source = scala.io.Source.fromURL(url)
    val data = source.getLines().mkString("\n")
    source.close()
    data
  }
}
