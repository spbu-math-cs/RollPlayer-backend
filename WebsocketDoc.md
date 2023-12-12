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

### Messages from client:

1) character creation
```
{
  "type": "character:new",
  "id": <UInt, required>,
  "name": <String, opt, "Dovahkiin">, 
  "row": <Int, opt, 0>,
  "col": <Int, opt, 0>,
  "own": <bool, required>,
  "basicProperties": <bool, opt, {}>,
  "avatarId": <UInt, opt, null>
}
```
2) character removal
```
{
  "type": "character:remove",
  "id": <UInt, required>
}
```
3) character movement
```
{
  "type": "character:move",
  "id": <Int, required>,
  "row": <Int, required>,
  "col": <Int, required>
}
```
4) character attack
```
{
  "type": "character:attack",
  "attackType": "melee"/"ranged"/"magic",
  "id": <UInt, required>,
  "opponentId": <UInt, required>,
}
```
5) character revival
```
{
  "type": "character:revive",
  "id": <Int, required>
}
```

### Messages from server

CharacterInfo:
```
{
    "id": <UInt>,
    "userId": <UInt>,
    "sessionId": <UInt>,
    "name": <String>,
    "row": <Int>,
    "col": <Int>,
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
        ...
    ]
}
```

1) character:new
```
{
  "type": "character:new",
  "character": <CharacterInfo>,
  "own": <Boolean>
}

```

2) character:leave
```
{
  "type": "character:leave",
  "id": <UInt>
}
```

3) character:move
```
{
  "type": "character:move",
  "id": <UInt>,
  "character": <CharacterInfo>
}

```

4) character:attack
```
{
  "type": "character:attack",
  "attackType": "melee"/"ranged"/"magic",
  "character": <CharacterInfo>,
  "opponent": <CharacterInfo>
}
```

5) character:status
```
{
  "type": "character:status",
  "id": <UInt>,
  "can_do_action": <opt, Boolean>,
  "is_defeated": <opt, Boolean>,
  "character": <opt, CharacterInfo>
}
```

### Messages with errors from server

Handling exceptions related to WebSocket communication.

```
{
  "type": "error",
  "on": <requestType>, 
  "message": "some exception" 
}
```

### Regular error messages

#### Action
Handling reasons of exceptions related to WebSocket communication.

```
{
  "type": "error",
  "on": <requestType>, 
  "reason": "not_your_turn"/"is_defeated"
  "message": <String>
}
```

#### Move
Handling exceptions related to character movement during WebSocket communication

```
{
  "type": "error",
  "on": "character:move",
  "reason": "big_dist"/"tile_obstacle"
  "message": <String>
}
```

#### Attack
Handling exceptions related to character attacks during WebSocket communication.

```
{
  "type": "error",
  "on": "character:attack",
  "attackType": "melee"/"ranged"/"magic",
  "reason": "big_dist"/"low_mana"
  "message": <String>
}
```
