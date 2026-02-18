# Brainstorm: Workflow Architecture for SACCO Business Processes

## Problem Statement

The InnerCircle SACCO has organic event-driven plumbing (18 event types, `@TransactionalEventListener` for GL posting, `@Scheduled` for batch jobs) but lacks a cohesive workflow architecture. Three compounding pain points:

1. **Silent failures** - Event handlers can fail without retry, dead letter, or alerting. A failed GL handler means the ledger drifts from business state with no automatic recovery.
2. **Unclear state transitions** - Loan lifecycle (APPLIED -> APPROVED -> DISBURSED -> REPAYING -> CLOSED/DEFAULTED) is enforced via scattered `setStatus()` calls across multiple services. No single source of truth for valid transitions, guards, or side effects.
3. **Hard to add new flows** - Adding a new business process (e.g., dividend distribution, member exit) requires wiring events, GL handlers, audit listeners, and state tracking across 4+ modules with no template or guardrails.

## Current Architecture Snapshot

```
┌─────────────────────────────────────────────────────────────┐
│                    Business Services                        │
│  MemberService  LoanService  ContributionService  Payout   │
│       │              │              │               │       │
│       └──────────────┴──────────────┴───────────────┘       │
│                         │ publishEvent()                    │
│                         ▼                                   │
│              ApplicationEventPublisher                      │
│                    │         │                              │
│        ┌───────────┘         └───────────────┐              │
│        ▼ BEFORE_COMMIT              ▼ AFTER_COMMIT          │
│  FinancialEventListener        AuditEventListener           │
│  (10 GL handlers)              (generic audit)              │
│        │                                                    │
│        ▼                                                    │
│  LedgerService.createJournalEntry()                         │
│                                                             │
│  Batch: @Scheduled cron -> LoanBatchServiceImpl             │
│         (interest accrual, penalty, overdue detection)      │
│         BatchProcessingLog table for idempotency            │
└─────────────────────────────────────────────────────────────┘
```

**What works well:**
- Event records as Java records implementing `AuditableEvent` - clean, typed
- `BEFORE_COMMIT` phase for GL ensures atomicity with business transaction
- `BatchProcessingLog` for batch idempotency/sequencing
- Cross-module communication via events preserves module boundaries

**What's missing:**
- No retry/dead letter for failed event handlers
- No idempotency keys on events themselves
- No correlation IDs for tracing a business operation across handlers
- No explicit state machine - valid transitions are implicit
- No compensation/rollback logic for multi-step operations
- No event catalog or schema registry
- No observability into event flow (which handler succeeded/failed)

## Constraints

| Constraint | Source |
|---|---|
| Spring Boot 3.4 monolith only | User preference, team size |
| No external workflow engine (Temporal, Camunda) | User preference |
| Single SACCO, < 500 members | Scale constraint |
| Modules must stay decoupled via events | CLAUDE.md architecture rules |
| All monetary operations require GL entries | Double-entry accounting requirement |
| Liquibase for all schema changes | Existing pattern |
| Java 21, Lombok, constructor injection | Existing conventions |

## Candidate Directions

---

### Candidate A: Domain State Guards + Event Hardening

**Philosophy:** Don't add frameworks. Harden what exists with targeted patterns.

**Who it serves:** Small team that wants incremental improvement without a big rewrite.

**In scope:**
- Explicit state transition guards on entities (`LoanApplication.transitionTo(REPAYING)` with validation)
- Transactional outbox table for reliable event publishing
- Dead letter table + retry mechanism for failed event handlers
- Idempotency keys on all events (de-duplication)
- Correlation IDs threaded through event chains
- Event handler outcome logging (success/fail/retry)

**Out of scope:**
- Full state machine framework
- Saga orchestrator
- Event sourcing

**Architecture sketch:**

```
Entity (e.g. LoanApplication)
  └── transitionTo(newStatus) — validates via allowed transitions map
        └── throws InvalidStateTransitionException if not allowed

Service
  └── business logic
        └── entity.transitionTo(...)
        └── save entity
        └── write to outbox table (event payload, correlation ID, idempotency key)

OutboxProcessor (@Scheduled, every 5s)
  └── SELECT * FROM event_outbox WHERE published = false
        └── publishEvent(event) via ApplicationEventPublisher
        └── mark published = true

EventHandler (existing @TransactionalEventListener)
  └── try { handle } catch → write to dead_letter table
  └── idempotency check: skip if event already processed

DeadLetterRetryJob (@Scheduled, every 5min)
  └── SELECT * FROM event_dead_letter WHERE retries < maxRetries
        └── re-publish event
        └── increment retry count
```

