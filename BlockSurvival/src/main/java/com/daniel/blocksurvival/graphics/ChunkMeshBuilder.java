package com.daniel.blocksurvival.graphics;

import com.daniel.blocksurvival.Main;
import com.daniel.blocksurvival.world.*;
import com.daniel.blocksurvival.graphics.RenderMaterial;
import java.util.ArrayList;
import java.util.List;

public class ChunkMeshBuilder {

    private final List<Float> opaqueVertices =
            new ArrayList<>();

    private final List<Integer> opaqueIndices =
            new ArrayList<>();

    private final List<Float> transparentVertices =
            new ArrayList<>();

    private final List<Integer> transparentIndices =
            new ArrayList<>();

    private List<Float> currentVertices;

    private List<Integer> currentIndices;

    private int blockCount;
    private int faceCount;


    private record FaceAO(
            float vertex1,
            float vertex2,
            float vertex3,
            float vertex4
    ) {
        private static final FaceAO FULL_BRIGHT =
                new FaceAO(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private record ModelVertex(
            float x,
            float y,
            float z
    ) {
    }

    public ChunkMeshData build(World world, Chunk chunk) {
        opaqueVertices.clear();
        opaqueIndices.clear();

        transparentVertices.clear();
        transparentIndices.clear();

        blockCount = 0;
        faceCount = 0;

        for (int localX = 0; localX < Chunk.SIZE; localX++) {
            for (int localY = 0; localY < Chunk.SIZE; localY++) {
                for (int localZ = 0; localZ < Chunk.SIZE; localZ++) {

                    BlockType type =
                            chunk.getBlock(
                                    localX,
                                    localY,
                                    localZ
                            );

                    if (type == null) {
                        continue;
                    }

                    blockCount++;

                    if (type == BlockType.WATER) {
                        currentVertices = transparentVertices;
                        currentIndices = transparentIndices;
                    } else {
                        currentVertices = opaqueVertices;
                        currentIndices = opaqueIndices;
                    }

                    int worldX =
                            chunk.getWorldOriginX() + localX;

                    int worldY =
                            chunk.getWorldOriginY() + localY;

                    int worldZ =
                            chunk.getWorldOriginZ() + localZ;

                    /*
                     * Only add a face when the neighboring
                     * position is empty.
                     */

                    switch (type.getModel()) {
                        case CUBE -> addCube(
                                world,
                                worldX,
                                worldY,
                                worldZ,
                                type,
                                type.getTopOffset()
                        );

                        case CROSS -> addCross(
                                world,
                                worldX,
                                worldY,
                                worldZ,
                                type
                        );

                        case TORCH -> addTorch(
                                world,
                                worldX,
                                worldY,
                                worldZ,
                                type,
                                world.getBlockDirection(
                                        worldX,
                                        worldY,
                                        worldZ
                                )
                        );
                    }
                }
            }
        }



        MeshData opaqueMeshData =
                createMeshData(
                        opaqueVertices,
                        opaqueIndices
                );

        MeshData transparentMeshData =
                createMeshData(
                        transparentVertices,
                        transparentIndices
                );

        return new ChunkMeshData(
                opaqueMeshData,
                transparentMeshData
        );
    }

    private boolean shouldRenderFace(
            World world,
            int neighborX,
            int neighborY,
            int neighborZ,
            BlockType currentType
    ) {
        BlockType neighbor =
                world.getBlock(
                        neighborX,
                        neighborY,
                        neighborZ
                );

        /*
         * Empty space always exposes the current face.
         */
        if (neighbor == null) {
            return true;
        }

        /*
         * Do not create hidden faces between two matching
         * non-opaque blocks, such as adjacent water blocks.
         */
        if (
                neighbor == currentType &&
                        !currentType.isOpaque()
        ) {
            return false;
        }

        /*
         * Faces beside non-opaque blocks remain visible.
         */
        return !neighbor.isOpaque();
    }

    private boolean blocksAmbientLight(
            World world,
            int x,
            int y,
            int z
    ) {
        BlockType block = world.getBlock(x, y, z);

        return block != null &&
                block.isOpaque();
    }

    private FaceAO calculateTopFaceAO(
            World world,
            int x,
            int y,
            int z
    ) {
        return new FaceAO(
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y + 1, z),
                        blocksAmbientLight(world, x, y + 1, z - 1),
                        blocksAmbientLight(world, x - 1, y + 1, z - 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y + 1, z),
                        blocksAmbientLight(world, x, y + 1, z + 1),
                        blocksAmbientLight(world, x - 1, y + 1, z + 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y + 1, z),
                        blocksAmbientLight(world, x, y + 1, z + 1),
                        blocksAmbientLight(world, x + 1, y + 1, z + 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y + 1, z),
                        blocksAmbientLight(world, x, y + 1, z - 1),
                        blocksAmbientLight(world, x + 1, y + 1, z - 1)
                )
        );
    }

