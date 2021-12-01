# Lightning Network Daemons (LND’s) Configuration
Here you can choose either put all configs in one directory, or split them up. if you split them, you will have two tls cert files, So we will be using `/.lnd_xsn/` for the XSN LND and `./lnd_ltc/` for the LTC LND.

## Example of LND_XSN: /.lnd_xsn/lnd.conf  

    alias=magic-xsn-ln-node
    color=#1d013c
    maxpendingchannels=9
    no-macaroons=true
    rpclisten=localhost:10003

    bitcoin.active=0
    [Bitcoin]
    ; If the Bitcoin chain should be active. Atm, only a single chain can be
    ; active.
    bitcoin.active=false

    [xsncoin]
    xsncoin.mainnet=1
    xsncoin.active=1
    xsncoin.node=xsnd

    [xsncoind]
    xsncoind.rpchost=localhost
    xsncoind.rpclisten=localhost:10003
    xsncoind.rpcuser=XSNUSER123123
    xsncoind.rpcpass=XSNPW123123
    xsncoind.zmqpubrawblock=127.0.0.1:28332
    xsncoind.zmqpubrawtx=127.0.0.1:28333

    [xsnd]
    xsnd.rpcuser=XSNUSER123123
    xsnd.rpcpass=XSNPW123123
    xsncoin.rpchost=127.0.0.1:10003
    xsncoin.zmqpubrawblock=127.0.0.1:28332
    xsncoin.zmqpubrawtx=127.0.0.1:28333



## Example of LND_LTC: /.lnd_ltc/lnd.conf  

    alias=magic-ltc-ln-node
    color=#1d013c
    maxpendingchannels=9
    no-macaroons=true
    rpclisten=localhost:10001
    bitcoin.active=0

    [litecoin]
    litecoin.mainnet=1
    litecoin.active=1
    litecoin.node=litecoind

    [litecoind]
    litecoind.rpchost=localhost
    litecoind.rpcuser=LTCUSER123123
    litecoind.rpcpass=LTCPW123123
    litecoind.zmqpubrawblock=127.0.0.1:28336
    litecoind.zmqpubrawtx=127.0.0.1:28337


# Some extra lnd arguments explained (recommended to use):

* nobootstrap=1 ( If true, then automatic network bootstrapping will not be attempted )

* <coinname>.defaultchanconfs=6 

* autopilot.maxchannels=N (max channels number that expected to be opened with autopilot, in wallet we are using 1 at the moment).

* autopilot.conftarget=6 ( all nodes are using 6 confirmations to apply channel as opened )

