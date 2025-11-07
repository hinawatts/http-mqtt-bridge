# HTTP ↔ MQTT Bridge

A **Spring Boot 3** service that bridges **HTTP REST APIs** to **MQTT brokers**, allowing you to:

- Register and manage MQTT brokers
- Publish messages via HTTP to MQTT topics
- Subscribe to MQTT topics via **Server-Sent Events (SSE)** stream

Built with:
- **Java 21**
- **Spring Boot 3**
- **PostgreSQL**
- **Flyway**
- **HiveMQ MQTT Client**
---

## Features

| Module/Package      | Description                                          |
|---------------------|------------------------------------------------------|
| **Broker Config**   | Manage MQTT broker hostnames, ports, and credentials |
| **Publish**         | Publish JSON payloads to MQTT topics                 |
| **Subscribe**       | Stream MQTT messages over HTTP using SSE             |
| **external.client** | Client provider to connect to HiveMqtt Brokers       |

---

## Build & run
### Prerequisites
- [Docker](https://docs.docker.com/get-docker/)
- Mqtt Broker configured via cloud. This application is an implementation of HiveMq Client.
- Mqtt Broker credentials are passed via environment variables.
  - MQTT_USERNAME
  - MQTT_PASSWORD
- For multiple broker support, you can configure username and password per broker in `application.yaml`.

### Build and start the application
- Set the environment variables for broker credentials in `.env` file or export them in your shell.
- Run the following command from the project root:

```bash
docker compose up --build i
```
---

### Verify
Once the containers are up, open:

Swagger UI: http://localhost:8080/swagger-ui
