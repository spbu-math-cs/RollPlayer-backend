# WebSocket Documentation

### /api/connect/{userId}/{sessionId}
Connect to WebSocket for real-time communication.

#### Parameters:
- {userId} (UInt): User ID.
- {sessionId} (UInt): Session ID.

#### Example Responses:
1) character creation
```json
{
  "type": "character:new",
  "name": "Dovakin",
  "row": 2,
  "col": 3,
  "basicProperties": {
    "health": 100,
    "damage": 20
  },
  "avatarId": 1
}
```
2) character removal
```json
{
  "type": "character:remove"
}
```
3) character movement
```json
{
  "type": "character:move",
  "row": 3,
  "col": 4
}
```
4) character attack
```json
{
  "type": "character:attack",
  "attackType": "melee"
}
```
5) character revival
```json
{
  "type": "character:revive"
}
```