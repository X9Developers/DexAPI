# DexAPI
## About

This project is composed of two components.
* Trading-bot: a project built in scala, which is responsible for generating orders to the lssd API, firstly it registers the currencies with which it will be working, after that it creates the trading_pair, which creates orders randomly, to both sides, Sell and Buy. 

The bot is configured to listen the nodes in the following                 


* Lssd application: the light wallet application without the code, is the grpc application running, to place orders to the orderbook, also is responsible for making the swaps, the light wallet connects with the lnd nodes to manage the wallets. 

## Installation

* Java 8 Oracle: https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04
* Scala build tool: https://www.scala-sbt.org/
* Clone the repository with 

    `git clone https://github.com/X9Developers/DexAPI`


## Run

The trading bot needs the lnd nodes running in the following ports: 
* ltc: localhost:10001
* xsn: localhost:10002
* btc: localhost:10003

Open a terminal in `lssd/app/` and run the lssd api with `./Lssd`

Open a terminal in `trading-bot/` and run the bot with `sbt run`

To see if the api is working correctly and placing orders open a terminal and run  
    `tail -f ~/.local/share/Stakenet/lssd/lssd.log`