    private FaceAO calculateBottomFaceAO(
            World world,
            int x,
            int y,
            int z
    ) {
        return new FaceAO(
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y - 1, z),
                        blocksAmbientLight(world, x, y - 1, z + 1),
                        blocksAmbientLight(world, x - 1, y - 1, z + 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y - 1, z),
                        blocksAmbientLight(world, x, y - 1, z - 1),
                        blocksAmbientLight(world, x - 1, y - 1, z - 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y - 1, z),
                        blocksAmbientLight(world, x, y - 1, z - 1),
                        blocksAmbientLight(world, x + 1, y - 1, z - 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y - 1, z),
                        blocksAmbientLight(world, x, y - 1, z + 1),
                        blocksAmbientLight(world, x + 1, y - 1, z + 1)
                )
        );
    }

    private FaceAO calculateFrontFaceAO(
            World world,
            int x,
            int y,
            int z
    ) {
        return new FaceAO(
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y, z + 1),
                        blocksAmbientLight(world, x, y + 1, z + 1),
                        blocksAmbientLight(world, x - 1, y + 1, z + 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y, z + 1),
                        blocksAmbientLight(world, x, y - 1, z + 1),
                        blocksAmbientLight(world, x - 1, y - 1, z + 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y, z + 1),
                        blocksAmbientLight(world, x, y - 1, z + 1),
                        blocksAmbientLight(world, x + 1, y - 1, z + 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y, z + 1),
                        blocksAmbientLight(world, x, y + 1, z + 1),
                        blocksAmbientLight(world, x + 1, y + 1, z + 1)
                )
        );
    }

    private FaceAO calculateBackFaceAO(
            World world,
            int x,
            int y,
            int z
    ) {
        return new FaceAO(
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y, z - 1),
                        blocksAmbientLight(world, x, y + 1, z - 1),
                        blocksAmbientLight(world, x + 1, y + 1, z - 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y, z - 1),
                        blocksAmbientLight(world, x, y - 1, z - 1),
                        blocksAmbientLight(world, x + 1, y - 1, z - 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y, z - 1),
                        blocksAmbientLight(world, x, y - 1, z - 1),
                        blocksAmbientLight(world, x - 1, y - 1, z - 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y, z - 1),
                        blocksAmbientLight(world, x, y + 1, z - 1),
                        blocksAmbientLight(world, x - 1, y + 1, z - 1)
                )
        );
    }

    private FaceAO calculateLeftFaceAO(
            World world,
            int x,
            int y,
            int z
    ) {
        return new FaceAO(
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y + 1, z),
                        blocksAmbientLight(world, x - 1, y, z - 1),
                        blocksAmbientLight(world, x - 1, y + 1, z - 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y - 1, z),
                        blocksAmbientLight(world, x - 1, y, z - 1),
                        blocksAmbientLight(world, x - 1, y - 1, z - 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y - 1, z),
                        blocksAmbientLight(world, x - 1, y, z + 1),
                        blocksAmbientLight(world, x - 1, y - 1, z + 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x - 1, y + 1, z),
                        blocksAmbientLight(world, x - 1, y, z + 1),
                        blocksAmbientLight(world, x - 1, y + 1, z + 1)
                )
        );
    }

    private FaceAO calculateRightFaceAO(
            World world,
            int x,
            int y,
            int z
    ) {
        return new FaceAO(
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y + 1, z),
                        blocksAmbientLight(world, x + 1, y, z + 1),
                        blocksAmbientLight(world, x + 1, y + 1, z + 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y - 1, z),
                        blocksAmbientLight(world, x + 1, y, z + 1),
                        blocksAmbientLight(world, x + 1, y - 1, z + 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y - 1, z),
                        blocksAmbientLight(world, x + 1, y, z - 1),
                        blocksAmbientLight(world, x + 1, y - 1, z - 1)
                ),
                calculateVertexAO(
                        blocksAmbientLight(world, x + 1, y + 1, z),
                        blocksAmbientLight(world, x + 1, y, z - 1),
                        blocksAmbientLight(world, x + 1, y + 1, z - 1)
                )
        );
    }

    private float calculateVertexAO(
            boolean side1,
            boolean side2,
            boolean corner
    ) {
        /*
         * When both side blocks are present, the diagonal
         * corner block cannot make this vertex any darker.
         */
        if (side1 && side2) {
            return 0.4f;
        }

        int occupiedNeighbors = 0;

        if (side1) {
            occupiedNeighbors++;
        }

        if (side2) {
            occupiedNeighbors++;
        }

        if (corner) {
            occupiedNeighbors++;
        }

        return switch (occupiedNeighbors) {
            case 0 -> 1.0f;
            case 1 -> 0.8f;
            case 2 -> 0.6f;
            default -> 0.4f;
        };
    }

    private void addCube(
            World world,
            int worldX,
            int worldY,
            int worldZ,
            BlockType type,
            float topOffset
    ) {/*
     * Water with another water block above it must extend
     * all the way to the upper block's bottom boundary.
     *
     * Otherwise, the lowered 0.4 surface leaves a visible
     * 0.1-block gap between stacked water blocks.
     */
        boolean hasWaterAbove =
                type == BlockType.WATER &&
                        world.getBlock(
                                worldX,
                                worldY + 1,
                                worldZ
                        ) == BlockType.WATER;

        float sideTopOffset =
                hasWaterAbove
                        ? 0.5f
                        : topOffset;
        RenderMaterial material;

        if (type == BlockType.WATER) {

            material = RenderMaterial.WATER;

        }
        else if (type == BlockType.LEAVES) {

            material = RenderMaterial.LEAVES;

        }
        else {

            material = RenderMaterial.DEFAULT;

        }
        if (shouldRenderFace(
                world,
                worldX,
                worldY + 1,
                worldZ,
                type
        )) {
            addTopFace(
                    world,
                    worldX,
                    worldY,
                    worldZ,
                    type,
                    topOffset,
                    material
            );
        }

        if (shouldRenderFace(
                world,
                worldX,
                worldY - 1,
                worldZ,
                type
        )) {
            addBottomFace(
                    world,
                    worldX,
                    worldY,
                    worldZ,
                    type,
                    material
            );
        }

        if (shouldRenderFace(
                world,
                worldX,
                worldY,
                worldZ + 1,
                type
        )) {
            addFrontFace(
                    world,
                    worldX,
                    worldY,
                    worldZ,
                    type,
                    sideTopOffset,
                    material
            );
        }

        if (shouldRenderFace(
                world,
                worldX,
                worldY,
                worldZ - 1,
                type
        )) {
            addBackFace(
                    world,
                    worldX,
                    worldY,
                    worldZ,
                    type,
                    sideTopOffset,
                    material
            );
        }

        if (shouldRenderFace(
                world,
                worldX - 1,
                worldY,
                worldZ,
                type
        )) {
            addLeftFace(
                    world,
                    worldX,
                    worldY,
                    worldZ,
                    type,
                    sideTopOffset,
                    material
            );
        }

        if (shouldRenderFace(
                world,
                worldX + 1,
                worldY,
                worldZ,
                type
        )) {
            addRightFace(
                    world,
                    worldX,
                    worldY,
                    worldZ,
                    type,
                    sideTopOffset,
                    material
            );
        }
    }

    private void addCross(
            World world,
            float x,
            float y,
            float z,
            BlockType type
    ) {
        int sky =
                world.getSkyLight((int) x, (int) y, (int) z);

        int block =
                world.getBlockLight((int) x, (int) y, (int) z);

        float skyLight =
                sky / 15.0f;

        float blockLight =
                block / 15.0f;

        blockLight =
                Math.max(
                        blockLight,
                        type.getEmittedLight() /
                                15.0f
                );
        /*
         * First diagonal quad.
         */
        addFace(
                x - 0.5f, y + 0.5f, z - 0.5f,
                x - 0.5f, y - 0.5f, z - 0.5f,
                x + 0.5f, y - 0.5f, z + 0.5f,
                x + 0.5f, y + 0.5f, z + 0.5f,
                type.getSideTexture(),
                FaceAO.FULL_BRIGHT,
                RenderMaterial.FOLIAGE,
                1.0f,
                0.0f,
                0.0f,
                1.0f,
                skyLight,
                blockLight
        );

        /*
         * Second diagonal quad.
         */
        addFace(
                x + 0.5f, y + 0.5f, z - 0.5f,
                x + 0.5f, y - 0.5f, z - 0.5f,
                x - 0.5f, y - 0.5f, z + 0.5f,
                x - 0.5f, y + 0.5f, z + 0.5f,
                type.getSideTexture(),
                FaceAO.FULL_BRIGHT,
                RenderMaterial.FOLIAGE,
                1.0f,
                0.0f,
                0.0f,
                1.0f,
                skyLight,
                blockLight
        );

    }

    private void addFrontFace(
            World world,
            int x,
            int y,
            int z,
            BlockType type,
            float topOffset,
            RenderMaterial material
    ) {
        FaceAO ao = calculateFrontFaceAO(
                world,
                x,
                y,
                z
        );

        int sky =
                world.getSkyLight(
                        x,
                        y,
                        z+1
                );

        int block =
                world.getBlockLight(
                        x,
                        y,
                        z+1
                );

        float skyLight =
                sky / 15.0f;

        float blockLight =
                block / 15.0f;

        blockLight =
                Math.max(
                        blockLight,
                        type.getEmittedLight() /
                                15.0f
                );
        addFace(
                x - 0.5f, y + topOffset, z + 0.5f,
                x - 0.5f, y - 0.5f, z + 0.5f,
                x + 0.5f, y - 0.5f, z + 0.5f,
                x + 0.5f, y + topOffset, z + 0.5f,
                type.getTextureForFace(BlockFace.NORTH),
                ao,
                material,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                skyLight,
                blockLight
        );
    }

    private void addBackFace(
            World world,
            int x,
            int y,
            int z,
            BlockType type,
            float topOffset,
            RenderMaterial material
    ) {
        FaceAO ao = calculateBackFaceAO(
                world,
                x,
                y,
                z
        );

        int sky =
                world.getSkyLight(
                        x,
                        y,
                        z -1
                );

        int block =
                world.getBlockLight(
                        x,
                        y,
                        z-1
                );

        float skyLight =
                sky / 15.0f;

        float blockLight =
                block / 15.0f;

        blockLight =
                Math.max(
                        blockLight,
                        type.getEmittedLight() /
                                15.0f
                );
        addFace(
                x + 0.5f, y + topOffset, z - 0.5f,
                x + 0.5f, y - 0.5f, z - 0.5f,
                x - 0.5f, y - 0.5f, z - 0.5f,
                x - 0.5f, y + topOffset, z - 0.5f,
                type.getTextureForFace(BlockFace.SOUTH),
                ao,
                material,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                skyLight,
                blockLight
        );
    }

    private void addLeftFace(
            World world,
            int x,
            int y,
            int z,
            BlockType type,
            float topOffset,
            RenderMaterial material
    ) {

        FaceAO ao = calculateLeftFaceAO(
                world,
                x,
                y,
                z
        );
        int sky =
                world.getSkyLight(
                        x-1,
                        y,
                        z
                );

        int block =
                world.getBlockLight(
                        x-1,
                        y,
                        z
                );

        float skyLight =
                sky / 15.0f;

        float blockLight =
                block / 15.0f;
        blockLight =
                Math.max(
                        blockLight,
                        type.getEmittedLight() /
                                15.0f
                );
        addFace(
                x - 0.5f, y + topOffset, z - 0.5f,
                x - 0.5f, y - 0.5f, z - 0.5f,
                x - 0.5f, y - 0.5f, z + 0.5f,
                x - 0.5f, y + topOffset, z + 0.5f,
                type.getTextureForFace(BlockFace.WEST),
                ao,
                material,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                skyLight,
                blockLight
        );
    }

    private void addRightFace(
            World world,
            int x,
            int y,
            int z,
            BlockType type,
            float topOffset,
            RenderMaterial material
    ) {

        FaceAO ao = calculateRightFaceAO(
                world,
                x,
                y,
                z
        );
        int sky =
                world.getSkyLight(
                        x+1,
                        y,
                        z
                );

        int block =
                world.getBlockLight(
                        x+1,
                        y,
                        z
                );

        float skyLight =
                sky / 15.0f;

        float blockLight =
                block / 15.0f;
        blockLight =
                Math.max(
                        blockLight,
                        type.getEmittedLight() /
                                15.0f
                );
        addFace(
                x + 0.5f, y + topOffset, z + 0.5f,
                x + 0.5f, y - 0.5f, z + 0.5f,
                x + 0.5f, y - 0.5f, z - 0.5f,
                x + 0.5f, y + topOffset, z - 0.5f,
                type.getTextureForFace(BlockFace.EAST),
                ao,
                material,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                skyLight,
                blockLight
        );
    }

    private void addTopFace(
            World world,
            int x,
            int y,
            int z,
            BlockType type,
            float topOffset,
            RenderMaterial material
    ) {

        FaceAO ao = calculateTopFaceAO(
                world,
                x,
                y,
                z
        );

        int sky =
                world.getSkyLight(
                        x,
                        y + 1,
                        z
                );

        int block =
                world.getBlockLight(
                        x,
                        y + 1,
                        z
                );

        float skyLight =
                sky / 15.0f;

        float blockLight =
                block / 15.0f;
        blockLight =
                Math.max(
                        blockLight,
                        type.getEmittedLight() /
                                15.0f
                );

        addFace(
                x - 0.5f, y + topOffset, z - 0.5f,
                x - 0.5f, y + topOffset, z + 0.5f,
                x + 0.5f, y + topOffset, z + 0.5f,
                x + 0.5f, y + topOffset, z - 0.5f,
                type.getTextureForFace(BlockFace.TOP),
                ao,
                material,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                skyLight,
                blockLight
        );
    }

    private void addBottomFace(
            World world,
            int x,
            int y,
            int z,
            BlockType type,
            RenderMaterial material
    ) {

        FaceAO ao = calculateBackFaceAO(
                world,
                x,
                y,
                z
        );
        int sky =
                world.getSkyLight(
                        x,
                        y - 1,
                        z
                );

        int block =
                world.getBlockLight(
                        x,
                        y - 1,
                        z
                );

        float skyLight =
                sky / 15.0f;

        float blockLight =
                block / 15.0f;
        blockLight =
                Math.max(
                        blockLight,
                        type.getEmittedLight() /
                                15.0f
                );
        addFace(
                x - 0.5f, y - 0.5f, z + 0.5f,
                x - 0.5f, y - 0.5f, z - 0.5f,
                x + 0.5f, y - 0.5f, z - 0.5f,
                x + 0.5f, y - 0.5f, z + 0.5f,
                type.getTextureForFace(BlockFace.BOTTOM),
                ao,
                material,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                skyLight,
                blockLight
        );
    }

    /*
     * Adds a face while sampling only a chosen rectangle
     * inside the supplied atlas tile.
     *
     * UV values use a local 0.0–1.0 range:
     *
     * 0.0 = left/top edge of the tile
     * 1.0 = right/bottom edge of the tile
     */
    private void addFaceUV(
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            AtlasTile tile,
            float minimumU,
            float minimumV,
            float maximumU,
            float maximumV,
            FaceAO ao,
            RenderMaterial material,
            float bend1,
            float bend2,
            float bend3,
            float bend4,
            float skyLight,
            float blockLight
    ) {
        faceCount++;

        int firstVertexIndex =
                currentVertices.size() / 10;

        float tileSize =
                BlockType.getTileSize();

        float tileOriginU =
                tile.column() * tileSize;

        float tileOriginV =
                tile.row() * tileSize;

        /*
         * Convert tile-local UV coordinates into complete
         * atlas UV coordinates.
         */
        float atlasMinimumU =
                tileOriginU +
                        minimumU * tileSize;

        float atlasMinimumV =
                tileOriginV +
                        minimumV * tileSize;

        float atlasMaximumU =
                tileOriginU +
                        maximumU * tileSize;

        float atlasMaximumV =
                tileOriginV +
                        maximumV * tileSize;

        addVertex(
                x1, y1, z1,
                atlasMinimumU,
                atlasMinimumV,
                ao.vertex1(),
                material,
                bend1,
                skyLight,
                blockLight
        );

        addVertex(
                x2, y2, z2,
                atlasMinimumU,
                atlasMaximumV,
                ao.vertex2(),
                material,
                bend2,
                skyLight,
                blockLight
        );

        addVertex(
                x3, y3, z3,
                atlasMaximumU,
                atlasMaximumV,
                ao.vertex3(),
                material,
                bend3,
                skyLight,
                blockLight
        );

        addVertex(
                x4, y4, z4,
                atlasMaximumU,
                atlasMinimumV,
                ao.vertex4(),
                material,
                bend4,
                skyLight,
                blockLight
        );

        /*
         * Preserve your ambient-occlusion diagonal selection.
         */
        if (
                ao.vertex1() + ao.vertex3() >
                        ao.vertex2() + ao.vertex4()
        ) {
            currentIndices.add(
                    firstVertexIndex
            );

            currentIndices.add(
                    firstVertexIndex + 1
            );

            currentIndices.add(
                    firstVertexIndex + 3
            );

            currentIndices.add(
                    firstVertexIndex + 1
            );

            currentIndices.add(
                    firstVertexIndex + 2
            );

            currentIndices.add(
                    firstVertexIndex + 3
            );
        }
        else {
            currentIndices.add(
                    firstVertexIndex
            );

            currentIndices.add(
                    firstVertexIndex + 1
            );

            currentIndices.add(
                    firstVertexIndex + 2
            );

            currentIndices.add(
                    firstVertexIndex + 2
            );

            currentIndices.add(
                    firstVertexIndex + 3
            );

            currentIndices.add(
                    firstVertexIndex
            );
        }
    }

    private void addFace(
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            AtlasTile tile,
            FaceAO ao,
            RenderMaterial material,
            float bend1,
            float bend2,
            float bend3,
            float bend4,
            float skyLight,
            float blockLight
    ) {
        addFaceUV(
                x1, y1, z1,
                x2, y2, z2,
                x3, y3, z3,
                x4, y4, z4,
                tile,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                ao,
                material,
                bend1,
                bend2,
                bend3,
                bend4,
                skyLight,
                blockLight
        );
    }

    private void addVertex(
            float x,
            float y,
            float z,
            float u,
            float v,
            float ao,
            RenderMaterial material,
            float bendWeight,
            float skyLight,
            float blockLight
    )
    {
        currentVertices.add(x);
        currentVertices.add(y);
        currentVertices.add(z);
        currentVertices.add(u);
        currentVertices.add(v);
        currentVertices.add(ao);
        currentVertices.add(material.getId());
        currentVertices.add(bendWeight);
        currentVertices.add(skyLight);
        currentVertices.add(blockLight);
    }

    private float[] convertVerticesToArray(
            List<Float> vertexList
    ) {
        float[] result =
                new float[vertexList.size()];

        for (int i = 0; i < vertexList.size(); i++) {
            result[i] = vertexList.get(i);
        }

        return result;
    }

    private int[] convertIndicesToArray(
            List<Integer> indexList
    ) {
        int[] result =
                new int[indexList.size()];

        for (int i = 0; i < indexList.size(); i++) {
            result[i] = indexList.get(i);
        }

        return result;
    }

    private MeshData createMeshData(
            List<Float> vertexList,
            List<Integer> indexList
    ) {
        if (indexList.isEmpty()) {
            return null;
        }

        return new MeshData(
                convertVerticesToArray(
                        vertexList
                ),
                convertIndicesToArray(
                        indexList
                )
        );
    }
    private float sampleSkyLight(
            World world,
            float worldX,
            float worldY,
            float worldZ
    ) {
        int blockX =
                (int) Math.floor(worldX);

        int blockY =
                (int) Math.floor(worldY);

        int blockZ =
                (int) Math.floor(worldZ);

        Chunk chunk =
                world.getChunkAtWorldBlock(
                        blockX,
                        blockY,
                        blockZ
                );

        if (chunk == null) {
            return 1.0f;
        }

        int localX =
                Math.floorMod(
                        blockX,
                        Chunk.SIZE
                );

        int localY =
                Math.floorMod(
                        blockY,
                        Chunk.SIZE
                );

        int localZ =
                Math.floorMod(
                        blockZ,
                        Chunk.SIZE
                );

        return
                chunk.getSkyLight(
                        localX,
                        localY,
                        localZ
                ) / 15.0f;
    }

    private ModelVertex transformTorchVertex(
            float localX,
            float localY,
            float localZ,
            int blockX,
            int blockY,
            int blockZ,
            BlockDirection direction
    ) {
        /*
         * Floor torches use the ordinary upright model.
         */
        if (direction == BlockDirection.UP) {
            return new ModelVertex(
                    blockX + localX,
                    blockY + localY,
                    blockZ + localZ
            );
        }

        /*
         * Pivot near the bottom of the torch.
         *
         * The model's bottom is local Y = -0.5.
         */
        float pivotY =
                -0.5f;

        float relativeY =
                localY - pivotY;

        /*
         * Roughly 22.5 degrees.
         *
         * Increase this for a more dramatic wall lean.
         */
        float angle =
                (float) Math.toRadians(
                        22.5
                );

        float sine =
                (float) Math.sin(
                        angle
                );

        float cosine =
                (float) Math.cos(
                        angle
                );

        float rotatedX =
                localX;

        float rotatedY =
                localY;

        float rotatedZ =
                localZ;

        /*
         * Rotate around the appropriate horizontal axis.
         */
        switch (direction) {
            case NORTH -> {
                rotatedY =
                        pivotY +
                                relativeY * cosine +
                                localZ * sine;

                rotatedZ =
                        -relativeY * sine +
                                localZ * cosine;
            }

            case SOUTH -> {
                rotatedY =
                        pivotY +
                                relativeY * cosine -
                                localZ * sine;

                rotatedZ =
                        relativeY * sine +
                                localZ * cosine;
            }

            case EAST -> {
                rotatedX =
                        relativeY * sine +
                                localX * cosine;

                rotatedY =
                        pivotY +
                                relativeY * cosine -
                                localX * sine;
            }

            case WEST -> {
                rotatedX =
                        -relativeY * sine +
                                localX * cosine;

                rotatedY =
                        pivotY +
                                relativeY * cosine +
                                localX * sine;
            }

            case UP -> {
                // Already handled above.
            }
        }

        /*
         * Move the entire tilted model toward its supporting wall.
         */
        float wallOffset =
                0.55f;
        float wallHeight =
                1.0f / 16.0f;


        switch (direction) {

            case NORTH -> {
                rotatedZ += wallOffset;
                rotatedY += wallHeight;
            }

            case SOUTH -> {
                rotatedZ -= wallOffset;
                rotatedY += wallHeight;
            }

            case EAST -> {
                rotatedX -= wallOffset;
                rotatedY += wallHeight;
            }

            case WEST -> {
                rotatedX += wallOffset;
                rotatedY += wallHeight;
            }
        }

        return new ModelVertex(
                blockX + rotatedX,
                blockY + rotatedY,
                blockZ + rotatedZ
        );
    }

    private void addTorch(
            World world,
            int x,
            int y,
            int z,
            BlockType type,
            BlockDirection direction
    ) {




        //determine shape
        float halfWidth =
                1/16f; //2 pixels wide, matches the texture

        float bottom =
                -0.5f;

        float top =
                0.25f; //perf

        float skyLight =
                world.getSkyLight(
                        x,
                        y,
                        z
                ) / 15.0f;

        float blockLight =
                Math.max(
                        world.getBlockLight(
                                x,
                                y,
                                z
                        ),
                        type.getEmittedLight()
                ) / 15.0f;

        AtlasTile tile =
                type.getSideTexture();

        /*
         * Coordinates inside a 16×16 texture tile.
         */
        float torchMinimumU = //left to right
                7.0f / 16.0f;  // Start after 7 empty pixels

        float torchMaximumU =
                9.0f / 16.0f;  // End at 9 (7 + 2 = 9)

        float torchMinimumV = //top to bottom
                5.0f / 16f;   //5 pixels down, start of main torch texture

        float torchMaximumV = //Bottom
                1.0f;

        float capMinimumU =
                7.0f / 16.0f;

        float capMaximumU =
                9.0f / 16.0f;

        float capMinimumV =
                3f / 16.0f; //3 pixels down, start of top of torch texture

        float capMaximumV =
                5.0f / 16.0f;




        /*
         * Front face: positive Z side.
         */
        ModelVertex front1 =
                transformTorchVertex(
                        -halfWidth,
                        top,
                        halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex front2 =
                transformTorchVertex(
                        -halfWidth,
                        bottom,
                        halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex front3 =
                transformTorchVertex(
                        halfWidth,
                        bottom,
                        halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex front4 =
                transformTorchVertex(
                        halfWidth,
                        top,
                        halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        /*
         * Back face: negative Z side.
         */
        ModelVertex back1 =
                transformTorchVertex(
                        halfWidth,
                        top,
                        -halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex back2 =
                transformTorchVertex(
                        halfWidth,
                        bottom,
                        -halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex back3 =
                transformTorchVertex(
                        -halfWidth,
                        bottom,
                        -halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex back4 =
                transformTorchVertex(
                        -halfWidth,
                        top,
                        -halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        /*
         * Left face: negative X side.
         */
        ModelVertex left1 =
                transformTorchVertex(
                        -halfWidth,
                        top,
                        -halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex left2 =
                transformTorchVertex(
                        -halfWidth,
                        bottom,
                        -halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex left3 =
                transformTorchVertex(
                        -halfWidth,
                        bottom,
                        halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex left4 =
                transformTorchVertex(
                        -halfWidth,
                        top,
                        halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        /*
         * Right face: positive X side.
         */
        ModelVertex right1 =
                transformTorchVertex(
                        halfWidth,
                        top,
                        halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex right2 =
                transformTorchVertex(
                        halfWidth,
                        bottom,
                        halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex right3 =
                transformTorchVertex(
                        halfWidth,
                        bottom,
                        -halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex right4 =
                transformTorchVertex(
                        halfWidth,
                        top,
                        -halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        /*
         * Top cap: all four vertices use the top Y value.
         */
        ModelVertex top1 =
                transformTorchVertex(
                        -halfWidth,
                        top,
                        -halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex top2 =
                transformTorchVertex(
                        -halfWidth,
                        top,
                        halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex top3 =
                transformTorchVertex(
                        halfWidth,
                        top,
                        halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        ModelVertex top4 =
                transformTorchVertex(
                        halfWidth,
                        top,
                        -halfWidth,
                        x,
                        y,
                        z,
                        direction
                );

        /*
         * Front.
         */
        addFaceUV(
                front1.x(), front1.y(), front1.z(),
                front2.x(), front2.y(), front2.z(),
                front3.x(), front3.y(), front3.z(),
                front4.x(), front4.y(), front4.z(),
                tile,
                torchMinimumU,
                torchMinimumV,
                torchMaximumU,
                torchMaximumV,
                FaceAO.FULL_BRIGHT,
                RenderMaterial.DEFAULT,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                skyLight,
                blockLight
        );

        /*
         * Back.
         */
        addFaceUV(
                back1.x(), back1.y(), back1.z(),
                back2.x(), back2.y(), back2.z(),
                back3.x(), back3.y(), back3.z(),
                back4.x(), back4.y(), back4.z(),
                tile,
                torchMinimumU,
                torchMinimumV,
                torchMaximumU,
                torchMaximumV,
                FaceAO.FULL_BRIGHT,
                RenderMaterial.DEFAULT,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                skyLight,
                blockLight
        );

        /*
         * Left.
         */
        addFaceUV(
                left1.x(), left1.y(), left1.z(),
                left2.x(), left2.y(), left2.z(),
                left3.x(), left3.y(), left3.z(),
                left4.x(), left4.y(), left4.z(),
                tile,
                torchMinimumU,
                torchMinimumV,
                torchMaximumU,
                torchMaximumV,
                FaceAO.FULL_BRIGHT,
                RenderMaterial.DEFAULT,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                skyLight,
                blockLight
        );

        /*
         * Right.
         */
        addFaceUV(
                right1.x(), right1.y(), right1.z(),
                right2.x(), right2.y(), right2.z(),
                right3.x(), right3.y(), right3.z(),
                right4.x(), right4.y(), right4.z(),
                tile,
                torchMinimumU,
                torchMinimumV,
                torchMaximumU,
                torchMaximumV,
                FaceAO.FULL_BRIGHT,
                RenderMaterial.DEFAULT,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                skyLight,
                blockLight
        );


        /*
         * Top cap - precisely adjustable
         */

        addFaceUV(
                top1.x(), top1.y(), top1.z(),
                top2.x(), top2.y(), top2.z(),
                top3.x(), top3.y(), top3.z(),
                top4.x(), top4.y(), top4.z(),
                tile,
                capMinimumU,
                capMinimumV,
                capMaximumU,
                capMaximumV,
                FaceAO.FULL_BRIGHT,
                RenderMaterial.DEFAULT,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                skyLight,
                blockLight
        );
    }
}