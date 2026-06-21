# Walkthrough: Implementation History

This document tracks completed implementation phases for **Themis Engine**.

---

## Phase 7: Productionization, Security, and Observability (Completed)

We have successfully implemented the productionization scaffolding to secure the API, rate limit traffic, expose health checks, log in structured JSON, and package the application with Docker and GitHub Actions.

### 1. Security & Traffic Management
* **Static API Key Authentication**: Implemented `ApiKeyAuthFilter` and `ApiKeyAuthenticationToken` to intercept requests to `/api/**` and validate the `X-API-KEY` header. Missing or invalid headers result in a structured `401 Unauthorized` JSON response.
* **IP-based Rate Limiting**: Added `RateLimitFilter` utilizing Bucket4j to restrict requests to a baseline of 100 requests per minute per IP address. Exceeded limits return a structured `429 Too Many Requests` JSON response.
* **CORS Settings**: Configured CORS within `SecurityConfiguration` to permit client integrations.

### 2. Observability & Monitoring
* **Health Checks**: Integrated `spring-boot-starter-actuator` to expose `/actuator/health` and `/actuator/prometheus` (for Prometheus scraping). Disabled Redis health checks specifically in tests to avoid test suite failures.
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
* **Turn Reset and Action Consumption**: Added `startTurn()` to [Character.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Character.java) which resets `TurnState` actions and handles ticking down condition durations.
* **REST Endpoints**: Exposed `POST /api/characters/{id}/start-turn` and `POST /api/characters/{id}/consume-action` in [CharacterController.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/api/CharacterController.java) to reset a turn's action economy and manually consume actions (Standard, Move, Swift, etc.) on the server.
* **Attack Checks**: Integrated TurnState checks in [CombatService.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/CombatService.java) to verify that an attacker has their standard action available before executing an attack, and consume it upon success.

### 2. Advanced Conditions & Stacking Rules
* **Condition Expiration**: Modified [Condition.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Condition.java) to support rounds durations (`durationRounds`) and stacking groups (`stackingGroup`). Conditions with a remaining duration are automatically decremented at the start of a turn and removed upon expiration.
* **Source-based Stacking**: Updated [ModifierStack.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/ModifierStack.java) to group modifiers by source before computing stacks. This prevents overlapping conditions (or conditions of the same stacking group, normalized to the same source in `Condition`) from stacking their penalties/bonuses, even if they are untyped or dodge.

### 3. JPA Cartesian Product Fix (Performance Hardening)
* **Switched EAGER Collections to Set**: Modified [CharacterEntity.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/infrastructure/CharacterEntity.java) to use `Set<>` (specifically `LinkedHashSet` to preserve insertion order) instead of `List<>` for its three eagerly fetched `@OneToMany` relationships (items, weapons, and conditions). This resolves the `MultipleBagFetchException` and Cartesian Product database fetch performance risks.
* **Flyway Migration V4**: Created [V4__add_turn_state_and_conditions.sql](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/resources/db/migration/V4__add_turn_state_and_conditions.sql) to add turn state tracking columns to the `characters` table and duration/stacking group columns to `character_active_conditions`.
* **Repository Mapping**: Extended [PostgresCharacterRepositoryAdapter.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/infrastructure/PostgresCharacterRepositoryAdapter.java) to map these new turn state and condition fields correctly between domain models and database tables.

---

## Phase 5: Technical Debt & Security Hardening (Completed)

We have successfully addressed the critical vulnerabilities, concurrency bugs, missing input validations, and rules logic gaps found during our comprehensive system audit.

### 1. Domain Hardening & Pathfinder Rules Alignment
* **Weapon Types Support**: Created [WeaponType.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/WeaponType.java) to support `MELEE`, `RANGED`, and `FINESSE` scaling rules.
* **Refined RuleEngine**: Updated [RuleEngine.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/RuleEngine.java) to correctly differentiate attack and damage modifiers:
  * `MELEE`: Attacks use STR, Damage uses STR.
  * `RANGED`: Attacks use DEX, Damage adds 0 attribute modifier.
  * `FINESSE`: Attacks use DEX, Damage uses STR.
