package com.daniel.blocksurvival.graphics;

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

    public ChunkRenderData build(World world, Chunk chunk) {
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
                                worldX,
                                worldY,
                                worldZ,
                                type
                        );
                    }
                }
            }
        }



        Mesh opaqueMesh =
                createMesh(
                        opaqueVertices,
                        opaqueIndices
                );

        Mesh transparentMesh =
                createMesh(
                        transparentVertices,
                        transparentIndices
                );

        return new ChunkRenderData(
                opaqueMesh,
                transparentMesh
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
            float x,
            float y,
            float z,
            BlockType type
    ) {
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
                1.0f
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
                1.0f
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
                0.0f
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
                0.0f
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
                0.0f
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
                0.0f
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
                0.0f
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
                0.0f
        );
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
            float bend4
    ) {
        faceCount++;
        int firstVertexIndex =
                currentVertices.size() / 8;

        float tileSize = BlockType.getTileSize();

        float atlasX =
                tile.column() * tileSize;

        float atlasY =
                tile.row() * tileSize;

        /*
         * Position plus final atlas UV coordinate.
         */
        addVertex(
                x1, y1, z1,
                atlasX,
                atlasY,
                ao.vertex1,
                material,
                bend1
        );

        addVertex(
                x2, y2, z2,
                atlasX,
                atlasY + tileSize,
                ao.vertex2,
                material,bend2
        );

        addVertex(
                x3, y3, z3,
                atlasX + tileSize,
                atlasY + tileSize,
                ao.vertex3,
                material,bend3
        );

        addVertex(
                x4, y4, z4,
                atlasX + tileSize,
                atlasY,
                ao.vertex4,
                material,bend4
        );

        /*
         * Choose the diagonal that produces the smoothest
         * interpolation between AO values.
         */
        if (ao.vertex1() + ao.vertex3() >
                ao.vertex2() + ao.vertex4()) {

            /*
             * Alternate diagonal: vertex 2 to vertex 4.
             */
            currentIndices.add(firstVertexIndex);
            currentIndices.add(firstVertexIndex + 1);
            currentIndices.add(firstVertexIndex + 3);

            currentIndices.add(firstVertexIndex + 1);
            currentIndices.add(firstVertexIndex + 2);
            currentIndices.add(firstVertexIndex + 3);

        } else {

            /*
             * Default diagonal: vertex 1 to vertex 3.
             */
            currentIndices.add(firstVertexIndex);
            currentIndices.add(firstVertexIndex + 1);
            currentIndices.add(firstVertexIndex + 2);
            currentIndices.add(firstVertexIndex + 2);
            currentIndices.add(firstVertexIndex + 3);
            currentIndices.add(firstVertexIndex);
        }
    }

    private void addVertex(
            float x,
            float y,
            float z,
            float u,
            float v,
            float ao,
            RenderMaterial material,
            float bendWeight
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

    private Mesh createMesh(
            List<Float> vertexList,
            List<Integer> indexList
    ) {
        /*
         * An empty bucket does not need a GPU mesh.
         */
        if (indexList.isEmpty()) {
            return null;
        }

        return new Mesh(
                convertVerticesToArray(vertexList),
                convertIndicesToArray(indexList)
        );
    }
}