# Http Documentation

## Pictures

### GET /api/pictures
Retrieves a list of all pictures.

#### Response example:
```json
{
  "type": "ok",
  "result": [
    {
      "id": "1",
      "filepath": ".\\pictures\\picture1.png"
    },
    {
      "id": "2",
      "filepath": ".\\pictures\\picture2.png"
    }
  ]
}
```

### GET /api/pictures/{id}
Retrieves a specific picture by ID.

#### Request Parameters:
- {id} (UInt): Picture ID.

#### Response example:
```json
{
  "type": "ok",
  "result": {
    "filepath": ".\\pictures\\picture1.png",
    "id": "1"
  }
}
```

### POST /api/pictures
Uploads a new picture.

#### Body:
Picture as bytes

#### Response example:
```json
{
  "type": "ok",
  "result": {
    "id": "1",
    "filepath": ".\\pictures\\picture1.png"
  }
}
```


## Maps

### GET /api/textures
Get a list of all textures.

#### Response example:
```json
{
  "type": "ok",
  "result": [
    {"filepath": ".\\textures\\tileset_packed.png", "id": "1"}
  ]
}
```

### GET /api/textures/{id}
Get a specific texture by ID.

#### Request Parameters:
- {id} (UInt): Texture ID.

#### Response example:
```json
{
  "type": "ok",
  "result": {
    "filepath": ".\\textures\\texture1.png",
    "id": "1"
  }
}
```

### GET /api/tilesets
Get a list of all tilesets.

#### Response example:
```json
{
  "type": "ok",
  "result": 
  [
    {
      "id": "1",
      "filepath": ".\\tilesets\\tileset1.json"
    }
  ]
}
```

### GET /api/tilesets/{id}
Get a specific tileset by ID.

#### Request Parameters:
- {id} (UInt): Tileset ID.

#### Response example:
```json
{
  "type": "ok",
  "result": {
    "id": "1",
    "filepath": ".\\tilesets\\tileset1.json"
  }
}
```

### GET /api/maps
Get a list of all maps.

#### Response example:
```json
{
  "type": "ok",
  "result": [
    {
      "id": "1",
      "filepath": ".\\maps\\map1.json"
    },
    {
      "id": "2",
      "filepath": ".\\maps\\map2.json"
    }
  ]
}
```

### GET /api/maps/{id}
Get a specific map by ID.

#### Request Parameters:
- {id} (UInt): Map ID.

#### Response example:
```json
{
  "type": "ok",
  "result": {
    "id": "1",
    "filepath": ".\\maps\\map1.json"
  }
}
```


## User

### POST /api/register
Registers a new user with the provided login, email and password.

#### Body:
Json:
- "login": String
- "email": String
- "password": String

#### Example Response:
```json
{
  "type": "ok",
  "message": "User 1 registered successfully",
  "result": {
    "id": "1",
    "login": "newuser",
    "email": "newuser@example.com",
    "password": "password123",
    "avatarId": null
  }
}
```

### POST /api/login
Logs in a user with the provided login/email and password and return jwt token.

#### Body:
Json:
- "login": String
- "email": String
- "password": String

#### Response example:
```json
{
  "type": "ok",
  "message": "User 1 logged in successfully",
  "result": "kvdnjnonv;kvsv"
}
```

### POST /api/logout (authorization)
Logs out a user.

#### Response example:
```json
{
  "type": "ok",
  "message": "User 1 logged out successfully"
}
```

### POST /api/user/edit (authorization)
Edits user data.

#### Response example:
```json
{
  "type": "ok",
  "message": "Data for user 1 edited successfully",
  "result": {
    "id": "1",
    "login": "newlogin",
    "email": "newemail@example.com",
    "password": "newpassword",
    "avatarId": null
  }
}
```

### GET /api/user/sessions (authorization)
Retrieves a list of sessions associated with a specific user.

#### Response example:
```json
{
  "type": "ok",
  "result": [
    {
      "id": "1",
      "mapID": "1",
      "active": true,
      "started": "2023-01-01T00:00:00Z"
    },
    {
      "id": "2",
      "mapID": "2",
      "active": false,
      "started": "2023-01-02T00:00:00Z"
    }
  ]
}
```

### GET /api/users (now not safety)
Retrieves a list of all users.

#### Response example:
```json
{
  "type": "ok",
  "result": [
    {
      "id": "1",
      "login": "user1",
      "email": "user1@example.com",
      "password": "password1"
    },
    {
      "id": "2",
      "login": "user2",
      "email": "user2@example.com",
      "password": "password2"
    }
  ]
}
```


## Game Session

### POST /api/game/create
Create a new game session based on the provided mapId.

#### Request Query Parameters:
- mapId (UInt, required): ID of the map for the session.

#### Response example:
```json
{
  "type": "ok",
  "message": "Session created",
  "result": {
    "id": 1,
    "mapID": 123,
    "active": false,
    "started": "2023-11-28T12:34:56Z"
  }
}
```

### GET /api/game/{sessionId}/mapId
Retrieves the map ID associated with a specific game session.

#### Request Parameters:
- {sessionId} (UInt): Session ID.

#### Response example:
```json
{
  "type": "ok",
  "result": "1"
}
```
