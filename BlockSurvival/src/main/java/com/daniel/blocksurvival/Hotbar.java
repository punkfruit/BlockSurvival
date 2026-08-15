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