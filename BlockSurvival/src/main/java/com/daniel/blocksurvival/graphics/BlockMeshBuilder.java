package com.daniel.blocksurvival.graphics;

import com.daniel.blocksurvival.world.AtlasTile;
import com.daniel.blocksurvival.world.BlockFace;
import com.daniel.blocksurvival.world.BlockType;

import java.util.ArrayList;
import java.util.List;

/*
 * Builds one isolated block model centered at the origin.
 *
 * Unlike ChunkMeshBuilder, this class does not:
 *
 * - inspect neighboring blocks;
 * - remove hidden faces;
 * - calculate ambient occlusion;
 * - sample world lighting;
 * - use world coordinates.
 *
 * Position, rotation, and scale will be applied later by
 * the entity renderer through a model matrix.
 */
public class BlockMeshBuilder {

    /*
     * This must match the world shader's vertex layout:
     *
     * position       3
     * UV             2
     * AO             1
     * material       1
     * bend weight    1
     * skylight       1
     * block light    1
     *
     * Total:         10 floats
     */
    private static final int FLOATS_PER_VERTEX =
            10;

    private final List<Float> vertices =
            new ArrayList<>();

    private final List<Integer> indices =
            new ArrayList<>();

    public MeshData buildBlock(
            BlockType blockType
    ) {
        if (blockType == null) {
            throw new IllegalArgumentException(
                    "Cannot build a null block."
            );
        }

        vertices.clear();
        indices.clear();

        switch (blockType.getModel()) {
            case CUBE ->
                    addCube(
                            blockType
                    );

            case CROSS ->
                    addCross(
                            blockType
                    );

            /*
             * Temporary fallback.
             *
             * We will add the correctly shaped dropped torch
             * after confirming crossed models work.
             */
            case TORCH ->
                    addTorch(
                            blockType
                    );
        }

        return new MeshData(
                convertVerticesToArray(),
                convertIndicesToArray()
        );
    }

    private void addCube(
            BlockType blockType
    ) {
        float minimum =
                -0.5f;

        float maximum =
                0.5f;

        float skyLight =
                1.0f;

        float blockLight =
                blockType.getEmittedLight() /
                        15.0f;

        /*
         * Front.
         */
        addFace(
                minimum, maximum, maximum,
                minimum, minimum, maximum,
                maximum, minimum, maximum,
                maximum, maximum, maximum,
                blockType.getTextureForFace(
                        BlockFace.NORTH
                ),
                skyLight,
                blockLight
        );

        /*
         * Back.
         */
        addFace(
                maximum, maximum, minimum,
                maximum, minimum, minimum,
                minimum, minimum, minimum,
                minimum, maximum, minimum,
                blockType.getTextureForFace(
                        BlockFace.SOUTH
                ),
                skyLight,
                blockLight
        );

        /*
         * Left.
         */
        addFace(
                minimum, maximum, minimum,
                minimum, minimum, minimum,
                minimum, minimum, maximum,
                minimum, maximum, maximum,
                blockType.getTextureForFace(
                        BlockFace.WEST
                ),
                skyLight,
                blockLight
        );

        /*
         * Right.
         */
        addFace(
                maximum, maximum, maximum,
                maximum, minimum, maximum,
                maximum, minimum, minimum,
                maximum, maximum, minimum,
                blockType.getTextureForFace(
                        BlockFace.EAST
                ),
                skyLight,
                blockLight
        );

        /*
         * Top.
         */
        addFace(
                minimum, maximum, minimum,
                minimum, maximum, maximum,
                maximum, maximum, maximum,
                maximum, maximum, minimum,
                blockType.getTextureForFace(
                        BlockFace.TOP
                ),
                skyLight,
                blockLight
        );

        /*
         * Bottom.
         */
        addFace(
                minimum, minimum, maximum,
                minimum, minimum, minimum,
                maximum, minimum, minimum,
                maximum, minimum, maximum,
                blockType.getTextureForFace(
                        BlockFace.BOTTOM
                ),
                skyLight,
                blockLight
        );
    }

