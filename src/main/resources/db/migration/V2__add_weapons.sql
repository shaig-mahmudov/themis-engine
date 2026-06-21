CREATE TABLE character_equipped_weapons (
    character_id VARCHAR(255) REFERENCES characters(id) ON DELETE CASCADE,
    weapon_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    modifiers_json TEXT NOT NULL,
    damage_roll VARCHAR(50) NOT NULL,
    critical_threat_min INTEGER NOT NULL,
    critical_multiplier INTEGER NOT NULL,
    PRIMARY KEY (character_id, weapon_id)
);
