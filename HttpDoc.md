# Http Documentation

### POST /api/game/create
Create a new game session based on the provided mapId.

#### Request Query Parameters:
- mapId (UInt, required): ID of the map for the session.

#### Example Request:
```bash
POST /api/game/create?mapId=123 HTTP/1.1
```
#### Status Codes:
- 200 OK: Successful creation of a new game session.
- 400 Bad Request: Validation checks failed (e.g. missing or invalid parameters).

#### Example Response:
```json
{
  "type": "ok",
  "message": "Session created",
  "result": {
    "mapID": 123u,
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
#### Status Codes:
- 200 OK: Successful retrieval of textures.
- 400 Bad Request: Validation checks failed.

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

#### Request Body:
- {id} (UInt): Texture ID.

#### Example Request:
```bash
GET /api/textures/1 HTTP/1.1
```
#### Status Codes:
- 200 OK: Successful retrieval of the specified texture.
- 400 Bad Request: Texture with the given ID does not exist.

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
#### Status Codes:
- 200 OK: Successful retrieval of the list of tilesets.
- 400 Bad Request: Validation checks failed.

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


### GET /api/maps

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

### GET /api/maps/{id}

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