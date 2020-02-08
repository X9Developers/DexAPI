package io.stakenet.dex

import io.stakenet.dex.lssd.LssdClientBuilder
import lssdrpc.lssdrpc.AddCurrencyRequest._
import lssdrpc.lssdrpc.PlaceOrderResponse.Outcome
import lssdrpc.lssdrpc.SwapResult.Value
import lssdrpc.lssdrpc._

import scala.annotation.tailrec
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

  private var myOpenOrders: Set[Order] = Set.empty
  private var openOrders: Set[Order] = Set.empty

  private def unsafeInsertMyOpenOrder(order: Order): Unit = {
    myOpenOrders = myOpenOrders + order
  }

  private def unsafeRemoveMyOpenOrder(order: Order): Unit = {
    myOpenOrders = myOpenOrders - order
  }

  private def unsafeInsertOpenOrder(order: Order): Unit = {
    openOrders = openOrders + order
  }

  private def unsafeRemoveOpenOrder(order: Order): Unit = {
    openOrders = openOrders - order
  }

  def getOpenOrders: ListOrdersResponse = {
    ordersLssd.listOrders(
      ListOrdersRequest(pairId = pair, includeOwnOrders = true, limit = 500)
    )
  }

  @tailrec
  final def placeOrder(request: PlaceOrderRequest, placeOrderTry: Int): Unit = {
    val response = ordersLssd.placeOrder(request)

    response.outcome match {
      case Outcome.Empty => println("Empty Response")
      case Outcome.SwapSuccess(value) => {
        println(s"Swap Success: $value")
        placeOrder(request, 1)
      }
      case Outcome.Order(value) => {
        println(s"My Order Placed: $value")
      }
      case Outcome.Failure(value) => {
        if (placeOrderTry < 6) {
          println(s"Retrying #$placeOrderTry: $request")
          Thread.sleep(300)
          placeOrder(request, placeOrderTry + 1)
        } else println(s"Failure: $value")
      }
    }
  }

  def placeOrder(order: Order, placeOrderTry: Int = 1): Unit = {
    placeOrder(
      PlaceOrderRequest(
        pairId = order.pairId,
        side = order.side,
        funds = order.funds,
        price = order.price,
      ),
      placeOrderTry
    )
  }

  private def handleOrderPlaced(order: Order): Unit = synchronized {
    if (order.isOwnOrder) {
      unsafeInsertMyOpenOrder(order)
    } else {
      unsafeInsertOpenOrder(order)
    }
  }

  private def handleOrderRemoved(order: Order): Unit = synchronized {
    if (order.isOwnOrder) {
      println(s"My Order Removed: ${order.orderId}")
      unsafeRemoveMyOpenOrder(order)
      placeOrder(order)
    } else {
      println(s"Order Removed: ${order.orderId}")
      unsafeRemoveOpenOrder(order)
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

    val (myOrders, orders) = getOpenOrders.orders.toSet.partition(_.isOwnOrder)
    myOpenOrders = myOrders
    openOrders = orders

    subscribe(
      onOrderPlaced = handleOrderPlaced,
      onOrderRemoved = handleOrderRemoved
    )
  }

  private def subscribe(onOrderPlaced: Order => Unit,
                        onOrderRemoved: Order => Unit) = {
    val a = Future {
      log("Subscribe to swaps")
      swapLssd.subscribeSwaps(SubscribeSwapsRequest()).foreach { f =>
        f.value match {
          case Value.Empty => println("unknown message")
          case Value.Success(value) =>
            val order = myOpenOrders.find(value.orderId == _.orderId)
            order.foreach(placeOrder(_, 1))
          case Value.Failure(value) =>
            val order = myOpenOrders.find(value.orderId == _.orderId)
            order.foreach(placeOrder(_, 1))
        }
      }
    }

    val b = Future {
      log("Subscribe to orders")
      ordersLssd.subscribeOrders(SubscribeOrdersRequest()).foreach {
        orderUpdate =>
          orderUpdate.update match {
            case OrderUpdate.Update.OrderRemoval(order) =>
              handleOrderRemoved(order)
            case OrderUpdate.Update.Order(order) =>
              handleOrderPlaced(order)
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

  def actualBotFlow(): Future[Unit] = Future {
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
        ),
        1
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
        ),
        1
      )
    }
    println("Exchange - Sell orders placed")
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

  private def readFile(name: String): String = {
    val url = getClass.getResource("/" + name)
    val source = scala.io.Source.fromURL(url)
    val data = source.getLines().mkString("\n")
    source.close()
    data
  }
}
