package com.daniel.blocksurvival.world;

import com.daniel.blocksurvival.world.noise.ValueNoise;

public class TerrainGenerator {

    private final ValueNoise noise;

    public TerrainGenerator(int seed) {
        noise = new ValueNoise(seed);
    }

    public void generateChunk(
            World world,
            Chunk chunk
    ) {
        int chunkOriginX =
                chunk.getWorldOriginX();

        int chunkOriginZ =
                chunk.getWorldOriginZ();

        for (int localX = 0; localX < Chunk.SIZE; localX++) {
            for (int localZ = 0; localZ < Chunk.SIZE; localZ++) {

                int worldX =
                        chunkOriginX + localX;

                int worldZ =
                        chunkOriginZ + localZ;

                int terrainHeight =
                        getTerrainHeight(
                                worldX,
                                worldZ
                        );

                for (
                        int worldY = 0;
                        worldY <= terrainHeight;
                        worldY++
                ) {
                    BlockType blockType;

                    if (worldY == terrainHeight) {
                        blockType = BlockType.GRASS;
                    } else {
                        blockType = BlockType.STONE;
                    }

                    world.setBlock(
                            worldX,
                            worldY,
                            worldZ,
                            blockType
                    );
                }
            }
        }
    }

    private int getTerrainHeight(
            int worldX,
            int worldZ
    ) {
        float noiseValue =
                noise.sampleOctaves(
                        worldX,
                        worldZ,
                        3,
                        0.5f
                );

        int minimumHeight = 2;
        int heightRange = 12;

        return minimumHeight +
                Math.round(
                        noiseValue * heightRange
                );
    }
}