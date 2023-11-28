# API Documentation

## WebSocket

_### /api/connect/{userId}/{sessionId}
Connect to WebSocket for real-time communication.

Parameters:
- {userId}: User ID for identifying the user
- {sessionId}: Session ID for identifying the session

Example request:
```bash
/ws/api/connect/1/2
```
Status Codes:
- 200 OK: Successful WebSocket connection initiation.
- 400 Bad Request: Invalid userId or sessionId (must be UInt) or user or session does not exist.

## Http

### POST /api/game/create
Create a new game session based on the provided mapId.

Request Body:
- mapId: ID of the map for the session.

Example Request:
```bash
POST /api/game/create?mapId=123 HTTP/1.1
```
Status Codes:
- 200 OK: Successful creation of a new game session.
- 400 Bad Request: Validation checks failed (e.g., missing or invalid parameters).

Example Response:
```json
HTTP/1.1 200 OK
Content-Type: application/json
{
    "type": "ok",
    "message": "Session created",
    "result": {
        "mapID": 123u,
        "active": false,
        "started": 2023-11-28T12:34:56Z,
        "whoCanMove": -1,
    }
}
```

### GET /api/textures
Get a list of all textures.

Example Response:
HTTP/1.1 200 OK
Content-Type: application/json

{
    "type": "ok",
    "result": [
        {"id": "1", "filepath": "/textures/texture1.png"},
        {"id": "2", "filepath": "/textures/texture2.png"}
    ]
}

### GET /api/textures/{id}
Get a specific texture by ID.

{id} – Texture ID
Tilesets API

### GET /api/tilesets
Get a list of all tilesets.

Example Response:
HTTP/1.1 200 OK
Content-Type: application/json

{
    "type": "ok",
    "result": [
        {"id": "1", "filepath": "/tilesets/tileset1.json"},
        {"id": "2", "filepath": "/tilesets/tileset2.json"}
    ]
}

### GET /api/tilesets/{id}
Get a specific tileset by ID.

{id} – Tileset ID

### GET /api/maps
Get a list of all maps.

Example Response:
HTTP/1.1 200 OK
Content-Type: application/json

{
    "type": "ok",
    "result": [
        {"id": "1", "filepath": "/maps/map1.json"},
        {"id": "2", "filepath": "/maps/map2.json"}
    ]
}

### GET /api/maps/{id}
Get a specific map by ID.

{id} – Map ID
User Authentication API

### POST /api/register
Register a new user.

Request Body:
login – User login
email – User email
password – User password

### POST /api/login
Login an existing user.

Request Body:
login or email – User login or email
password – User password

### POST /api/logout
Logout the current user.

Request Body:
userId – User ID

### POST /api/edit/{userId}
Edit user information.

{userId} – User ID
Request Body (Optional):
login – New user login
email – New user email
password – New user password

### GET /api/users
Get a list of all users.

### GET /api/{userId}/sessions
Get sessions associated with a specific user.

{userId} – User ID