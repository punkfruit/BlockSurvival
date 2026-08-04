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

    public MeshData buildCube(
            BlockType blockType
    ) {
        if (blockType == null) {
            throw new IllegalArgumentException(
                    "Cannot build a null block."
            );
        }

        vertices.clear();
        indices.clear();

        /*
         * The model is one block wide and centered at zero.
         *
         * The entity renderer will later shrink it to 0.25.
         */
        float minimum =
                -0.5f;

        float maximum =
                0.5f;

        /*
         * Standalone items begin fully illuminated.
         *
         * Once they render correctly, we can sample lighting
         * from the entity's world position.
         */
        float skyLight =
                1.0f;

        float blockLight =
                blockType.getEmittedLight() /
                        15.0f;

        /*
         * Front: positive Z.
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
         * Back: negative Z.
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
         * Left: negative X.
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
         * Right: positive X.
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
         * Top: positive Y.
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
         * Bottom: negative Y.
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

        return new MeshData(
                convertVerticesToArray(),
                convertIndicesToArray()
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
        int firstVertexIndex =
                vertices.size() /
                        FLOATS_PER_VERTEX;

        float tileSize =
                BlockType.getTileSize();

        float minimumU =
                tile.column() *
                        tileSize;

        float minimumV =
                tile.row() *
                        tileSize;

        float maximumU =
                minimumU +
                        tileSize;

        float maximumV =
                minimumV +
                        tileSize;

        addVertex(
                x1,
                y1,
                z1,
                minimumU,
                minimumV,
                skyLight,
                blockLight
        );

        addVertex(
                x2,
                y2,
                z2,
                minimumU,
                maximumV,
                skyLight,
                blockLight
        );

        addVertex(
                x3,
                y3,
                z3,
                maximumU,
                maximumV,
                skyLight,
                blockLight
        );

        addVertex(
                x4,
                y4,
                z4,
                maximumU,
                minimumV,
                skyLight,
                blockLight
        );

        /*
         * Two triangles forming the quad.
         *
         * The winding matches the existing chunk faces.
         */
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