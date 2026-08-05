package com.daniel.blocksurvival.inventory;

import com.daniel.blocksurvival.world.AtlasTile;
import com.daniel.blocksurvival.world.BlockType;

public record ItemDefinition(
        String id,
        String displayName,
        int gridWidth,
        int gridHeight,
        int maximumStackSize,
        boolean rotatable,
        AtlasTile inventoryIcon,
        BlockType placedBlock
) {
    public ItemDefinition {
        if (
                id == null ||
                        id.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Item ID cannot be empty."
            );
        }

        if (
                displayName == null ||
                        displayName.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Display name cannot be empty."
            );
        }

        if (
                gridWidth <= 0 ||
                        gridHeight <= 0
        ) {
            throw new IllegalArgumentException(
                    "Inventory dimensions must be positive."
            );
        }

        if (maximumStackSize <= 0) {
            throw new IllegalArgumentException(
                    "Maximum stack size must be positive."
            );
        }

        if (inventoryIcon == null) {
            throw new IllegalArgumentException(
                    "Item requires an inventory icon."
            );
        }
    }

    public int getPlacedWidth(
            boolean rotated
    ) {
        return rotated
                ? gridHeight
                : gridWidth;
    }

    public int getPlacedHeight(
            boolean rotated
    ) {
        return rotated
                ? gridWidth
                : gridHeight;
    }
}