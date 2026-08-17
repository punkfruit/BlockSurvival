package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.world.BlockDirection;
import com.daniel.blocksurvival.world.BlockPosition;

import java.util.List;

public abstract class Machine {

    public abstract String getTypeId();
    private final BlockPosition anchor;
    private final BlockDirection facing;

    protected Machine(
            BlockPosition anchor,
            BlockDirection facing
    ) {
        if (anchor == null) {
            throw new IllegalArgumentException(
                    "Machine requires an anchor position."
            );
        }

        if (
                facing == null ||
                        facing == BlockDirection.UP
        ) {
            throw new IllegalArgumentException(
                    "Machine requires a horizontal facing direction."
            );
        }

        this.anchor =
                anchor;

        this.facing =
                facing;
    }

    public BlockDirection getFacing() {
        return facing;
    }

    public BlockPosition getAnchor() {
        return anchor;
    }

    /*
     * Every machine tells the world which cells it occupies.
     */
    public abstract List<BlockPosition> getOccupiedBlocks();
}