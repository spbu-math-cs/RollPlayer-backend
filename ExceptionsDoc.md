# Exceptions Documentation

## From client

### Regular error messages

##### HandleHTTPRequestException
Handling exceptions that occur during regular HTTP requests.
- Type of response:
```json
{
  "type": "error",
  "message": "Failed GET /api/someEndpoint request from 192.168.1.1: Connection timed out"
}
```
##### HandleWebsocketIncorrectMessage
Handling exceptions related to WebSocket communication.
- Type of response:
```json
{
  "type": "error",
  "on": "character:move",  // Example operation/event where the failure occurred
  "message": "Invalid move request."  // Example exception message
}
```
##### SendActionExceptionReason
Handling reasons of exceptions related to WebSocket communication.
- Type of response:
```json
{
  "type": "error",
  "on": "character:attack",  // Example operation/event where the failure occurred
  "reason": "InvalidTarget",  // Example specific reason for the action failure
  "message": "Invalid attack target."  // Example exception message
}
```

#### Not regular error messages

##### SendMoveExceptionReason
Handling exceptions related to character movement during WebSocket communication
- Type of response:
```json
{
  "type": "error",
  "on": "character:move",
  "reason": "Invalid move",
  "message": "The specified move is not valid."
}
```
##### SendAttackExceptionReason
Handling exceptions related to character attacks during WebSocket communication.
- Type of response:
```json
{
  "type": "error",
  "on": "character:attack",
  "attackType": "melee",
  "reason": "Invalid target",
  "message": "The specified target for the melee attack is not valid."
}
```

## From server

