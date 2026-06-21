package com.themis.engine.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import com.themis.engine.domain.EquippableItem;
import com.themis.engine.domain.StatType;
import com.themis.engine.domain.Modifier;

/**
 * Request DTO for equipping items to a character.
 */
public record EquipItemRequestDto(
    @NotBlank(message = "Item ID cannot be blank")
    String id,

    @NotBlank(message = "Item Name cannot be blank")
    String name,

    Map<StatType, List<Modifier>> modifiers
) {
    public EquippableItem toDomain() {
        return new EquippableItem(id, name, modifiers);
    }
}
