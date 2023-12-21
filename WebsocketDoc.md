# WebSocket Documentation

### /api/user/sessions/{sessionId}/connect (authorization)
Connect to WebSocket for real-time communication.

#### Parameters:
- {sessionId} (UInt): Session ID.

### Messages from client:

1) character creation
```
{
  "type": "character:new",
  "id": <UInt, required>,
  "name": <String, opt, "Dovahkiin">, 
  "row": <Int, opt, 0>,
  "col": <Int, opt, 0>,
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
  "id": <UInt, required>,
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
  "id": <UInt, required>
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
          "strength": <Int, 1 = default>,
          "dexterity": <Int, 1 = default>,
          "constitution": <Int, 1 = default>,
          "intelligence": <Int, 1 = default>,
          "wisdom": <Int, 1 = default>,
          "charisma": <Int, 1 = default>
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
Handling exceptions related to not defeated character actions during WebSocket communication

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
  "reason": "big_dist"/"low_mana"/"opponent_is_defeated"
  "message": <String>
}
```

#### Revive
Handling exceptions related to character revival during WebSocket communication.

```
{
  "type": "error",
  "on": "character:revive",
  "reason": "not_your_turn"/"is_not_defeated"
  "message": <String>
}
```
