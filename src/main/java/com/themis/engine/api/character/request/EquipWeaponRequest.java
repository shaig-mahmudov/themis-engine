package com.themis.engine.api.character.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import com.themis.engine.domain.DiceRoll;
import com.themis.engine.domain.Modifier;
import com.themis.engine.domain.StatType;
import com.themis.engine.domain.Weapon;
import com.themis.engine.domain.WeaponType;

/**
 * Request DTO for equipping weapons to a character.
 */
public record EquipWeaponRequest(
    @NotBlank(message = "Weapon ID cannot be blank")
    String id,

    @NotBlank(message = "Weapon Name cannot be blank")
    String name,

    @NotNull(message = "Weapon type cannot be null")
    WeaponType type,

    Map<StatType, List<Modifier>> modifiers,

    @NotBlank(message = "Damage roll cannot be blank")
    String damageRoll,

    @Min(value = 15, message = "Critical threat minimum must be at least 15")
    @Max(value = 20, message = "Critical threat minimum cannot be greater than 20")
    int criticalThreatMin,

    @Min(value = 2, message = "Critical multiplier must be at least 2")
    int criticalMultiplier
) {
    public Weapon toDomain() {
        return new Weapon(
            id,
            name,
            type,
            modifiers,
            DiceRoll.parse(damageRoll),
            criticalThreatMin,
            criticalMultiplier
        );
    }
}
