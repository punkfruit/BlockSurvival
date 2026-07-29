package com.daniel.blocksurvival.world;

import com.daniel.blocksurvival.world.noise.ValueNoise;

public class TerrainGenerator {

    private final ValueNoise terrainNoise;
    private final ValueNoise biomeNoise;

    private static final float DESERT_FOREST_BORDER = 0.33f;
    private static final float FOREST_SNOW_BORDER = 0.66f;

    /*
     * Total width of each transition zone.
     *
     * 0.10 means:
     * 0.28–0.38 for desert/forest
     * 0.61–0.71 for forest/snow
     */
    private static final float BIOME_BLEND_WIDTH = 0.01f;

    private final int seed;

    public TerrainGenerator(int seed) {
        this.seed = seed;

        terrainNoise =
                new ValueNoise(seed);

        biomeNoise =
                new ValueNoise(seed + 1000);
    }

    public void generateChunk(
            World world,
            Chunk chunk
    ) {
        printChunkBiome(chunk);

        generateTerrain(
                world,
                chunk
        );

        generateDecorations(
                world,
                chunk
        );

        generateStructures(
                world,
                chunk
        );
    }
    private void generateTerrain(
            World world,
            Chunk chunk
    ) {
        int chunkOriginX =
                chunk.getWorldOriginX();

        int chunkOriginZ =
                chunk.getWorldOriginZ();

        for (int localX = 0;
             localX < Chunk.SIZE;
             localX++) {

            for (int localZ = 0;
                 localZ < Chunk.SIZE;
                 localZ++) {

                int worldX =
                        chunkOriginX + localX;

                int worldZ =
                        chunkOriginZ + localZ;

                generateTerrainColumn(
                        world,
                        worldX,
                        worldZ
                );
            }
        }
    }
    private void generateTerrainColumn(
            World world,
            int worldX,
            int worldZ
    ) {
        int terrainHeight =
                getTerrainHeight(
                        worldX,
                        worldZ
                );

        int dirtDepth =
                getDirtDepth(
                        worldX,
                        worldZ
                );

        BlockType surfaceBlock =
                getSurfaceBlock(
                        worldX,
                        worldZ
                );

        for (int y = 0;
             y <= terrainHeight;
             y++) {

            BlockType blockType =
                    getTerrainBlockType(
                            y,
                            terrainHeight,
                            dirtDepth,
                            surfaceBlock
                    );

            world.setBlock(
                    worldX,
                    y,
                    worldZ,
                    blockType
            );
        }
    }

    private BlockType getTerrainBlockType(
            int y,
            int terrainHeight,
            int dirtDepth,
            BlockType surfaceBlock
    ) {
        if (y == terrainHeight) {
            return surfaceBlock;
        }

        if (y >= terrainHeight - dirtDepth) {
            if (surfaceBlock == BlockType.SAND) {
                return BlockType.SAND;
            }

            return BlockType.DIRT;
        }

        return BlockType.STONE;
    }

    private void generateDecorations(
            World world,
            Chunk chunk
    ) {
        int chunkOriginX =
                chunk.getWorldOriginX();

        int chunkOriginZ =
                chunk.getWorldOriginZ();

        for (int localX = 0;
             localX < Chunk.SIZE;
             localX++) {

            for (int localZ = 0;
                 localZ < Chunk.SIZE;
                 localZ++) {

                int worldX =
                        chunkOriginX + localX;

                int worldZ =
                        chunkOriginZ + localZ;

                tryPlaceFlower(
                        world,
                        worldX,
                        worldZ
                );
            }
        }
    }

    private void tryPlaceFlower(
            World world,
            int worldX,
            int worldZ
    ) {
        if (getSurfaceBlock(
                worldX,
                worldZ
        ) != BlockType.GRASS) {
            return;
        }

        if (!shouldPlaceFlower(
                worldX,
                worldZ
        )) {
            return;
        }

        int terrainHeight =
                getTerrainHeight(
                        worldX,
                        worldZ
                );

        world.setBlock(
                worldX,
                terrainHeight + 1,
                worldZ,
                BlockType.FLOWER
        );
    }

    private boolean shouldPlaceFlower(
            int worldX,
            int worldZ
    ) {
        long hash =
                worldX * 87312871L
                        + worldZ * 1299721L
                        + seed * 61L
                        + 3456789L;

        hash ^= hash >>> 13;
        hash *= 1274126177L;
        hash ^= hash >>> 16;

        /*
         * Roughly 1 in 30 grass blocks.
         */
        return Math.floorMod(
                hash,
                30L
        ) == 0;
    }