* **Deep Modifiers Copying**: Hardened constructors of [Weapon.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Weapon.java), [Condition.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Condition.java), and [EquippableItem.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/EquippableItem.java) to deep-copy the internal lists of modifiers, eliminating mutable state leaks.
* **Aggregates & Value Objects Validation**: Enforced constructor range checks in [Character.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Character.java), [Weapon.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Weapon.java), [SpellcastingFeature.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/SpellcastingFeature.java), and [DiceRoll.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/DiceRoll.java) to catch out-of-bounds stats, negative HP, and invalid critical modifiers.
* **Entity Identity**: Implemented proper `equals()` and `hashCode()` methods in `Character.java` based on the unique aggregate ID.

### 2. Transaction Boundaries & Service Layer
* **Created CharacterService**: Built [CharacterService.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/CharacterService.java) to wrap all character-mutating endpoints in a single `@Transactional` boundary, preventing concurrent update anomalies.
* **Created CombatService**: Built [CombatService.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/CombatService.java) to ensure both combatants' states are saved atomically in one transaction, eliminating partial state updates on failure.

### 3. API Hardening & Input Validation
* **Decoupled API Contract**: Created flat request DTOs [EquipItemRequestDto.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/api/EquipItemRequestDto.java), [EquipWeaponRequestDto.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/api/EquipWeaponRequestDto.java), and [ApplyConditionRequestDto.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/api/ApplyConditionRequestDto.java) to prevent direct domain deserialization in request bodies.
* **Bean Validation**: Added `spring-boot-starter-validation` and applied `@Valid`, `@NotBlank`, `@Min`, and `@Max` constraints to request DTOs.
* **Global Exception Handling**: Implemented [GlobalExceptionHandler.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/api/GlobalExceptionHandler.java) and [ErrorResponse.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/api/ErrorResponse.java) to map all validations and business exceptions into structured JSON responses (e.g. 400 Bad Request) instead of returning 500 errors.

### 4. Infrastructure Security & Caching Improvements
* **Flyway V3 Migration**: Created [V3__add_weapon_type.sql](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/resources/db/migration/V3__add_weapon_type.sql) to add a `type` column to `character_equipped_weapons` with a sensible default of `'MELEE'`.
* **JSON Redis Serialization**: Replaced the fragile `JdkSerializationRedisSerializer` in [RedisConfiguration.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/RedisConfiguration.java) with `GenericJackson2JsonRedisSerializer` configured with class typing, preventing serialization vulnerabilities and boosting cache debugging.
* **Secure Settings**: Parameterized all sensitive credentials in [application.yaml](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/resources/application.yaml) using environment variable placeholders.

---

## Phase 4: RuleEngine, API, and Caching (Completed)

We have successfully implemented the combat rules engine, weapon systems, a fully functional REST API, and Redis caching of fully computed character aggregates.

### 1. Combat Rules Engine
* Created [DiceRoll.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/DiceRoll.java): A Value Object parsing standard dice notations (e.g. `1d8+4`, `2d6`, `d20-1`). It resolves rolls using method argument injection, allowing custom `IntSupplier` (great for deterministic tests) or standard `RandomGenerator` instances.
* Created [Weapon.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Weapon.java): A domain record representing equipped weapons, tracking their damage rolls, critical threat ranges, and critical multipliers.
* Created [AttackResult.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/AttackResult.java): An immutable record capturing hit/miss outcomes, critical threat status, roll totals, damage dealt, and formatted descriptive combat logs.
* Created [RuleEngine.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/RuleEngine.java): A stateless domain service resolving melee attack combat math. Enforces natural 20 automatic hits, natural 1 automatic misses, threat ranges, d20 confirmations, and critical damage scaling (sum of multiple damage rolls plus multiplied Strength modifiers).

### 2. Weapons Persistence
* Created Flyway migration script [V2__add_weapons.sql](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/resources/db/migration/V2__add_weapons.sql) to add the `character_equipped_weapons` table to store equipped weapon parameters.
* Implemented [CharacterEquippedWeaponEntity.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/CharacterEquippedWeaponEntity.java) and [CharacterEquippedWeaponId.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/CharacterEquippedWeaponId.java) to map weapons to JPA.
* Updated [CharacterEntity.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/CharacterEntity.java) and [PostgresCharacterRepositoryAdapter.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/PostgresCharacterRepositoryAdapter.java) to save and load equipped weapons.

