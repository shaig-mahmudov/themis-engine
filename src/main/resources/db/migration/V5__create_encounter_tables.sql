CREATE TABLE encounters (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    current_round INTEGER NOT NULL DEFAULT 1,
    active_participant_index INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE encounter_participants (
    encounter_id VARCHAR(255) NOT NULL REFERENCES encounters(id) ON DELETE CASCADE,
    combatant_id VARCHAR(255) NOT NULL,
    combatant_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    initiative_roll INTEGER,
    initiative_total INTEGER,
    dexterity_modifier INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (encounter_id, combatant_id)
);
