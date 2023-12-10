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
{
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
}
```

1) character:new
```json
{
  "type": "character:new",
  "character": {
    "id": 123,
    "userId": 456,
    "sessionId": 789,
    "avatarId": 987,
    "isDefeated": false,
    "basicProperties": {
      "strength": 10,
      "dexterity": 15,
      "constitution": 12,
      "intelligence": 8,
      "wisdom": 14,
      "charisma": 16
    },
    "properties": [
      {
        "name": "abc",
        "value": 100
      }
    ]
  },
  "own": true
}

```

2) character:leave
```json
{
  "type": "character:leave",
  "id": 123
}
```

2) character:move
```json
{
  "type": "character:move",
  "id": 123,
  "character": {
    "id": 123,
    "userId": 456,
    "sessionId": 789,
    "avatarId": 987,
    "isDefeated": false,
    "basicProperties": {
      "strength": 10,
      "dexterity": 15,
      "constitution": 12,
      "intelligence": 8,
      "wisdom": 14,
      "charisma": 16
    },
    "properties": [
      {
        "name": "abc",
        "value": 100
      }
    ]
  }
}

```

3) character:attack
```json
{
  "type": "character:attack",
  "attackType": "melee",
  "character": {
    "id": 123,
    "userId": 456,
    "sessionId": 789,
    "avatarId": 987,
    "isDefeated": false,
    "basicProperties": {
      "strength": 10,
      "dexterity": 15,
      "constitution": 12,
      "intelligence": 8,
      "wisdom": 14,
      "charisma": 16
    },
    "properties": [
      {
        "name": "abc",
        "value": 80
      }
    ]
  },
  "opponent": {
    "id": 456,
    "userId": 789,
    "sessionId": 1011,
    "avatarId": 1213,
    "isDefeated": false,
    "basicProperties": {
      "strength": 8,
      "dexterity": 12,
      "constitution": 10,
      "intelligence": 15,
      "wisdom": 13,
      "charisma": 14
    },
    "properties": [
      {
        "name": "dcb",
        "value": 70
      }
    ]
  }
}
```

4) character:status
```json
{
  "type": "character:status",
  "id": 123,
  "can_do_action": true
}
```