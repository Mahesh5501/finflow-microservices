# Postman — Notification Service

Port: **18084** · env: `notificationBaseUrl`

## Import

1. `postman/FinFlow-Local.postman_environment.json` → **FinFlow Local**
2. `notification-service/postman/FinFlow-Notification-Service.postman_collection.json`

## Notes

- Inbox is always `/api/v1/notifications/me` (JWT subject).
- Events arrive from commerce via Kafka; empty inbox usually means Kafka not running or no commerce events yet.
