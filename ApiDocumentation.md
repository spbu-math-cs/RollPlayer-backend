# API Documentation

### webSocket("/api/connect/{userId}/{sessionId}")
Connect to WebSocket for real-time communication.

{userId} – User ID
{sessionId} – Session ID



### POST /api/game/create
Create a new game session.

Request Body:
mapId – ID of the map for the session
Example Request:

POST /api/game/create?mapId=123 HTTP/1.1
Host: example.com



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