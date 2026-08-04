package com.daniel.blocksurvival.inventory;

public class ItemStack {

    private final ItemDefinition definition;

    private int quantity;

    private int gridX =
            -1;

    private int gridY =
            -1;

    private boolean rotated;

    public ItemStack(
            ItemDefinition definition,
            int quantity
    ) {
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Item stack requires a definition."
            );
        }

        if (
                quantity <= 0 ||
                        quantity >
                                definition.maximumStackSize()
        ) {
            throw new IllegalArgumentException(
                    "Invalid stack quantity: " +
                            quantity
            );
        }

        this.definition =
                definition;

        this.quantity =
                quantity;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getRemainingCapacity() {
        return definition.maximumStackSize() -
                quantity;
    }

    public void addQuantity(
            int amount
    ) {
        if (
                amount < 0 ||
                        quantity + amount >
                                definition.maximumStackSize()
        ) {
            throw new IllegalArgumentException(
                    "Stack exceeds its maximum size."
            );
        }

        quantity +=
                amount;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public boolean isRotated() {
        return rotated;
    }

    public void place(
            int x,
            int y,
            boolean rotated
    ) {
        gridX =
                x;

        gridY =
                y;

        this.rotated =
                rotated;
    }
}