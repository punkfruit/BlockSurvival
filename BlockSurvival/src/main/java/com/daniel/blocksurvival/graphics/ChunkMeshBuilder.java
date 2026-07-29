package com.daniel.blocksurvival.graphics;

import com.daniel.blocksurvival.world.*;

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
            BlockType type
    ) {
        if (shouldRenderFace(
                world,
                worldX,
                worldY + 1,
                worldZ
        )) {
            addTopFace(
                    world,
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
                    world,
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
                    world,
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
                    world,
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
                    world,
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
                    world,
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
                type.getSideTexture()
        );

        /*
         * Second diagonal quad.
         */
        addFace(
                x + 0.5f, y + 0.5f, z - 0.5f,
                x + 0.5f, y - 0.5f, z - 0.5f,
                x - 0.5f, y - 0.5f, z + 0.5f,
                x - 0.5f, y + 0.5f, z + 0.5f,
                type.getSideTexture()
        );

    }

    private void addFrontFace(
            World world,
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
                type.getTextureForFace(BlockFace.NORTH)
        );
    }

    private void addBackFace(
            World world,
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
                type.getTextureForFace(BlockFace.SOUTH)
        );
    }

    private void addLeftFace(
            World world,
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
                type.getTextureForFace(BlockFace.WEST)
        );
    }

    private void addRightFace(
            World world,
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
                type.getTextureForFace(BlockFace.EAST)
        );
    }

    private void addTopFace(
            World world,
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
                type.getTextureForFace(BlockFace.TOP)
        );
    }

    private void addBottomFace(
            World world,
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
                type.getTextureForFace(BlockFace.BOTTOM)
        );
    }

    private void addFace(
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            AtlasTile tile
    ) {
        faceCount++;
        int firstVertexIndex =
                vertices.size() / 6;

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
                1f
        );

        addVertex(
                x2, y2, z2,
                atlasX,
                atlasY + tileSize,
                1f
        );

        addVertex(
                x3, y3, z3,
                atlasX + tileSize,
                atlasY + tileSize,
                1f
        );

        addVertex(
                x4, y4, z4,
                atlasX + tileSize,
                atlasY,
                1f
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
            float v,
            float ao
    )
    {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);

        vertices.add(u);
        vertices.add(v);

        vertices.add(ao);
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