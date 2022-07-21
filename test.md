# READ

## Read Crash 1

| Crash                                          | CrashMsg           | Description                                                                           |
|------------------------------------------------|--------------------|---------------------------------------------------------------------------------------|
| Crash of L2 before forwarding the read request | BEFORE_READ_REQ_FW | The Client detect the crash of the L2 cache and choose another L2 cache as its parent |

```
    LOGGER.info("Cache200 crashes before forwarding the read request");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_READ_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
```
## Read Crash 2

| Crash                                         | CrashMsg          | Description                                                                                                                                                                        |
|-----------------------------------------------|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash of L2 after forwarding the read request | AFTER_READ_REQ_FW | The Client detect the crash of the L2 cache and choose another L2 cache as its parent. The L1 cache will store the value because the request has succesfully reached the database. |

```
    LOGGER.info("Cache200 crashes after forwarding the read request");
    l2List.get(0).tell(new CrashMsg(CrashType.AFTER_READ_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
```


## Read Crash 3

| Crash                                          | CrashMsg           | Description                                                                                                                                                                                                      |
|------------------------------------------------|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash of L1 before forwarding the read request | BEFORE_READ_REQ_FW | The L2 cache will detect the crash of its parent and will connect directly with the database which will update its children list. The client will receive an error message and will not change the parent cache. |

```
    LOGGER.info("Cache100 crashes before forwarding the read request");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_READ_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
```

## Read Crash 4

| Crash                                         | CrashMsg          | Description                                                                                                                                                                                                      |
|-----------------------------------------------|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash of L1 after forwarding the read request | AFTER_READ_REQ_FW | The L2 cache will detect the crash of its parent and will connect directly with the database which will update its children list. The client will receive an error message and will not change the parent cache. |

```
    LOGGER.info("Cache100 crashes after forwarding the read request");
    l1List.get(0).tell(new CrashMsg(CrashType.AFTER_READ_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
```

## Read Crash 5

| Crash                                           | CrashMsg            | Description                                                                                                                                                                                                      |
|-------------------------------------------------|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash of L1 before forwarding the read response | BEFORE_READ_RESP_FW | The L2 cache will detect the crash of its parent and will connect directly with the database which will update its children list. The client will receive an error message and will not change the parent cache. |

```
    LOGGER.info("Cache100 crashes before forwarding the read request");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_READ_RESP_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender())
```

## Read Crash 6

| Crash                                           | CrashMsg            | Description                                                                                                                                                                        |
|-------------------------------------------------|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash of L2 before forwarding the read response | BEFORE_READ_RESP_FW | The Client detect the crash of the L2 cache and choose another L2 cache as its parent. The L1 cache will store the value because the request has succesfully reached the database. |

```
    LOGGER.info("Cache200 crashes before forwarding the read request");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_READ_RESP_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
```

## Read Crash 7

| Crash                                                                       | CrashMsg         | Description                                                                            |
|-----------------------------------------------------------------------------|------------------|----------------------------------------------------------------------------------------|
| Crash of L2 before responding to a client read request with the cached item | BEFORE_READ_RESP | The Client detect the crash of the L2 cache and choose another L2 cache as its parent. |

```
    LOGGER.info("Cache200 crashes before forwarding the read request");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_READ_RESP), ActorRef.noSender());

    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
```

## Read Crash 8

| Crash                                                                       | CrashMsg         | Description                                                                                                                                                                                                      |
|-----------------------------------------------------------------------------|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Crash of L1 before responding to a client read request with the cached item | BEFORE_READ_RESP | The L2 cache will detect the crash of its parent and will connect directly with the database which will update its children list. The client will receive an error message and will not change the parent cache. |

```
    LOGGER.info("Cache202 performs a read request");
    clientList.get(2).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crashes before responding to a read request with the cached item");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_READ_RESP), ActorRef.noSender());

    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
```



# Write

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

| Crash                                                                                  | CrashMsg                 | Description                                                                                                                                                                                                                                                     |
|----------------------------------------------------------------------------------------|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| L1 cache crashes while multicasting the refill to the L2 caches that are its children  | DURING_REFILL_MULTICAST  | It depends on the order in which the children are saved. If the L2 cache in the path between the originator and the db is not filled, then the client will not receive the write confirmation, but instead an error. Otherwise it will receive the confirmation |