### 3. REST API Endpoints
* Created [CharacterRequestDto.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/api/CharacterRequestDto.java) and [CharacterResponseDto.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/api/CharacterResponseDto.java) to map raw requests and fully calculated character aggregate states.
* Created [CharacterController.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/api/CharacterController.java): Exposes endpoints for creating/retrieving characters, equipping items/weapons, applying conditions, dealing damage, healing, and resting.
* Created [AttackRequestDto.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/api/AttackRequestDto.java) and [CombatController.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/api/CombatController.java): Resolves melee attacks using the `RuleEngine` over HTTP and persists updated character states.

### 4. Redis Caching
* Created [RedisConfiguration.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/RedisConfiguration.java) to configure a standard `RedisCacheManager` utilizing a `JdkSerializationRedisSerializer`.
* Added `@Cacheable` and `@CachePut` caching annotations to [PostgresCharacterRepositoryAdapter.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/PostgresCharacterRepositoryAdapter.java) to cache the *fully computed* `Character` domain aggregate root rather than raw database entities, boosting read performance.
* Set up conditional loading via `@ConditionalOnProperty` so that the Redis configuration remains inactive during testing to prevent localhost connection errors.

### 5. Combat and API Testing
* Created [DiceRollTest.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/test/java/com/themis/engine/domain/DiceRollTest.java) and [RuleEngineTest.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/test/java/com/themis/engine/domain/RuleEngineTest.java) to thoroughly cover combat rules, critical confirmations, and damage calculations under seedable/mocked random generators.
* Created [CharacterControllerTest.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/test/java/com/themis/engine/api/CharacterControllerTest.java) and [CombatControllerTest.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/test/java/com/themis/engine/api/CombatControllerTest.java) to verify REST API requests and combat flows.

---

## Phase 3: Systems and Content Infrastructure (Completed)

We have successfully implemented turn-based action economy tracking, spell slot tracking, dynamic spell save DC calculations, and the PostgreSQL database persistence layer utilizing Flyway migrations and strict Hexagonal Architecture boundaries.

### 1. Action Economy System
* Created [ActionType.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/ActionType.java): Enum for standard, move, swift, free, and full-round actions.
* Created [TurnState.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/TurnState.java): Tracks turn-based actions. Implements rules for full-round action consumption (depleting standard and move) and downgrading a standard action to a move action (allowing double movement).
* Integrated turn state into the [Character.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Character.java) aggregate.

### 2. Spellcasting Domain
* Created [Spell.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Spell.java): Record representing spell ID, name, level, and action economy cost.
* Created [SpellcastingFeature.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/SpellcastingFeature.java): Class representing magical stats. Tracks max and available spell slots (levels 0-9) and calculates spell save DCs (`10 + Spell Level + Casting Attribute Modifier`).
* Integrated spellcasting features into `Character.java`, exposing the dynamic `getSpellSaveDC(spellLevel)` helper which computes save DCs using the current modified attribute score (incorporating items and active conditions).

### 3. PostgreSQL Persistence & Flyway Migrations
* Configured [application.yaml](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/resources/application.yaml) with PostgreSQL datasource settings and validation checks.
* Created [V1__init_schema.sql](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/resources/db/migration/V1__init_schema.sql): Initial migration defining `characters`, `character_equipped_items` (with cascade delete), and `character_active_conditions` tables.
* Created pure domain outbound port [CharacterStore.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/CharacterStore.java) to avoid leaking persistence annotations.
* Implemented JPA entities and composite keys:
  * [CharacterEntity.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/CharacterEntity.java)
  * [CharacterEquippedItemEntity.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/CharacterEquippedItemEntity.java) / [CharacterEquippedItemId.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/CharacterEquippedItemId.java)
  * [CharacterActiveConditionEntity.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/CharacterActiveConditionEntity.java) / [CharacterActiveConditionId.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/CharacterActiveConditionId.java)
* Implemented [PostgresCharacterRepositoryAdapter.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/infrastructure/PostgresCharacterRepositoryAdapter.java): Maps domain aggregates to database entity representations, storing equipment and condition modifiers as JSON strings. Reconstitutes the domain aggregate in correct sequence to prevent incorrect state clamping.

