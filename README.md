# Event-Driven Microservices with Apache Kafka

This project is built on an event-driven architecture using **Apache Kafka** and consists of two microservices:
- **Product Service** (Producer) - [GitHub Repository](https://github.com/dobrevd/ProductMicroservice_kafka)
- **Email Notification Service** (Consumer & Producer)

---

## EmailNotificationService

**EmailNotificationService** is a microservice that acts as both a **Kafka consumer and producer**. It processes messages from Kafka topics and sends notifications about product-related events.

### 🔹 **Core Functionality**

The microservice listens to the following Kafka topics:
- **`${email-notification-service.kafka.created-topic}`**
- **`${email-notification-service.kafka.delete-topic}`**

It processes events related to **product creation and deletion** and sends a confirmation message to the topic:
- **`${email-notification-service.kafka.info-topic}`** when a product is successfully deleted.

---

### 🔹 **Message Processing Logic**

#### `productCreatedHandler` – Processing Product Creation Events

This method listens to the **`${product-service.kafka.created-topic}`** and follows these steps:

1. **Idempotency Check**
    - If the message with the same `messageId` is already processed (exists in the database), it is ignored, and the event is logged.

2. **Event Handling**
    - If the message is new, an **HTTP request** is sent to an external service (`requestUrl`).
    - If the external service responds with **200 OK**, the event is logged and saved in the database.

---

#### `productDeleteHandler` – Processing Product Deletion Events

This method listens to the **`${product-service.kafka.delete-topic}`** and executes the following logic **within a single transaction**:

1. Sends an event to the **`${email-notification-service.kafka.delete-topic}`**.
2. Deletes the product from the database by its ID.
3. If the product does not exist, an exception is thrown, and the **transaction is rolled back**.
    - The event from step **1** remains **uncommitted** and will be **marked for deletion**.

---

### 🔹 **Error Handling**

- If the external service is **unavailable** (`ResourceAccessException`), a **`RetryableException`** is thrown, and the message will be retried based on retry policies:
    - **Fixed retry interval**: `FixedBackOff(5000, 3)` → Retries every **5 seconds**, up to **3 times**.

- **Error Handler (`DefaultErrorHandler`)**:
    - Exceptions like `NotRetryableException` and `HttpServerErrorException` **are not retried**.
    - `RetryableException` is retried according to the retry settings.

---

## 🔹 **Kafka Producer Configuration**

| Parameter | Description |
|-----------|------------|
| `bootstrap.servers` | Kafka broker addresses. |
| `key.serializer` | Serializer for message keys (`StringSerializer`). |
| `value.serializer` | Serializer for message values (`JsonSerializer`). |
| `acks` | Acknowledgment strategy (`all` ensures all replicas confirm the message). |
| `delivery.timeout.ms` | Maximum time for message delivery confirmation. |
| `linger.ms` | Delay before sending messages to batch them. |
| `request.timeout.ms` | Maximum wait time for a broker response before retrying. |
| `enable.idempotence` | Enables idempotency to prevent duplicate messages. |
| `max.in.flight.requests.per.connection` | Number of unacknowledged messages per connection. |
| `transactional.id` | Unique transaction ID for producer (required when `enable.idempotence=true`). |

---

## 🔹 **Kafka Consumer Configuration**

| Parameter | Description |
|-----------|------------|
| `key.deserializer` | Deserializer for message keys (`StringDeserializer`). |
| `value.deserializer` | Deserializer for message values (`ErrorHandlingDeserializer` with `JsonDeserializer`). |
| `spring.json.trusted.packages` | Trusted packages for object deserialization. |
| `group.id` | Consumer group ID (messages are distributed among group members). |
| `auto.offset.reset` | Defines offset behavior (`earliest` reads from the beginning, `latest` reads only new messages). |

---