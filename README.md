## EV Charging - Outlet Backend 

### A Backend service for Charging Outlet Devices

The project implements a Service that **functions as a backend to outlet devices** and **mediates charging requests between the devices and the main Application**. Users would issue commands to begin or stop charging by presenting a physical token, an RFID token to the device or by submitting these commands in a mobile app. The App Backend -project ([link](https://github.com/anzo-p/ev-charging.app-backend)) works as backend to such mobile app. These backends services would then communicate to each others via a Kinesis stream.

This Project works as playground for implementing services using Scala ZIO, into an architecture provisioned by AWS Serverless.

## 1 Main flow

1. Register Outlets
2. Register Customers (using the [App Backend](https://github.com/anzo-p/ev-charging.app-backend))
3. Issue charging commands

### 1.1 The State Machine and possible commands, ie. state transitions

- see OutletStateMachine in the project ev-charger-shared ([link](https://github.com/anzo-p/ev-charger-shared))
- see event handlers in Outlet Backend (this project) at `/outlet_backend/events`
- see respective event handlers in App Backend ([link](https://github.com/anzo-p/ev-charging.app-backend)).

### 1.2 Issuing command to begin charging with the RFID tag
1. User connects the cable between their Vehicle and the Outlet Device
2. *User presents the RFID token to the Outlet Device in order to begin charging*
3. Outlet Device would be connected to a WebSocket via AWS API Gateway, which uses an SQS queue to communicate with Outlet backend
4. __Outlet Backend forwards the request to App Backend via Kinesis stream__
5. App Backend verifies the RFID tag and user
6. App Backend ACKs back in the Kinesis stream
7. __Outlet Backend sends an event to SQS which commands the Outlet Device to begin charging__
8. *AWS API Gateway communicates this to the right device.*

Legends
- *Bold* = this service
- normal = APP Backend service ([link](https://github.com/anzo-p/ev-charging.app-backend))
- __Italics__ = I have not yet published The API Gateway project.

### 1.3 Charging from App or stopping the charging with RFID tag or App

Further details of all these flows can be observed in
- the event handlers at `/outlet_backend/events/`
- respective event handler in the App Backend -project.

## 2 Requirements
- `ev-charger-shared`-library - obtain from [link](https://github.com/anzo-p/ev-charger-shared) and publish as locally available by running `sbt publishLocal`
- AWS Serverless resources - run Terraform script *.tf from [link](https://github.com/anzo-p/ev-charging.infra) to provision the required queues, streams, and tables.

## 3 Running

### 1. Run this service as well as the App-Backend service

### 2. Register a charger outlet
Make POST request to following url with json payload

`< domain, eg. localhost >:8081/api/chargers`

```
{
    "chargerGroupId": "86c911e2-bbf3-42c8-8925-f9d380c1f329",
    "outletCode": "< eg. A123F11 >",
    "address": "< Address to outlet >",
    "maxPower": 22000,
    "outletState": "Available"
}
```

### 3 Simulate the Outlet Device message that a charging cable has been plugged in

Send the following message into SQS queue. Note that all the dates are in `ISO-8601 format`.

```
{
    "outletId": < outletId from the registered outlet above >,
    "rfidTag": "",
    "periodStart": "2023-06-13T10:15:30.123Z",
    "periodEnd": "2023-06-13T10:15:30.123Z",
    "outletStateChange": "CablePlugged",
    "powerConsumption": 0.0
}
```

### 4 Simulate presentation of RFID tag to begin charging

Send the following message into SQS queue

```
{
    "outletId": < outletId from the registered outlet above >,
    "rfidTag": < rfidTag from the registerted user above >,
    "periodStart": "2023-06-13T10:15:30.123Z",
    "periodEnd": "2023-06-13T10:15:30.123Z",
    "outletStateChange": "DeviceRequestsCharging",
    "powerConsumption": 0.0
}
```

### 5 Observe that the charging has begun in AWS resources

- DynamoDB table `ev-charging_charger-outlet_table` has the state `Charging`
- DynamoDB table `ev-charging_charging-session_table"` has a new row which also has the state `Charging`
- Kinesis stream `ev-charging_charging-events_stream` shows a roundtrip of events to and back from App Backend
- SQS queue `ev-charging_outlet-backend-to-device_queue` has a pollable message that commands the Outlet Device top begin charging.

### 6 Simulate a periodic Charging Report from the Device

Send the following message into SQS queue

```
{
    "outletId": < outletId from the registered outlet above >,
    "rfidTag": < rfidTag from the registerted user above >,
    "periodStart": "2023-06-13T10:15:30.123Z",
    "periodEnd": "2023-06-13T10:15:30.123Z",
    "outletStateChange": "Charging",
    "powerConsumption": 1.234
}
```

### 7 Observe accumulation of charging totals in AWS resources

- DynamoDB table `ev-charging_charger-outlet_table` has an increase in the field `powerConsumption`
- DynamoDB table `ev-charging_charging-session_table"` also has a similar increase in the field `powerConsumption`.

### 8 Issue further possible commands

..according to what is possible in the State machine and the event handles..
