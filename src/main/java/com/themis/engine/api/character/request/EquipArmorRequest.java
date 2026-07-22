package com.themis.engine.api.character.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import com.themis.engine.domain.Armor;
import com.themis.engine.domain.Modifier;
import com.themis.engine.domain.StatType;

/**
 * Request DTO for equipping armor to a character.
 */
public record EquipArmorRequest(
    @NotBlank(message = "Armor ID cannot be blank")
    String id,

    @NotBlank(message = "Armor Name cannot be blank")
    String name,

    Map<StatType, List<Modifier>> modifiers,

    @Min(value = 0, message = "Max dexterity bonus cannot be negative")
    Integer maxDexterityBonus
) {
    public Armor toDomain() {
        return new Armor(
            id,
            name,
            modifiers,
            maxDexterityBonus
        );
    }
}