    private void addCross(
            BlockType blockType
    ) {
        float minimum =
                -0.5f;

        float maximum =
                0.5f;

        float skyLight =
                1.0f;

        float blockLight =
                blockType.getEmittedLight() /
                        15.0f;

        AtlasTile tile =
                blockType.getSideTexture();

        /*
         * First diagonal plane:
         *
         * \ when viewed from above.
         */
        addFace(
                minimum, maximum, minimum,
                minimum, minimum, minimum,
                maximum, minimum, maximum,
                maximum, maximum, maximum,
                tile,
                skyLight,
                blockLight
        );

        /*
         * Second diagonal plane:
         *
         * / when viewed from above.
         */
        addFace(
                maximum, maximum, minimum,
                maximum, minimum, minimum,
                minimum, minimum, maximum,
                minimum, maximum, maximum,
                tile,
                skyLight,
                blockLight
        );
    }

    private void addDoubleSidedFace(
            float x1,
            float y1,
            float z1,

            float x2,
            float y2,
            float z2,

            float x3,
            float y3,
            float z3,

            float x4,
            float y4,
            float z4,

            AtlasTile tile,
            float skyLight,
            float blockLight
    ) {
        /*
         * Front side.
         */
        addFace(
                x1, y1, z1,
                x2, y2, z2,
                x3, y3, z3,
                x4, y4, z4,
                tile,
                skyLight,
                blockLight
        );

        /*
         * Reverse the vertex order to generate the back side.
         */
        addFace(
                x4, y4, z4,
                x3, y3, z3,
                x2, y2, z2,
                x1, y1, z1,
                tile,
                skyLight,
                blockLight
        );
    }

    private void addTorch(
            BlockType blockType
    ) {
        float halfWidth =
                1.0f / 16.0f;

        float bottom =
                -0.5f;

        float top =
                0.25f;

        float skyLight =
                1.0f;

        float blockLight =
                Math.max(
                        blockType.getEmittedLight() /
                                15.0f,
                        0.0f
                );

        AtlasTile sideTile =
                blockType.getSideTexture();

        AtlasTile topTile =
                blockType.getTopTexture();

        float torchMinimumU =
                7.0f / 16.0f;

        float torchMaximumU =
                9.0f / 16.0f;

        float torchMinimumV =
                5.0f / 16.0f;

        float torchMaximumV =
                1.0f;

        float capMinimumU =
                7.0f / 16.0f;

        float capMaximumU =
                9.0f / 16.0f;

        float capMinimumV =
                3.0f / 16.0f;

        float capMaximumV =
                5.0f / 16.0f;

        /*
         * Front.
         */
        addFaceUV(
                -halfWidth, top, halfWidth,
                -halfWidth, bottom, halfWidth,
                halfWidth, bottom, halfWidth,
                halfWidth, top, halfWidth,
                sideTile,
                torchMinimumU,
                torchMinimumV,
                torchMaximumU,
                torchMaximumV,
                skyLight,
                blockLight
        );

        /*
         * Back.
         */
        addFaceUV(
                halfWidth, top, -halfWidth,
                halfWidth, bottom, -halfWidth,
                -halfWidth, bottom, -halfWidth,
                -halfWidth, top, -halfWidth,
                sideTile,
                torchMinimumU,
                torchMinimumV,
                torchMaximumU,
                torchMaximumV,
                skyLight,
                blockLight
        );

        /*
         * Left.
         */
        addFaceUV(
                -halfWidth, top, -halfWidth,
                -halfWidth, bottom, -halfWidth,
                -halfWidth, bottom, halfWidth,
                -halfWidth, top, halfWidth,
                sideTile,
                torchMinimumU,
                torchMinimumV,
                torchMaximumU,
                torchMaximumV,
                skyLight,
                blockLight
        );

        /*
         * Right.
         */
        addFaceUV(
                halfWidth, top, halfWidth,
                halfWidth, bottom, halfWidth,
                halfWidth, bottom, -halfWidth,
                halfWidth, top, -halfWidth,
                sideTile,
                torchMinimumU,
                torchMinimumV,
                torchMaximumU,
                torchMaximumV,
                skyLight,
                blockLight
        );

        /*
         * Top cap.
         */
        addFaceUV(
                -halfWidth, top, -halfWidth,
                -halfWidth, top, halfWidth,
                halfWidth, top, halfWidth,
                halfWidth, top, -halfWidth,
                topTile,
                capMinimumU,
                capMinimumV,
                capMaximumU,
                capMaximumV,
                skyLight,
                blockLight
        );
    }