    private void generateStructures(
            World world,
            Chunk chunk
    ) {
        int chunkOriginX =
                chunk.getWorldOriginX();

        int chunkOriginZ =
                chunk.getWorldOriginZ();

        /*
         * We stay two blocks away from the chunk edges
         * because the tree canopy extends two blocks outward.
         */
        for (int localX = 2;
             localX < Chunk.SIZE - 2;
             localX++) {

            for (int localZ = 2;
                 localZ < Chunk.SIZE - 2;
                 localZ++) {

                int worldX =
                        chunkOriginX + localX;

                int worldZ =
                        chunkOriginZ + localZ;

                tryPlaceTree(
                        world,
                        worldX,
                        worldZ
                );
                tryPlaceCactus(
                        world,
                        worldX,
                        worldZ
                );
            }
        }
    }

    private void tryPlaceCactus(
            World world,
            int worldX,
            int worldZ
    ) {
        BlockType surfaceBlock =
                getSurfaceBlock(
                        worldX,
                        worldZ
                );

        if (surfaceBlock != BlockType.SAND) {
            return;
        }

        if (!shouldPlaceCactus(
                worldX,
                worldZ
        )) {
            return;
        }

        int terrainHeight =
                getTerrainHeight(
                        worldX,
                        worldZ
                );

        int cactusHeight =
                getCactusHeight(
                        worldX,
                        worldZ
                );

        placeCactus(
                world,
                worldX,
                terrainHeight + 1,
                worldZ,
                cactusHeight
        );
    }

    private boolean shouldPlaceCactus(
            int worldX,
            int worldZ
    ) {
        long hash =
                worldX * 4987142L
                        + worldZ * 5947611L
                        + seed * 31L
                        + 817263L;

        hash ^= hash >>> 13;
        hash *= 1274126177L;
        hash ^= hash >>> 16;

        /*
         * Approximately a 1% chance per valid sand column.
         */
        return Math.floorMod(hash, 100L) == 0;
    }

    private int getCactusHeight(
            int worldX,
            int worldZ
    ) {
        long hash =
                worldX * 912931L
                        + worldZ * 73428767L
                        + seed * 47L
                        + 192837L;

        hash ^= hash >>> 13;
        hash *= 1274126177L;
        hash ^= hash >>> 16;

        /*
         * Produces a height from 2 through 4 blocks.
         */
        return 2 + (int) Math.floorMod(hash, 3L);
    }

    private void placeCactus(
            World world,
            int worldX,
            int startY,
            int worldZ,
            int height
    ) {
        for (int offsetY = 0;
             offsetY < height;
             offsetY++) {

            world.setBlock(
                    worldX,
                    startY + offsetY,
                    worldZ,
                    BlockType.CACTUS
            );
        }
    }

    private void tryPlaceTree(
            World world,
            int worldX,
            int worldZ
    ) {
        BlockType surfaceBlock =
                getSurfaceBlock(
                        worldX,
                        worldZ
                );

        if (surfaceBlock != BlockType.GRASS) {
            return;
        }

        if (!shouldPlaceTree(
                worldX,
                worldZ
        )) {
            return;
        }

        int terrainHeight =
                getTerrainHeight(
                        worldX,
                        worldZ
                );

        placeTree(
                world,
                worldX,
                terrainHeight,
                worldZ
        );
    }

    private void printChunkBiome(
            Chunk chunk
    ) {
        int chunkCenterX =
                chunk.getWorldOriginX()
                        + Chunk.SIZE / 2;

        int chunkCenterZ =
                chunk.getWorldOriginZ()
                        + Chunk.SIZE / 2;

        Biome centerBiome =
                getBiome(
                        chunkCenterX,
                        chunkCenterZ
                );

        System.out.println(
                "Chunk "
                        + chunk.getChunkX()
                        + ", "
                        + chunk.getChunkZ()
                        + " biome: "
                        + centerBiome
        );
    }

    private int getDirtDepth(
            int worldX,
            int worldZ
    ) {
        long hash =
                worldX * 73428767L
                        + worldZ * 912931L
                        + seed * 31L;

        hash ^= hash >>> 13;
        hash *= 1274126177L;
        hash ^= hash >>> 16;

        /*
         * Produces a dirt depth between 2 and 4.
         */
        return 2 + (int) Math.floorMod(hash, 3L);
    }