```
        //fill some caches with the item 1
    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());

    //fill some caches with the item 1
    LOGGER.info("Client 302 performs a read request");
    clientList.get(2).tell(new DoReadMsg(1), ActorRef.noSender());

    //fill some caches with the item 1
    LOGGER.info("Client 304 performs a read request");
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crash while multicasting the refill");
    l1List.get(0).tell(new CrashDuringMulticastMsg(CrashType.DURING_REFILL_MULTICAST, 1), ActorRef.noSender());

    LOGGER.info("Client 302 performs a write request");
    clientList.get(2).tell(new DoWriteMsg(1, 5), ActorRef.noSender());
```

```
        //fill some caches with the item 1
    LOGGER.info("Client 300 performs a read request");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());

    //fill some caches with the item 1
    LOGGER.info("Client 302 performs a read request");
    clientList.get(2).tell(new DoReadMsg(1), ActorRef.noSender());

    //fill some caches with the item 1
    LOGGER.info("Client 304 performs a read request");
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crash while multicasting the refill");
    l1List.get(0).tell(new CrashDuringMulticastMsg(CrashType.DURING_REFILL_MULTICAST, 1), ActorRef.noSender());

    LOGGER.info("Client 300 performs a write request");
    clientList.get(0).tell(new DoWriteMsg(1, 5), ActorRef.noSender());
```


## Write Crash 9

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

# CRITICAL READ

## Critical Read Crash 1
| Crash                                                            | CrashMsg                | Description                                                                                              |
|------------------------------------------------------------------|-------------------------|----------------------------------------------------------------------------------------------------------|
| The L2 Cache crashes before forwarding the Critical Read Request | BEFORE_CRIT_READ_REQ_FW | The client, thanks to the timeout, will detect the crash of the L2 cache and will choose another parent. |

```
    LOGGER.info("Cache200 crashes before forwarding the critical read request");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_CRIT_READ_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical read request");
    clientList.get(0).tell(new DoCritReadMsg(1), ActorRef.noSender());
```

## Critical Read Crash 2

| Crash                                             | CrashMsg               | Description                                                                                              |
|---------------------------------------------------|------------------------|----------------------------------------------------------------------------------------------------------|
| The L2 Cache crashes after forwarding the request | AFTER_CRIT_READ_REQ_FW | The client, thanks to the timeout, will detect the crash of the L2 cache and will choose another parent. |


```
    LOGGER.info("Cache200 crashes before forwarding the critical read request");
    l2List.get(0).tell(new CrashMsg(CrashType.AFTER_CRIT_READ_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical read request");
    clientList.get(0).tell(new DoCritReadMsg(1), ActorRef.noSender());
```

## Critical Read Crash 3

| Crash                                                            | CrashMsg                | Description                                                                                                                                                             |
|------------------------------------------------------------------|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 Cache crashes before forwarding the Critical Read Request | BEFORE_CRIT_READ_REQ_FW | The L2 cache, thanks to the timeout, will detect the crash of the L1 cache and will connect directly to the database. An error message will also be sent to the client. |

```
    LOGGER.info("Cache100 crashes before forwarding the critical read request");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_CRIT_READ_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical read request");
    clientList.get(0).tell(new DoCritReadMsg(1), ActorRef.noSender());
```

## Critical Read Crash 4

| Crash                                                           | CrashMsg               | Description                                                                                                                                                             |
|-----------------------------------------------------------------|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 Cache crashes after forwarding the Critical Read Request | AFTER_CRIT_READ_REQ_FW | The L2 cache, thanks to the timeout, will detect the crash of the L1 cache and will connect directly to the database. An error message will also be sent to the client. |

```
    LOGGER.info("Cache100 crashes after forwarding the critical read request");
    l1List.get(0).tell(new CrashMsg(CrashType.AFTER_CRIT_READ_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical read request");
    clientList.get(0).tell(new DoCritReadMsg(1), ActorRef.noSender());
```