### 4. Tests and Verification
* Created [ActionEconomyAndSpellcastingTest.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/test/java/com/themis/engine/domain/ActionEconomyAndSpellcastingTest.java): Validates the full-round consumption, standard-to-move downgrade, slot exhaustion, rest/recovery mechanics, and dynamic save DC calculations under item stat buffs.
* Created [PostgresCharacterRepositoryAdapterTest.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/test/java/com/themis/engine/infrastructure/PostgresCharacterRepositoryAdapterTest.java): Uses H2 database in test environment to test Flyway migrations and verify the complete save/load cycle of a complex character.

---

## Phase 2: Core Entities and Character Aggregate (Completed)

We have successfully implemented and verified the **Character Aggregate Root** and the associated stats framework, integrating it with the Phase 1 Modifier Stacking Engine.

### 1. Architecture Decision Records (ADR)
* Updated [DECISIONS.md](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/DECISIONS.md) to log:
  * **ADR 2:** Capping Dexterity bonuses to AC via Armor rules is deferred to Phase 3.
  * **ADR 3:** Hit Points are calculated dynamically based on Base HP, Constitution Modifier, and Level. Current health is tracked via cumulative `currentDamage` (Current HP = Max HP - `currentDamage`), allowing automatic HP scaling when Constitution changes.

### 2. Domain Implementation
* Created [StatType.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/StatType.java): Enum identifying all target statistics (Attributes, Saves, Armor Class, Hit Points, BAB).
* Created [Attribute.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Attribute.java): Represents a core attribute (e.g. Strength) with its own `ModifierStack`. Calculates scores dynamically and determines modifiers using integer floor division.
* Created [DerivedStat.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/DerivedStat.java): Represents derived stats (AC, Saves) combining a base value, attribute modifier, and generic modifiers.
* Created [EquippableItem.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/EquippableItem.java) and [Condition.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Condition.java): Records that map target stats to lists of active modifiers they provide.
* Created [Character.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Character.java): The Aggregate Root. Orchestrates attribute maps, derived saving throws, HP, and AC calculations. Exposes:
  * `equip(item)` and `unequip(item)`
  * `applyCondition(condition)` and `removeCondition(condition)`
  * `damage(amount)` and `heal(amount)`
  * `isConscious()` (staggered/conscious at >= 0 HP)
  * `isDead()` (death when negative HP exceeds Constitution score)

### 3. Tests and Verification
* Created [CharacterTest.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/test/java/com/themis/engine/domain/CharacterTest.java): Validates base attribute scoring, derived stats math, equippable items, condition stacking, damage/healing thresholds, and dynamic Constitution increases (which scale max/current HP in real-time).

---

## Phase 1: The Modifier Stacking Engine (Completed)

Enforces stacking rules for modifiers in Pathfinder 1e.

### 1. Architecture Decision Records (ADR)
* Created [DECISIONS.md](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/DECISIONS.md) to log:
  * **ADR 1:** Represents modifier sources as `String` for Phase 1, marking it as critical tech debt to refactor before Phase 2.

### 2. Domain Implementation
* Created [ModifierType.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/ModifierType.java): Enum flagging stackable (`DODGE`, `UNTYPED`) and non-stackable modifier types.
* Created [Modifier.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/Modifier.java): Immutable record wrapping value, type, and source.
* Created [ModifierStack.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/main/java/com/themis/engine/domain/ModifierStack.java): Aggregates modifiers and implements Pathfinder 1e stacking calculations (resolving non-stackable bonuses and penalties separately).

### 3. Tests and Verification
* Created [ModifierStackTest.java](file:///c:/Users/Guven%20Servis/Desktop/themis-engine/src/test/java/com/themis/engine/domain/ModifierStackTest.java): Validates Dodge stacking, highest-only rules for non-stackable types, and penalty resolution.

---

## Global Verification Results

We executed the full test suite using `mvn test`:
* **Total Tests Run:** 55
* **Failures / Errors:** 0
* **Skipped (Spring Testcontainers Integration Test):** 1 (requires Docker environment)
* **All 54 unit and integration tests passed successfully!**
