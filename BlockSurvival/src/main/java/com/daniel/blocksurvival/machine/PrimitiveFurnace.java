package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.world.BlockPosition;

import java.util.List;

public class PrimitiveFurnace
        extends Machine {

    public PrimitiveFurnace(
            BlockPosition anchor
    ) {
        super(
                anchor
        );
    }

    @Override
    public List<BlockPosition> getOccupiedBlocks() {
        BlockPosition anchor =
                getAnchor();

        /*
         * First prototype:
         *
         * 2 x 2 horizontal footprint.
         *
         * A B
         * C D
         *
         * Anchor = A
         */
        return List.of(
                new BlockPosition(
                        anchor.x(),
                        anchor.y(),
                        anchor.z()
                ),

                new BlockPosition(
                        anchor.x() + 1,
                        anchor.y(),
                        anchor.z()
                ),

                new BlockPosition(
                        anchor.x(),
                        anchor.y(),
                        anchor.z() + 1
                ),

                new BlockPosition(
                        anchor.x() + 1,
                        anchor.y(),
                        anchor.z() + 1
                )
        );
    }
}