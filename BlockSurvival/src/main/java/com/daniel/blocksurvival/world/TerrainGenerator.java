package com.daniel.blocksurvival.world;

import com.daniel.blocksurvival.world.noise.ValueNoise;
import com.daniel.blocksurvival.world.noise.ValueNoise3D;


public class TerrainGenerator {

    private final ValueNoise terrainNoise;
    private final ValueNoise biomeNoise;
    private final ValueNoise3D caveNoise;

    private final ValueNoise3D tunnelNoiseA;
    private final ValueNoise3D tunnelNoiseB;

    private final ValueNoise3D shaftNoiseA;
    private final ValueNoise3D shaftNoiseB;
    private final ValueNoise3D shaftPlacementNoise;
    private final ValueNoise3D entranceNoise;

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
        caveNoise =
                new ValueNoise3D(seed + 2000);
        tunnelNoiseA =
                new ValueNoise3D(seed + 3000);

        tunnelNoiseB =
                new ValueNoise3D(seed + 4000);
        shaftNoiseA =
                new ValueNoise3D(seed + 5000);

        shaftNoiseB =
                new ValueNoise3D(seed + 6000);

        shaftPlacementNoise =
                new ValueNoise3D(seed + 7000);
        entranceNoise =
                new ValueNoise3D(seed + 8000);
    }

    public void generateChunk(
            World world,
            Chunk chunk
    ) {
        /*
         * Only print the biome once for each horizontal chunk
         * column rather than once for every vertical layer.
         */
        if (chunk.getChunkY() == 0) {
            printChunkBiome(chunk);
        }

        generateTerrain(
                world,
                chunk
        );

        /*
         * Surface decorations and structures are generated only
         * by the surface chunk.
         */
        if (chunk.getChunkY() == 0) {
            generateDecorations(
                    world,
                    chunk
            );

            generateStructures(
                    world,
                    chunk
            );
        }




    }
    private void generateTerrain(
            World world,
            Chunk chunk
    ) {
        int chunkOriginX =
                chunk.getWorldOriginX();

        int chunkOriginY =
                chunk.getWorldOriginY();

        int chunkOriginZ =
                chunk.getWorldOriginZ();

        int chunkMaximumY =
                chunkOriginY +
                        Chunk.SIZE -
                        1;

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
                        worldZ,
                        chunkOriginY,
                        chunkMaximumY
                );
            }
        }
    }
    private void generateTerrainColumn(
            World world,
            int worldX,
            int worldZ,
            int minimumY,
            int maximumY
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

        int columnMinimumY =
                Math.max(
                        minimumY,
                        WorldGenerationSettings.MIN_WORLD_Y
                );

        int columnMaximumY =
                Math.min(
                        maximumY,
                        terrainHeight
                );

        for (int y = columnMinimumY;
             y <= columnMaximumY;
             y++) {

            BlockType blockType =
                    getTerrainBlockType(
                            y,
                            terrainHeight,
                            dirtDepth,
                            surfaceBlock
                    );

            boolean carveEntrance =
                    shouldCarveEntrance(
                            worldX,
                            y,
                            worldZ,
                            terrainHeight
                    );

            if (carveEntrance) {
                blockType = null;

            } else if (blockType == BlockType.STONE) {
                boolean carveChamber =
                        shouldCarveChamber(
                                worldX,
                                y,
                                worldZ,
                                terrainHeight
                        );

                boolean carveTunnel =
                        shouldCarveTunnel(
                                worldX,
                                y,
                                worldZ,
                                terrainHeight
                        );

                boolean carveShaft =
                        shouldCarveShaft(
                                worldX,
                                y,
                                worldZ,
                                terrainHeight
                        );

                if (
                        carveChamber ||
                                carveTunnel ||
                                carveShaft
                ) {
                    blockType = null;
                }
            }

            /*
             * Remove unsupported surface-layer blocks exposed by caves.
             */
            if (
                    blockType == BlockType.DIRT ||
                            blockType == BlockType.GRASS ||
                            blockType == BlockType.SAND ||
                            blockType == BlockType.SNOW
            ) {
                BlockType blockBelow =
                        world.getBlock(
                                worldX,
                                y - 1,
                                worldZ
                        );

                if (blockBelow == null) {
                    blockType = null;
                }
            }

            world.setGeneratedBlock(
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
        BlockType groundBlock =
                world.getBlock(
                        worldX,
                        terrainHeight,
                        worldZ
                );

        if (groundBlock != BlockType.GRASS) {
            return;
        }

        world.setGeneratedBlock(
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
        BlockType groundBlock =
                world.getBlock(
                        worldX,
                        terrainHeight,
                        worldZ
                );

        if (groundBlock != BlockType.SAND) {
            return;
        }

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

            world.setGeneratedBlock(
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
        BlockType groundBlock =
                world.getBlock(
                        worldX,
                        terrainHeight,
                        worldZ
                );

        if (groundBlock != BlockType.GRASS) {
            return;
        }

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

        /*
        System.out.println(
                "Chunk "
                        + chunk.getChunkX()
                        + ", "
                        + chunk.getChunkZ()
                        + " biome: "
                        + centerBiome
        );

         */
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

    public int getTerrainHeight(
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
            world.setGeneratedBlock(
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
                        world.setGeneratedBlock(
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
        world.setGeneratedBlock(
                worldX,
                canopyCenterY + 2,
                worldZ,
                BlockType.LEAVES
        );
    }

    private boolean shouldCarveChamber(
            int worldX,
            int worldY,
            int worldZ,
            int terrainHeight
    ) {
        int surfaceProtectionDepth = 5;

        if (
                worldY >=
                        terrainHeight -
                                surfaceProtectionDepth
        ) {
            return false;
        }

        if (
                worldY <=
                        WorldGenerationSettings.MIN_WORLD_Y + 2
        ) {
            return false;
        }

        float noiseValue =
                caveNoise.sampleOctaves(
                        worldX,
                        worldY,
                        worldZ,
                        2,
                        0.5f
                );

        return noiseValue > 0.65f;
    }


    private boolean isInsideTunnelShape(
            int worldX,
            int worldY,
            int worldZ
    ) {
        float horizontalScale = 1.20f;
        float verticalScale = 2.40f;

        float tunnelValueA =
                tunnelNoiseA.sample(
                        worldX * horizontalScale,
                        worldY * verticalScale,
                        worldZ * horizontalScale
                );

        float tunnelValueB =
                tunnelNoiseB.sample(
                        worldX * horizontalScale,
                        worldY * verticalScale,
                        worldZ * horizontalScale
                );

        float tunnelWidth = 0.075f;

        boolean insideFirstBand =
                Math.abs(tunnelValueA - 0.5f)
                        < tunnelWidth;

        boolean insideSecondBand =
                Math.abs(tunnelValueB - 0.5f)
                        < tunnelWidth;

        return insideFirstBand &&
                insideSecondBand;
    }

    private boolean shouldCarveTunnel(
            int worldX,
            int worldY,
            int worldZ,
            int terrainHeight
    ) {
        int surfaceProtectionDepth = 8;

        if (worldY >= terrainHeight - surfaceProtectionDepth) {
            return false;
        }

        if (worldY <= WorldGenerationSettings.MIN_WORLD_Y + 2) {
            return false;
        }

        return isInsideTunnelShape(
                worldX,
                worldY,
                worldZ
        );
    }

    private boolean shouldCarveShaft(
            int worldX,
            int worldY,
            int worldZ,
            int terrainHeight
    ) {
        int surfaceProtectionDepth = 5;

        if (worldY >= terrainHeight - surfaceProtectionDepth) {
            return false;
        }

        /*
         * Preserve the bottom layers of the world.
         */
        if (worldY <= WorldGenerationSettings.MIN_WORLD_Y + 2) {
            return false;
        }

        /*
         * Shafts should be uncommon.
         *
         * This broad noise field creates large regions where
         * shafts are either permitted or forbidden.
         */
        float placementValue =
                shaftPlacementNoise.sample(
                        worldX * 0.30f,
                        worldY * 0.30f,
                        worldZ * 0.30f
                );

        if (placementValue < 0.72f) {
            return false;
        }

        /*
         * These scales do the opposite of the ordinary tunnels.
         *
         * X and Z change quickly, creating a narrow cross-section.
         * Y changes slowly, stretching the shape vertically.
         */
        float horizontalScale = 2.20f;
        float verticalScale = 0.45f;

        float shaftValueA =
                shaftNoiseA.sample(
                        worldX * horizontalScale,
                        worldY * verticalScale,
                        worldZ * horizontalScale
                );

        float shaftValueB =
                shaftNoiseB.sample(
                        worldX * horizontalScale,
                        worldY * verticalScale,
                        worldZ * horizontalScale
                );

        float shaftWidth = 0.065f;

        boolean insideFirstBand =
                Math.abs(shaftValueA - 0.5f)
                        < shaftWidth;

        boolean insideSecondBand =
                Math.abs(shaftValueB - 0.5f)
                        < shaftWidth;

        return insideFirstBand &&
                insideSecondBand;
    }

    private boolean shouldCarveEntrance(
            int worldX,
            int worldY,
            int worldZ,
            int terrainHeight
    ) {
        if (!shouldAllowEntrance(
                worldX,
                worldZ,
                terrainHeight
        )) {
            return false;
        }

        /*
         * Entrance carving only occurs near the surface.
         *
         * This lets an existing tunnel cut through the stone,
         * dirt, and surface block without affecting deep caves.
         */
        int entranceDepth = 6;

        if (worldY < terrainHeight - entranceDepth) {
            return false;
        }

        if (worldY > terrainHeight) {
            return false;
        }

        return isInsideTunnelShape(
                worldX,
                worldY,
                worldZ
        );
    }

    private boolean shouldAllowEntrance(
            int worldX,
            int worldZ,
            int terrainHeight
    ) {
        /*
         * Don't make cave entrances in low-lying areas.
         * They look much better in hills and mountains.
         */
        if (terrainHeight < 10) {
            return false;
        }

        float value =
                entranceNoise.sample(
                        worldX * 0.08f,
                        0,
                        worldZ * 0.08f
                );

        return value > 0.65f;
    }
}