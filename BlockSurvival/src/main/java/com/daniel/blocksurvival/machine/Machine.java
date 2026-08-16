package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.world.BlockPosition;

import java.util.List;

public abstract class Machine {

    private final BlockPosition anchor;

    protected Machine(
            BlockPosition anchor
    ) {
        if (anchor == null) {
            throw new IllegalArgumentException(
                    "Machine requires an anchor position."
            );
        }

        this.anchor =
                anchor;
    }

    public BlockPosition getAnchor() {
        return anchor;
    }

    /*
     * Every machine tells the world which cells it occupies.
     */
    public abstract List<BlockPosition> getOccupiedBlocks();
}