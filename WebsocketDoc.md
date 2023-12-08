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
  "name": "Dovakin",
  "row": 2,
  "col": 3,
  <CharacterInfo>
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