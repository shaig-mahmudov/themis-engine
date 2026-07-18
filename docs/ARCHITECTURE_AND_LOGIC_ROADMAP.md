# Themis Engine Architecture and Logic Roadmap

**Created:** 2026-07-18  
**Scope:** Architecture, domain logic, persistence, API, testing, security, and operational maturity

## Purpose

This roadmap converts the architecture and rules-engine recommendations from the project assessment into an incremental delivery plan. The intended outcome is a production-capable modular monolith with a pure, explainable, and extensible Pathfinder 1e rules core.

The sequence deliberately strengthens correctness and boundaries before expanding game content. Each phase should leave the application deployable and should avoid a large, all-at-once rewrite.

## Target architecture

The target dependency direction is:

```text
API → Application → Domain ← Infrastructure
```

- **API:** HTTP contracts, request validation, response mapping, and authentication context.
- **Application:** use cases, commands, transaction boundaries, authorization, and orchestration.
- **Domain:** framework-independent aggregates, value objects, policies, rules, events, and ports.
- **Infrastructure:** JPA, PostgreSQL, Redis, message/event delivery, and external adapters.

The project should remain a **modular monolith**. Microservices are not recommended until independent scaling, ownership, or deployment requirements justify their consistency and operational costs.

## Guiding principles

1. Correct state is more important than broader rules coverage.
2. Domain calculations should be deterministic, explainable, and testable without Spring.
3. PostgreSQL remains the system of record; caches and projections are disposable.
4. New Pathfinder interpretations should be captured in tests and ADRs.
5. Refactoring should proceed through small, behavior-preserving changes.
6. Domain events should support integration and audit needs without requiring full event sourcing.

---

## Phase 1: State Consistency and Production Safety

### Goal

Eliminate the highest-risk data consistency and deployment problems before changing the domain structure.

### Architecture work

- Add JPA `@Version` columns to character and encounter persistence entities.
- Add corresponding Flyway migrations and propagate aggregate versions through repository mappings.
- Translate optimistic-lock failures into HTTP `409 Conflict` responses.
- Consider exposing versions as response fields or ETags so clients can perform conditional mutations.
- Set `spring.jpa.open-in-view=false` and ensure all required aggregate data is loaded inside transactions.
- Make production API keys and database credentials mandatory instead of relying on known defaults.
- Restrict CORS through environment-specific origin allowlists.
- Define an explicit actuator exposure and authentication policy.
- Trust `X-Forwarded-For` only when the application is behind configured trusted proxies.
- Replace the unbounded, application-local rate-limit map with bounded or distributed storage.
- Stop returning internal exception messages from unexpected failures; log a correlation ID instead.

### Logic work

- Verify that every state-changing use case has one clear transaction boundary.
- Define conflict behavior for simultaneous attacks, turn advancement, slot consumption, and condition expiration.
- Validate duplicate participant, equipment, condition, and client-supplied character-ID behavior.
- Document whether retries are safe for every mutating endpoint.

### Testing work

- Run PostgreSQL Testcontainers tests as part of the normal CI pipeline.
- Add concurrency tests demonstrating that stale updates fail rather than overwrite newer state.
- Add migration tests for existing character and encounter data.
- Add security tests for CORS, actuator access, trusted proxies, and safe error responses.
- Resolve or replace the Windows Maven wrapper bootstrap path so local verification is consistent.

### Deliverables

- Versioned character and encounter aggregates.
- HTTP 409 conflict contract.
- Production-safe configuration defaults.
- PostgreSQL-backed integration suite in CI.
- Documented retry and conflict behavior.

### Completion criteria

- Two concurrent writes cannot silently overwrite one another.
- CI validates all Flyway migrations and repository adapters against PostgreSQL.
- The application refuses to start in a production profile without required secrets.
- Public error responses do not contain stack traces, SQL, or internal exception details.

---

## Phase 2: Enforce Application and Domain Boundaries

### Goal

Turn the current hexagonal-inspired structure into an explicit architecture that keeps Spring and persistence concerns outside the rules core.

### Architecture work

- Create an `application` package for use-case orchestration.
- Move `CharacterService`, `CombatService`, and `EncounterService` into the application layer.
- Remove `@Service`, `@Transactional`, and other Spring annotations from domain types such as `RuleEngine`.
- Keep transaction annotations on application services or infrastructure transaction adapters.
- Route `EncounterController#getEncounter` through an application query service instead of accessing `EncounterStore` directly.
- Introduce command/query objects for non-trivial use cases, for example:
  - `ResolveAttackCommand`
  - `AdvanceEncounterTurnCommand`
  - `ApplyConditionCommand`
  - `ConfigureSpellcastingCommand`
