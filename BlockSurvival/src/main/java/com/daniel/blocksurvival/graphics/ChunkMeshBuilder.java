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

                    if (!world.hasBlock(worldX, worldY + 1, worldZ)) {
                        addTopFace(worldX, worldY, worldZ, type);
                    }

                    if (!world.hasBlock(worldX, worldY - 1, worldZ)) {
                        addBottomFace(worldX, worldY, worldZ, type);
                    }

                    if (!world.hasBlock(worldX, worldY, worldZ + 1)) {
                        addFrontFace(worldX, worldY, worldZ, type);
                    }

                    if (!world.hasBlock(worldX, worldY, worldZ - 1)) {
                        addBackFace(worldX, worldY, worldZ, type);
                    }

                    if (!world.hasBlock(worldX - 1, worldY, worldZ)) {
                        addLeftFace(worldX, worldY, worldZ, type);
                    }

                    if (!world.hasBlock(worldX + 1, worldY, worldZ)) {
                        addRightFace(worldX, worldY, worldZ, type);
                    }
                }
            }
        }

        int vertexCount = vertices.size() / 5;
        int triangleCount = indices.size() / 3;
        int indexCount = indices.size();

        System.out.println("World mesh built:");
        System.out.println("  Blocks: " + blockCount);
        System.out.println("  Visible faces: " + faceCount);
        System.out.println("  Vertices: " + vertexCount);
        System.out.println("  Triangles: " + triangleCount);
        System.out.println("  Indices: " + indexCount);

        return new Mesh(
                convertVerticesToArray(),
                convertIndicesToArray()
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

        float tileSize = 0.5f;

        /*
         * Position plus final atlas UV coordinate.
         */
        addVertex(
                x1, y1, z1,
                atlasX,
                atlasY + tileSize
        );

        addVertex(
                x2, y2, z2,
                atlasX,
                atlasY
        );

        addVertex(
                x3, y3, z3,
                atlasX + tileSize,
                atlasY
        );

        addVertex(
                x4, y4, z4,
                atlasX + tileSize,
                atlasY + tileSize
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