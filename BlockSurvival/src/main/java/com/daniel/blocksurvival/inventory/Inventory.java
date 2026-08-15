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
        return canPlace(
                definition,
                x,
                y,
                rotated,
                null
        );
    }

    private boolean canPlace(
            ItemDefinition definition,
            int x,
            int y,
            boolean rotated,
            ItemStack ignoredStack
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
            /*
             * Ignore the item we're currently moving.
             */
            if (stack == ignoredStack) {
                continue;
            }

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

    public boolean moveStack(
            ItemStack stack,
            int newX,
            int newY,
            boolean rotated
    ) {
        if (
                stack == null ||
                        !stacks.contains(stack)
        ) {
            return false;
        }

        if (
                rotated &&
                        !stack.getDefinition()
                                .rotatable()
        ) {
            return false;
        }

        if (
                !canPlace(
                        stack.getDefinition(),
                        newX,
                        newY,
                        rotated,
                        stack
                )
        ) {
            return false;
        }

        stack.place(
                newX,
                newY,
                rotated
        );

        return true;
    }

    public ItemStack getStackAt(
            int gridX,
            int gridY
    ) {
        for (ItemStack stack : stacks) {
            ItemDefinition definition =
                    stack.getDefinition();

            int itemWidth =
                    definition.getPlacedWidth(
                            stack.isRotated()
                    );

            int itemHeight =
                    definition.getPlacedHeight(
                            stack.isRotated()
                    );

            boolean inside =
                    gridX >=
                            stack.getGridX() &&
                            gridX <
                                    stack.getGridX() +
                                            itemWidth &&
                            gridY >=
                                    stack.getGridY() &&
                            gridY <
                                    stack.getGridY() +
                                            itemHeight;

            if (inside) {
                return stack;
            }
        }

        return null;
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
    public boolean restoreStack(
            ItemDefinition definition,
            int quantity,
            int gridX,
            int gridY,
            boolean rotated
    ) {
        if (
                definition == null ||
                        quantity <= 0 ||
                        quantity >
                                definition.maximumStackSize()
        ) {
            return false;
        }

        if (
                rotated &&
                        !definition.rotatable()
        ) {
            return false;
        }

        if (
                !canPlace(
                        definition,
                        gridX,
                        gridY,
                        rotated
                )
        ) {
            return false;
        }

        ItemStack stack =
                new ItemStack(
                        definition,
                        quantity
                );

        stack.place(
                gridX,
                gridY,
                rotated
        );

        stacks.add(
                stack
        );

        return true;
    }

    public int getQuantity(
            ItemDefinition definition
    ) {
        if (definition == null) {
            return 0;
        }

        int total =
                0;

        for (ItemStack stack : stacks) {
            if (
                    stack.getDefinition() ==
                            definition
            ) {
                total +=
                        stack.getQuantity();
            }
        }

        return total;
    }

    public boolean contains(
            ItemDefinition definition,
            int quantity
    ) {
        if (
                definition == null ||
                        quantity <= 0
        ) {
            return false;
        }

        return getQuantity(
                definition
        ) >= quantity;
    }

    public boolean remove(
            ItemDefinition definition,
            int quantity
    ) {
        if (
                definition == null ||
                        quantity <= 0
        ) {
            return false;
        }

        /*
         * Check first so removal is all-or-nothing.
         *
         * We don't want a request for 5 Stone to remove 3
         * and then discover that the other 2 don't exist.
         */
        if (
                !contains(
                        definition,
                        quantity
                )
        ) {
            return false;
        }

        int remainingToRemove =
                quantity;

        for (
                int index =
                stacks.size() - 1;
                index >= 0 &&
                        remainingToRemove > 0;
                index--
        ) {
            ItemStack stack =
                    stacks.get(
                            index
                    );

            if (
                    stack.getDefinition() !=
                            definition
            ) {
                continue;
            }

            int amountToRemove =
                    Math.min(
                            remainingToRemove,
                            stack.getQuantity()
                    );

            stack.removeQuantity(
                    amountToRemove
            );

            remainingToRemove -=
                    amountToRemove;

            /*
             * Empty stacks should no longer occupy grid cells.
             */
            if (
                    stack.getQuantity() ==
                            0
            ) {
                stacks.remove(
                        index
                );
            }
        }

        return true;
    }

    public void clear() {
        stacks.clear();
    }
}