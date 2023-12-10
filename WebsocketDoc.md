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

#### Response types:

1) character creation
```json
{
  "type": "character:new",
  "id": <UInt, required>,
  "name": <String, opt, "Dovakin" = default>, 
  "row": <Int, opt, 0 = default>,
  "col": <Int, opt, 0 = default>,
  "own": <bool, required>,
  "basicProperties": <bool, {} = default>,
  "avatarId": <UInt, required>
}
```
2) character removal
```json
{
  "type": "character:remove",
  "id": <UInt, required>
}
```
3) character movement
```json
{
  "type": "character:move",
  "id": <Int, required>,
  "row": <Int>,
  "col": <Int>
}
```
4) character attack
```json
{
  "type": "character:attack",
  "id": <Int, required>,
  "opponentId": <Int, required>,
  "attackType": <String, required>
}
```
5) character revival
```json
{
  "type": "character:revive",
  "id": <Int, required>
}
```

#### Responses from server

CharacterInfo:
```json
"id": <UInt>,
"userId": <UInt>,
"sessionId": <UInt>,
"avatarId": <UInt>
"isDefeated": <Boolean>
"basicProperties": 
    {
      "strength": <Int, 0 = default>,
      "dexterity": <Int, 0 = default>,
      "constitution": <Int, 0 = default>,
      "intelligence": <Int, 0 = default>,
      "wisdom": <Int, 0 = default>,
      "charisma": <Int, 0 = default>
    }
"properties": [
    {
        "name": <String>,
        "value": <Int>,
    }
]
```
1) character creation
```json
{
  "type": "character:new",
  "id": 123,
  "name": "Sechenov",
  "row": 2,
  "col": 3,
  "own": true,
  "basicProperties": {
    "strength": 50,
    "dexterity": 100,
    "constitution": 20,
    "intelligence": 1000,
    "wisdom": 1000,
    "charisma": 999
  },
  "avatarId": 456
}
```
2) character removal
```json
{
  "type": "character:remove",
  "id": 123
}
```
3) character movement
```json
{
  "type": "character:move",
  "id": 123,
  "row": 4,
  "col": 5
}
```
4) character attack
```json
{
  "type": "character:attack",
  "id": 123,
  "opponentId": 456,
  "attackType": "melee"
}
```
5) character revival
```json
{
  "type": "character:revive",
  "id": 123
}
```