# WebSocket Documentation

### /api/connect/{userId}/{sessionId}
Connect to WebSocket for real-time communication.

#### Parameters:
- {userId} (UInt): User ID.
- {sessionId} (UInt): Session ID.

#### Example Request:
```bash
ws/api/connect/1/2
```

#### Example Responses:

CharacterInfo:
```json
"id": <UInt>,
"basicProperties": 
{
    "strength": <Int, 0>,
    ...
}
"properties": [
{
    "name": <String>,
    "value": <Int>
}
...
]
```

1) character creation
```json
{
  "type": "character:new",
  "id": <UInt>,
  "name": "Dovakin",
  "row": <UInt>,
  "col": <UInt>,
  "own": <bool, required>, <CharacterInfo>
}
```
2) character removal
```json
{
  "type": "character:remove",
  "id": <UInt>
}
```
3) character movement
```json
{
  "type": "character:move",
  "id": <UInt>,
  "row": <UInt>,
  "col": <UInt>
}
```
4) character attack
```json
{
  "type": "character:attack",
  "id": <UInt>,
  "opponentId": <UInt>,
  "attackType": "melee" (or "ranged" or "magic")
}
```
5) character revival
```json
{
  "type": "character:revive",
  "id": <UInt>
}
```