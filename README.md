# RollPlayer-backend

This is a Kotlin server for a multiplayer game. It handles communication between clients and manages game-related data. The server is built using the Ktor framework.

# Getting Started
Before running the server, make sure you have Kotlin and Gradle installed. To build and run the project, follow these steps:

# Clone the repository:
```bash
git clone <repository-url>
cd server-project-directory
```

# Build the project:
```bash
gradle build
```

# Run the server:
```bash
gradle run
```
The server will start running at http://localhost:9999 by default. You can change the port and other configurations in the application.conf file.

# Configuration
Port Configuration
You can configure the server port in the application.conf file located in the resources directory.

```properties
ktor {
    deployment {
        port = 9999 # Change this port number to your desired port
    }
    # Other configurations...
}
```

# Logging Configuration
Logging is configured in the server code. Logs are written to both the console and a log file named server.log.
