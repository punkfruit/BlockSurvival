package com.daniel.blocksurvival;

import com.daniel.blocksurvival.world.BlockType;

public class Hotbar {

    /*
     * The blocks available in the creative hotbar.
     *
     * The array order determines which block appears
     * in each slot.
     */
    private final BlockType[] blocks = {
            BlockType.GRASS,
            BlockType.DIRT,
            BlockType.STONE,
            BlockType.SAND,
            BlockType.WOOD,
            BlockType.LEAVES,
            BlockType.SNOW,
            BlockType.GLOWSTONE,
            BlockType.WATER
    };

    /*
     * Array indexes begin at zero:
     *
     * Slot 1 = index 0
     * Slot 2 = index 1
     * ...
     * Slot 9 = index 8
     */
    private int selectedIndex = 0;

    public void scroll(double verticalOffset) {
        if (verticalOffset > 0.0) {
            selectPrevious();
        }

        if (verticalOffset < 0.0) {
            selectNext();
        }
    }

    public void selectNext() {
        selectedIndex++;

        /*
         * Wrap from the final slot back to the first.
         */
        if (selectedIndex >= blocks.length) {
            selectedIndex = 0;
        }
    }

    public void selectPrevious() {
        selectedIndex--;

        /*
         * Wrap from the first slot to the final slot.
         */
        if (selectedIndex < 0) {
            selectedIndex =
                    blocks.length - 1;
        }
    }

    public void selectSlot(int slotNumber) {
        /*
         * The player uses slot numbers 1 through 9,
         * while the array uses indexes 0 through 8.
         */
        int requestedIndex =
                slotNumber - 1;

        if (
                requestedIndex < 0 ||
                        requestedIndex >= blocks.length
        ) {
            return;
        }

        selectedIndex =
                requestedIndex;
    }

    public BlockType getSelectedBlock() {
        return blocks[selectedIndex];
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public int getSlotCount() {
        return blocks.length;
    }

    public BlockType getBlock(int index) {
        if (
                index < 0 ||
                        index >= blocks.length
        ) {
            throw new IndexOutOfBoundsException(
                    "Invalid hotbar slot: " + index
            );
        }

        return blocks[index];
    }
}