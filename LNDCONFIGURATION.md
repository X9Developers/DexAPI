# Lightning Network Daemons (LND’s) Configuration
Here you can choose either put all configs in one directory, or split them up. if you split them, you will have two tls cert files, So we will be using `/.lnd_xsn/` for the XSN LND and `./lnd_ltc/` for the LTC LND.

## LND_XSN: /.lnd_xsn/lnd.conf  

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



## LND_LTC: /.lnd_ltc/lnd.conf  

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
    4 if the output of 3. was empty, you can manually connect to a static node to get the full network topology: ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons connect 02a49dc96ebcfd889f2cc694e9135fc8a502f7df4aa42b9a6f88d57759ddee5385@134.209.164.91:8384


## LND LTC Commands to test

    ./lncli --lnddir="/root/.lnd_ltc" -rpcserver="localhost:10001" --no-macaroons unlock
    ./lncli --lnddir=/root/.lnd_ltc -rpcserver=localhost:10001 --no-macaroons walletbalance
    ./lncli --lnddir="~/.lnd_ltc" -rpcserver="localhost:10001" --no-macaroons connect 032c6e03e7a316baa3fb64fb360ebd8520e90a21b5a3c4bfca7fd75689a1564ae3@134.209.164.91:8002
    
## Fund your XSN LND

    ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons unlock
    ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons walletbalance
    ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons newaddress p2wkh
    deposit some XSN to the output of step 3.
    ./lncli --lnddir="/root/.lnd_xsn" -rpcserver="localhost:10003" --no-macaroons walletbalance
    wait for confirmations
