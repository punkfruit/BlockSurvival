package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.inventory.ItemDefinition;

public record FuelDefinition(
        ItemDefinition item,
        int fuelValue
) {
    public FuelDefinition {
        if (item == null) {
            throw new IllegalArgumentException(
                    "Fuel requires an item."
            );
        }

        if (fuelValue <= 0) {
            throw new IllegalArgumentException(
                    "Fuel value must be positive."
            );
        }
    }
}