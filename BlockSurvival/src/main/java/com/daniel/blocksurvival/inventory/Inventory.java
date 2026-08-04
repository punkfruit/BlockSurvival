package com.daniel.blocksurvival.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Inventory
        implements ItemCollector {
    @Override
    public int collect(
            ItemDefinition item,
            int quantity
    ) {
        int remaining =
                add(
                        item,
                        quantity
                );

        System.out.println(
                item.displayName() +
                        ": accepted " +
                        (
                                quantity -
                                        remaining
                        ) +
                        ", remaining " +
                        remaining
        );

        return remaining;
    }

    private final int width;
    private final int height;

    private final List<ItemStack> stacks =
            new ArrayList<>();

    public Inventory(
            int width,
            int height
    ) {
        if (
                width <= 0 ||
                        height <= 0
        ) {
            throw new IllegalArgumentException(
                    "Inventory size must be positive."
            );
        }

        this.width =
                width;

        this.height =
                height;
    }

    /*
     * Returns how many items could not fit.
     *
     * 0 means the complete quantity was accepted.
     */
    public int add(
            ItemDefinition definition,
            int quantity
    ) {
        if (
                definition == null ||
                        quantity <= 0
        ) {
            return quantity;
        }

        int remaining =
                quantity;

        /*
         * Fill existing compatible stacks first.
         */
        for (ItemStack stack : stacks) {
            if (
                    stack.getDefinition() !=
                            definition
            ) {
                continue;
            }

            int amountToAdd =
                    Math.min(
                            remaining,
                            stack.getRemainingCapacity()
                    );

            if (amountToAdd <= 0) {
                continue;
            }

            stack.addQuantity(
                    amountToAdd
            );

            remaining -=
                    amountToAdd;

            if (remaining == 0) {
                return 0;
            }
        }

        /*
         * Create additional stacks and place them into the
         * first available grid position.
         */
        while (remaining > 0) {
            int stackQuantity =
                    Math.min(
                            remaining,
                            definition.maximumStackSize()
                    );

            Placement placement =
                    findFirstPlacement(
                            definition
                    );

            if (placement == null) {
                break;
            }

            ItemStack stack =
                    new ItemStack(
                            definition,
                            stackQuantity
                    );

            stack.place(
                    placement.x(),
                    placement.y(),
                    placement.rotated()
            );

            stacks.add(
                    stack
            );

            remaining -=
                    stackQuantity;
        }

        return remaining;
    }

    private Placement findFirstPlacement(
            ItemDefinition definition
    ) {
        /*
         * Try the ordinary orientation first.
         */
        Placement normalPlacement =
                findFirstPlacement(
                        definition,
                        false
                );

        if (normalPlacement != null) {
            return normalPlacement;
        }

        /*
         * Rotating a square item changes nothing.
         */
        if (
                !definition.rotatable() ||
                        definition.gridWidth() ==
                                definition.gridHeight()
        ) {
            return null;
        }

        return findFirstPlacement(
                definition,
                true
        );
    }

    private Placement findFirstPlacement(
            ItemDefinition definition,
            boolean rotated
    ) {
        int itemWidth =
                definition.getPlacedWidth(
                        rotated
                );

        int itemHeight =
                definition.getPlacedHeight(
                        rotated
                );

        for (
                int y = 0;
                y <= height - itemHeight;
                y++
        ) {
            for (
                    int x = 0;
                    x <= width - itemWidth;
                    x++
            ) {
                if (
                        canPlace(
                                definition,
                                x,
                                y,
                                rotated
                        )
                ) {
                    return new Placement(
                            x,
                            y,
                            rotated
                    );
                }
            }
        }

        return null;
    }

    private boolean canPlace(
            ItemDefinition definition,
            int x,
            int y,
            boolean rotated
    ) {
        int itemWidth =
                definition.getPlacedWidth(
                        rotated
                );

        int itemHeight =
                definition.getPlacedHeight(
                        rotated
                );

        if (
                x < 0 ||
                        y < 0 ||
                        x + itemWidth > width ||
                        y + itemHeight > height
        ) {
            return false;
        }

        for (ItemStack stack : stacks) {
            ItemDefinition existingDefinition =
                    stack.getDefinition();

            int existingWidth =
                    existingDefinition.getPlacedWidth(
                            stack.isRotated()
                    );

            int existingHeight =
                    existingDefinition.getPlacedHeight(
                            stack.isRotated()
                    );

            boolean overlaps =
                    x <
                            stack.getGridX() +
                                    existingWidth &&
                            x +
                                    itemWidth >
                                    stack.getGridX() &&
                            y <
                                    stack.getGridY() +
                                            existingHeight &&
                            y +
                                    itemHeight >
                                    stack.getGridY();

            if (overlaps) {
                return false;
            }
        }

        return true;
    }

    public List<ItemStack> getStacks() {
        return Collections.unmodifiableList(
                stacks
        );
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private record Placement(
            int x,
            int y,
            boolean rotated
    ) {
    }

}