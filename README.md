# Liminal Chat

Liminal Chat is a temporary, room-based chat application built with Java, Spring Boot, Vue, and WebSockets. Users can create a room, share its five-character code, and communicate in real time without creating an account.

**Live Demo:** [https://liminal.michaelriley.au](https://liminal.michaelriley.au)

## How It Works

1. **Create a Room:** Enter a display name to create a temporary chatroom with a unique five-character code.
2. **Share the Code:** Other users can join the room using the code and their own display name.
3. **Chat in Real Time:** Messages, participant changes, and join/leave events are delivered using WebSockets.
4. **Automatic Expiry:** Rooms expire after one hour without activity. Joining a room or sending a message resets the inactivity timer.

Liminal Chat deliberately uses in-memory storage rather than a database. It is designed as a lightweight, single-instance application where rooms are temporary and messages are not persisted.

## Features

- **Temporary Chatrooms:** Rooms are created on demand and require no user accounts.
- **Real-Time Messaging:** WebSocket communication delivers messages immediately to connected participants.
- **Five-Character Room Codes:** Rooms use five-character codes for joining.
- **Participant Tracking:** Users can see who is currently connected to a room.
- **Unique Display Names:** Display names must be unique within each room, ignoring capitalisation.
- **Join and Leave Notifications:** Participant changes are reflected in real time.
- **Room Expiration:** Inactive rooms are automatically removed after one hour.
- **Input Validation:** Room codes, display names, WebSocket connections, and messages are validated by the backend.
- **Responsive Interface:** Designed for both desktop and mobile use.

## Architecture

The application is split into separate frontend and backend services.

- The **Vue frontend** is built with Vite and served by Nginx.
- The **Spring Boot backend** manages rooms, participants, validation, and WebSocket communication.
- Nginx proxies REST requests and WebSocket connections from the frontend to the backend.
- Both services run as Docker containers using Docker Compose.

The backend is not exposed directly to the internet.

## Technologies Used

- **Java:** Backend implementation
- **Spring Boot:** REST API and application framework
- **Spring WebSockets:** Real-time communication
- **Vue 3:** Frontend user interface
- **Vite:** Frontend build tooling
- **Nginx:** Frontend web server and internal reverse proxy
- **Docker & Docker Compose:** Containerisation and service orchestration

## Testing

The project includes automated backend and frontend tests covering room behaviour, validation, WebSocket communication, and user interactions.

GitHub Actions runs backend tests, frontend tests, formatting and linting checks, production builds, Docker Compose validation, and container image builds.

## Running Locally

Clone the repository:

```
git clone https://github.com/michaelriley87/liminal-chat.git
cd liminal-chat
```

Build and start the application:

```
docker compose up --build
```

The application will be available at:

```
http://localhost:8080
```

Rooms are stored only in memory and are removed when the backend restarts. Messages are not persisted or saved.
