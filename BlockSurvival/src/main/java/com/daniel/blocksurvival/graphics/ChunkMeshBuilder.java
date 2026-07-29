package com.daniel.blocksurvival.graphics;

import com.daniel.blocksurvival.world.BlockType;
import com.daniel.blocksurvival.world.Chunk;
import com.daniel.blocksurvival.world.World;

import java.util.ArrayList;
import java.util.List;

public class ChunkMeshBuilder {

    private final List<Float> vertices =
            new ArrayList<>();

    private final List<Integer> indices =
            new ArrayList<>();

    private int blockCount;
    private int faceCount;

    public Mesh build(World world, Chunk chunk) {
        vertices.clear();
        indices.clear();

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
                                type
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



        return new Mesh(
                convertVerticesToArray(),
                convertIndicesToArray()
        );
    }

    private boolean shouldRenderFace(
            World world,
            int neighborX,
            int neighborY,
            int neighborZ
    ) {
        BlockType neighbor =
                world.getBlock(
                        neighborX,
                        neighborY,
                        neighborZ
                );

        return neighbor == null ||
                !neighbor.isOpaque();
    }

    private void addCube(
            World world,
            int worldX,
            int worldY,
            int worldZ,
            BlockType type
    ) {
        if (shouldRenderFace(
                world,
                worldX,
                worldY + 1,
                worldZ
        )) {
            addTopFace(
                    worldX,
                    worldY,
                    worldZ,
                    type
            );
        }

        if (shouldRenderFace(
                world,
                worldX,
                worldY - 1,
                worldZ
        )) {
            addBottomFace(
                    worldX,
                    worldY,
                    worldZ,
                    type
            );
        }

        if (shouldRenderFace(
                world,
                worldX,
                worldY,
                worldZ + 1
        )) {
            addFrontFace(
                    worldX,
                    worldY,
                    worldZ,
                    type
            );
        }

        if (shouldRenderFace(
                world,
                worldX,
                worldY,
                worldZ - 1
        )) {
            addBackFace(
                    worldX,
                    worldY,
                    worldZ,
                    type
            );
        }

        if (shouldRenderFace(
                world,
                worldX - 1,
                worldY,
                worldZ
        )) {
            addLeftFace(
                    worldX,
                    worldY,
                    worldZ,
                    type
            );
        }

        if (shouldRenderFace(
                world,
                worldX + 1,
                worldY,
                worldZ
        )) {
            addRightFace(
                    worldX,
                    worldY,
                    worldZ,
                    type
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
                type
        );

        /*
         * Second diagonal quad.
         */
        addFace(
                x + 0.5f, y + 0.5f, z - 0.5f,
                x + 0.5f, y - 0.5f, z - 0.5f,
                x - 0.5f, y - 0.5f, z + 0.5f,
                x - 0.5f, y + 0.5f, z + 0.5f,
                type
        );

    }

    private void addFrontFace(
            float x,
            float y,
            float z,
            BlockType type
    ) {
        addFace(
                x - 0.5f, y + 0.5f, z + 0.5f,
                x - 0.5f, y - 0.5f, z + 0.5f,
                x + 0.5f, y - 0.5f, z + 0.5f,
                x + 0.5f, y + 0.5f, z + 0.5f,
                type
        );
    }

    private void addBackFace(
            float x,
            float y,
            float z,
            BlockType type
    ) {
        addFace(
                x + 0.5f, y + 0.5f, z - 0.5f,
                x + 0.5f, y - 0.5f, z - 0.5f,
                x - 0.5f, y - 0.5f, z - 0.5f,
                x - 0.5f, y + 0.5f, z - 0.5f,
                type
        );
    }

    private void addLeftFace(
            float x,
            float y,
            float z,
            BlockType type
    ) {
        addFace(
                x - 0.5f, y + 0.5f, z - 0.5f,
                x - 0.5f, y - 0.5f, z - 0.5f,
                x - 0.5f, y - 0.5f, z + 0.5f,
                x - 0.5f, y + 0.5f, z + 0.5f,
                type
        );
    }

    private void addRightFace(
            float x,
            float y,
            float z,
            BlockType type
    ) {
        addFace(
                x + 0.5f, y + 0.5f, z + 0.5f,
                x + 0.5f, y - 0.5f, z + 0.5f,
                x + 0.5f, y - 0.5f, z - 0.5f,
                x + 0.5f, y + 0.5f, z - 0.5f,
                type
        );
    }

    private void addTopFace(
            float x,
            float y,
            float z,
            BlockType type
    ) {
        addFace(
                x - 0.5f, y + 0.5f, z - 0.5f,
                x - 0.5f, y + 0.5f, z + 0.5f,
                x + 0.5f, y + 0.5f, z + 0.5f,
                x + 0.5f, y + 0.5f, z - 0.5f,
                type
        );
    }

    private void addBottomFace(
            float x,
            float y,
            float z,
            BlockType type
    ) {
        addFace(
                x - 0.5f, y - 0.5f, z + 0.5f,
                x - 0.5f, y - 0.5f, z - 0.5f,
                x + 0.5f, y - 0.5f, z - 0.5f,
                x + 0.5f, y - 0.5f, z + 0.5f,
                type
        );
    }

    private void addFace(
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            BlockType type
    ) {
        faceCount++;
        int firstVertexIndex =
                vertices.size() / 5;

        float atlasX = type.getAtlasX();
        float atlasY = type.getAtlasY();

        float tileSize = BlockType.getTileSize();

        /*
         * Position plus final atlas UV coordinate.
         */
        addVertex(
                x1, y1, z1,
                atlasX,
                atlasY
        );

        addVertex(
                x2, y2, z2,
                atlasX,
                atlasY + tileSize
        );

        addVertex(
                x3, y3, z3,
                atlasX + tileSize,
                atlasY + tileSize
        );

        addVertex(
                x4, y4, z4,
                atlasX + tileSize,
                atlasY
        );

        /*
         * Two triangles forming one square face.
         */
        indices.add(firstVertexIndex);
        indices.add(firstVertexIndex + 1);
        indices.add(firstVertexIndex + 2);

        indices.add(firstVertexIndex + 2);
        indices.add(firstVertexIndex + 3);
        indices.add(firstVertexIndex);
    }

    private void addVertex(
            float x,
            float y,
            float z,
            float u,
            float v
    ) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);

        vertices.add(u);
        vertices.add(v);
    }

    private float[] convertVerticesToArray() {
        float[] result =
                new float[vertices.size()];

        for (int i = 0; i < vertices.size(); i++) {
            result[i] = vertices.get(i);
        }

        return result;
    }

    private int[] convertIndicesToArray() {
        int[] result =
                new int[indices.size()];

        for (int i = 0; i < indices.size(); i++) {
            result[i] = indices.get(i);
        }

        return result;
    }
}