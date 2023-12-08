# Http Documentation

### POST /api/game/create
Create a new game session based on the provided mapId.

#### Request Query Parameters:
- mapId (UInt, required): ID of the map for the session.

#### Example Request:
```bash
POST /api/game/create?mapId=123 HTTP/1.1
```

#### Example Response:
```json
{
  "type": "ok",
  "message": "Session created",
  "result": {
    "mapID": 123,
    "active": false,
    "started": "2023-11-28T12:34:56Z",
    "whoCanMove": -1
  }
}
```

### GET /api/game/{sessionId}/mapId
Retrieves the map ID associated with a specific game session.

#### Request Parameters:
- {sessionId} (UInt): Session ID.

#### Example Request:
```bash
GET /api/game/1/mapId HTTP/1.1
```

#### Example Response:
```json
{
  "type": "ok",
  "result": "1"
}
```

### GET /api/textures
Get a list of all textures.

#### Example Request:
```bash
GET /api/textures HTTP/1.1
```

#### Example Response:
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

#### Example Request:
```bash
GET /api/textures/1 HTTP/1.1
```

#### Example Response:
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

#### Example Request:
```bash
GET /api/tilesets HTTP/1.1
```

#### Example Response:
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

#### Example Request:
```bash
GET /api/tilesets/1 HTTP/1.1
```

#### Example Response:
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

#### Example Request:
```bash
GET /api/maps HTTP/1.1
```

#### Example Response:
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

#### Example Request:
```bash
GET /api/maps/1 HTTP/1.1
```

#### Example Response:
```json
{
  "type": "ok",
  "result": {
    "id": "1",
    "filepath": ".\\maps\\map1.json"
  }
}
```

### GET /api/pictures
Retrieves a list of all pictures.

#### Example Request:
```bash
GET /api/pictures HTTP/1.1
```

#### Example Response:
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

#### Example Request:
```bash
GET /api/pictures/1 HTTP/1.1
```

#### Example Response:
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

#### Example Request:
```bash
POST /api/pictures HTTP/1.1
```

#### Example Response:
```json
{
  "type": "ok",
  "result": {
    "id": "1",
    "filepath": ".\\pictures\\picture1.png"
  }
}
```

### POST /api/register
Registers a new user with the provided login, email, and password.

#### Example Request:
```bash
POST /api/register HTTP/1.1
{
  "login": "newuser",
  "email": "newuser@example.com",
  "password": "password123"
}
```

#### Example Response:
```json
{
  "type": "ok",
  "message": "User 1 registered successfully",
  "result": {
    "id": "1",
    "login": "newuser",
    "email": "newuser@example.com",
    "password": "password123"
  }
}
```

### POST /api/login
Logs in a user with the provided login, email, and password.

#### Example Request:
```bash
POST /api/login HTTP/1.1
{
  "login": "newuser",
  "email": "newuser@example.com",
  "password": "password123"
}
```

#### Example Response:
```json
{
  "type": "ok",
  "message": "User 1 logged in successfully",
  "result": {
    "id": "1",
    "login": "existinguser",
    "email": "existinguser@example.com",
    "password": "password123"
  }
}
```

### POST /api/logout
Logs out a user.

#### Example Request:
```bash
POST /api/logout HTTP/1.1
{
  "userId": 1
}
```

#### Example Response:
```json
{
  "type": "ok",
  "message": "User 1 logged out successfully"
}
```

### POST /api/edit/{userId}
Edits user information, such as login, email, or password.

#### Request Parameters:
- {userId} (UInt): User ID.

#### Example Request:
```bash
POST /api/edit/1 HTTP/1.1
{
  "login": "newlogin",
  "email": "newemail@example.com",
  "password": "newpassword"
}
```

#### Example Response:
```json
{
  "type": "ok",
  "message": "Data for user 1 edited successfully",
  "result": {
    "id": "1",
    "login": "newlogin",
    "email": "newemail@example.com",
    "password": "newpassword"
  }
}
```

### GET /api/users
Retrieves a list of all users.

#### Example Request:
```bash
GET /api/users HTTP/1.1
```

#### Example Response:
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

### GET /api/{userId}/sessions
Retrieves a list of sessions associated with a specific user.

#### Request Parameters:
- {userId} (UInt): User ID.

#### Example Request:
```bash
GET /api/1/sessions HTTP/1.1
```

#### Example Response:
```json
{
  "type": "ok",
  "result": [
    {
      "id": "1",
      "mapID": "1",
      "active": true,
      "started": "2023-01-01T00:00:00Z",
      "whoCanMove": 1
    },
    {
      "id": "2",
      "mapID": "2",
      "active": false,
      "started": "2023-01-02T00:00:00Z",
      "whoCanMove": 2
    }
  ]
}

```