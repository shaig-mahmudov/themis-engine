package com.themis.engine.domain;

import java.util.random.RandomGenerator;

/**
 * A stateless domain service that acts as the combat referee in Pathfinder 1e rules.
 */
public class RuleEngine {

    /**
     * Resolves a melee attack from an attacker against a target using a specific weapon.
     * Enforces automatic hits (20), automatic misses (1), critical threat confirmation,
     * strength scaling, and applying resolved damage.
     *
     * @param attacker     the attacking Character
     * @param weapon       the Weapon being used
     * @param target       the target Character
     * @param d20Roll      the d20 attack roll result (must be between 1 and 20)
     * @param random       a RandomGenerator for confirm rolls and damage rolls
     * @return the detailed AttackResult
     */
    public AttackResult resolveAttack(
        Character attacker,
        Weapon weapon,
        Character target,
        int d20Roll,
        RandomGenerator random
    ) {
        if (attacker == null || weapon == null || target == null || random == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        if (d20Roll < 1 || d20Roll > 20) {
            throw new IllegalArgumentException("d20 roll must be between 1 and 20");
        }

        int attackBonus = attacker.getBaseAttackBonus() + attacker.getAttributeModifier(StatType.STRENGTH);
        int totalAttackRoll = d20Roll + attackBonus;
        int targetAc = target.getArmorClass();

        boolean isHit = (d20Roll == 20) || (d20Roll != 1 && totalAttackRoll >= targetAc);
        
        if (!isHit) {
            return new AttackResult(
                false,
                false,
                totalAttackRoll,
                0,
                0,
                String.format("%s missed %s with a roll of %d (total %d vs AC %d)", 
                    attacker.getName(), target.getName(), d20Roll, totalAttackRoll, targetAc)
            );
        }

        boolean isThreat = d20Roll >= weapon.criticalThreatMin();
        boolean isCritical = false;
        int confirmD20 = 0;
        int confirmAttackRoll = 0;

        if (isThreat) {
            confirmD20 = random.nextInt(1, 21);
            if (confirmD20 < 1 || confirmD20 > 20) {
                throw new IllegalArgumentException("Confirmation d20 roll must be between 1 and 20");
            }
            confirmAttackRoll = confirmD20 + attackBonus;
            isCritical = (confirmD20 == 20) || (confirmD20 != 1 && confirmAttackRoll >= targetAc);
        }

        int multiplier = isCritical ? weapon.criticalMultiplier() : 1;
        int damageSum = 0;
        int strMod = attacker.getAttributeModifier(StatType.STRENGTH);
        
        for (int i = 0; i < multiplier; i++) {
            damageSum += weapon.damageRoll().roll(random) + strMod;
        }
        
        int damageDealt = Math.max(1, damageSum);
        target.damage(damageDealt);

        String description;
        if (isCritical) {
            description = String.format("%s scored a CRITICAL HIT on %s with a roll of %d (confirmed with %d, total %d vs AC %d) for %d damage!",
                attacker.getName(), target.getName(), d20Roll, confirmD20, confirmAttackRoll, targetAc, damageDealt);
        } else {
            description = String.format("%s hit %s with a roll of %d (total %d vs AC %d) for %d damage.",
                attacker.getName(), target.getName(), d20Roll, totalAttackRoll, targetAc, damageDealt);
        }

        return new AttackResult(
            true,
            isCritical,
            totalAttackRoll,
            confirmAttackRoll,
            damageDealt,
            description
        );
    }
}
