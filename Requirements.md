# Take-Home Assignment — HTTP ↔ MQTT Bridge

Thank you for your interest and for taking the time to participate in our interview process. This take-home assignment is a practical exercise to showcase your approach, code quality, and ability to build a solid foundation for a technical discussion in the next interview stage.

Contact: joao.monteiro@hivemq.com

---

## Before you start

- Objective: We are not expecting a perfectly polished, production-ready solution. Focus on correctness, structure, tests, and clear trade-offs. Use your available time wisely.
- Technologies: Any modern JVM framework is acceptable (e.g., Spring Boot, Micronaut, Quarkus, Vert.x).
- Build system: Use Gradle (Gradle wrapper must be included).
- MQTT broker: You can use HiveMQ Cloud (free tier) or any MQTT broker for testing.

---

## The task

Build a small HTTP → MQTT bridge application that exposes a REST API to configure an MQTT broker, publish messages to MQTT topics, and stream messages from MQTT topics over HTTP.

You may store broker configuration in-memory or persist it. Support for multiple brokers (IDs) is a plus.

---

## Functional requirements (API contract)

1) Broker configuration
- PUT /mqtt/
  - Request body (JSON):
    {
      "id": "optional-id",
      "host": "broker.hivemq.cloud",
      "port": 8883,
      "username": "user",       // optional
      "password": "pass"        // optional
    }
  - Action: store or update broker configuration.

- GET /mqtt/
  - Response: list of configured brokers or single broker config as JSON.

- DELETE /mqtt/
  - Action: remove stored configuration(s). Accept an optional broker id parameter.

2) Publish message
- POST /mqtt/{brokerId}/send/{topic}
  - Action: publish message to the specified topic on the selected broker.

3) Subscribe / receive messages (stream)
- GET /mqtt/{brokerId}/receive/{topic}
  - Action: subscribe to the given MQTT topic and stream messages to the HTTP client.
---