**Top risks:**
1. Outbox processor adds eventual consistency delay (mitigated: runs every 5s, negligible for a small SACCO)
2. Dead letter retry could cause duplicate processing (mitigated: idempotency keys)
3. State guard methods on entities might feel scattered (mitigated: single `ALLOWED_TRANSITIONS` map per entity)

**MVP slice:**
1. Add state transition guard to `LoanApplication` (the most complex lifecycle)
2. Create `event_outbox` + `event_dead_letter` tables
3. Add outbox writer + processor
4. Add dead letter writer + retry job
5. Add correlation ID to all events

**Estimated effort:** 3-4 focused implementation tasks

---

### Candidate B: Lifecycle State Machine Module

**Philosophy:** Formalize state machines as a first-class pattern, using plain Java (not Spring State Machine framework) in a shared module.

**Who it serves:** Team that wants explicit, testable, documented business process definitions.

**In scope:**
- Generic `StateMachine<S, E>` abstraction in `sacco-common`
- Concrete state machines: `LoanLifecycle`, `PayoutLifecycle`, `ContributionLifecycle`
- Each defines: states, events, transitions, guards (predicates), and actions (side effects)
- Transition logging to a `state_transitions` audit table
- State machine drives event publishing (transitions ARE the events)
- Event hardening from Candidate A (outbox, dead letter)

**Out of scope:**
- Spring State Machine dependency
- Saga orchestration
- Event sourcing

**Architecture sketch:**

```java
// Definition
StateMachineDefinition<LoanStatus, LoanEvent> LOAN_LIFECYCLE = StateMachine.define()
    .initialState(APPLIED)
    .transition(APPLIED, APPROVE, APPROVED)
        .guard(loan -> loan.getApprovedBy() != null)
        .action(loan -> publishEvent(new LoanApplicationEvent(...)))
    .transition(APPROVED, DISBURSE, DISBURSED)
        .guard(loan -> loan.getDisbursedAt() != null)
        .action(loan -> publishEvent(new LoanDisbursedEvent(...)))
    .transition(DISBURSED, START_REPAYMENT, REPAYING)
    .transition(REPAYING, CLOSE, CLOSED)
        .guard(loan -> loan.getOutstandingBalance() == 0)
    .transition(REPAYING, DEFAULT, DEFAULTED)
    .build();

// Usage in service
loanLifecycle.fire(loan, LoanEvent.APPROVE);
// -> validates guard -> executes action -> sets status -> logs transition
```

**New module structure:**
```
sacco-common/
  └── workflow/
      ├── StateMachine.java          # Generic state machine
      ├── StateMachineDefinition.java # Builder for defining machines
      ├── Transition.java            # State A -> Event -> State B + guard + action
      ├── TransitionLog.java         # Audit entity
      └── InvalidTransitionException.java
```

**Top risks:**
1. Over-engineering for a small system (mitigated: keep the abstraction minimal, < 200 lines)
2. Existing code needs refactoring to use state machine (mitigated: adopt incrementally, loan first)
3. Guard predicates might need service-layer dependencies (mitigated: pass context object, not entity alone)

**MVP slice:**
1. Build generic `StateMachine` abstraction (minimal: states, events, transitions, guards)
2. Define `LoanLifecycle` state machine
3. Refactor `LoanServiceImpl` to use it for all status changes
4. Add `state_transitions` audit table
5. Add event hardening (outbox + dead letter) from Candidate A

**Estimated effort:** 4-5 focused implementation tasks

---

### Candidate C: Process Template + Event Catalog

**Philosophy:** Make "adding a new business process" easy by providing templates and a registry. Fix the discoverability problem.

**Who it serves:** Team that keeps adding features (dividend distribution, member exit, AGM voting) and needs guardrails to do it consistently.

**In scope:**
- `EventCatalog` - annotated registry of all domain events with metadata
- `ProcessTemplate` - base class/interface for defining a business process
  - Declares: trigger, steps, events published, GL entries expected, audit requirements
- Event schema validation (compile-time via annotation processor or runtime)
- Automated GL completeness check (every event that modifies money must have a GL handler)
- Process documentation generator (from annotations -> markdown)
- Event hardening from Candidate A

