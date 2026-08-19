package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.inventory.Inventory;
import com.daniel.blocksurvival.inventory.ItemStack;

public class FurnaceScreen {

    private PrimitiveFurnace furnace;

    /*
     * 0 = player inventory
     * 1 = furnace input
     * 2 = furnace fuel
     * 3 = furnace output
     */
    private int selectedSection =
            0;

    private int playerX =
            0;

    private int playerY =
            0;

    private int machineSlot =
            0;

    public void open(
            PrimitiveFurnace furnace
    ) {
        this.furnace =
                furnace;

        selectedSection =
                0;

        playerX =
                0;

        playerY =
                0;

        machineSlot =
                0;

        System.out.println(
                "Opened Primitive Furnace."
        );
    }

    public void close() {
        furnace =
                null;

        System.out.println(
                "Closed Primitive Furnace."
        );
    }

    public boolean isOpen() {
        return furnace != null;
    }

    public PrimitiveFurnace getFurnace() {
        return furnace;
    }

    public void moveHorizontal(
            int direction,
            Inventory playerInventory
    ) {
        if (!isOpen()) {
            return;
        }

        if (selectedSection == 0) {
            playerX =
                    Math.floorMod(
                            playerX + direction,
                            playerInventory.getWidth()
                    );

            return;
        }

        Inventory inventory =
                getSelectedInventory();

        machineSlot =
                Math.floorMod(
                        machineSlot + direction,
                        inventory.getWidth()
                );
    }

    public void moveVertical(
            int direction,
            Inventory playerInventory
    ){
        if (!isOpen()) {
            return;
        }

        /*
         * While inside the player inventory, move normally
         * until we try to leave its top edge.
         */
        if (selectedSection == 0) {
            if (
                    direction < 0 &&
                            playerY == 0
            ) {
                selectedSection =
                        1;

                machineSlot =
                        0;

                return;
            }

            playerY =
                    Math.floorMod(
                            playerY + direction,
                            playerInventory.getHeight()
                    );

            return;
        }

        /*
         * Machine inventories are arranged:
         *
         * INPUT
         * FUEL
         * OUTPUT
         * PLAYER
         */
        selectedSection +=
                direction;

        if (selectedSection < 1) {
            selectedSection =
                    0;
        }

        if (selectedSection > 3) {
            selectedSection =
                    0;
        }

        machineSlot =
                0;
    }

    public void transferSelected(
            Inventory playerInventory
    ) {
        if (!isOpen()) {
            return;
        }

        Inventory selectedInventory =
                getSelectedInventory();

        ItemStack stack =
                getSelectedStack(
                        playerInventory
                );

        if (stack == null) {
            return;
        }

        boolean transferred;

        if (selectedSection == 0) {
            transferred =
                    furnace.transferFromPlayer(
                            playerInventory,
                            stack
                    );
        }
        else {
            transferred =
                    furnace.transferToPlayer(
                            selectedInventory,
                            playerInventory,
                            stack
                    );
        }

        if (transferred) {
            System.out.println(
                    "Transferred one " +
                            stack.getDefinition()
                                    .displayName()
            );
        }
    }

    public Inventory getSelectedInventory() {
        if (!isOpen()) {
            return null;
        }

        return switch (selectedSection) {
            case 0 -> null;
            case 1 ->
                    furnace.getInputInventory();

            case 2 ->
                    furnace.getFuelInventory();

            case 3 ->
                    furnace.getOutputInventory();

            default ->
                    throw new IllegalStateException(
                            "Unknown furnace section."
                    );
        };
    }

    public ItemStack getSelectedStack(
            Inventory playerInventory
    ) {
        if (selectedSection == 0) {
            return playerInventory.getStackAt(
                    playerX,
                    playerY
            );
        }

        return getSelectedStack();
    }

    private ItemStack getSelectedStack() {
        Inventory inventory =
                getSelectedInventory();

        if (inventory == null) {
            return null;
        }

        return inventory.getStackAt(
                machineSlot,
                0
        );
    }

    public int getSelectedSection() {
        return selectedSection;
    }

    public int getPlayerX() {
        return playerX;
    }

    public int getPlayerY() {
        return playerY;
    }

    public int getMachineSlot() {
        return machineSlot;
    }
}