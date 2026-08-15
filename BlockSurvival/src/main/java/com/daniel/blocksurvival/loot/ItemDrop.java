package com.daniel.blocksurvival.loot;

import com.daniel.blocksurvival.inventory.ItemDefinition;

public record ItemDrop(
        ItemDefinition item,
        int quantity
) {
    public ItemDrop {
        if (item == null) {
            throw new IllegalArgumentException(
                    "Item drop requires an item."
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Item drop quantity must be positive."
            );
        }
    }
}