**Out of scope:**
- State machine framework
- Runtime workflow orchestration
- Event sourcing

**Architecture sketch:**

```java
@BusinessProcess(
    name = "Loan Repayment",
    trigger = "POST /api/v1/loans/{id}/repayments",
    module = "sacco-loan"
)
@PublishesEvents({LoanRepaymentEvent.class, PenaltyPaidEvent.class})
@RequiresGLEntry(events = {LoanRepaymentEvent.class})
public class LoanRepaymentProcess implements ProcessTemplate {

    @Override
    public List<ProcessStep> steps() {
        return List.of(
            step("Validate repayment amount"),
            step("Allocate: interest -> penalties -> principal"),
            step("Update loan balances"),
            step("Mark installments paid"),
            step("Publish LoanRepaymentEvent"),
            step("GL handler posts journal entry (via event)")
        );
    }
}

// At startup or in tests:
EventCatalog.verify(); // Ensures every @RequiresGLEntry has a handler
```

**Top risks:**
1. Annotations add ceremony without runtime behavior (mitigated: use for compile-time/test-time validation, not just docs)
2. ProcessTemplate might be too abstract to be useful (mitigated: keep it concrete - it's a checklist, not an executor)
3. Doesn't solve the state transition problem (mitigated: combine with entity-level guards from Candidate A)

**MVP slice:**
1. Define `@BusinessProcess`, `@PublishesEvents`, `@RequiresGLEntry` annotations
2. Build `EventCatalog` scanner that finds all events and their handlers
3. Write test that verifies GL completeness (every financial event has a handler)
4. Annotate the 5 core processes (contribution, loan disbursement, repayment, payout, batch)
5. Add event hardening from Candidate A

**Estimated effort:** 3-4 focused implementation tasks

---

## Comparison Matrix

| Dimension | A: Guards + Hardening | B: State Machine | C: Process Template |
|---|---|---|---|
| Addresses silent failures | Yes (outbox + dead letter) | Yes (outbox + dead letter) | Yes (outbox + dead letter) |
| Addresses unclear transitions | Partial (entity guards) | Full (explicit state machine) | Minimal (documentation) |
| Addresses hard to add flows | No | Partial (pattern to follow) | Full (template + catalog + validation) |
| Complexity added | Low | Medium | Medium |
| Refactoring required | Low (additive) | Medium (loan service rewrite) | Low (annotations + new test) |
| Testability improvement | Moderate | High (state machine is pure) | Moderate (catalog tests) |
| Time to value | Fastest | Moderate | Fast |

## Recommendation

**Primary: Candidate A (Domain State Guards + Event Hardening)**

Rationale:
- Addresses the most critical pain point (silent failures / GL drift) immediately
- Lowest risk - additive changes, no service rewrites
- Entity-level state guards solve 80% of the "unclear transitions" problem with 20% of the effort
- Outbox + dead letter is a well-understood pattern that directly fixes reliability
- For a single small SACCO (< 500 members), this is proportional to the problem

**Backup: Candidate B (Lifecycle State Machine)**

Consider upgrading to B after A is in place, specifically for the loan module. The loan lifecycle is complex enough (7 states, multiple transitions with business rules) that a formal state machine pays for itself in clarity and testability. But start with A to get reliability first.

**Not recommended: Candidate C alone**

The process template/catalog is valuable as documentation and test infrastructure, but it doesn't solve the runtime problems (failures, retries, state enforcement). It's a good *addition* to A or B later, not a standalone solution.

## Suggested Sequence

```
Phase 1: Event Hardening (Candidate A core)
  → event_outbox table + outbox processor
  → event_dead_letter table + retry job
  → idempotency keys on all events
  → correlation IDs

Phase 2: State Guards (Candidate A entities)
  → LoanApplication.transitionTo() with ALLOWED_TRANSITIONS map
  → PayoutRequest state guards
  → ContributionStatus guards

Phase 3 (optional): Loan State Machine (Candidate B for loans only)
  → Formalize the loan lifecycle as a tested state machine
  → Only if Phase 2 reveals the guards are insufficient

Phase 4 (optional): Event Catalog (Candidate C test infra)
  → @BusinessProcess annotations on core flows
  → GL completeness test
  → Process documentation generation
```

## Next Step

```
/discuss "Domain State Guards + Event Hardening (Candidate A)"
```

---
*Brainstormed: 2026-02-16*
