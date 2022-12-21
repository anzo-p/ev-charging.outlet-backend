package charger.backend.http

/*
  serve grpc

  - post - device has detected start charging { device id, consumer token details }
    - send to kinesis: device detected start charging from device id by consumer token

  - post - device has detected stop charging { device id, consumer token details }
    - send to kinesis: device detected stop charging at device id

  read kinesis
  - consumer backend has requested device id to start charging
    - push to device
    - ack to consumer backend
    - send to kinesis: billing - initiate charging session

  - consumer backend has requests device id to stop charging
    - push to device
    - ack to consumer backend
    - send to kinesis: completed charging session data

  persist in dynamodb
  - chargers
    - status
    - history aggregated to a set of chargers, paginated

 */