## Critical Read Crash 5

| Crash                                                             | CrashMsg                 | Description                                                                                                                                                             |
|-------------------------------------------------------------------|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 Cache crashes before forwarding the Critical Read Response | BEFORE_CRIT_READ_RESP_FW | The L2 cache, thanks to the timeout, will detect the crash of the L1 cache and will connect directly to the database. An error message will also be sent to the client. |

```
    LOGGER.info("Cache100 crashes before forwarding the critical read response");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_CRIT_READ_RESP_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical read request");
    clientList.get(0).tell(new DoCritReadMsg(1), ActorRef.noSender());
```

## Critical Read Crash 6

| Crash                                                             | CrashMsg                 | Description                                                                                             |
|-------------------------------------------------------------------|--------------------------|---------------------------------------------------------------------------------------------------------|
| The L2 Cache crashes before forwarding the Critical Read Response | BEFORE_CRIT_READ_RESP_FW | The client, thanks to the timeout, will detect the crash of the L2 cache and will choose another parent |

```
    LOGGER.info("Cache100 crashes before forwarding the critical read response");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_CRIT_READ_RESP_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical read request");
    clientList.get(0).tell(new DoCritReadMsg(1), ActorRef.noSender());
```

# Critical Write

## Critical Write Crash 1

| Crash                                                             | CrashMsg                 | Description                                                                                              |
|-------------------------------------------------------------------|--------------------------|----------------------------------------------------------------------------------------------------------|
| The L2 Cache crashes before forwarding the Critical Write Request | BEFORE_CRIT_WRITE_REQ_FW | The client, thanks to the timeout, will detect the crash of the L2 cache and will choose another parent. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache200 crashes before forwarding the critical write request");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_CRIT_WRITE_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 2

