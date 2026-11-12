# Notification service — package layout

Rooted at `com.finflow.notification`. Matches commerce-style **feature + integration** modules.

```text
com.finflow.notification
|-- NotificationServiceApplication
|-- notification/
|   |-- api: NotificationController
|   |-- application: NotificationDispatcher
|   |-- domain: NotificationRecord, NotificationStatus
|   |-- persistence: NotificationRecordRepository
|   `-- dto: NotificationResponse
|-- integration/
|   |-- event: CommerceDomainEvent, CommerceEventType
|   |-- kafka: CommerceEventListener, KafkaConsumerConfig, KafkaEnableConfig
|   `-- mail: MailChannel
`-- security: SecurityConfig, JwtAuthFilter, JwksKeyProvider, AuthenticatedUser
```

| Package role | Responsibility |
|--------------|----------------|
| `notification.api` | HTTP inbox API (`/api/v1/notifications/**`) |
| `notification.application` | Persist + dispatch notifications |
| `integration.kafka` | Optional commerce event consumer |
| `integration.mail` | Email channel stub |
| `security` | JWT resource server (same pattern as identity/wallet/commerce) |

Tests live under `notification.application` (e.g. `NotificationDispatcherTest`).

Legacy flat packages (`controller`, `service`, `entity`, `repository`, root `kafka`, `mail`, `event`) are removed — use the modules above only.
