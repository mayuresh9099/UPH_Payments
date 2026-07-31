# Payment Workflow Orchestration — Architecture Flow Diagram

## End-to-End Architecture

```mermaid
flowchart TD
    Client["Client (Postman / curl)"]

    subgraph REST["REST Layer (port 9099)"]
        C1["POST /api/payments/workflows/start"]
        C2["GET  /api/payments/workflows/{id}"]
        C3["POST /api/payments/workflows/{id}/retry"]
    end

    subgraph Service["PaymentWorkflowService"]
        WS["startWorkflow() / resumeWorkflow()"]
    end

    subgraph StateMachine["Spring State Machine"]
        S1([PAYMENT_RECEIVED])
        S2([VALIDATE_PAYMENT])
        S3([FIRCO_SCREENING])
        S4([FLEX_POSTING])
        S5([ACCOUNTING])
        S6([NOTIFICATION])
        S7([PAYMENT_COMPLETED])
        S8([PAYMENT_FAILED])
        S1 --> S2 --> S3 --> S4 --> S5 --> S6 --> S7
    end

    subgraph Actions["WorkflowAction implementations (ActionFactory)"]
        A1["ValidatePaymentAction"]
        A2["FircoScreeningAction"]
        A3["FlexPostingAction"]
        A4["AccountingAction"]
        A5["NotificationAction"]
    end

    subgraph RetryEngine["Retry Engine"]
        RS["RetryService\n(FIXED / LINEAR / EXPONENTIAL)"]
        RSC["@Scheduled RetryScheduler\n(every 60 s)"]
        RSC -->|"re-queue FAILED instances"| RS
    end

    subgraph DB["MySQL Database (ORCLPDB1)"]
        T1[("payment_workflow\n(workflow definitions)")]
        T2[("payment_workflow_action\n(action sequence)")]
        T3[("payment_workflow_instance\n(runtime state)")]
        T4[("payment_action_instance\n(execution audit)")]
        T5[("retry_configuration\n(max retries, backoff)")]
    end

    subgraph Flyway["Flyway Migrations"]
        F1["V1__create_workflow_tables.sql\n(DDL + seed data)"]
        F2["V2__ensure_seed_data.sql\n(INSERT IGNORE)"]
    end

    Client --> C1 & C2 & C3
    C1 & C3 --> WS
    WS -->|"load action sequence"| T2
    WS --> StateMachine
    StateMachine -->|"execute step"| Actions
    Actions -->|"success → next state"| StateMachine
    Actions -->|"failure → RetryService"| RS
    RS -->|"retry exhausted"| S8
    StateMachine -->|"persist transitions"| T3
    Actions -->|"persist execution log"| T4
    RS -->|"read config"| T5
    Flyway --> DB
```

## Payment Lifecycle State Diagram

```mermaid
stateDiagram-v2
    [*] --> PAYMENT_RECEIVED : POST /start
    PAYMENT_RECEIVED --> VALIDATE_PAYMENT : trigger
    VALIDATE_PAYMENT --> FIRCO_SCREENING : SUCCESS
    FIRCO_SCREENING --> FLEX_POSTING : SUCCESS
    FLEX_POSTING --> ACCOUNTING : SUCCESS
    ACCOUNTING --> NOTIFICATION : SUCCESS
    NOTIFICATION --> PAYMENT_COMPLETED : SUCCESS
    PAYMENT_COMPLETED --> [*]

    VALIDATE_PAYMENT --> PAYMENT_FAILED : BUSINESS_FAILURE
    FIRCO_SCREENING --> PAYMENT_FAILED : BUSINESS_FAILURE or retry exhausted
    FLEX_POSTING --> PAYMENT_FAILED : retry exhausted
    ACCOUNTING --> PAYMENT_FAILED : retry exhausted
    NOTIFICATION --> PAYMENT_FAILED : retry exhausted
    PAYMENT_FAILED --> [*]
```

## Retry Flow

```mermaid
flowchart TD
    A["Action Executes"] --> B{Result?}
    B -->|SUCCESS| C["Advance to next state"]
    B -->|BUSINESS_FAILURE| D["Move to PAYMENT_FAILED immediately\n(no retry)"]
    B -->|TECHNICAL_FAILURE| E["Read retry_configuration\nfrom DB"]
    E --> F{Retry count\n< max_retries?}
    F -->|Yes| G["Apply backoff:\nFIXED / LINEAR / EXPONENTIAL"]
    G --> H["Increment retry count\nPersist error details"]
    H --> I["@Scheduled RetryScheduler\nre-queues after interval"]
    I --> A
    F -->|No| D
```

## Component Map

| Layer | Class / Interface | Responsibility |
|-------|-------------------|----------------|
| Controller | `PaymentWorkflowController` | REST endpoints |
| Service | `PaymentWorkflowService` | Orchestration, idempotent execution |
| Factory | `ActionFactory` | Resolves `WorkflowAction` beans by name |
| Actions | `ValidatePaymentAction` … `NotificationAction` | Mock downstream integrations |
| Retry | `RetryService`, `RetryScheduler` | Backoff + scheduled re-try |
| State | Spring State Machine config | State / event definitions |
| Entities | `PaymentWorkflow`, `PaymentWorkflowAction`, `PaymentWorkflowInstance`, `PaymentActionInstance` | JPA persistence |
| DB | `retry_configuration` | DB-driven retry policy |
| Migration | `V1__create_workflow_tables.sql`, `V2__ensure_seed_data.sql` | Flyway DDL + seed |
