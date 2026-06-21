CREATE TABLE characters (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    level INTEGER NOT NULL,
    base_str INTEGER NOT NULL,
    base_dex INTEGER NOT NULL,
    base_con INTEGER NOT NULL,
    base_int INTEGER NOT NULL,
    base_wis INTEGER NOT NULL,
    base_cha INTEGER NOT NULL,
    base_hit_points INTEGER NOT NULL,
    base_attack_bonus INTEGER NOT NULL,
    base_fortitude INTEGER NOT NULL,
    base_reflex INTEGER NOT NULL,
    base_will INTEGER NOT NULL,
    current_damage INTEGER NOT NULL,
    spellcasting_caster_level INTEGER,
    spellcasting_attribute VARCHAR(50),
    spellcasting_max_slots VARCHAR(255),
    spellcasting_remaining_slots VARCHAR(255)
);

CREATE TABLE character_equipped_items (
    character_id VARCHAR(255) REFERENCES characters(id) ON DELETE CASCADE,
    item_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    modifiers_json TEXT NOT NULL,
    PRIMARY KEY (character_id, item_id)
);

CREATE TABLE character_active_conditions (
    character_id VARCHAR(255) REFERENCES characters(id) ON DELETE CASCADE,
    condition_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    modifiers_json TEXT NOT NULL,
    PRIMARY KEY (character_id, condition_id)
);
