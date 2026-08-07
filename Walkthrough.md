# Walkthrough: Implementation History

This document tracks completed implementation phases for **Themis Engine**.

---

## Phase 10: Application Layer Boundaries and Command Encapsulation (Completed)

We introduced an explicit application layer that isolates use-case orchestration from both the API and the domain core, eliminating direct store access from controllers and promoting use-case parameters into strongly typed command records.

### 1. Command/Query Service Split
* Split each bounded context's orchestration into separate `*CommandService` (state-changing, `@Transactional`) and `*QueryService` (read-only) classes under [com/themis/engine/application](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/application):
  * [CharacterCommandService.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/application/character/CharacterCommandService.java) / [CharacterQueryService.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/application/character/CharacterQueryService.java)
  * [CombatCommandService.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/application/combat/CombatCommandService.java)
  * [EncounterCommandService.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/application/encounter/EncounterCommandService.java) / [EncounterQueryService.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/application/encounter/EncounterQueryService.java)
* Removed the deprecated monolithic application services and rewired every controller to inject the new command/query services.

### 2. Command Records for Use-Case Inputs
* Introduced explicit command records under `application/{context}/command/`:
  * [ConfigureSpellcastingCommand.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/application/character/command/ConfigureSpellcastingCommand.java)
  * [ResolveAttackCommand.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/application/combat/command/ResolveAttackCommand.java)
  * [AddParticipantCommand.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/application/encounter/command/AddParticipantCommand.java)
  * [StartEncounterCommand.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/application/encounter/command/StartEncounterCommand.java)
* Controllers now translate request DTOs into commands before invoking services (see `CharacterController.configureSpellcasting` for the pattern).

