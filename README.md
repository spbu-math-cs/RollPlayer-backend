# RollPlayer-backend
This is a Kotlin server for a multiplayer game, built using the Ktor framework. It handles communication between clients and manages game-related data. The server uses the H2 database for storing game data.

## Getting Started
Before running the server, make sure you have Kotlin and Gradle installed. To build and run the project, follow these steps:

### Clone the repository:
```bash
git clone <repository-url>
cd server-project-directory
```

### Build the project:
```bash
gradle build
```

### Run the server:
```bash
gradle run
```
The server will start running at http://localhost:9999 by default. You can change the port and other configurations in the application.conf file.

## Configuration
Port Configuration
You can configure the server port in the application.conf file located in the resources directory.

```properties
ktor {
    deployment {
        port = 9999 # Change this port number to your desired port
        host = "127.0.0.1"
    }
    # Other configurations...
}
```

## Logging Configuration
Logging is configured in the server code. Logs are written to both the console and a log file named server.log.

## Map format
To store maps we use [Tiled](https://doc.mapeditor.org/en/stable/reference/json-map-format/) json map format. For each map we have a corresponding json (`.tmj`) file, it looks something like:

```json
{
 "height":2,
 "layers":[
    {
     "data":[1, 2, 0
             10, 5, 11],
     "height":2,
     "width":3,
    }   
 ],
 "renderorder":"right-down",
 "orientation":"orthogonal",
 "tilesets":[
    {
     "firstgid":1,
     "source":"tmp.tsj"
    },
    {
     "firstgid":2,
     "source":"tmp2.tsj"
    },
    {
     "firstgid":11,
     "source":"tmp3.tsj"
    }
 ],
 "width":3
}
```

The map itself is stored in `"data"` field. If the value is `0` then this tile is empty otherwise we must find the corresponding tileset. 

`"firstgid"` field shows the first id corresponding to this tileset, `"source"` field shows which file contains information about needed texture.

`.tsj` file looks something like: 

```json
{
 "image":"/path/to/image.png",
 "imageheight":96,
 "imagewidth":96,
 "tilecount":9,
 "tileheight":32,
 "tilewidth":32
}
```

If we have only one tile in this tileset then we can render file stored in `"image"` field as a texture, otherwise we need to get the corresponding tile from the file. In that case the picture from specified file is divided into tiles with specified height and width and tiles' ids ascending from left to right, from top to bottom starting with `"firstgid"` and needed tile is selected.
