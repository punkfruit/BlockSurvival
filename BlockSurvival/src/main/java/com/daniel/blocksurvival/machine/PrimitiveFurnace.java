package com.daniel.blocksurvival.machine;

import com.daniel.blocksurvival.world.AtlasTile;
import com.daniel.blocksurvival.world.BlockDirection;
import com.daniel.blocksurvival.world.BlockFace;
import com.daniel.blocksurvival.world.BlockPosition;
import java.util.ArrayList;

import java.util.List;

public class PrimitiveFurnace
        extends Machine {


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
}