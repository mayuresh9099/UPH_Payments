# Payment Workflow Orchestration Framework

Spring Boot payment orchestration framework for an enterprise payments processing platform. The implementation focuses on workflow orchestration, persistent audit state, database-driven retry configuration, and mock placeholders for downstream systems.

## Payment Flow

```text
PAYMENT_RECEIVED
        |
        v
REQUEST_VALIDATION
        |
        v
FIRCO_SCREENING
        |
        v
PAYMENT_POSTING
        |
        v
ACCOUNTING
        |
        v
NOTIFICATION
        |
        v
PAYMENT_COMPLETED
```

Failure path:

```text
Action Failed
      |
      v
Retry only for technical failures
      |
      v
Max Retry Reached
      |
      v
PAYMENT_FAILED
```

## Action Result Semantics

Actions return `ActionResult` with:

```java
ActionStatus status; // SUCCESS, BUSINESS_FAILURE, TECHNICAL_FAILURE
String errorCode;
String errorMessage;
```

Retry decisions are based on `ActionStatus`:

- `SUCCESS`: advance to the next payment state.
- `BUSINESS_FAILURE`: do not retry; immediately move to `PAYMENT_FAILED`.
- `TECHNICAL_FAILURE`: retry using `retry_configuration`.

Examples retried: timeout, HTTP 500, connection failure, network error, temporary unavailable.

Examples not retried: invalid request, duplicate payment, FIRCO rejection, sanctioned customer, AML failure, invalid account.

## Payment Context

Every action receives `PaymentContext` containing:

- `paymentId`
- `transactionId`
- `correlationId`
- `customerId`
- `sourceAccount`
- `destinationAccount`
- `currency`
- `amount`
- `paymentType`
- `channel`
- `requestTimestamp`
- request/response payload and metadata

## Payment Stages

- `REQUEST_VALIDATION`: validates mandatory payment fields and business rules locally.
- `FIRCO_SCREENING`: mock fraud, AML, and sanctions screening. Business rejection stops the workflow; technical failure retries.
- `PAYMENT_POSTING`: placeholder for Flexcube, CBS, or core banking posting.
- `ACCOUNTING`: placeholder for GL, debit, and credit entries.
- `NOTIFICATION`: placeholder for SMS, email, Kafka events, or push notifications.

## Persistence And Audit

Flyway migration:

```text
src/main/resources/db/migration/V1__create_workflow_tables.sql
```

Tables:

- `workflow_instance`: workflow name, business key, current state, status, timestamps.
- `workflow_action`: payment ID, action name, state, audit status, retry count, error code, error message, start/end timestamps.
- `retry_configuration`: action retry policy loaded from the database.

Retry config can be changed without code changes:

```sql
UPDATE retry_configuration
SET max_retry = 5, retry_interval_seconds = 10
WHERE action_name = 'PAYMENT_POSTING';
```

## REST API

Start payment workflow:

```http
POST /api/workflows/start
Content-Type: application/json

{
  "workflowName": "PAYMENT",
  "businessKey": "PAY-12345",
  "paymentId": "PAY-12345",
  "transactionId": "TXN-12345",
  "correlationId": "CORR-12345",
  "customerId": "CUST-1",
  "sourceAccount": "100001",
  "destinationAccount": "200001",
  "currency": "AED",
  "amount": 100.50,
  "paymentType": "DOMESTIC",
  "channel": "API",
  "requestTimestamp": "2026-07-30T13:00:00"
}
```

Fetch payment workflow audit:

```http
GET /api/workflows/{id}
```

## Extending The Payment Flow

Future steps such as fraud engine, FX conversion, compliance, limit check, treasury, SWIFT gateway, settlement, and reconciliation can be added by:

1. Implementing `WorkflowAction`.
2. Adding retry configuration for the action.
3. Adding the action to `workflow.definitions.PAYMENT.actions`.
4. Adding the corresponding state transition and event mapping.

No orchestration loop changes are required.

## Running

The project is configured for Java 21:

```xml
<java.version>21</java.version>
```

Run with JDK 21:

```bash
mvn test
mvn spring-boot:run
```
