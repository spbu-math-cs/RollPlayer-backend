# WebSocket Documentation

### /api/connect/{userId}/{sessionId}
Connect to WebSocket for real-time communication.

#### Parameters:
- {userId}: User ID for identifying the user
- {sessionId}: Session ID for identifying the session

#### Example request:
```bash
/api/connect/1/2
```
#### Status Codes:
- 200 OK: Successful WebSocket connection initiation.
- 400 Bad Request: Invalid userId or sessionId (must be UInt) or user or session does not exist.