| Crash                                                            | CrashMsg                | Description                                                                                                                                                                             |
|------------------------------------------------------------------|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L2 Cache crashes after forwarding the Critical Write Request | AFTER_CRIT_WRITE_REQ_FW | The client detect the crash of the L2 cache and will choose another cache as its parent. However the request will reach the database which will perform the Critical Write as expected. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache200 crashes after forwarding the critical write request");
    l2List.get(0).tell(new CrashMsg(CrashType.AFTER_CRIT_WRITE_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 3

| Crash                                                             | CrashMsg                 | Description                                                                                                                                                             |
|-------------------------------------------------------------------|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 Cache crashes before forwarding the Critical Write Request | BEFORE_CRIT_WRITE_REQ_FW | The L2 cache, thanks to the timeout, will detect the crash of the L1 cache and will connect directly to the database. An error message will also be sent to the client. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crashes before forwarding the critical write request");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_CRIT_WRITE_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 4

| Crash                                                            | CrashMsg                | Description                                                                                                                                                                                                                                                    |
|------------------------------------------------------------------|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 Cache crashes after forwarding the Critical Write Request | AFTER_CRIT_WRITE_REQ_FW | The L2 cache will detect the crash of the L1 cache and will connect directly to the database. An error message will be sent to the client. The critical write will fail because the database cannot ensure that all the caches have the old value invalidated. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crashes after forwarding the critical write request");
    l1List.get(0).tell(new CrashMsg(CrashType.AFTER_CRIT_WRITE_REQ_FW), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 5

| Crash                                                                                           | CrashMsg                 | Description                                                                                                                                                                                                                                                    |
|-------------------------------------------------------------------------------------------------|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 cache in the path between the client and the database crashes before invalidate the item | BEFORE_ITEM_INVALIDATION | The L2 cache will detect the crash of the L1 cache and will connect directly to the database. An error message will be sent to the client. The critical write will fail because the database cannot ensure that all the caches have the old value invalidated. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crashes before receiving the invalidation item message");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_ITEM_INVALIDATION), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 6

| Crash                                                                                               | CrashMsg                 | Description                                                                                                                                                                                                                                                                                  |
|-----------------------------------------------------------------------------------------------------|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 cache not in the path between the client and the database crashes before invalidate the item | BEFORE_ITEM_INVALIDATION | The database will timeout while waiting for the invalidation item response and will abort the transaction sending an error to all its children. The error message will be propagated from the alive L1 cache to the alive L2 cache and will also reach the client which started the process. |

```
        LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache101 crashes before receiving the invalidation item message");
    l1List.get(1).tell(new CrashMsg(CrashType.BEFORE_ITEM_INVALIDATION), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 7

| Crash                                                                                           | CrashMsg                 | Description                                                                                                                                                                                                            |
|-------------------------------------------------------------------------------------------------|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L2 cache in the path between the client and the database crashes before invalidate the item | BEFORE_ITEM_INVALIDATION | The database will perform the critical write but the client will not receive the confirmation and will timeout. The client will choose another parent but has no clue if its critical write has been performed or not. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache200 crashes before receiving the invalidation item message");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_ITEM_INVALIDATION), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 8

| Crash                                                                                               | CrashMsg                 | Description                                                                                |
|-----------------------------------------------------------------------------------------------------|--------------------------|--------------------------------------------------------------------------------------------|
| The L2 cache not in the path between the client and the database crashes before invalidate the item | BEFORE_ITEM_INVALIDATION | The database will perform the critical write and the client will receive the confirmation. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache201 crashes before receiving the invalidation item message");
    l2List.get(1).tell(new CrashMsg(CrashType.BEFORE_ITEM_INVALIDATION), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 9

| Crash                                                                                                    | CrashMsg                         | Description                                                                                                                                                                                                            |
|----------------------------------------------------------------------------------------------------------|----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L2 cache in the path between the client and the database crashes before sending the invalidation ack | BEFORE_ITEM_INVALID_CONFIRM_SEND | The database will perform the critical write but the client will not receive the confirmation and will timeout. The client will choose another parent but has no clue if its critical write has been performed or not. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache200 crashes before sending the invalidation ack message");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_ITEM_INVALID_CONFIRM_SEND), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 10

| Crash                                                                                                        | CrashMsg                         | Description                                                                                |
|--------------------------------------------------------------------------------------------------------------|----------------------------------|--------------------------------------------------------------------------------------------|
| The L2 cache not in the path between the client and the database crashes before sending the invalidation ack | BEFORE_ITEM_INVALID_CONFIRM_SEND | The database will perform the critical write and the client will receive the confirmation. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache201 crashes before sending the invalidation ack message");
    l2List.get(1).tell(new CrashMsg(CrashType.BEFORE_ITEM_INVALID_CONFIRM_SEND), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 11

| Crash                                                                                                    | CrashMsg                         | Description                                                                                                                                                                                                                                                                                                                                                      |
|----------------------------------------------------------------------------------------------------------|----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 cache in the path between the client and the database crashes before sending the invalidation ack | BEFORE_ITEM_INVALID_CONFIRM_SEND | The L2 cache will detect the crash of the L1 cache and will connect directly to the database. An error message will be sent to the client and the item will be removed from the cache which timed out while waiting for the critical refill. The critical write will fail because the database cannot ensure that all the caches have the old value invalidated. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crashes before sending the invalidation ack message");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_ITEM_INVALID_CONFIRM_SEND), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 12

| Crash                                                                                                        | CrashMsg                         | Description                                                                                                                                                                                                                                                                                  |
|--------------------------------------------------------------------------------------------------------------|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 cache not in the path between the client and the database crashes before sending the invalidation ack | BEFORE_ITEM_INVALID_CONFIRM_SEND | The database will timeout while waiting for the invalidation item response and will abort the transaction sending an error to all its children. The error message will be propagated from the alive L1 cache to the alive L2 cache and will also reach the client which started the process. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache101 crashes before sending the invalidation ack message");
    l1List.get(1).tell(new CrashMsg(CrashType.BEFORE_ITEM_INVALID_CONFIRM_SEND), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 13

| Crash                                                                                                      | CrashMsg                         | Description                                                                                                                                                                                                                                                                                                                                                      |
|------------------------------------------------------------------------------------------------------------|----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 cache in the path between the client and the database crashes before receiving the invalidation ack | BEFORE_ITEM_INVALID_CONFIRM_RESP | The L2 cache will detect the crash of the L1 cache and will connect directly to the database. An error message will be sent to the client and the item will be removed from the cache which timed out while waiting for the critical refill. The critical write will fail because the database cannot ensure that all the caches have the old value invalidated. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crashes before receiving the invalidation ack response from the L2 caches");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_ITEM_INVALID_CONFIRM_RESP), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 14

| Crash                                                                                                      | CrashMsg                         | Description                                                                                                                                                                                                                                                                                  |
|------------------------------------------------------------------------------------------------------------|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 cache in the path between the client and the database crashes before receiving the invalidation ack | BEFORE_ITEM_INVALID_CONFIRM_RESP | The database will timeout while waiting for the invalidation item response and will abort the transaction sending an error to all its children. The error message will be propagated from the alive L1 cache to the alive L2 cache and will also reach the client which started the process. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache101 crashes before receiving the invalidation ack response from the L2 caches");
    l1List.get(1).tell(new CrashMsg(CrashType.BEFORE_ITEM_INVALID_CONFIRM_RESP), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 15

| Crash                                                                                                     | CrashMsg           | Description                                                                                                                                                                                                                                                   |
|-----------------------------------------------------------------------------------------------------------|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 cache in the path between the client and the database crashes before receiving the refill message. | BEFORE_CRIT_REFILL | The L2 cache will timeout while waiting for the response and will connect to the database. The cache will also remove the item from its memory because it cannot ensure that the critical write has been performed. The client will receive an error message. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache100 crashes before receiving the refill message");
    l1List.get(0).tell(new CrashMsg(CrashType.BEFORE_CRIT_REFILL), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 16

| Crash                                                                                                         | CrashMsg           | Description                                                                                                                                                                                       |
|---------------------------------------------------------------------------------------------------------------|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L1 cache not in the path between the client and the database crashes before receiving the refill message. | BEFORE_CRIT_REFILL | The critical write will be performed and the client will receive the confirmations. However the L2 caches connected with the crashed L1 cache will timeout and remove the item from their memory. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache101 crashes before receiving the refill message");
    l1List.get(1).tell(new CrashMsg(CrashType.BEFORE_CRIT_REFILL), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 17

| Crash                                                                                                     | CrashMsg           | Description                                                                                                                                                                               |
|-----------------------------------------------------------------------------------------------------------|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L2 cache in the path between the client and the database crashes before receiving the refill message. | BEFORE_CRIT_REFILL | The client will timeout and choose another parent. It will have no clue about the result of the critical write operation. The other cache will have the new value stored in their memory. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache200 crashes before receiving the refill message");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_CRIT_REFILL), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 18

| Crash                                                                                                         | CrashMsg           | Description                               |
|---------------------------------------------------------------------------------------------------------------|--------------------|-------------------------------------------|
| The L2 cache not in the path between the client and the database crashes before receiving the refill message. | BEFORE_CRIT_REFILL | The client will receive the confirmation. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache201 crashes before receiving the refill message");
    l2List.get(1).tell(new CrashMsg(CrashType.BEFORE_CRIT_REFILL), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash 19

| Crash                                                                                                         | CrashMsg                  | Description                                                                                                                                                                               |
|---------------------------------------------------------------------------------------------------------------|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| The L2 cache in the path between the client and the database crashes before sending the confirmation message. | BEFORE_CRIT_WRITE_CONFIRM | The client will timeout and choose another parent. It will have no clue about the result of the critical write operation. The other cache will have the new value stored in their memory. |

```
    LOGGER.info("Filling up some caches with item 1");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(3).tell(new DoReadMsg(1), ActorRef.noSender());
    clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    inputContinue();

    LOGGER.info("Cache200 crashes before receiving the refill message");
    l2List.get(0).tell(new CrashMsg(CrashType.BEFORE_CRIT_WRITE_CONFIRM), ActorRef.noSender());

    LOGGER.info("Client 300 performs a critical write request");
    clientList.get(0).tell(new DoCritWriteMsg(1, 5), ActorRef.noSender());
```

## Critical Write Crash N

| Crash | CrashMsg | Description |
|-------|----------|-------------|
|       |          |             |

```

```


























































