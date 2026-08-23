package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.inventory.ItemDefinition;

public record FurnaceRecipe(
        String id,
        ItemDefinition input,
        ItemDefinition output,
        int inputQuantity,
        int outputQuantity,
        float processingSeconds,
        int fuelCost
) {
    public FurnaceRecipe {
        if (
                id == null ||
                        id.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Furnace recipe requires an ID."
            );
        }

        if (
                input == null ||
                        output == null
        ) {
            throw new IllegalArgumentException(
                    "Furnace recipe requires input and output."
            );
        }

        if (
                inputQuantity <= 0 ||
                        outputQuantity <= 0 ||
                        processingSeconds <= 0.0f ||
                        fuelCost <= 0
        ) {
            throw new IllegalArgumentException(
                    "Invalid furnace recipe values."
            );
        }
    }
}