- Keep `CharacterStore` and `EncounterStore` as outbound ports.
- Add architecture tests, such as ArchUnit rules, to prevent API and domain code from importing infrastructure packages.

### Suggested package direction

```text
com.themis.engine
├── character
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── combat
├── encounter
└── shared
```

Feature-oriented packages can be introduced gradually. A full package move is not required in one pull request.

### Logic work

- Make randomness an explicit domain dependency through a small dice/random port.
- Return domain outcomes from rules without performing persistence inside the rule objects.
- Keep HTTP DTOs and persistence entities from crossing into domain APIs.
- Introduce strongly typed identifiers where they materially prevent mixing character, encounter, item, and effect IDs.

### Testing work

- Ensure core rule and aggregate tests run as plain JUnit tests without a Spring context.
- Add architecture tests for dependency direction.
- Preserve controller and adapter integration tests at the application boundary.

### Deliverables

- Explicit API, application, domain, and infrastructure layers.
- Framework-independent rules core.
- Application commands and query handlers for major use cases.
- Automated architecture-boundary checks.

### Completion criteria

- Domain tests do not require Spring startup.
- The domain has no dependency on Spring MVC, JPA, Redis, or security packages.
- Controllers cannot call persistence adapters or store ports directly.

---

## Phase 3: Decompose the Character Model and Generalize Effects

### Goal

Prevent `Character` from becoming a god object while retaining it as the consistency boundary.

### Architecture work

Keep `Character` as the aggregate root, but delegate focused behavior to internal domain components:

- `StatBlock` — base attributes, derived statistics, and stat queries.
- `HealthState` — damage, healing, temporary HP, and life-state rules.
- `ActionEconomy` — action resources and restrictions.
- `EquipmentLoadout` — equipped items, weapons, armor, and equipment slots.
- `EffectCollection` — active effects, duration, stacking, and expiration.
- `SpellcastingCollection` — one or more casting progressions.

These objects should not become independently mutable repositories. Mutations should still pass through `Character` unless a new consistency boundary is deliberately introduced.

### Logic work: effects

Generalize conditions, spells, feats, items, and similar sources around a shared effect model:

```text
Effect
├── id and source
├── duration
├── modifiers
├── tags
├── stacking group/policy
└── lifecycle triggers
```

- Keep named Pathfinder conditions as definitions or templates built on effects.
- Define lifecycle points such as apply, turn start, turn end, expire, and remove.
- Separate permanent effects from timed effects.
- Make replacement, suppression, immunity, and duplicate-source behavior explicit.
- Persist an effect schema/version when serialized JSON must remain compatible across releases.

### Logic work: health

Preserve cumulative lethal damage, but prepare `HealthState` for distinct concepts:

- lethal damage;
- nonlethal damage;
- temporary hit points;
- regeneration and fast healing;
- stabilization, dying, unconscious, and dead states;
- ability damage and drain;
- temporary and permanent Constitution changes.

Do not represent all these concepts with one damage integer.

### Logic work: spellcasting

Replace the single optional `SpellcastingFeature` with a collection keyed by casting source. Each progression should be capable of representing:

- class/source ID;
- caster level;
- casting attribute;
- maximum and remaining slots;
- prepared or spontaneous mode;
- spell list or tradition;
- independent recovery behavior.

### Testing work

- Add aggregate invariants covering component interactions.
- Add effect lifecycle/state-machine tests.
- Add health transition tests for boundary values and Constitution changes.
- Add multiclass spellcasting tests before exposing the model through the API.

### Deliverables

- Smaller, focused internal character components.
- Shared effect lifecycle and persistence contract.
- Extensible health model.
- Multiple spellcasting progressions.

### Completion criteria

- `Character` coordinates its components but no longer directly implements every calculation.
- Timed effects expire predictably at defined lifecycle points.
- New effect sources do not require duplicating condition/equipment lifecycle logic.

---

## Phase 4: Build an Explainable, Contextual Rules Engine

### Goal

Move from fixed getters and coarse enums to reusable, explainable rule evaluations that can support Pathfinder exceptions.

### Architecture work

Introduce query and context objects such as:

```java
StatResult calculate(StatQuery query, RuleContext context);
```

A `RuleContext` can carry relevant facts without coupling the rule to controllers or persistence:

- acting and target combatants;
- attack or defense mode;
- current encounter/turn facts;
- tags such as touch, flat-footed, charge, ranged, or opportunity;
- environment and effect context.

A `StatResult` or `RuleResult` should contain:

