## Write crash 1
| Crash                                           | CrashMsg            | Description                                                                           |
|-------------------------------------------------|---------------------|---------------------------------------------------------------------------------------|
| Crash of L2 before forwarding the write request | BEFORE_WRITE_REQ_FW | The Client detect the crash of the L2 cache and choose another L2 cache as its parent |


```
    //fill some caches with the item 1
    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    //fill some caches with the item 1
    LOGGER.info("Client 304 performs a read request");
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache200 crash before forwarding the write req");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_WRITE_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a write request");
    clientList.get(0).tell(new DoWriteMsg(1, 5), ActorRef.noSender());
```

## Write crash 2
| Crash                                          | CrashMsg           | Description                                                                                                                                                                                                                                                             |
|------------------------------------------------|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash of L2 after forwarding the write request | AFTER_WRITE_REQ_FW | The Client detect the crash of the L2 cache and choose another L2 cache as its parent. The write operation is performed and all the alive cache will receive the RefillMsg and update its value. The Client will not know that the write operation have been performed. |

```
    //fill some caches with the item 1
    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    //fill some caches with the item 1
    LOGGER.info("Client 304 performs a read request");
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache200 crash after forwarding the write req");
    l2List.get(0).tell(new CrashMsg(CrashType.AFTER_WRITE_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a write request");
    clientList.get(0).tell(new DoWriteMsg(1, 5), ActorRef.noSender());
```


## Write crash 3

| Crash                                           | CrashMsg            | Description                                                                                                                                                 |
|-------------------------------------------------|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash of L1 before forwarding the write request | BEFORE_WRITE_REQ_FW | The L2 detect the crash of the L1 cache and set the database as its parent. The L2 cache will also send an error message to the client to stop the timeout. |

```
    //fill some caches with the item 1
    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    //fill some caches with the item 1
    LOGGER.info("Client 304 performs a read request");
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crash before forwarding the write req");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_WRITE_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a write request");
    clientList.get(0).tell(new DoWriteMsg(1, 5), ActorRef.noSender());
```

## Write crash 4

| Crash                                          | CrashMsg           | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|------------------------------------------------|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash of L1 after forwarding the write request | AFTER_WRITE_REQ_FW | The L2 detect the crash of the L1 cache and set the database as its parent. It will start the refresh process and will see the new value but cannot tell if is the one it has just sent. The cache will be up to date but an error message to the client will be sent to stop the timeout. In our system the error message means that there has been an error while processing the request and we cannot ensure that the operation has been performed. We follow the at least once semantic. The client will decide if to perform again the write or not. |

```
    //fill some caches with the item 1
    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    //fill some caches with the item 1
    LOGGER.info("Client 304 performs a read request");
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crash after forwarding the write req");
    l1List.get(0).tell(new CrashMsg(CrashType.AFTER_WRITE_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a write request");
    clientList.get(0).tell(new DoWriteMsg(1, 5), ActorRef.noSender());
```

## Write crash 5


| Crash                                                                                                          | CrashMsg      | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|----------------------------------------------------------------------------------------------------------------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash before applying the refill to the saved items of the L1 cache in the path between the client and the DB. | BEFORE_REFILL | The L2 which originate the request detect the crash of the L1 cache and set the database as its parent. It will start the refresh process and will see the new value but cannot tell if is the one it has just sent. The cache will be up to date but an error message to the client will be sent to stop the timeout. In our system the error message means that there has been an error while processing the request and we cannot ensure that the operation has been performed. We follow the at least once semantic. The client will decide if to perform again the write or not. |





```
    //fill some caches with the item 1
    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    //fill some caches with the item 1
    LOGGER.info("Client 304 performs a read request");
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crash before applying the refill");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_REFILL), ActorRef.noSender());

    LOGGER.info("Client 300 performs a write request");
    clientList.get(0).tell(new DoWriteMsg(1, 5), ActorRef.noSender());
```

## Write crash 6

| Crash                                                                                                              | CrashMsg      | Description                                                                                                                                                                                                                          |
|--------------------------------------------------------------------------------------------------------------------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash before applying the refill to the saved items of the L1 cache not in the path between the client and the DB. | BEFORE_REFILL | The client will receive the confirmations of the write operation. The L2 caches connected with the crashed L1 cache will not see the update but will eventually get update when their parent recovers or when they detect the crash. |

```
    //fill some caches with the item 1
    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    //fill some caches with the item 1
    LOGGER.info("Client 304 performs a read request");
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache101 crash before applying the refill");
    l1List.get(1).tell(new CrashMsg(CrashType.BEFORE_REFILL), ActorRef.noSender());

    LOGGER.info("Client 300 performs a write request");
    clientList.get(0).tell(new DoWriteMsg(1, 5), ActorRef.noSender());
```

## Write Crash 7

| Crash                                                                                                                     | CrashMsg      | Description                                                                                                                                                      |
|---------------------------------------------------------------------------------------------------------------------------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash before applying the refill to the saved items of the L2 cache connected to the client which originated the request. | BEFORE_REFILL | The client thanks to the timeout will detect the crash of the L2 cache and choose another parent. It cannot be sure that its write has been correctly performed. |

```
    //fill some caches with the item 1
    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    //fill some caches with the item 1
    LOGGER.info("Client 304 performs a read request");
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache200 crash before applying the refill");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_REFILL), ActorRef.noSender());

    LOGGER.info("Client 300 performs a write request");
    clientList.get(0).tell(new DoWriteMsg(1, 5), ActorRef.noSender());
```

## Write Crash 8

| Crash                                                                     | CrashMsg             | Description                                                                                                                                                      |
|---------------------------------------------------------------------------|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L2 Cache crash before sending the confirmation message to the client. | BEFORE_WRITE_CONFIRM | The client thanks to the timeout will detect the crash of the L2 cache and choose another parent. It cannot be sure that its write has been correctly performed. |

```
    //fill some caches with the item 1
    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    //fill some caches with the item 1
    LOGGER.info("Client 304 performs a read request");
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache200 crash before sending the confirmation to the client");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_WRITE_CONFIRM), ActorRef.noSender());

    LOGGER.info("Client 300 performs a write request");
    clientList.get(0).tell(new DoWriteMsg(1, 5), ActorRef.noSender());
```



