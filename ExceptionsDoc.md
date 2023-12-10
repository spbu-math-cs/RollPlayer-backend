# Exceptions Documentation

## From client



## From server about Exceptions

### Not Regular error messages

##### HandleWebsocketIncorrectMessage
Handling exceptions related to WebSocket communication.

- Type of response:
```json
{
  "type": "error",
  "on": "character:move", 
  "message": "some exception" 
}
```

#### Regular error messages

##### SendActionExceptionReason
Handling reasons of exceptions related to WebSocket communication.
- Type of response:
```json
{
  "type": "error",
  "on": "character:attack", 
  "reason": "not_your_turn",  (or "is_defeated")
  "message": "Can't do action: not your turn now" (or "Can't move: character is defeated")
}
```

##### SendMoveExceptionReason
Handling exceptions related to character movement during WebSocket communication
- Type of response:
```json
{
  "type": "error",
  "on": "character:move",
  "reason": "big_dist", (or "tile_obstacle")
  "message": "Can't move: target tile is too far" (or "Can't move: target tile is obstacle")
}
```
##### SendAttackExceptionReason
Handling exceptions related to character attacks during WebSocket communication.
- Type of response:
```json
{
  "type": "error",
  "on": "character:attack",
  "attackType": "melee" (for example),
  "reason": "big_dist", (or "low_mana")
  "message": "Can't attack: too far for {type of attack} attack" (or "Can't attack: too low mana for {type of attack} attack")
}
```