- final value or outcome;
- applied modifiers;
- suppressed modifiers and reasons;
- dice values;
- rule notes suitable for audit or UI explanations.

### Logic work: modifier policies

Replace a single `ModifierType.isStackable()` flag with a policy model:

```text
ModifierType → StackingPolicy
```

Policies should support:

- highest bonus only;
- all bonuses stack;
- strongest/worst penalty handling;
- same-source suppression;
- circumstance and source exceptions;
- replacement and suppression effects;
- context-dependent applicability.

Property-based tests are recommended for commutativity, ordering independence, duplicate-source behavior, and extreme values.

### Logic work: weapons and attacks

Replace the coarse `MELEE`, `RANGED`, and `FINESSE` type model with composable weapon properties:

- attack ability: Strength, Dexterity, or custom;
- damage ability: Strength, none, limited Strength, or custom;
- range mode: melee, ranged, or thrown;
- handedness and Strength multiplier;
- traits such as finesse, composite, light, reach, and ammunition.

Separate attack calculation from state mutation:

1. Validate that the action is legal.
2. Produce an `AttackResolution` containing rolls and effects.
3. Apply the resolution to aggregates within the application transaction.
4. Persist the result and emit audit/domain events after success.

### Logic work: contextual statistics

Support distinct calculations instead of one global AC or attack value:

- normal, touch, and flat-footed AC;
- combat maneuver bonus/defense;
- size modifiers;
- opponent-specific bonuses;
- concealment and miss chance;
- saving throws against contextual sources;
- immunity, resistance, and damage reduction.

### Testing work

- Add golden tests for representative Pathfinder rule examples.
- Add property-based tests for modifier policies and dice parsing.
- Record deterministic dice inputs and full calculation breakdowns in assertions.
- Add regression fixtures whenever a rules dispute or bug is resolved.

### Deliverables

- Contextual stat-query API.
- Explainable calculation results.
- Policy-based modifier resolver.
- Trait-based weapon and attack model.
- Deterministic attack-resolution pipeline.

### Completion criteria

- A client can explain why a value or attack result was produced.
- Adding a weapon trait does not require creating combinations in a large enum.
- Modifier results are independent of input ordering and covered by property tests.

---

## Phase 5: Complete Action Economy and Encounter Semantics

### Goal

Make turn and encounter behavior robust enough for advanced combat rules and concurrent clients.

### Logic work: action economy

Replace independent booleans with an action-resource model capable of representing:

- standard, move, swift, immediate, free, and full-round actions;
- immediate actions consuming the next swift action;
- full attacks;
- attacks of opportunity or reactions outside the normal turn;
- readied actions;
- delayed turns;
- surprise rounds;
- effects that grant, exchange, or prevent actions.

Action legality should be evaluated by policies rather than scattered `if` statements.

### Logic work: encounters

- Define participant removal/replacement rules.
- Define duplicate combatant-ID behavior.
- Support delay and ready operations without corrupting initiative order.
- Distinguish turn-start and round-start lifecycle events.
- Define what happens when an active participant is removed, defeated, or unavailable.
- Preserve a deterministic combat log containing commands, dice, outcomes, and turn transitions.
- Add idempotency support for turn advancement and attacks so client retries cannot duplicate mutations.

### Architecture work

- Treat encounter commands as explicit application use cases.
- Use optimistic versions or ETags on encounter mutations.
- Emit domain events for turn, round, participant, action, and attack changes.
- Keep an append-only audit/combat log separate from the current aggregate state.

### Testing work

- Add state-machine tests for complete encounter lifecycles.
- Add concurrency tests for duplicate next-turn and attack requests.
- Add deterministic replay tests for recorded combat commands and dice.
- Test effect expiration at every lifecycle boundary.

### Deliverables

- Resource-based action economy.
- Advanced encounter transitions.
- Idempotent combat mutations.
- Persisted combat audit log.

### Completion criteria

- Retrying a command cannot apply damage or advance a turn twice.
- Encounter state remains valid when participants delay, ready, leave, or are defeated.
- Every important combat mutation has an auditable explanation.

---

## Phase 6: Stable API, Events, Observability, and Real-Time Delivery

### Goal

Publish a stable integration contract and add real-time capabilities only after state transitions and domain events are reliable.

### API work

- Add OpenAPI documentation with request, response, and error examples.
- Adopt consistent problem responses, preferably RFC 9457-compatible.
- Define API versioning and compatibility policy.
- Add missing symmetric operations:
  - unequip item;
  - unequip weapon;
  - remove condition/effect;
  - remove encounter participant;
  - delete/archive character and encounter where required.
