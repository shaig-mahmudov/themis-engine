# ADR-001: Modifier Source Representation

## Status

Resolved (Completed)

## Context

In Pathfinder 1e, modifiers are applied from various sources (for example, spells, feats, racial traits, item enhancements, and temporary conditions). To prevent multiple modifiers from the same source stacking—even when their types normally allow stacking—and to support cleanup of active effects, the source of every modifier must be tracked.

## Decision

Refactor `Modifier`'s source field from a raw `String` to a structured `ModifierSource` record containing:

- `id` (`String`)
- `name` (`String`)
- `type` (`SourceType`: `ITEM`, `SPELL`, `CONDITION`, `FEAT`, `RACIAL`, `TRAIT`, or `GENERIC`)

Use the **Expand-Migrate-Contract** pattern to safely migrate existing serialized database records:

1. **Expand:** Introduce the structured `ModifierSource` representation and temporarily accept legacy string-based sources. Map legacy values to `GENERIC`, emit warnings, and write all new values in the structured format.
2. **Migrate:** Convert historical `modifiers_json` values to the structured source representation with a Flyway migration.
3. **Contract:** Remove the legacy deserialization fallback after migration and retain only the structured domain contract.

## Consequences

### Positive

- Improves type safety and gives modifier sources an explicit schema.
- Makes source-aware stacking and effect cleanup reliable.
- Allows existing persisted data to be upgraded without downtime.

### Negative

- The transition temporarily increases serialization and migration complexity.
- Source types and persisted JSON must remain compatible across releases.

## Implementation

The decision is implemented by `ModifierSource`, `SourceType`, source-aware grouping in `ModifierStack`, and Flyway Java migration `V6__MigrateModifierSources`. The domain now uses the structured representation without the legacy constructor fallback.
