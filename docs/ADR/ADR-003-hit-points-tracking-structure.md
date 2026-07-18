# ADR-003: Hit Points Tracking Structure

## Status

Accepted

## Context

Characters take damage and receive healing while their maximum hit points can change dynamically, especially when their Constitution modifier changes.

## Decision

Calculate maximum hit points dynamically from base hit points, Constitution, level, and applicable modifiers. Persist cumulative `currentDamage` instead of a mutable `currentHitPoints` value.

Current hit points are derived as:

```text
current hit points = maximum hit points - current damage
```

The current implementation calculates maximum hit points as:

```text
max(base level floor, base hit points + (Constitution modifier × level) + hit-point modifiers)
```

## Consequences

### Positive

- Constitution changes automatically affect both maximum and current hit points without synchronizing two stored health values.
- The persistence model has one canonical mutable health value.
- Damage and healing remain simple aggregate operations.

### Negative

- Reading current hit points requires a small derived calculation.
- Rule changes that alter maximum hit points can also change current hit points, so temporary and permanent Constitution effects must be modeled carefully.
- Consumers must not treat `currentDamage` as an independently meaningful hit-point total.

## Implementation

The decision is implemented in the `Character` aggregate through `getMaxHitPoints`, `getCurrentHitPoints`, `damage`, and `heal`, with `current_damage` persisted on the `characters` table.
