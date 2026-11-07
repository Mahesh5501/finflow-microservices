# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture Overview

This is a microservices-based project (Spring Boot) with the following core services:

- **`identity-service`**: Handles user authentication, authorization, organization management, and JWT issuance.
- **`wallet-service`**: Manages user wallets, balances, deposits, withdrawals, and internal transfers.
- **`commerce-service`**: Manages products, product catalogs, shopping carts, and order processing.
- **`api-gateway`**: Acts as the entry point for the system.

Communication between services is primarily handled via REST APIs (e.g., `commerce-service` uses `WalletPaymentAdapter` to interact with `wallet-service` for payment processing).

## Common Development Tasks

### Building and Testing
This is a Gradle-based project. From the root (or within a service directory):

- **Build**: `./gradlew build`
- **Run all tests**: `./gradlew test`
- **Run a single test class**: `./gradlew test --tests "com.finflow.<service>.service.<TestClassName>"`

### Running Services
Each service is a Spring Boot application. To run a specific service:

- Navigate to the service directory (e.g., `cd wallet-service`).
- Run the application: `./gradlew bootRun`
