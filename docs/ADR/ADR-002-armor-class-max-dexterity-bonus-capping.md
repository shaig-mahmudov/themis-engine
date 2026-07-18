# ADR-002: Armor Class Max Dexterity Bonus Capping

## Status

Resolved (Completed)

## Context

In Pathfinder 1e, worn armor can cap the maximum Dexterity modifier added to Armor Class (AC). This restriction needs a clear home without adding armor-specific behavior to the generic `EquippableItem` model.

## Decision

Introduce a dedicated `Armor` domain record containing:

- `id` (`String`)
- `name` (`String`)
- `modifiers` (`Map<StatType, List<Modifier>>`)
- `maxDexterityBonus` (`Integer`)

The `Character` aggregate maintains equipped armor and shields separately from generic items. When calculating AC, it caps the Dexterity modifier at the lowest non-null `maxDexterityBonus` among equipped armor. Armor is persisted in the dedicated `character_equipped_armors` table.

## Consequences

### Positive

- Keeps the generic equipment model free of armor-specific rules.
- Makes AC calculation rules-compliant for armored characters.
- Gives armor persistence and future armor behavior a clear extension point.

### Negative

- Adds a domain type, aggregate collection, persistence entity, mapping logic, API DTOs, and a database table.
- Equipping multiple armor-like records requires the aggregate to consistently apply the most restrictive cap.

## Implementation

The decision is implemented by `Armor`, `Character#getArmorClass`, armor endpoints and DTOs, JPA armor entities, repository mapping, and Flyway migration `V7__add_armor.sql`.
