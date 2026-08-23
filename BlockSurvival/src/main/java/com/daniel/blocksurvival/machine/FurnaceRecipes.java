package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.inventory.ItemDefinition;
import com.daniel.blocksurvival.inventory.Items;

public final class FurnaceRecipes {

    private static final FurnaceRecipe RAW_IRON =
            new FurnaceRecipe(
                    "raw_iron_to_iron_ingot",
                    Items.RAW_IRON,
                    Items.IRON_INGOT,
                    1,
                    1,
                    5.0f, //normal is 5, changing for testing :)
                    100
            );

    private static final FurnaceRecipe RAW_COPPER =
            new FurnaceRecipe(
                    "raw_copper_to_copper_ingot",
                    Items.RAW_COPPER,
                    Items.COPPER_INGOT,
                    1,
                    1,
                    5.0f,
                    100
            );

    private FurnaceRecipes() {
    }

    public static FurnaceRecipe get(
            ItemDefinition input
    ) {
        if (input == Items.RAW_IRON) {
            return RAW_IRON;
        }

        if (input == Items.RAW_COPPER) {
            return RAW_COPPER;
        }

        return null;
    }

    public static FurnaceRecipe getById(
            String id
    ) {
        if (id == null) {
            return null;
        }

        return switch (id) {
            case "raw_iron_to_iron_ingot" ->
                    RAW_IRON;

            case "raw_copper_to_copper_ingot" ->
                    RAW_COPPER;

            default ->
                    null;
        };
    }
}