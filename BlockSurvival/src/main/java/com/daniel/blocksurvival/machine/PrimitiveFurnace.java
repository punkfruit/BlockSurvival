package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.world.BlockDirection;
import com.daniel.blocksurvival.world.BlockPosition;
import java.util.ArrayList;

import java.util.List;

public class PrimitiveFurnace
        extends Machine {

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
}