# Exceptions Documentation

## From client



## From server about Exceptions

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

###  ActionException

#### NotYourTurn
This error occurs when attempting to perform an action, but it's not the turn of the character associated with the action.

Example Error Message: "Can't do action: not your turn now"

```json
{
    "type": "error",
    "on": "character:move",
    "attackType": "melee",
    "reason": "not_your_turn",
    "message": "Can't do action: not your turn now"
}

```

#### IsDefeated
This error occurs when attempting to perform an action, but the character is defeated.

Example Error Message: "Can't do action: character is defeated"

```json
{
    "type": "error",
    "on": "character:move",
    "attackType": "melee",
    "reason": "is_defeated",
    "message": "Can't move: character is defeated"
}
```

###  MoveException

#### BigDist
This error occurs when attempting to move a character, but the target tile is too far from the current position based on the character's SPEED property.

Example Error Message: "Can't move: target tile is too far"

```json
{
  "type": "error",
  "on": "character:move",
  "reason": "big_dist",
  "message": "Can't move: target tile is too far"
}
```

#### TileObstacle
This error occurs when attempting to move a character, but the target tile is an obstacle, preventing movement.

Example Error Message: "Can't move: target tile is obstacle"

```json
{
  "type": "error",
  "on": "character:move",
  "reason": "tile_obstacle",
  "message": "Can't move: target tile is obstacle"
}

```

###  AttackException

#### BigDist
This error occurs when attempting to perform a melee, ranged, or magic attack, but the target is too far away.

Example Error Message: "Can't attack: too far for {attackType} attack"

```json
{
  "type": "error",
  "on": "character:attack",
  "attackType": "melee",
  "reason": "big_dist",
  "message": "Can't attack: too far for melee attack"
}
```

#### LowMana
This error occurs when attempting to perform a magic attack, but the character has insufficient mana.

Example Error Message: "Can't attack: too low mana for {attackType} attack"

```json
{
  "type": "error",
  "on": "character:attack",
  "attackType": "melee",
  "reason": "big_dist",
  "message": "Can't attack: too far for melee attack"
}
```