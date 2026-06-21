# Themis Engine
**Lead Architect:** Shaig
**Document Status:** Core Architecture Definition

---

## 1. Executive Summary

The **Themis Engine** is a robust backend rules engine designed to programmatically resolve the complex mechanics of the Pathfinder First Edition tabletop role-playing game. Acting as an impartial computational referee, the engine automates systemic calculations, including base statistics, hit points, spell slots, condition effects, and intricate modifier stacking rules. By abstracting the extensive rule set into a highly cohesive domain model, Themis Engine provides a reliable, scalable foundation for character management, encounter simulation, and digital tabletop integrations.

## 2. Technology Stack

The infrastructure prioritizes high performance, strict domain isolation, and maintainability to handle the heavily relational nature of tabletop mechanics.

| Component | Technology | Rationale |
| :--- | :--- | :--- |
| **Language** | Java 21 LTS | Leverages mature pattern matching, records, and sealed interfaces to parse complex rules and conditions efficiently. |
| **Framework** | Spring Boot 3.x | Delivers robust dependency injection, REST/WebSocket capabilities, and aligns perfectly with modern Java enterprise standards. |
| **Database** | PostgreSQL | Handles the highly relational data architecture required for game entities (classes, features, prerequisites, spells). |
| **Caching** | Redis | Mitigates the computational expense of dynamic stat generation by caching calculated character states. |
| **Testing** | JUnit 5 & Mockito | Essential for the rigorous Test-Driven Development (TDD) required to validate stacking rules and domain logic. |
| **Infrastructure**| Docker Compose | Facilitates isolated, reproducible local environments for data and caching layers. |
| **CI/CD** | GitHub Actions | Automates testing pipelines to enforce rule validations on every commit. |

---

## 3. System Architecture

The system utilizes a **Hexagonal Architecture** (Ports and Adapters) to completely isolate the core Pathfinder rules from web controllers and database constraints. The core logic is structured strictly using **Domain-Driven Design (DDD)** principles.

### Value Objects (Immutable)
* **DiceRoll:** Represents a static roll formula (e.g., `1d8+4`).
* **Modifier:** Represents a numerical bonus and its categorical type (e.g., `+2 Enhancement`).
* **StatValue:** Represents a discrete statistic measurement.
* **Distance:** Represents spatial measurements on a grid.

### Entities (Mutable with Identity)
* **Spell:** Represents a castable magical effect and its parameters.
* **Weapon:** Represents an equippable combat item with specific damage and critical properties.
* **Feat:** Represents a specific character ability, passive trait, or combat maneuver.
* **Condition:** Represents an active status effect altering state (e.g., *Shaken*, *Grappled*).

### Aggregate Roots
* **Character:** Functions as the primary transaction boundary. State changes, such as taking damage or recalculating Armor Class (AC), are routed exclusively through this root.

### Domain Services
* **RuleEngine:** Acts as the system's "referee". A stateless domain service responsible for resolving complex, multi-entity interactions (e.g., Character A attacking Character B, calculating AC vs. Attack Roll, and applying damage).

---

## 4. Implementation Roadmap

### Phase 1: The Modifier Stacking Engine (TDD Focus)
* Initialize the core project in pure Java, strictly deferring database and API integration.
* Develop the `Modifier` value object.
* Implement the core algorithmic stacking logic enforcing official rules: Dodge and Untyped bonuses stack indefinitely, while Enhancement, Morale, Deflection, and Armor bonuses apply only the highest respective value.
* Establish a comprehensive test suite to validate all edge cases and variable interactions.

### Phase 2: Core Entities and Character Aggregate
* Construct the `Character` aggregate, encompassing base attributes (Strength, Dexterity, Constitution, Intelligence, Wisdom, Charisma).
* Implement standard derived statistics: Hit Points, Base Attack Bonus (BAB), Saving Throws, and Armor Class.
* Integrate the Phase 1 Modifier Engine to ensure item equipment and condition applications dynamically update the calculated character state.

### Phase 3: Systems and Content Infrastructure
* Develop the Action Economy system regulating Standard, Move, Swift, Free, and Full-Round actions.
* Construct the Spellcasting domain, managing Spell Slots, Caster Levels, and Save Difficulty Classes (DCs).
* Design and deploy the PostgreSQL schema to persist raw, relational game data and feature lists.

### Phase 4: API, Caching, and Automation
* Expose domain operations through Spring Boot REST controllers and WebSocket endpoints for real-time synchronization.
* Integrate Redis caching to store fully calculated character sheets, implementing precise cache invalidation triggers for active state changes.
* Finalize GitHub Actions pipelines to execute the complete TDD validation suite continuously during deployment.