    private void addFace(
            float x1,
            float y1,
            float z1,

            float x2,
            float y2,
            float z2,

            float x3,
            float y3,
            float z3,

            float x4,
            float y4,
            float z4,

            AtlasTile tile,
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
            float skyLight,
            float blockLight
    ) {
        vertices.add(
                x
        );

        vertices.add(
                y
        );

        vertices.add(
                z
        );

        vertices.add(
                u
        );

        vertices.add(
                v
        );

        /*
         * No neighboring blocks means no ambient-occlusion
         * darkening for this isolated item model.
         */
        vertices.add(
                1.0f
        );

        vertices.add(
                RenderMaterial.DEFAULT.getId()
        );

        /*
         * Dropped cubes do not bend like foliage.
         */
        vertices.add(
                0.0f
        );

        vertices.add(
                skyLight
        );

        vertices.add(
                blockLight
        );
    }

    private void addFaceUV(
            float x1,
            float y1,
            float z1,

            float x2,
            float y2,
            float z2,

            float x3,
            float y3,
            float z3,

            float x4,
            float y4,
            float z4,

            AtlasTile tile,
            float minimumU,
            float minimumV,
            float maximumU,
            float maximumV,
            float skyLight,
            float blockLight
    ) {
        int firstVertexIndex =
                vertices.size() /
                        FLOATS_PER_VERTEX;

        float tileSize =
                BlockType.getTileSize();

        float tileOriginU =
                tile.column() *
                        tileSize;

        float tileOriginV =
                tile.row() *
                        tileSize;

        float atlasMinimumU =
                tileOriginU +
                        minimumU *
                                tileSize;

        float atlasMinimumV =
                tileOriginV +
                        minimumV *
                                tileSize;

        float atlasMaximumU =
                tileOriginU +
                        maximumU *
                                tileSize;

        float atlasMaximumV =
                tileOriginV +
                        maximumV *
                                tileSize;

        addVertex(
                x1, y1, z1,
                atlasMinimumU,
                atlasMinimumV,
                skyLight,
                blockLight
        );

        addVertex(
                x2, y2, z2,
                atlasMinimumU,
                atlasMaximumV,
                skyLight,
                blockLight
        );

        addVertex(
                x3, y3, z3,
                atlasMaximumU,
                atlasMaximumV,
                skyLight,
                blockLight
        );

        addVertex(
                x4, y4, z4,
                atlasMaximumU,
                atlasMinimumV,
                skyLight,
                blockLight
        );

        indices.add(
                firstVertexIndex
        );

        indices.add(
                firstVertexIndex + 1
        );

        indices.add(
                firstVertexIndex + 2
        );

        indices.add(
                firstVertexIndex + 2
        );

        indices.add(
                firstVertexIndex + 3
        );

        indices.add(
                firstVertexIndex
        );
    }

    private float[] convertVerticesToArray() {
        float[] result =
                new float[
                        vertices.size()
                        ];

        for (
                int index = 0;
                index < vertices.size();
                index++
        ) {
            result[index] =
                    vertices.get(
                            index
                    );
        }

        return result;
    }

    private int[] convertIndicesToArray() {
        int[] result =
                new int[
                        indices.size()
                        ];

        for (
                int index = 0;
                index < indices.size();
                index++
        ) {
            result[index] =
                    indices.get(
                            index
                    );
        }

        return result;
    }
}