### 3. API Mappers and Thin Controllers
* Added [CharacterApiMapper.java](file:///c://Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/api/character/CharacterApiMapper.java), [CombatApiMapper.java](file:///c://Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/api/combat/CombatApiMapper.java), and [EncounterApiMapper.java](file:///c://Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/api/encounter/EncounterApiMapper.java) to translate between request/response DTOs and domain aggregates. Controllers now act as thin delegates with no domain-construction logic.
* Reorganized the API package into feature-oriented subpackages (`api/character`, `api/combat`, `api/encounter`, `api/common`).
* Removed the direct `EncounterStore` dependency from `EncounterController`; encounter reads now flow through `EncounterQueryService.getEncounter`.
* Renamed `AttackRequest` to `ResolveAttackRequest`, introduced `AttackResponse`, and rewired the combat controller through `CombatApiMapper`.

### 4. Domain Layer Purity
* Decapsulated Spring annotations from [RuleEngine.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/RuleEngine.java) and registered it as an infrastructure bean via a new [DomainConfiguration.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/infrastructure/config/DomainConfiguration.java). The rules core retains no Spring annotations.
* Added [HandlerMethodValidationException](file:///c://Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/api/common/error/GlobalExceptionHandler.java) handling to `GlobalExceptionHandler` so method-parameter validation failures produce structured `400` responses instead of generic `500`s.

### 5. Architecture Guardrails
* Added [ArchUnit](https://www.archunit.org/) dependency and the [ArchitectureTest.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/test/java/com/themis/engine/architecture/ArchitectureTest.java) suite. The rules forbid the API and domain packages from importing infrastructure classes and enforce that application command records satisfy value-style invariants.
* Added dedicated unit tests for the character, combat, and encounter application services, mapping behavior, and controller validation/null-safety.

---

## Phase 9: Armor, Optimistic Locking, and Concurrency Safety (Completed)

We added armor equip/unequip support with Pathfinder 1e Dexterity-cap rules, introduced optimistic locking for both aggregates, and hardened the security configuration so production deployments cannot start with known fallbacks.

### 1. Armor Domain and Persistence
* Created [Armor.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Armor.java): An immutable record representing equipped armor or shields. Tracks equipment modifiers per stat plus an optional `maxDexterityBonus`. Constructor deep-copies the modifier map and enforces non-null/non-blank `id` and `name`, and non-negative Dexterity cap.
* Updated [Character.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Character.java) with `equipArmor`/`unequipArmorById` and the armor-aware AC calculation. The aggregate now applies the lowest non-null armor `maxDexterityBonus` as a cap on the Dexterity bonus to AC, following [ADR-002](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/docs/ADR/ADR-002-armor-class-max-dexterity-bonus-capping.md).
* Created Flyway migration [V7__add_armor.sql](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/resources/db/migration/V7__add_armor.sql) adding the `character_equipped_armors` table. Added the matching [CharacterEquippedArmorEntity.java](file:///c://Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/infrastructure/CharacterEquippedArmorEntity.java) and [CharacterEquippedArmorId.java](file:///c://Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/infrastructure/CharacterEquippedArmorId.java) composite key, and extended [CharacterEntity.java](file:///c://Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/infrastructure/CharacterEntity.java) and [PostgresCharacterRepositoryAdapter.java](file:///c://Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/infrastructure/PostgresCharacterRepositoryAdapter.java) to persist/restore armor as a `LinkedHashSet`.
* Added [EquipArmorRequest.java](file:///c://Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/api/character/request/EquipArmorRequest.java) and exposed `POST /api/characters/{id}/equip-armor` and `POST /api/characters/{id}/unequip-armor?armorId=…` on [CharacterController.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/api/character/CharacterController.java).

### 2. Optimistic Locking for Concurrent Writes
* Created Flyway migration [V8__add_optimistic_lock_versions.sql](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/resources/db/migration/V8__add_optimistic_lock_versions.sql) adding a `version BIGINT NOT NULL DEFAULT 0` column to both `characters` and `encounters`.
* Added JPA `@Version` fields on [CharacterEntity.java](file:///c://Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/infrastructure/CharacterEntity.java) and [EncounterEntity.java](file:///c//Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/infrastructure/EncounterEntity.java), and propagated version values through both repository adapter mappings so concurrent read-modify-write requests can no longer silently overwrite each other.
* Surfaced optimistic-lock failures as structured `409 Conflict` responses via a new handler in [GlobalExceptionHandler.java](file:///c://Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/api/common/error/GlobalExceptionHandler.java) covering both `ObjectOptimisticLockingFailureException` and `jakarta.persistence.OptimisticLockException`.
* Exposed the aggregate version in [EncounterResponse.java](file:///c//Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/api/encounter/response/EncounterResponse.java) to enable conditional `If-Match`/ETag workflows.
* Added [OptimisticLockingIntegrationTest.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/test/java/com/themis/engine/infrastructure/OptimisticLockingIntegrationTest.java) to assert that stale updates fail rather than overwrite newer state.

### 3. Production-Safe Configuration and CI
* Tightened [SecurityConfiguration.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/infrastructure/security/SecurityConfiguration.java):
  * CORS now refuses to start with a wildcard or blank `THEMIS_CORS_ALLOWED_ORIGINS` allowlist, exposes `ETag`, and permits `If-Match` headers. Credentials remain disabled.
  * Actuator is split so only `/actuator/health` and `/error` are public; all other routes require the API key.
  * `RateLimitFilter` accepts an explicit `themis.rate-limit.trust-forwarded-headers` flag (default `false`) and a bounded `max-clients` LRU map to prevent unbounded growth when `X-Forwarded-For` is honored.
* Added `.env.example`, `docker-compose.override.yml`, and environment-variable placeholders so production deployments require explicit secrets instead of falling back to development defaults.
* Configured the GitHub Actions build workflow [build.yml](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/.github/workflows/build.yml) with PostgreSQL and Redis service containers so the CI pipeline validates Flyway migrations and adapter behavior against a real PostgreSQL instance rather than only H2.
* Isolated the Redis health check from the test cache configuration to prevent skipped-context failures during local `mvn test`.

### 4. Active Condition Removal by ID
* Added `removeConditionById(conditionId)` to [Character.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Character.java) and plumbed it through `CharacterCommandService.removeCondition` and a new `DELETE /api/characters/{id}/conditions/{conditionId}` endpoint on [CharacterController.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/api/character/CharacterController.java), closing the previously-asymmetric "apply but cannot remove by ID" gap.

---

## Phase 8: Encounter Management (Completed)

We have successfully implemented turn-based combat Grouping/Encounter management. The tracker organizes characters, rolls initiative dynamically (with server and client inputs), handles rounds and turn sequencing, and resets participant action economy on turn-start.

### 1. Domain Design & Generic Combatants
* **Generic Combatants**: Decoupled the initiative tracker from the `Character` class using `EncounterParticipant` record with a `combatantType` (`CHARACTER`, `LAIR_ACTION`, `ENVIRONMENT`) to support arbitrary non-character combatants in the future.
* **Initiative Sorting & Tie-Breakers**: Implemented sorting logic in `Encounter.java` that orders participants descending by initiative total. Ties are resolved by Dexterity modifier descending, and finally by combatant ID.
* **Round & Turn Sequencer**: Advancing the active participant index automatically wraps around and increments the current round index.

### 2. Hybrid Initiative & Turn Reset
* **Hybrid Initiative**: Allowed the API to pass manual initiative rolls (to support physical dice inputs), while automatically rolling 1d20 + Dexterity modifier for any participants missing manual inputs.
* **Automatic Turn Actions Reset**: Advancing the encounter's turn to a `CHARACTER` automatically triggers `character.startTurn()`, resetting standard/move/swift action availability and ticking active condition durations.

### 3. API & Database Persistence
* **Database Schema**: Created Flyway migration `V5` mapping `encounters` and `encounter_participants` tables with ordering indexes to preserve list order.
* **REST Endpoints**: Exposed endpoints under `/api/encounters` to create encounters, add participants, start combat, step to the next turn, and end encounters.

---

## Phase 7: Productionization, Security, and Observability (Completed)

We have successfully implemented the productionization scaffolding to secure the API, rate limit traffic, expose health checks, log in structured JSON, and package the application with Docker and GitHub Actions.

### 1. Security & Traffic Management
* **Static API Key Authentication**: Implemented `ApiKeyAuthFilter` and `ApiKeyAuthenticationToken` to intercept requests to `/api/**` and validate the `X-API-KEY` header. Missing or invalid headers result in a structured `401 Unauthorized` JSON response.
* **IP-based Rate Limiting**: Added `RateLimitFilter` utilizing Bucket4j to restrict requests to a baseline of 100 requests per minute per IP address. Exceeded limits return a structured `429 Too Many Requests` JSON response.
* **CORS Settings**: Configured CORS within `SecurityConfiguration` to permit client integrations.

### 2. Observability & Monitoring
* **Health Checks**: Integrated `spring-boot-starter-actuator` to expose `/actuator/health`. Disabled Redis health checks specifically in tests to avoid test suite failures.
* **Structured JSON Logging**: Implemented `logback-spring.xml` utilizing `logstash-logback-encoder` to format console logs as standardized structured JSON containing trace/span IDs, log levels, thread info, and stack traces.

### 3. Containerization & CI/CD
* **Multi-Stage Dockerfile**: Created a production-ready `Dockerfile` that packages the application with Eclipse Temurin 21 JRE, running under a non-root `appuser` for security hardening.
* **Docker Compose**: Created a `docker-compose.yml` to launch the database (PostgreSQL), cache (Redis), and application (Themis Engine) as a unified local environment with health checks.
* **GitHub Actions Workflow**: Created `.github/workflows/build.yml` to automatically verify code compilations and run unit/integration tests on pull requests and pushes to `main`. It uses the default test profile configurations (H2 database and simple cache) to maintain fast, deterministic builds without requiring external container dependencies.

### 4. Integration Tests
* Added `SecurityAndRateLimitTest.java` to test actuator public routing, API Key enforcement, and Bucket4j IP rate-limiting thresholds.
* Updated all existing API controller integration tests to supply the `X-API-KEY` header.

---

## Phase 6: Action Economy, Advanced Conditions, and Performance Hardening (Completed)

We have successfully implemented the Pathfinder 1e Action Economy flow, extended the Condition system to support durations and stacking groups, and eliminated a critical JPA Cartesian product performance risk.

### 1. Action Economy Flow
* **Turn Reset and Action Consumption**: Added `startTurn()` to [Character.java](src/main/java/com/themis/engine/domain/Character.java) which resets `TurnState` actions and handles ticking down condition durations.
* **REST Endpoints**: Exposed `POST /api/characters/{id}/start-turn` and `POST /api/characters/{id}/consume-action` in [CharacterController.java](src/main/java/com/themis/engine/api/character/CharacterController.java) to reset a turn's action economy and manually consume actions (Standard, Move, Swift, etc.) on the server.
* **Attack Checks**: Integrated TurnState checks in [CombatService.java](src/main/java/com/themis/engine/application/combat/CombatCommandService.java) to verify that an attacker has their standard action available before executing an attack, and consume it upon success.

### 2. Advanced Conditions & Stacking Rules
* **Condition Expiration**: Modified [Condition.java](src/main/java/com/themis/engine/domain/Condition.java) to support rounds durations (`durationRounds`) and stacking groups (`stackingGroup`). Conditions with a remaining duration are automatically decremented at the start of a turn and removed upon expiration.
* **Source-based Stacking**: Updated [ModifierStack.java](src/main/java/com/themis/engine/domain/ModifierStack.java) to group modifiers by source before computing stacks. This prevents overlapping conditions (or conditions of the same stacking group, normalized to the same source in `Condition`) from stacking their penalties/bonuses, even if they are untyped or dodge.

### 3. JPA Cartesian Product Fix (Performance Hardening)
* **Switched EAGER Collections to Set**: Modified [CharacterEntity.java](src/main/java/com/themis/engine/infrastructure/CharacterEntity.java) to use `Set<>` (specifically `LinkedHashSet` to preserve insertion order) instead of `List<>` for its three eagerly fetched `@OneToMany` relationships (items, weapons, and conditions). This resolves the `MultipleBagFetchException` and Cartesian Product database fetch performance risks.
* **Flyway Migration V4**: Created [V4__add_turn_state_and_conditions.sql](src/main/resources/db/migration/V4__add_turn_state_and_conditions.sql) to add turn state tracking columns to the `characters` table and duration/stacking group columns to `character_active_conditions`.
* **Repository Mapping**: Extended [PostgresCharacterRepositoryAdapter.java](src/main/java/com/themis/engine/infrastructure/PostgresCharacterRepositoryAdapter.java) to map these new turn state and condition fields correctly between domain models and database tables.

---

## Phase 5: Technical Debt & Security Hardening (Completed)

We have successfully addressed the critical vulnerabilities, concurrency bugs, missing input validations, and rules logic gaps found during our comprehensive system audit.

### 1. Domain Hardening & Pathfinder Rules Alignment
* **Weapon Types Support**: Created [WeaponType.java](src/main/java/com/themis/engine/domain/WeaponType.java) to support `MELEE`, `RANGED`, and `FINESSE` scaling rules.
* **Refined RuleEngine**: Updated [RuleEngine.java](src/main/java/com/themis/engine/domain/RuleEngine.java) to correctly differentiate attack and damage modifiers:
  * `MELEE`: Attacks use STR, Damage uses STR.
  * `RANGED`: Attacks use DEX, Damage adds 0 attribute modifier.
  * `FINESSE`: Attacks use DEX, Damage uses STR.
* **Deep Modifiers Copying**: Hardened constructors of [Weapon.java](src/main/java/com/themis/engine/domain/Weapon.java), [Condition.java](src/main/java/com/themis/engine/domain/Condition.java), and [EquippableItem.java](src/main/java/com/themis/engine/domain/EquippableItem.java) to deep-copy the internal lists of modifiers, eliminating mutable state leaks.
* **Aggregates & Value Objects Validation**: Enforced constructor range checks in [Character.java](src/main/java/com/themis/engine/domain/Character.java), [Weapon.java](src/main/java/com/themis/engine/domain/Weapon.java), [SpellcastingFeature.java](src/main/java/com/themis/engine/domain/SpellcastingFeature.java), and [DiceRoll.java](src/main/java/com/themis/engine/domain/DiceRoll.java) to catch out-of-bounds stats, negative HP, and invalid critical modifiers.
* **Entity Identity**: Implemented proper `equals()` and `hashCode()` methods in `Character.java` based on the unique aggregate ID.

### 2. Transaction Boundaries & Service Layer
* **Created CharacterService**: Built [CharacterService.java](src/main/java/com/themis/engine/application/character/CharacterCommandService.java) to wrap all character-mutating endpoints in a single `@Transactional` boundary, preventing concurrent update anomalies.
* **Created CombatService**: Built [CombatService.java](src/main/java/com/themis/engine/application/combat/CombatCommandService.java) to ensure both combatants' states are saved atomically in one transaction, eliminating partial state updates on failure.

### 3. API Hardening & Input Validation
* **Decoupled API Contract**: Created flat request DTOs [EquipItemRequest.java](src/main/java/com/themis/engine/api/character/request/EquipItemRequest.java), [EquipWeaponRequest.java](src/main/java/com/themis/engine/api/character/request/EquipWeaponRequest.java), and [ApplyConditionRequest.java](src/main/java/com/themis/engine/api/character/request/ApplyConditionRequest.java) to prevent direct domain deserialization in request bodies.
* **Bean Validation**: Added `spring-boot-starter-validation` and applied `@Valid`, `@NotBlank`, `@Min`, and `@Max` constraints to request DTOs.
* **Global Exception Handling**: Implemented [GlobalExceptionHandler.java](src/main/java/com/themis/engine/api/common/error/GlobalExceptionHandler.java) and [ErrorResponse.java](src/main/java/com/themis/engine/api/common/error/ErrorResponse.java) to map all validations and business exceptions into structured JSON responses (e.g. 400 Bad Request) instead of returning 500 errors.

### 4. Infrastructure Security & Caching Improvements
* **Flyway V3 Migration**: Created [V3__add_weapon_type.sql](src/main/resources/db/migration/V3__add_weapon_type.sql) to add a `type` column to `character_equipped_weapons` with a sensible default of `'MELEE'`.
* **JSON Redis Serialization**: Replaced the fragile `JdkSerializationRedisSerializer` in [RedisConfiguration.java](src/main/java/com/themis/engine/infrastructure/RedisConfiguration.java) with `GenericJackson2JsonRedisSerializer` configured with class typing, preventing serialization vulnerabilities and boosting cache debugging.
* **Secure Settings**: Parameterized all sensitive credentials in [application.yaml](src/main/resources/application.yaml) using environment variable placeholders.

---

## Phase 4: RuleEngine, API, and Caching (Completed)

We have successfully implemented the combat rules engine, weapon systems, a fully functional REST API, and Redis caching of fully computed character aggregates.

### 1. Combat Rules Engine
* Created [DiceRoll.java](src/main/java/com/themis/engine/domain/DiceRoll.java): A Value Object parsing standard dice notations (e.g. `1d8+4`, `2d6`, `d20-1`). It resolves rolls using method argument injection, allowing custom `IntSupplier` (great for deterministic tests) or standard `RandomGenerator` instances.
* Created [Weapon.java](src/main/java/com/themis/engine/domain/Weapon.java): A domain record representing equipped weapons, tracking their damage rolls, critical threat ranges, and critical multipliers.
* Created [AttackResult.java](src/main/java/com/themis/engine/domain/AttackResult.java): An immutable record capturing hit/miss outcomes, critical threat status, roll totals, damage dealt, and formatted descriptive combat logs.
* Created [RuleEngine.java](src/main/java/com/themis/engine/domain/RuleEngine.java): A stateless domain service resolving melee attack combat math. Enforces natural 20 automatic hits, natural 1 automatic misses, threat ranges, d20 confirmations, and critical damage scaling (sum of multiple damage rolls plus multiplied Strength modifiers).

### 2. Weapons Persistence
* Created Flyway migration script [V2__add_weapons.sql](src/main/resources/db/migration/V2__add_weapons.sql) to add the `character_equipped_weapons` table to store equipped weapon parameters.
* Implemented [CharacterEquippedWeaponEntity.java](src/main/java/com/themis/engine/infrastructure/CharacterEquippedWeaponEntity.java) and [CharacterEquippedWeaponId.java](src/main/java/com/themis/engine/infrastructure/CharacterEquippedWeaponId.java) to map weapons to JPA.
* Updated [CharacterEntity.java](src/main/java/com/themis/engine/infrastructure/CharacterEntity.java) and [PostgresCharacterRepositoryAdapter.java](src/main/java/com/themis/engine/infrastructure/PostgresCharacterRepositoryAdapter.java) to save and load equipped weapons.

### 3. REST API Endpoints
* Created [CreateCharacterRequest.java](src/main/java/com/themis/engine/api/character/request/CreateCharacterRequest.java) and [CharacterResponse.java](src/main/java/com/themis/engine/api/character/response/CharacterResponse.java) to map raw requests and fully calculated character aggregate states.
* Created [CharacterController.java](src/main/java/com/themis/engine/api/character/CharacterController.java): Exposes endpoints for creating/retrieving characters, equipping items/weapons, applying conditions, dealing damage, healing, and resting.
* Created [ResolveAttackRequest.java](src/main/java/com/themis/engine/api/combat/request/ResolveAttackRequest.java) and [CombatController.java](src/main/java/com/themis/engine/api/combat/CombatController.java): Resolves melee attacks using the `RuleEngine` over HTTP and persists updated character states.

### 4. Redis Caching
* Created [RedisConfiguration.java](src/main/java/com/themis/engine/infrastructure/RedisConfiguration.java) to configure a standard `RedisCacheManager` utilizing a `JdkSerializationRedisSerializer`.
* Added `@Cacheable` and `@CachePut` caching annotations to [PostgresCharacterRepositoryAdapter.java](src/main/java/com/themis/engine/infrastructure/PostgresCharacterRepositoryAdapter.java) to cache the *fully computed* `Character` domain aggregate root rather than raw database entities, boosting read performance.
* Set up conditional loading via `@ConditionalOnProperty` so that the Redis configuration remains inactive during testing to prevent localhost connection errors.

### 5. Combat and API Testing
* Created [DiceRollTest.java](src/test/java/com/themis/engine/domain/DiceRollTest.java) and [RuleEngineTest.java](src/test/java/com/themis/engine/domain/RuleEngineTest.java) to thoroughly cover combat rules, critical confirmations, and damage calculations under seedable/mocked random generators.
* Created [CharacterControllerTest.java](src/test/java/com/themis/engine/api/CharacterControllerTest.java) and [CombatControllerTest.java](src/test/java/com/themis/engine/api/CombatControllerTest.java) to verify REST API requests and combat flows.

---

## Phase 3: Systems and Content Infrastructure (Completed)

We have successfully implemented turn-based action economy tracking, spell slot tracking, dynamic spell save DC calculations, and the PostgreSQL database persistence layer utilizing Flyway migrations and strict Hexagonal Architecture boundaries.

### 1. Action Economy System
* Created [ActionType.java](src/main/java/com/themis/engine/domain/ActionType.java): Enum for standard, move, swift, free, and full-round actions.
* Created [TurnState.java](src/main/java/com/themis/engine/domain/TurnState.java): Tracks turn-based actions. Implements rules for full-round action consumption (depleting standard and move) and downgrading a standard action to a move action (allowing double movement).
* Integrated turn state into the [Character.java](src/main/java/com/themis/engine/domain/Character.java) aggregate.

### 2. Spellcasting Domain
* Created [Spell.java](src/main/java/com/themis/engine/domain/Spell.java): Record representing spell ID, name, level, and action economy cost.
* Created [SpellcastingFeature.java](src/main/java/com/themis/engine/domain/SpellcastingFeature.java): Class representing magical stats. Tracks max and available spell slots (levels 0-9) and calculates spell save DCs (`10 + Spell Level + Casting Attribute Modifier`).
* Integrated spellcasting features into `Character.java`, exposing the dynamic `getSpellSaveDC(spellLevel)` helper which computes save DCs using the current modified attribute score (incorporating items and active conditions).

### 3. PostgreSQL Persistence & Flyway Migrations
* Configured [application.yaml](src/main/resources/application.yaml) with PostgreSQL datasource settings and validation checks.
* Created [V1__init_schema.sql](src/main/resources/db/migration/V1__init_schema.sql): Initial migration defining `characters`, `character_equipped_items` (with cascade delete), and `character_active_conditions` tables.
* Created pure domain outbound port [CharacterStore.java](src/main/java/com/themis/engine/domain/CharacterStore.java) to avoid leaking persistence annotations.
* Implemented JPA entities and composite keys:
  * [CharacterEntity.java](src/main/java/com/themis/engine/infrastructure/CharacterEntity.java)
  * [CharacterEquippedItemEntity.java](src/main/java/com/themis/engine/infrastructure/CharacterEquippedItemEntity.java) / [CharacterEquippedItemId.java](src/main/java/com/themis/engine/infrastructure/CharacterEquippedItemId.java)
  * [CharacterActiveConditionEntity.java](src/main/java/com/themis/engine/infrastructure/CharacterActiveConditionEntity.java) / [CharacterActiveConditionId.java](src/main/java/com/themis/engine/infrastructure/CharacterActiveConditionId.java)
* Implemented [PostgresCharacterRepositoryAdapter.java](src/main/java/com/themis/engine/infrastructure/PostgresCharacterRepositoryAdapter.java): Maps domain aggregates to database entity representations, storing equipment and condition modifiers as JSON strings. Reconstitutes the domain aggregate in correct sequence to prevent incorrect state clamping.

### 4. Tests and Verification
* Created [ActionEconomyAndSpellcastingTest.java](src/test/java/com/themis/engine/domain/ActionEconomyAndSpellcastingTest.java): Validates the full-round consumption, standard-to-move downgrade, slot exhaustion, rest/recovery mechanics, and dynamic save DC calculations under item stat buffs.
* Created [PostgresCharacterRepositoryAdapterTest.java](src/test/java/com/themis/engine/infrastructure/PostgresCharacterRepositoryAdapterTest.java): Uses H2 database in test environment to test Flyway migrations and verify the complete save/load cycle of a complex character.

---

## Phase 2: Core Entities and Character Aggregate (Completed)

We have successfully implemented and verified the **Character Aggregate Root** and the associated stats framework, integrating it with the Phase 1 Modifier Stacking Engine.

### 1. Architecture Decision Records (ADR)
* Updated [DECISIONS.md](docs/ADR/DECISIONS.md) to log:
  * **ADR 2:** Capping Dexterity bonuses to AC via Armor rules is deferred to Phase 3.
  * **ADR 3:** Hit Points are calculated dynamically based on Base HP, Constitution Modifier, and Level. Current health is tracked via cumulative `currentDamage` (Current HP = Max HP - `currentDamage`), allowing automatic HP scaling when Constitution changes.

### 2. Domain Implementation
* Created [StatType.java](src/main/java/com/themis/engine/domain/StatType.java): Enum identifying all target statistics (Attributes, Saves, Armor Class, Hit Points, BAB).
* Created [Attribute.java](src/main/java/com/themis/engine/domain/Attribute.java): Represents a core attribute (e.g. Strength) with its own `ModifierStack`. Calculates scores dynamically and determines modifiers using integer floor division.
* Created [DerivedStat.java](src/main/java/com/themis/engine/domain/DerivedStat.java): Represents derived stats (AC, Saves) combining a base value, attribute modifier, and generic modifiers.
* Created [EquippableItem.java](src/main/java/com/themis/engine/domain/EquippableItem.java) and [Condition.java](src/main/java/com/themis/engine/domain/Condition.java): Records that map target stats to lists of active modifiers they provide.
* Created [Character.java](src/main/java/com/themis/engine/domain/Character.java): The Aggregate Root. Orchestrates attribute maps, derived saving throws, HP, and AC calculations. Exposes:
  * `equip(item)` and `unequip(item)`
  * `applyCondition(condition)` and `removeCondition(condition)`
  * `damage(amount)` and `heal(amount)`
  * `isConscious()` (staggered/conscious at >= 0 HP)
  * `isDead()` (death when negative HP exceeds Constitution score)

### 3. Tests and Verification
* Created [CharacterTest.java](src/test/java/com/themis/engine/domain/CharacterTest.java): Validates base attribute scoring, derived stats math, equippable items, condition stacking, damage/healing thresholds, and dynamic Constitution increases (which scale max/current HP in real-time).

---

## Phase 1: The Modifier Stacking Engine (Completed)

Enforces stacking rules for modifiers in Pathfinder 1e.

### 1. Architecture Decision Records (ADR)
* Created [DECISIONS.md](docs/ADR/DECISIONS.md) to log:
  * **ADR 1:** Represents modifier sources as a structured `ModifierSource` record (refactored from the legacy `String` representation, migrating old database schemas via Flyway Java-based migration and contracting the domain model).

### 2. Domain Implementation
* Created [ModifierType.java](src/main/java/com/themis/engine/domain/ModifierType.java): Enum flagging stackable (`DODGE`, `UNTYPED`) and non-stackable modifier types.
* Created [Modifier.java](src/main/java/com/themis/engine/domain/Modifier.java): Immutable record wrapping value, type, and source.
* Created [ModifierStack.java](src/main/java/com/themis/engine/domain/ModifierStack.java): Aggregates modifiers and implements Pathfinder 1e stacking calculations (resolving non-stackable bonuses and penalties separately).

### 3. Tests and Verification
* Created [ModifierStackTest.java](src/test/java/com/themis/engine/domain/ModifierStackTest.java): Validates Dodge stacking, highest-only rules for non-stackable types, and penalty resolution.

---

## Global Verification Results

We executed the full test suite using `mvn test` on 2026-08-03 (Java 21, Spring Boot 3.5.15):
* **Total Tests Run:** 134
* **Failures / Errors:** 0
* **Skipped:** 1 (`ThemisEngineApplicationTests` — guarded by Testcontainers and requires Docker)
* **Architecture (ArchUnit) tests:** 27 enforcing layer boundaries and command-record invariants
* **All 133 active unit and integration tests passed successfully; BUILD SUCCESS.**

Test coverage spans the domain rules, application command/query services, REST controllers, API mappers, security and rate-limit filters, optimistic locking, JPA repository adapters, and the ArchUnit guardrail suite.
