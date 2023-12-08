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

### POST /api/register

#### Request Body:
- 

#### Example Request:
```bash

```
#### Status Codes:
- 

#### Example Response:
```json

```

### POST /api/login

#### Request Body:
- 

#### Example Request:
```bash

```
#### Status Codes:
- 

#### Example Response:
```json

```

### POST /api/logout

#### Request Body:
- 

#### Example Request:
```bash

```
#### Status Codes:
- 

#### Example Response:
```json

```

### POST /api/edit/{userId}

#### Request Body:
- 

#### Example Request:
```bash

```
#### Status Codes:
- 

#### Example Response:
```json

```

### GET /api/users

#### Request Body:
- 

#### Example Request:
```bash

```
#### Status Codes:
- 

#### Example Response:
```json

```

### GET /api/{userId}/sessions

#### Request Body:
- 

#### Example Request:
```bash

```
#### Status Codes:
- 

#### Example Response:
```json

```