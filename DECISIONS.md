# Architecture Decision Log (ADR)

This file documents key design and architectural decisions made during the development of Themis Engine.

## ADR 1: Modifier Source Representation

### Status
Accepted (Temporary/Tech Debt)

### Context
In Pathfinder 1e, modifiers are applied from various sources (e.g., spells, feats, racial traits, item enhancement, temporary conditions). To prevent multiple modifiers from the same source stacking (even if their types normally allow it, or to track active effects for cleanup), we need to track the source of each modifier.

### Decision
For Phase 1 (MVP/Prototype), the `source` field in `Modifier` will be represented as a simple `String` (e.g., `"Bless"`, `"Amulet of Natural Armor +1"`). 

### Consequences
* **Pros:** Extremely fast to implement, lightweight, and easy to print/debug in tests.
* **Cons:** Creates significant technical debt. String matching is error-prone, doesn't enforce schema (like distinguishing an Item ID from a Spell ID), and makes validation of source-uniqueness or dynamic cleanup fragile.
* **Refactoring Note:** **CRITICAL.** Before Phase 2 begins or the codebase grows significantly, this must be refactored into a structured `Source` value object or entity (e.g., containing `SourceType` enum and `SourceId` UUID/String) to prevent domain degradation.

---

## ADR 2: Armor Class Max Dexterity Bonus Capping

### Status
Accepted (Deferred to Phase 3)

### Context
In Pathfinder 1e, wearing medium or heavy armor caps the maximum Dexterity modifier that can be added to the character's Armor Class (AC). 

### Decision
For Phase 2, we will calculate Armor Class simply as `10 + Dex Modifier + AC Modifiers` without applying any maximum cap. The capping logic will be implemented in Phase 3 when full equipment rules are introduced.

### Consequences
* **Pros:** Simplifies the Phase 2 implementation of the Character aggregate.
* **Cons:** Calculated AC for heavily armored characters with high Dexterity will be temporarily incorrect according to official rules.

---

## ADR 3: Hit Points Tracking Structure

### Status
Accepted

### Context
Characters can take damage and be healed, altering their current health status. They also have a maximum hit points pool, which is dynamically affected by changes to their Constitution modifier.

### Decision
We will calculate maximum hit points dynamically as `baseHitPoints + ConstitutionModifier` (plus any temporary HP or other modifiers). Current health will be tracked by storing the cumulative `currentDamage` rather than storing a mutable `currentHitPoints` value. 
* Current HP is computed as: `MaxHP - currentDamage`.

### Consequences
* **Pros:** Automatically adjusts the character's current health when their Constitution modifier changes (e.g., if a character gains Constitution, their Max HP and Current HP both increase by the same amount, which matches the official Pathfinder rules).
* **Cons:** Slightly more calculation overhead (subtraction on retrieval) but negligible in a JVM environment.

