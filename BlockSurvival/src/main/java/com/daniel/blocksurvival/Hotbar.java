package com.daniel.blocksurvival;

import com.daniel.blocksurvival.inventory.ItemDefinition;
import com.daniel.blocksurvival.inventory.Items;
import com.daniel.blocksurvival.world.BlockType;

public class Hotbar {

    private static final int SLOT_COUNT =
            9;

    /*
     * The hotbar stores references to item definitions.
     *
     * The actual quantities still live in the player's
     * Inventory. This is only a quick-access assignment.
     */
    private final ItemDefinition[] items =
            new ItemDefinition[
                    SLOT_COUNT
                    ];

    private int selectedIndex =
            0;

    public Hotbar() {
        /*
         * Temporary assignments matching the old creative
         * hotbar so existing controls continue to work.
         *
         * Later these will be assigned by the player through
         * the inventory UI.
         */
        assignSlot(
                0,
                Items.fromBlock(
                        BlockType.GRASS
                )
        );

        assignSlot(
                1,
                Items.fromBlock(
                        BlockType.DIRT
                )
        );

        assignSlot(
                2,
                Items.fromBlock(
                        BlockType.STONE
                )
        );

        assignSlot(
                3,
                Items.fromBlock(
                        BlockType.TORCH
                )
        );

        assignSlot(
                4,
                Items.fromBlock(
                        BlockType.WOOD
                )
        );

        assignSlot(
                5,
                Items.fromBlock(
                        BlockType.LEAVES
                )
        );

        assignSlot(
                6,
                Items.fromBlock(
                        BlockType.SNOW
                )
        );

        assignSlot(
                7,
                Items.fromBlock(
                        BlockType.GLOWSTONE
                )
        );

        assignSlot(
                8,
                Items.fromBlock(
                        BlockType.WATER
                )
        );
    }

    public void scroll(
            double verticalOffset
    ) {
        if (verticalOffset > 0.0) {
            selectPrevious();
        }

        if (verticalOffset < 0.0) {
            selectNext();
        }
    }

    public void selectNext() {
        selectedIndex++;

        if (
                selectedIndex >=
                        items.length
        ) {
            selectedIndex =
                    0;
        }
    }

    public void selectPrevious() {
        selectedIndex--;

        if (selectedIndex < 0) {
            selectedIndex =
                    items.length - 1;
        }
    }

    public void selectSlot(
            int slotNumber
    ) {
        int requestedIndex =
                slotNumber - 1;

        if (
                requestedIndex < 0 ||
                        requestedIndex >=
                                items.length
        ) {
            return;
        }

        selectedIndex =
                requestedIndex;
    }

    public void assignSlot(
            int index,
            ItemDefinition item
    ) {
        if (
                index < 0 ||
                        index >=
                                items.length
        ) {
            throw new IndexOutOfBoundsException(
                    "Invalid hotbar slot: " +
                            index
            );
        }

        items[index] =
                item;
    }

    public void clearSlot(
            int index
    ) {
        assignSlot(
                index,
                null
        );
    }

    public ItemDefinition getSelectedItem() {
        return items[
                selectedIndex
                ];
    }

    public ItemDefinition getItem(
            int index
    ) {
        if (
                index < 0 ||
                        index >=
                                items.length
        ) {
            throw new IndexOutOfBoundsException(
                    "Invalid hotbar slot: " +
                            index
            );
        }

        return items[index];
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public int getSlotCount() {
        return items.length;
    }
}