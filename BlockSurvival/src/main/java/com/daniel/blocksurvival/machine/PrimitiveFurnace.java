package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.inventory.*;
import com.daniel.blocksurvival.world.AtlasTile;
import com.daniel.blocksurvival.world.BlockDirection;
import com.daniel.blocksurvival.world.BlockFace;
import com.daniel.blocksurvival.world.BlockPosition;

import java.util.ArrayList;

import java.util.List;

public class PrimitiveFurnace
        extends Machine {

    private int storedFuel =
            0;

    private static final int MAXIMUM_STORED_FUEL =
            1600;

    private float processingProgress =
            0.0f;

    private FurnaceRecipe activeRecipe;

    private boolean active =
            false;

    private boolean visualStateChanged =
            false;

    private void setActive(
            boolean newActive
    ) {
        if (active == newActive) {
            return;
        }

        active =
                newActive;

        visualStateChanged =
                true;
    }

    public boolean isActive() {
        return active;
    }

    public boolean consumeVisualStateChanged() {
        if (!visualStateChanged) {
            return false;
        }

        visualStateChanged =
                false;

        return true;
    }




    private static final AtlasTile CASING_TEXTURE =
            new AtlasTile(
                    1,
                    5
            );

    private static final AtlasTile INPUT_TEXTURE =
            new AtlasTile(
                    2,
                    5
            );

    private static final AtlasTile OUTPUT_TEXTURE =
            new AtlasTile(
                    3,
                    5
            );

    private static final AtlasTile FURNACE_OFF_TEXTURE =
            new AtlasTile(
                    4,
                    5
            );

    private static final AtlasTile FURNACE_ON_TEXTURE =
            new AtlasTile(
                    3,
                    4
            );

    private static final AtlasTile FURNACE_OFF_LEFT =
            new AtlasTile(
                    4,
                    5
            );

    private static final AtlasTile FURNACE_OFF_RIGHT =
            new AtlasTile(
                    4 + 1,
                    5
            );
    private static final AtlasTile FURNACE_ON_LEFT =
            new AtlasTile(
                    3,
                    4
            );

    private static final AtlasTile FURNACE_ON_RIGHT =
            new AtlasTile(
                    3 + 1,
                    4
            );

    private final Inventory inputInventory =
            new Inventory(
                    2,
                    1
            );

    private final Inventory fuelInventory =
            new Inventory(
                    1,
                    1
            );

    private final Inventory outputInventory =
            new Inventory(
                    2,
                    1
            );

    public PrimitiveFurnace(
            BlockPosition anchor,
            BlockDirection facing
    ) {
        super(
                anchor,
                facing
        );
    }
    @Override
    public String getTypeId() {
        return "primitive_furnace";
    }

    @Override
    public List<BlockPosition> getOccupiedBlocks() {
        List<BlockPosition> blocks =
                new ArrayList<>();

        /*
         * 2 wide
         * 2 deep
         * 2 tall
         */
        for (
                int localY = 0;
                localY < 2;
                localY++
        ) {
            for (
                    int localZ = 0;
                    localZ < 2;
                    localZ++
            ) {
                for (
                        int localX = 0;
                        localX < 2;
                        localX++
                ) {
                    blocks.add(
                            localToWorld(
                                    localX,
                                    localY,
                                    localZ
                            )
                    );
                }
            }
        }

        return blocks;
    }

    @Override
    public AtlasTile getTextureForFace(
            BlockPosition worldPosition,
            BlockFace worldFace
    ) {
        BlockPosition localPosition =
                worldToLocal(
                        worldPosition
                );

        BlockFace localFace =
                getLocalFace(
                        worldFace
                );

        /*
         * FRONT
         *
         * Bottom row gets the furnace grill.
         */
        if (
                localFace == BlockFace.NORTH &&
                        localPosition.y() == 0
        ) {
            if (isActive()) {
                return localPosition.x() == 0
                        ? FURNACE_ON_LEFT
                        : FURNACE_ON_RIGHT;
            }

            return localPosition.x() == 0
                    ? FURNACE_OFF_LEFT
                    : FURNACE_OFF_RIGHT;
        }

        /*
         * LEFT SIDE
         *
         * Bottom row is the orange item input.
         */
        if (
                localFace == BlockFace.WEST &&
                        localPosition.y() == 0 &&
                        localPosition.z() == 0
        ) {
            return INPUT_TEXTURE;
        }

        /*
         * RIGHT SIDE
         *
         * Bottom row is the blue output.
         */
        if (
                localFace == BlockFace.EAST &&
                        localPosition.y() == 0 &&
                        localPosition.z() == 0
        ) {
            return OUTPUT_TEXTURE;
        }

        /*
         * Everything else uses generic casing.
         */
        return CASING_TEXTURE;
    }

    public Inventory getInputInventory() {
        return inputInventory;
    }

    public Inventory getFuelInventory() {
        return fuelInventory;
    }

    public Inventory getOutputInventory() {
        return outputInventory;
    }

    public boolean canAcceptInput(
            ItemDefinition item
    ) {
        return item == Items.RAW_IRON ||
                item == Items.RAW_COPPER;
    }

    public boolean canAcceptFuel(
            ItemDefinition item
    ) {
        return item == Items.COAL;
    }

    public int insertInput(
            ItemDefinition item,
            int quantity
    ) {
        if (!canAcceptInput(item)) {
            return quantity;
        }

        return inputInventory.collect(
                item,
                quantity
        );
    }

    public int insertFuel(
            ItemDefinition item,
            int quantity
    ) {
        if (!canAcceptFuel(item)) {
            return quantity;
        }

        return fuelInventory.collect(
                item,
                quantity
        );
    }

    public void printInventoryStatus() {
        System.out.println(
                "Input stacks: " +
                        inputInventory.getStacks()
                                .size()
        );

        System.out.println(
                "Fuel stacks: " +
                        fuelInventory.getStacks()
                                .size()
        );

        System.out.println(
                "Output stacks: " +
                        outputInventory.getStacks()
                                .size()
        );
    }

    public BlockPosition localToWorld(
            int localX,
            int localY,
            int localZ
    ) {
        BlockPosition anchor =
                getAnchor();

        int worldX;
        int worldZ;

        switch (getFacing()) {
            case NORTH -> {
                worldX =
                        anchor.x() +
                                localX;

                worldZ =
                        anchor.z() +
                                localZ;
            }

            case SOUTH -> {
                worldX =
                        anchor.x() -
                                localX;

                worldZ =
                        anchor.z() -
                                localZ;
            }

            case EAST -> {
                worldX =
                        anchor.x() +
                                localZ;

                worldZ =
                        anchor.z() -
                                localX;
            }

            case WEST -> {
                worldX =
                        anchor.x() -
                                localZ;

                worldZ =
                        anchor.z() +
                                localX;
            }

            default ->
                    throw new IllegalStateException(
                            "Unsupported machine direction: " +
                                    getFacing()
                    );
        }

        return new BlockPosition(
                worldX,
                anchor.y() +
                        localY,
                worldZ
        );


    }

    private BlockPosition worldToLocal(
            BlockPosition worldPosition
    ) {
        BlockPosition anchor =
                getAnchor();

        int differenceX =
                worldPosition.x() -
                        anchor.x();

        int differenceY =
                worldPosition.y() -
                        anchor.y();

        int differenceZ =
                worldPosition.z() -
                        anchor.z();

        int localX;
        int localZ;

        switch (getFacing()) {
            case NORTH -> {
                localX =
                        differenceX;

                localZ =
                        differenceZ;
            }

            case SOUTH -> {
                localX =
                        -differenceX;

                localZ =
                        -differenceZ;
            }

            case EAST -> {
                localX =
                        -differenceZ;

                localZ =
                        differenceX;
            }

            case WEST -> {
                localX =
                        differenceZ;

                localZ =
                        -differenceX;
            }

            default ->
                    throw new IllegalStateException(
                            "Unsupported furnace direction: " +
                                    getFacing()
                    );
        }

        return new BlockPosition(
                localX,
                differenceY,
                localZ
        );
    }

    private BlockFace getLocalFace(
            BlockFace worldFace
    ) {
        /*
         * Top and bottom do not rotate around Y.
         */
        if (
                worldFace == BlockFace.TOP ||
                        worldFace == BlockFace.BOTTOM
        ) {
            return worldFace;
        }

        return switch (getFacing()) {
            case NORTH ->
                    worldFace;

            case SOUTH ->
                    switch (worldFace) {
                        case NORTH -> BlockFace.SOUTH;
                        case SOUTH -> BlockFace.NORTH;
                        case EAST -> BlockFace.WEST;
                        case WEST -> BlockFace.EAST;
                        default -> worldFace;
                    };

            case EAST ->
                    switch (worldFace) {
                        case NORTH -> BlockFace.WEST;
                        case SOUTH -> BlockFace.EAST;
                        case EAST -> BlockFace.NORTH;
                        case WEST -> BlockFace.SOUTH;
                        default -> worldFace;
                    };

            case WEST ->
                    switch (worldFace) {
                        case NORTH -> BlockFace.EAST;
                        case SOUTH -> BlockFace.WEST;
                        case EAST -> BlockFace.SOUTH;
                        case WEST -> BlockFace.NORTH;
                        default -> worldFace;
                    };

            default ->
                    throw new IllegalStateException(
                            "Unsupported furnace direction: " +
                                    getFacing()
                    );
        };
    }

    public int transferFromPlayer(
            Inventory playerInventory,
            ItemStack stack,
            int quantity
    ) {
        if (
                playerInventory == null ||
                        stack == null ||
                        quantity <= 0
        ) {
            return 0;
        }

        ItemDefinition item =
                stack.getDefinition();

        Inventory destination;

        if (canAcceptInput(item)) {
            destination =
                    inputInventory;
        }
        else if (canAcceptFuel(item)) {
            destination =
                    fuelInventory;
        }
        else {
            return 0;
        }

        return playerInventory.transferTo(
                stack,
                destination,
                quantity
        );
    }

    public int transferToPlayer(
            Inventory sourceInventory,
            Inventory playerInventory,
            ItemStack stack,
            int quantity
    ) {
        if (
                sourceInventory == null ||
                        playerInventory == null ||
                        stack == null ||
                        quantity <= 0
        ) {
            return 0;
        }

        return sourceInventory.transferTo(
                stack,
                playerInventory,
                quantity
        );
    }

    public int getStoredFuel() {
        return storedFuel;
    }

    public int getMaximumStoredFuel() {
        return MAXIMUM_STORED_FUEL;
    }

    public float getProcessingProgress() {
        return processingProgress;
    }

    public FurnaceRecipe getActiveRecipe() {
        return activeRecipe;
    }

    public boolean isBurning() {
        return activeRecipe != null &&
                processingProgress > 0.0f;
    }

    public void update(
            float deltaTime
    ) {

        FurnaceRecipe recipe =
                findAvailableRecipe();

        if (recipe == null) {
            setActive(
                    false
            );

            activeRecipe =
                    null;

            processingProgress =
                    0.0f;

            return;
        }

        /*
         * If recipe changed, restart progress.
         */
        if (activeRecipe != recipe) {
            activeRecipe =
                    recipe;

            processingProgress =
                    0.0f;
        }

        /*
         * Make sure enough fuel exists.
         */
        if (
                storedFuel <
                        recipe.fuelCost()
        ) {
            tryLoadFuel();
        }

        if (
                storedFuel <
                        recipe.fuelCost()
        ) {
            setActive(
                    false
            );

            return;
        }

        setActive(true);

        processingProgress +=
                deltaTime;

        if (
                processingProgress <
                        recipe.processingSeconds()
        ) {
            return;
        }

        completeRecipe(
                recipe
        );

        processingProgress =
                0.0f;
    }

    private FurnaceRecipe findAvailableRecipe() {
        for (
                ItemStack stack :
                inputInventory.getStacks()
        ) {
            FurnaceRecipe recipe =
                    FurnaceRecipes.get(
                            stack.getDefinition()
                    );

            if (recipe == null) {
                continue;
            }

            if (
                    stack.getQuantity() <
                            recipe.inputQuantity()
            ) {
                continue;
            }

            /*
             * Check whether output can actually accept it.
             */
            int remaining =
                    outputInventory.add(
                            recipe.output(),
                            recipe.outputQuantity()
                    );

            if (remaining == 0) {
                /*
                 * Undo temporary test insertion.
                 */
                outputInventory.remove(
                        recipe.output(),
                        recipe.outputQuantity()
                );

                return recipe;
            }
        }

        return null;
    }

    private void tryLoadFuel() {
        for (
                ItemStack stack :
                fuelInventory.getStacks()
        ) {
            FuelDefinition fuel =
                    Fuels.get(
                            stack.getDefinition()
                    );

            if (fuel == null) {
                continue;
            }

            /*
             * Don't waste fuel through overflow.
             */
            if (
                    storedFuel +
                            fuel.fuelValue() >
                            MAXIMUM_STORED_FUEL
            ) {
                continue;
            }

            boolean removed =
                    fuelInventory.remove(
                            fuel.item(),
                            1
                    );

            if (!removed) {
                return;
            }

            storedFuel +=
                    fuel.fuelValue();

            return;
        }
    }

    private void completeRecipe(
            FurnaceRecipe recipe
    ) {
        boolean removedInput =
                inputInventory.remove(
                        recipe.input(),
                        recipe.inputQuantity()
                );

        if (!removedInput) {
            return;
        }

        int remaining =
                outputInventory.add(
                        recipe.output(),
                        recipe.outputQuantity()
                );

        if (remaining != 0) {
            /*
             * This should not happen because we checked space first.
             */
            System.err.println(
                    "Furnace output became full unexpectedly."
            );

            return;
        }

        storedFuel -=
                recipe.fuelCost();

        System.out.println(
                "Smelted " +
                        recipe.input().displayName() +
                        " into " +
                        recipe.output().displayName()
        );
    }

    public float getProcessingPercentage() {
        if (activeRecipe == null) {
            return 0.0f;
        }

        return Math.min(
                1.0f,
                processingProgress /
                        activeRecipe.processingSeconds()
        );
    }

    public float getFuelPercentage() {
        return Math.min(
                1.0f,
                (float) storedFuel /
                        (float) MAXIMUM_STORED_FUEL
        );
    }

    public void restoreProcessingState(
            int storedFuel,
            float processingProgress,
            FurnaceRecipe activeRecipe
    ) {
        this.storedFuel =
                Math.max(
                        0,
                        Math.min(
                                storedFuel,
                                MAXIMUM_STORED_FUEL
                        )
                );

        this.activeRecipe =
                activeRecipe;

        if (activeRecipe == null) {
            this.processingProgress =
                    0.0f;

            setActive(false);

            return;
        }

        this.processingProgress =
                Math.max(
                        0.0f,
                        Math.min(
                                processingProgress,
                                activeRecipe.processingSeconds()
                        )
                );

        /*
         * The next update decides whether the furnace is
         * genuinely able to continue processing.
         */
        setActive(false);
    }

    //debug
    public void addDebugContents() {
        insertInput(
                Items.RAW_IRON,
                3
        );

        insertFuel(
                Items.COAL,
                2
        );
    }
}