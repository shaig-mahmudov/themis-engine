# Architecture Decision Log (ADR)

This file documents key design and architectural decisions made during the development of Themis Engine.

## ADR 1: Modifier Source Representation

### Status
Resolved (Completed)

### Context
In Pathfinder 1e, modifiers are applied from various sources (e.g., spells, feats, racial traits, item enhancement, temporary conditions). To prevent multiple modifiers from the same source stacking (even if their types normally allow it, or to track active effects for cleanup), we need to track the source of each modifier.

### Decision
Refactor `Modifier`'s source field from a raw `String` to a structured `ModifierSource` record containing:
- `id` (String)
- `name` (String)
- `type` (SourceType enum: `ITEM`, `SPELL`, `CONDITION`, `FEAT`, `RACIAL`, `TRAIT`, `GENERIC`)

To safely migrate existing serialized database records without downtime, we follow the **Expand-Migrate-Contract** pattern:
1. **Phase 1: Expand (Current)**: Implement the new structured `ModifierSource` representation. Deploy a temporary `@JsonCreator` fallback mechanism in `Modifier` to safely read legacy string-based source fields and map them to `GENERIC` type with `WARN` logs. All new writes will strictly use the structured JSON format.
2. **Phase 2: Migrate**: Run a background chunk-based database migration (via script or Flyway) to batch-update historical rows to the new structured JSON representation.
3. **Phase 3: Contract**: Once legacy usage metrics drop to zero, remove the `@JsonCreator` fallback code and keep the codebase clean.

### Consequences
* **Pros:** Enhances type safety, enforces schema for modifiers, improves stacking correctness, and avoids downtime during migration.
* **Cons:** Temporary code complexity due to fallback serialization support.

---

## ADR 2: Armor Class Max Dexterity Bonus Capping

### Status
Resolved (Completed)

### Context
In Pathfinder 1e, wearing armor caps the maximum Dexterity modifier that can be added to the character's Armor Class (AC). We need to implement this restriction cleanly without cluttering the generic EquippableItem definition.

### Decision
We introduced a dedicated `Armor` domain record containing:
- `id` (String)
- `name` (String)
- `modifiers` (Map<StatType, List<Modifier>>)
- `maxDexterityBonus` (Integer)

The `Character` aggregate root was modified to maintain a collection of equipped armors and shields, and `getArmorClass()` was updated to cap the Dexterity modifier by the lowest `maxDexterityBonus` of all equipped items. The data is persisted in a separate `character_equipped_armors` table mapping 1-to-1 with database entities.

### Consequences
* **Pros:** Clean domain model separation, keeps EquippableItem generic, rules-compliant AC calculation for armored characters.
* **Cons:** Introduces a new database table and domain concept, adding slightly more persistence mapping.

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

