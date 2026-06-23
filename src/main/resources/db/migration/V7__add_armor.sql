CREATE TABLE character_equipped_armors (
    character_id VARCHAR(255) REFERENCES characters(id) ON DELETE CASCADE,
    armor_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    modifiers_json TEXT NOT NULL,
    max_dexterity_bonus INTEGER,
    PRIMARY KEY (character_id, armor_id)
);
