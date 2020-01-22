## Overview
 
Trading-bot is a project built in scala, which is responsible for generating and placing orders to the orderbook, to make this, the bot has to connect with the lssd grpc app.  
Firstly it registers the currencies with which it will be working, after that it creates the trading_pair, then creates orders randomly, to both sides, Sell and Buy and send them to the lssd. 

## How works: 

1. Trading bot defines where to find the lnd nodes.
2. Create the currencies 
3. Define the trading pair
4. Create the order
5. Place the order
6. Wait until order gets matched
7. Wait until swap is completed 
8. Place a new order when swap is completed

## Installation

To run this project you have to install the next components: 

* Java 8 Oracle: https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04

* Scala build tool: https://www.scala-sbt.org/

Before running the bot we must configure the lnd in the next ports without macaroons:

* LTC: localhost:10001
* XSN: localhost:10002
* BTC: localhost:10003

Clone the repository with 

    `git clone https://github.com/X9Developers/DexAPI`


## Run 


Open the project and you must add the cert file into  
    `trading-bot/src/main/resources` 