- Add list/search endpoints with pagination.
- Standardize ID generation and validation.
- Add idempotency-key and optimistic-version requirements to mutation documentation.

### Event and real-time work

Publish domain events only after successful commits, for example:

- `CharacterDamaged`
- `EffectApplied`
- `EffectExpired`
- `AttackResolved`
- `TurnStarted`
- `RoundStarted`
- `EncounterAdvanced`

Use those events to drive WebSocket or Server-Sent Events projections. Do not let a WebSocket connection directly mutate aggregates outside application use cases.

If events must be delivered to external systems reliably, introduce a transactional outbox. Full event sourcing remains unnecessary unless replay becomes a core product requirement.

### Observability work

- Add `micrometer-registry-prometheus` or remove the unsupported Prometheus configuration.
- Add metrics for request latency, rule failures, conflicts, cache results, rate-limit rejections, and encounter operations.
- Include correlation IDs in logs and error responses.
- Add tracing only when there is a real distributed call path to trace.
- Configure alerts and dashboards around service-level objectives.

### Cache work

- Cache immutable response snapshots or projections rather than mutable domain aggregates.
- Version cache payloads and define invalidation after successful commits.
- Avoid permissive polymorphic deserialization where possible.
- Add Redis Testcontainers tests and an explicit outage/degradation policy.

### Deliverables

- Published OpenAPI contract.
- Stable error, versioning, idempotency, and concurrency conventions.
- Domain-event-driven real-time updates.
- Production metrics, correlation, and cache policy.

### Completion criteria

- Consumers can integrate without reading Java source code.
- Real-time messages are projections of committed state.
- Redis failure behavior is tested and documented.
- Operational dashboards expose conflicts, latency, failures, and rule-engine health.

---

## Phase 7: Controlled Pathfinder Rules Expansion

### Goal

Expand game coverage on top of stable calculation, effect, action, and integration foundations.

### Suggested rule modules

- saves, spell targeting, and saving-throw resolution;
- combat maneuvers and combat maneuver defense;
- damage types, resistance, immunity, vulnerability, and damage reduction;
- temporary HP, nonlethal damage, dying, and stabilization;
- attacks of opportunity, reach, threatened areas, and movement triggers;
- class features, feats, racial traits, and prerequisites;
- prepared/spontaneous spell workflows and spell effects;
- creature/monster combatants and templates;
- environment, concealment, cover, and grid/spatial rules.

### Delivery rules

- Introduce one coherent rule slice at a time.
- Create ADRs for ambiguous or intentionally simplified Pathfinder interpretations.
- Add golden examples from the rulebooks without copying copyrighted text into the repository.
- Keep content definitions separate from the execution engine where practical.
- Prefer data-driven definitions only when the rules model is stable enough to validate them safely.

### Completion criteria

- Each module has domain tests, API examples, and explicit compatibility expectations.
- New modules use the shared effect, context, modifier, and explanation mechanisms.
- No module bypasses aggregate invariants or application transaction boundaries.

---

## Cross-phase quality gates

Every phase should satisfy these gates before it is considered complete:

- all unit and integration tests pass;
- PostgreSQL migrations are forward-only and verified against existing schemas;
- domain changes include tests for invariants and boundary values;
- public contract changes update OpenAPI and examples once Phase 6 is available;
- architecture changes update relevant ADRs or introduce a superseding ADR;
- security-sensitive defaults remain safe in the production profile;
- performance-sensitive changes include a measurement or representative benchmark;
- documentation distinguishes current behavior from planned behavior.

## Recommended first implementation backlog

The first practical batch should remain small and focus on Phase 1:

1. Add version columns and optimistic locking to characters and encounters.
2. Return HTTP 409 for stale mutations.
3. Add concurrent-update integration tests.
4. Make PostgreSQL Testcontainers tests mandatory in CI.
5. Disable open-in-view and resolve any resulting loading issues.
6. Introduce production-only mandatory secrets and a CORS allowlist.
7. Secure actuator endpoints and sanitize unexpected errors.
8. Define trusted-proxy and distributed rate-limit behavior.

Only after these are complete should the project begin the application/domain package restructuring in Phase 2.

## Related documentation

- [Project Report](PROJECT_REPORT.md)
- [Architecture Decision Index](ADR/DECISIONS.md)
- [ADR-001: Modifier Source Representation](ADR/ADR-001-modifier-source-representation.md)
- [ADR-002: Armor Class Max Dexterity Bonus Capping](ADR/ADR-002-armor-class-max-dexterity-bonus-capping.md)
- [ADR-003: Hit Points Tracking Structure](ADR/ADR-003-hit-points-tracking-structure.md)
