ALTER TABLE characters ADD COLUMN standard_used BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE characters ADD COLUMN move_used BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE characters ADD COLUMN swift_used BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE character_active_conditions ADD COLUMN duration_rounds INTEGER;
ALTER TABLE character_active_conditions ADD COLUMN stacking_group VARCHAR(255);
