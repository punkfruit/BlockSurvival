package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.inventory.ItemDefinition;
import com.daniel.blocksurvival.inventory.Items;

public final class Fuels {

    private static final FuelDefinition COAL =
            new FuelDefinition(
                    Items.COAL,
                    800
            );

    private Fuels() {
    }

    public static FuelDefinition get(
            ItemDefinition item
    ) {
        if (item == Items.COAL) {
            return COAL;
        }

        return null;
    }
}