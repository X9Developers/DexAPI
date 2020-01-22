# DexAPI
The project is an example of a bot for placing orders to the orderbook.
Contains the description of how to create your own bot.

# Components

* Trading-bot: a project built in scala, which is responsible for generating and placing orders to the orderbook, to make this, the bot has to connect with the lssd grpc app.  
Firstly it registers the currencies with which it will be working, after that it creates the trading_pair, then creates orders randomly, to both sides, Sell and Buy and send them to the lssd. You can see the docs [Here](https://github.com/X9Developers/DexAPI/tree/master/trading-bot) 


* Lssd app: the light wallet daemon, is the grpc application to place orders to the orderbook, also is responsible for making the swaps, the light wallet connects with the lnd nodes to manage the wallets. 
This service runs in `localhost: 50051`

    To run lssd: Open a terminal in `lssd/app/` and run `./lssd`. 

    To see if the api is working you can see the logs with:   

    `tail -f ~/.local/share/Stakenet/lssd/lssd.log`


* Custom lnds 
    They can be downloaded from [Here](https://github.com/X9Developers/DexAPI/releases/download/v2020.01.23/customlnds.zip)

    You must install the custom lnds in the next ports:
    * LTC: localhost:10001
    * BTC: localhost:10002
    * XSN: localhost:10003

# Create your own Bot 

After running the services of lssd and lnd, these are the data we will need to create our own bot:
From lssd
* Ip
* Port

  From lnd:
* Ip
* Port:
* Tls cert:
 
To create the bot you need to follow the protobuf file (lssdrpc.proto) that comes within the lssd zip, in the app folder. which is downloaded from [here](https://github.com/X9Developers/DexAPI/releases/download/v2020.01.23/lssd.zip)


Step 1: Create the grpc client for each service,  and create the trading pair for example: 

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
        }
    }

Step 2: Create the trading pair: 

First we must add a currency to our lssd with: 

    AddCurrencyRequest(
        currency = "XSN,
        lndChannel = "localhost:1003",
        tlsCert = "/resources/tls.cert" )
    currenciesLssd.addCurrency(request)

Step 3 Then you must create the trading pair with:

    tradingPairLssd.enableTradingPair(EnableTradingPairRequest(pair))


Step 4: Place orders: 
We have to create the order request for example:

    val request = PlaceOrderRequest(
          pairId = "XSN_LTC",
          side = OrderSide.buy,
          funds = Some(BigInteger(funds.toString)),
          price = Some(BigInteger(price.toString)),
        )


Step 5: Send the request to the lssd with:

    exchange.placeOrder(request)

Step 6: Wait until lssd daemon completes the swap, you can subscribe to swaps to know if swap is success or failure for example: 

      swapLssd.subscribeSwaps(SubscribeSwapsRequest()).foreach { f =>
        f.value match {
          case Value.Success(value) =>
            log(s"swap success: $value")
          case Value.Failure(value) =>
            log(s"swap failure: $value")
          case Value.Empty => println("unknown message")
        }
      }
