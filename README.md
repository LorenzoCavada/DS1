# DS1
Project for the DS1 UniTN exam


### 05/07/2022 - Lorenzo

Modified the log4j configuration file in order to have different layoutPatterns for INFO, DEBUG and ERROR messages.

### 06/07/2022 - Lorenzo & Ale

Added timeout to client and L2 cache. Each device now have a Map attributed where the `UUID` of each request is associated to a timer. When a response is received if the timer is still going it gets cancelled.
  If the timer finish while still waiting the response it is considered as a timeout and a `timeoutMsg` is sent to themselves.

This `timeoutMsg` message will contain the request which timed out in order to be able to handle it in the best way possible.


### 07/07/2022 - Lorenzo

Modified comments of the cache comments in order to be in the JavaDoc formats. Also, I moved some methods in order to have a better understanding of the message handling.
Modified comments of the client and db comments in order to be in the JavaDoc formats. Also, I moved some methods in order to have a better understanding of the message handling.
Changed some methods which was directly using `.tell()` to send messages and, so, was not applying the network delay. Used the `sendMessage` instead.


### 08/07/2022 - Lorenzo

Added the refresh procedure to a L2 cache that detects the crash of its L1 parent. Also check the code with Alessandro

### 10/07/2022 - Lorenzo

Finished the critical read method. Also added a verbose log mode.