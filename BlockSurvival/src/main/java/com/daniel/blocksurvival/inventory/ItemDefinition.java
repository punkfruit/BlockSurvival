package com.daniel.blocksurvival.inventory;

import com.daniel.blocksurvival.world.BlockType;

public record ItemDefinition(
        String id,
        String displayName,
        int gridWidth,
        int gridHeight,
        int maximumStackSize,
        boolean rotatable,
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