    private int getTerrainHeight(
            int worldX,
            int worldZ
    ) {
        float noiseValue =
                terrainNoise.sampleOctaves(
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

    private Biome getBiome(
            int worldX,
            int worldZ
    ) {
        float biomeValue =
                getBiomeValue(worldX, worldZ);

        if (biomeValue < DESERT_FOREST_BORDER) {
            return Biome.DESERT;

        } else if (biomeValue < FOREST_SNOW_BORDER) {
            return Biome.FOREST;

        } else {
            return Biome.SNOW;
        }
    }

    private float getBiomeValue(
            int worldX,
            int worldZ
    ) {
        return biomeNoise.sample(
                worldX / 128.0f,
                worldZ / 128.0f
        );
    }

    private float getColumnRandom(
            int worldX,
            int worldZ
    ) {
        long hash =
                worldX * 341873128712L
                        + worldZ * 132897987541L
                        + seed * 31L
                        + 918273645L;

        hash ^= hash >>> 13;
        hash *= 1274126177L;
        hash ^= hash >>> 16;

        /*
         * Produce a deterministic value from 0.0 to 1.0.
         */
        long positiveValue =
                Math.floorMod(hash, 1_000_000L);

        return positiveValue / 1_000_000.0f;
    }

    private float smoothstep(float value) {
        value =
                Math.max(
                        0.0f,
                        Math.min(1.0f, value)
                );

        return value * value * (3.0f - 2.0f * value);
    }

    private BlockType getSurfaceBlock(
            int worldX,
            int worldZ
    ) {
        float biomeValue =
                getBiomeValue(
                        worldX,
                        worldZ
                );

        float randomValue =
                getColumnRandom(
                        worldX,
                        worldZ
                );

        float halfBlendWidth =
                BIOME_BLEND_WIDTH / 2.0f;

        /*
         * Desert-to-forest transition.
         */
        float desertForestStart =
                DESERT_FOREST_BORDER -
                        halfBlendWidth;

        float desertForestEnd =
                DESERT_FOREST_BORDER +
                        halfBlendWidth;

        if (biomeValue < desertForestStart) {
            return BlockType.SAND;
        }

        if (biomeValue < desertForestEnd) {
            float blendAmount =
                    (biomeValue - desertForestStart)
                            / (desertForestEnd - desertForestStart);

            blendAmount =
                    smoothstep(blendAmount);

            /*
             * At the start, almost everything is sand.
             * At the end, almost everything is grass.
             */
            if (randomValue < blendAmount) {
                return BlockType.GRASS;
            }

            return BlockType.SAND;
        }

        /*
         * Forest-to-snow transition.
         */
        float forestSnowStart =
                FOREST_SNOW_BORDER -
                        halfBlendWidth;

        float forestSnowEnd =
                FOREST_SNOW_BORDER +
                        halfBlendWidth;

        if (biomeValue < forestSnowStart) {
            return BlockType.GRASS;
        }

        if (biomeValue < forestSnowEnd) {
            float blendAmount =
                    (biomeValue - forestSnowStart)
                            / (forestSnowEnd - forestSnowStart);

            blendAmount =
                    smoothstep(blendAmount);

            /*
             * At the start, almost everything is grass.
             * At the end, almost everything is snow.
             */
            if (randomValue < blendAmount) {
                return BlockType.SNOW;
            }

            return BlockType.GRASS;
        }

        return BlockType.SNOW;
    }

    private boolean shouldPlaceTree(
            int worldX,
            int worldZ
    ) {
        long hash =
                worldX * 341873128712L
                        + worldZ * 132897987541L
                        + seed * 31L;

        /*
         * Mix the bits so nearby coordinates do not produce
         * obviously related results.
         */
        hash ^= hash >>> 13;
        hash *= 1274126177L;
        hash ^= hash >>> 16;

        /*
         * Approximately one out of every 55 eligible positions
         * will contain a tree.
         */
        return Math.floorMod(hash, 55L) == 0;
    }

    private void placeTree(
            World world,
            int worldX,
            int groundY,
            int worldZ
    ) {
        int trunkHeight = 4;

        /*
         * Build the trunk above the grass block.
         */
        for (int y = 1; y <= trunkHeight; y++) {
            world.setBlock(
                    worldX,
                    groundY + y,
                    worldZ,
                    BlockType.WOOD
            );
        }

        int canopyCenterY =
                groundY + trunkHeight;

        /*
         * Create a roughly cubic leaf canopy.
         */
        for (int offsetX = -2; offsetX <= 2; offsetX++) {
            for (int offsetY = -1; offsetY <= 2; offsetY++) {
                for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {

                    /*
                     * Remove the canopy's extreme corners,
                     * making it less box-shaped.
                     */
                    int distance =
                            Math.abs(offsetX)
                                    + Math.abs(offsetY)
                                    + Math.abs(offsetZ);

                    if (distance > 4) {
                        continue;
                    }

                    int leafX =
                            worldX + offsetX;

                    int leafY =
                            canopyCenterY + offsetY;

                    int leafZ =
                            worldZ + offsetZ;

                    /*
                     * Do not overwrite the trunk.
                     */
                    if (
                            leafX == worldX &&
                                    leafZ == worldZ &&
                                    leafY <= canopyCenterY
                    ) {
                        continue;
                    }

                    /*
                     * Only place leaves into empty cells.
                     */
                    if (!world.hasBlock(leafX, leafY, leafZ)) {
                        world.setBlock(
                                leafX,
                                leafY,
                                leafZ,
                                BlockType.LEAVES
                        );
                    }
                }
            }
        }

        /*
         * Add one leafy cap above the trunk.
         */
        world.setBlock(
                worldX,
                canopyCenterY + 2,
                worldZ,
                BlockType.LEAVES
        );
    }
}