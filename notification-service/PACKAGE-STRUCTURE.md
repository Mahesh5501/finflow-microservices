# Notification service — package layout

Aligned with commerce **integration** and feature modules.

## Modules

| Module | Package | Responsibility |
|--------|---------|----------------|
| **notification** | `com.finflow.notification.notification` | Inbox records, HTTP API, dispatch |
| **integration.event** | `com.finflow.notification.integration.event` | Commerce event DTOs |
| **integration.kafka** | `com.finflow.notification.integration.kafka` | Optional Kafka consumer |
| **integration.mail** | `com.finflow.notification.integration.mail` | Email channel |
| **security** | `com.finflow.notification.security` | JWT/JWKS |

## Notification feature layers

```
notification/
  api/            → NotificationController
  application/    → NotificationDispatcher
  domain/         → NotificationRecord, NotificationStatus
  persistence/    → NotificationRecordRepository
  dto/            → NotificationResponse
```