* chan-enable-timeout=1m ( The duration a peer's connect must remain stable before attempting to reenable the channel ).

* maxpendingchannels=N ( should be bigger then 1 for dual funding to work properly, recommended 5-10 )

* maxlogfiles=N ( number of logfiles to be saved in a logs folder, each file is 10mb gz archive, recommended to use at least 10 files )

* max-cltv-expiry=N ( should be configured for each chain, for XSN  =10080, for LTC =4032, for BTC – no need, will use default, set to 1000 ).


# Run the LND’s
Create a “lightning” directory to control all of the things (optional).

Important: Every restart needs a two step process. First time you start a lnd, you need to do “lncli create” and follow the process that creates you certificates. The next time, you need to do “lncli unlock”. So you need to have the LND running in the background but it requires actions from the lncli as well.

The LND dont come with a daemon=1 setup so we have to create a little script that runs the LND’s in the background which are the following:

## **LND_LTC:** /lightning/run_lnd_ltc.sh

if you created the LND with “--no-macaroons”, you also need to add this here:

    #!/usr/bin/env bash
    nohup ./lnd_ltc --lnddir=/root/.lnd_ltc --litecoin.active --litecoin.mainnet --litecoin.node=litecoind --litecoind.rpcuser=LTCUSER123123 --litecoind.rpcpass=LTCPW123123 --litecoind.zmqpubrawblock=tcp://127.0.0.1:28336 --litecoind.zmqpubrawtx=tcp://127.0.0.1:28337 --rpclisten=localhost:10001 --restlisten=127.0.0.1:8081 --no-macaroons > lnd_ltc.out 2> lnd_ltc.err < /dev/null &

## **LND_XSN:** /lightning/run_lnd_xsn.sh

if you created the LND with “--no-macaroons”, you also need to add this here:
   
    #!/usr/bin/env bash
    nohup ./lnd_xsn --lnddir=/root/.lnd_xsn --xsncoin.active --xsncoin.mainnet --xsncoin.node=xsnd --xsnd.rpcuser=XSNUSER123123 --xsnd.rpcpass=XSNPW123123 --xsnd.zmqpubrawblock=tcp://127.0.0.1:28332 --xsnd.zmqpubrawtx=tcp://127.0.0.1:28333 --rpclisten=localhost:10003 > lnd_xsn.out 2> lnd_xsn.err < /dev/null &



## LNCLI: /lightning/lncli (also part of the Beta testing wallet)

## LND XSN Commands to test
    1 ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons unlock
    2 ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons walletbalance
    3 ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons describe graph 
    4 if the output of 3. was empty, you can manually connect to a static node to get the full network topology: ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons connect 02a49dc96ebcfd889f2cc694e9135fc8a502f7df4aa42b9a6f88d57759ddee5385@178.128.97.48:8384


## LND LTC Commands to test

    ./lncli --lnddir="/root/.lnd_ltc" -rpcserver="localhost:10001" --no-macaroons unlock
    ./lncli --lnddir=/root/.lnd_ltc -rpcserver=localhost:10001 --no-macaroons walletbalance
    ./lncli --lnddir="~/.lnd_ltc" -rpcserver="localhost:10001" --no-macaroons connect 032c6e03e7a316baa3fb64fb360ebd8520e90a21b5a3c4bfca7fd75689a1564ae3@178.128.97.48:8002
    
## Fund your XSN LND

    ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons unlock
    ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons walletbalance
    ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons newaddress p2wkh
    deposit some XSN to the output of step 3.
    ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons walletbalance
    wait for confirmations

# Creating channels:

In order to swap successfully there’s a need in payment channels to be created. 	


Channels can be created in two ways:

1. Manually ( using openchannel comand from lncli );
2. Using autopilot configuration ( activate autopilot on start + make connections to hub nodes );

	For bot making, it is better to use manual open, as this way you can specify exact capacities for your channels.

	At the moment all users have channels with our hub nodes, so for bot, to successfully swap orders, it is enough to have channels with at least one hub node per coin. 


Currently existing nodes described below:

## btc hub nodes:
 
    "03757b80302c8dfe38a127c252700ec3052e5168a7ec6ba183cdab2ac7adad3910@178.128.97.48:11000"

## ltc hub nodes:

    "0375e7d882b442785aa697d57c3ed3aef523eb2743193389bd205f9ae0c609e6f3@178.128.97.48:11002"
    "0211eeda84950d7078aa62383c7b91def5cf6c5bb52d209a324cda0482dbfbe4d2@178.128.97.48:21002"

## xsn hub nodes:

    "0396ca2f7cec03d3d179464acd57b4e6eabebb5f201705fa56e83363e3ccc622bb@178.128.97.48:11384"
    "03bc3a97ffad197796fc2ea99fc63131b2fd6158992f174860c696af9f215b5cf1@178.128.97.48:21384"


When setting up channels and placing orders, you must count next restrictions:

## Channel capacities: 

    xsn min channel: 0.0006 XSN
    xsn max channel: 1000 XSN

    btc min channel: 0.0002 BTC
    btc max channel: 0.16 BTC

    ltc min channel: 0.00275 LTC
    ltc max channel: 10 LTC


## Payment capacities: 

    xsn max payment: 250 XSN
    ltc max payment: 2.5 LTC
    btc max payment: 0.04 BTC

## Example of channel open:

    lncli <node params (rpcserver, macaroons) > connect $HUB_PUBKEY@HUB_IP:NODE_PORT
    lncli <node params (rpcserver, macaroons) > openchannel --node_key=$HUB_PUBKEY –-local_amt=N(in satoshi)

Now need to wait until the payment is confirmed ( 6 blocks generated) though the above command generated a transaction id you can track it in the xsnexplorer.io, the pending channels can be seen with the pending channels command:

    lncli <node params (rpcserver, macaroons) > pendingchannels

Once the channel is opened, you can list the payment channel with the following:

    lncli <node params (rpcserver, macaroons) > listchannels
