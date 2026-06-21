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
