package com.daniel.blocksurvival.world;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class LightEngine {

    /*
     * Initial direct skylight currently depends on the
     * procedural terrain height.
     *
     * Later, dynamic lighting will inspect the actual world
     * blocks instead, but this preserves today's behavior.
     */
    private final TerrainGenerator terrainGenerator;

    /*
     * Skylight can decrease at most 15 blocks from a source.
     *
     * One neighboring chunk on every side provides at least
     * a 16-block relighting buffer.
     */
    private static final int RELIGHT_CHUNK_RADIUS = 1;

    private static final int[][] DIRECTIONS = {
            {-1, 0, 0},
            {1, 0, 0},
            {0, -1, 0},
            {0, 1, 0},
            {0, 0, -1},
            {0, 0, 1}
    };

    private record LightNode(
            int worldX,
            int worldY,
            int worldZ
    ) {
    }

    public LightEngine(
            TerrainGenerator terrainGenerator
    ) {
        this.terrainGenerator =
                terrainGenerator;
    }

    /*
     * Calculates initial sunlight for one chunk and then
     * spreads that light through all available neighboring
     * chunks.
     *
     * The returned set contains other chunks whose light data
     * changed and therefore need background remeshing.
     */
    public Set<Chunk> generateSkyLight(
            World world,
            Chunk chunk
    ) {
        generateInitialSkyLight(
                chunk
        );

        return propagateSkyLight(
                world,
                chunk
        );
    }

    private void generateInitialSkyLight(
            Chunk chunk
    ) {
        int chunkOriginX =
                chunk.getWorldOriginX();

        int chunkOriginY =
                chunk.getWorldOriginY();

        int chunkOriginZ =
                chunk.getWorldOriginZ();

        for (
                int localX = 0;
                localX < Chunk.SIZE;
                localX++
        ) {
            for (
                    int localZ = 0;
                    localZ < Chunk.SIZE;
                    localZ++
            ) {
                int worldX =
                        chunkOriginX +
                                localX;

                int worldZ =
                        chunkOriginZ +
                                localZ;

                int terrainHeight =
                        terrainGenerator.getTerrainHeight(
                                worldX,
                                worldZ
                        );

                for (
                        int localY = 0;
                        localY < Chunk.SIZE;
                        localY++
                ) {
                    int worldY =
                            chunkOriginY +
                                    localY;

                    BlockType block =
                            chunk.getBlock(
                                    localX,
                                    localY,
                                    localZ
                            );

                    boolean allowsLight =
                            block == null ||
                                    !block.isOpaque();

                    int lightLevel =
                            worldY > terrainHeight &&
                                    allowsLight
                                    ? 15
                                    : 0;

                    chunk.setSkyLight(
                            localX,
                            localY,
                            localZ,
                            lightLevel
                    );
                }
            }
        }
    }

    private Set<Chunk> propagateSkyLight(
            World world,
            Chunk sourceChunk
    ) {
        ArrayDeque<LightNode> lightQueue =
                new ArrayDeque<>();

        Set<Chunk> changedChunks =
                new HashSet<>();

        int chunkOriginX =
                sourceChunk.getWorldOriginX();

        int chunkOriginY =
                sourceChunk.getWorldOriginY();

        int chunkOriginZ =
                sourceChunk.getWorldOriginZ();

        /*
         * Seed the queue with direct sunlight originating
         * inside this chunk.
         */
        for (
                int localX = 0;
                localX < Chunk.SIZE;
                localX++
        ) {
            for (
                    int localY = 0;
                    localY < Chunk.SIZE;
                    localY++
            ) {
                for (
                        int localZ = 0;
                        localZ < Chunk.SIZE;
                        localZ++
                ) {
                    if (
                            sourceChunk.getSkyLight(
                                    localX,
                                    localY,
                                    localZ
                            ) != 15
                    ) {
                        continue;
                    }

                    lightQueue.addLast(
                            new LightNode(
                                    chunkOriginX +
                                            localX,
                                    chunkOriginY +
                                            localY,
                                    chunkOriginZ +
                                            localZ
                            )
                    );
                }
            }
        }

        /*
         * Pull in light from neighboring chunks that completed
         * their own lighting before this one.
         */
        importNeighborSkyLight(
                world,
                sourceChunk,
                lightQueue
        );



        while (!lightQueue.isEmpty()) {
            LightNode current =
                    lightQueue.removeFirst();

            int currentLight =
                    world.getSkyLight(
                            current.worldX(),
                            current.worldY(),
                            current.worldZ()
                    );

            if (currentLight <= 1) {
                continue;
            }

            int nextLight =
                    currentLight - 1;

            for (int[] direction : DIRECTIONS) {
                int neighborX =
                        current.worldX() +
                                direction[0];

                int neighborY =
                        current.worldY() +
                                direction[1];

                int neighborZ =
                        current.worldZ() +
                                direction[2];

                if (
                        neighborY <
                                WorldGenerationSettings.MIN_WORLD_Y
                ) {
                    continue;
                }

                if (!world.allowsSkyLight(
                        neighborX,
                        neighborY,
                        neighborZ
                )) {
                    continue;
                }

                int existingLight =
                        world.getSkyLight(
                                neighborX,
                                neighborY,
                                neighborZ
                        );

                if (nextLight <= existingLight) {
                    continue;
                }

                Chunk changedChunk =
                        world.getChunkAtWorldBlock(
                                neighborX,
                                neighborY,
                                neighborZ
                        );

                world.setSkyLight(
                        neighborX,
                        neighborY,
                        neighborZ,
                        nextLight
                );

                if (
                        changedChunk != null &&
                                changedChunk != sourceChunk
                ) {
                    changedChunks.add(
                            changedChunk
                    );
                }

                lightQueue.addLast(
                        new LightNode(
                                neighborX,
                                neighborY,
                                neighborZ
                        )
                );
            }
        }

        return changedChunks;
    }

    private void importNeighborSkyLight(
            World world,
            Chunk chunk,
            ArrayDeque<LightNode> lightQueue
    ) {
        int originX =
                chunk.getWorldOriginX();

        int originY =
                chunk.getWorldOriginY();

        int originZ =
                chunk.getWorldOriginZ();

        for (
                int localX = 0;
                localX < Chunk.SIZE;
                localX++
        ) {
            for (
                    int localY = 0;
                    localY < Chunk.SIZE;
                    localY++
            ) {
                for (
                        int localZ = 0;
                        localZ < Chunk.SIZE;
                        localZ++
                ) {
                    boolean onBoundary =
                            localX == 0 ||
                                    localX ==
                                            Chunk.SIZE - 1 ||
                                    localY == 0 ||
                                    localY ==
                                            Chunk.SIZE - 1 ||
                                    localZ == 0 ||
                                    localZ ==
                                            Chunk.SIZE - 1;

                    if (!onBoundary) {
                        continue;
                    }

                    int worldX =
                            originX +
                                    localX;

                    int worldY =
                            originY +
                                    localY;

                    int worldZ =
                            originZ +
                                    localZ;

                    BlockType block =
                            world.getBlock(
                                    worldX,
                                    worldY,
                                    worldZ
                            );

                    if (
                            block != null &&
                                    block.isOpaque()
                    ) {
                        continue;
                    }

                    int bestLight =
                            chunk.getSkyLight(
                                    localX,
                                    localY,
                                    localZ
                            );

                    if (localX == 0) {
                        bestLight =
                                Math.max(
                                        bestLight,
                                        world.getSkyLight(
                                                worldX - 1,
                                                worldY,
                                                worldZ
                                        ) - 1
                                );
                    }

                    if (
                            localX ==
                                    Chunk.SIZE - 1
                    ) {
                        bestLight =
                                Math.max(
                                        bestLight,
                                        world.getSkyLight(
                                                worldX + 1,
                                                worldY,
                                                worldZ
                                        ) - 1
                                );
                    }

                    if (localY == 0) {
                        bestLight =
                                Math.max(
                                        bestLight,
                                        world.getSkyLight(
                                                worldX,
                                                worldY - 1,
                                                worldZ
                                        ) - 1
                                );
                    }

                    if (
                            localY ==
                                    Chunk.SIZE - 1
                    ) {
                        bestLight =
                                Math.max(
                                        bestLight,
                                        world.getSkyLight(
                                                worldX,
                                                worldY + 1,
                                                worldZ
                                        ) - 1
                                );
                    }

                    if (localZ == 0) {
                        bestLight =
                                Math.max(
                                        bestLight,
                                        world.getSkyLight(
                                                worldX,
                                                worldY,
                                                worldZ - 1
                                        ) - 1
                                );
                    }

                    if (
                            localZ ==
                                    Chunk.SIZE - 1
                    ) {
                        bestLight =
                                Math.max(
                                        bestLight,
                                        world.getSkyLight(
                                                worldX,
                                                worldY,
                                                worldZ + 1
                                        ) - 1
                                );
                    }

                    bestLight =
                            Math.max(
                                    0,
                                    bestLight
                            );

                    int existingLight =
                            chunk.getSkyLight(
                                    localX,
                                    localY,
                                    localZ
                            );

                    if (bestLight <= existingLight) {
                        continue;
                    }

                    chunk.setSkyLight(
                            localX,
                            localY,
                            localZ,
                            bestLight
                    );

                    lightQueue.addLast(
                            new LightNode(
                                    worldX,
                                    worldY,
                                    worldZ
                            )
                    );
                }
            }
        }
    }
    public Set<Chunk> blockChanged(
            World world,
            int worldX,
            int worldY,
            int worldZ,
            BlockType oldBlock,
            BlockType newBlock
    ) {
        Set<Chunk> changedChunks =
                new HashSet<>();

        boolean oldBlocksLight =
                oldBlock != null &&
                        oldBlock.isOpaque();

        boolean newBlocksLight =
                newBlock != null &&
                        newBlock.isOpaque();

        int oldEmission =
                oldBlock == null
                        ? 0
                        : oldBlock.getEmittedLight();

        int newEmission =
                newBlock == null
                        ? 0
                        : newBlock.getEmittedLight();

        boolean opacityChanged =
                oldBlocksLight !=
                        newBlocksLight;

        boolean emissionChanged =
                oldEmission !=
                        newEmission;

        int editedChunkX =
                Math.floorDiv(
                        worldX,
                        Chunk.SIZE
                );

        int editedChunkZ =
                Math.floorDiv(
                        worldZ,
                        Chunk.SIZE
                );

        int minimumChunkX =
                editedChunkX -
                        RELIGHT_CHUNK_RADIUS;

        int maximumChunkX =
                editedChunkX +
                        RELIGHT_CHUNK_RADIUS;

        int minimumChunkZ =
                editedChunkZ -
                        RELIGHT_CHUNK_RADIUS;

        int maximumChunkZ =
                editedChunkZ +
                        RELIGHT_CHUNK_RADIUS;

        /*
         * Skylight only needs recalculation if the edit changed
         * whether sunlight can pass.
         */
        if (opacityChanged) {
            changedChunks.addAll(
                    relightRegion(
                            world,
                            minimumChunkX,
                            maximumChunkX,
                            minimumChunkZ,
                            maximumChunkZ
                    )
            );
        }

        /*
         * Block light changes if:
         *
         * - a light source was added or removed;
         * - an opaque block opened or closed a path.
         */
        if (
                emissionChanged ||
                        opacityChanged
        ) {
            changedChunks.addAll(
                    relightBlockRegion(
                            world,
                            minimumChunkX,
                            maximumChunkX,
                            minimumChunkZ,
                            maximumChunkZ
                    )
            );
        }

        return changedChunks;
    }

    private void clearRegionBlockLight(
            World world,
            int minimumChunkX,
            int maximumChunkX,
            int minimumChunkZ,
            int maximumChunkZ,
            Set<Chunk> changedChunks
    ) {
        for (
                int chunkX = minimumChunkX;
                chunkX <= maximumChunkX;
                chunkX++
        ) {
            for (
                    int chunkZ = minimumChunkZ;
                    chunkZ <= maximumChunkZ;
                    chunkZ++
            ) {
                for (
                        int chunkY =
                        WorldGenerationSettings.MIN_CHUNK_Y;
                        chunkY <=
                                WorldGenerationSettings.MAX_CHUNK_Y;
                        chunkY++
                ) {
                    Chunk chunk =
                            world.getChunk(
                                    chunkX,
                                    chunkY,
                                    chunkZ
                            );

                    if (
                            chunk == null ||
                                    !chunk.hasTerrain()
                    ) {
                        continue;
                    }

                    boolean changed =
                            false;

                    for (
                            int localX = 0;
                            localX < Chunk.SIZE;
                            localX++
                    ) {
                        for (
                                int localY = 0;
                                localY < Chunk.SIZE;
                                localY++
                        ) {
                            for (
                                    int localZ = 0;
                                    localZ < Chunk.SIZE;
                                    localZ++
                            ) {
                                if (
                                        chunk.getBlockLight(
                                                localX,
                                                localY,
                                                localZ
                                        ) == 0
                                ) {
                                    continue;
                                }

                                chunk.setBlockLight(
                                        localX,
                                        localY,
                                        localZ,
                                        0
                                );

                                changed =
                                        true;
                            }
                        }
                    }

                    if (changed) {
                        changedChunks.add(
                                chunk
                        );
                    }
                }
            }
        }
    }

    private void seedBlockLightSources(
            World world,
            int minimumChunkX,
            int maximumChunkX,
            int minimumChunkZ,
            int maximumChunkZ,
            ArrayDeque<LightNode> lightQueue,
            Set<Chunk> changedChunks
    ) {
        int minimumWorldX =
                minimumChunkX *
                        Chunk.SIZE;

        int maximumWorldX =
                (
                        maximumChunkX + 1
                ) * Chunk.SIZE - 1;

        int minimumWorldZ =
                minimumChunkZ *
                        Chunk.SIZE;

        int maximumWorldZ =
                (
                        maximumChunkZ + 1
                ) * Chunk.SIZE - 1;

        for (
                int worldX = minimumWorldX;
                worldX <= maximumWorldX;
                worldX++
        ) {
            for (
                    int worldY =
                    WorldGenerationSettings.MIN_WORLD_Y;
                    worldY <=
                            WorldGenerationSettings.MAX_WORLD_Y;
                    worldY++
            ) {
                for (
                        int worldZ = minimumWorldZ;
                        worldZ <= maximumWorldZ;
                        worldZ++
                ) {
                    BlockType block =
                            world.getBlock(
                                    worldX,
                                    worldY,
                                    worldZ
                            );

                    if (block == null) {
                        continue;
                    }

                    int emittedLight =
                            block.getEmittedLight();

                    if (emittedLight <= 0) {
                        continue;
                    }

                    world.setBlockLight(
                            worldX,
                            worldY,
                            worldZ,
                            emittedLight
                    );

                    Chunk changedChunk =
                            world.getChunkAtWorldBlock(
                                    worldX,
                                    worldY,
                                    worldZ
                            );

                    if (changedChunk != null) {
                        changedChunks.add(
                                changedChunk
                        );
                    }

                    lightQueue.addLast(
                            new LightNode(
                                    worldX,
                                    worldY,
                                    worldZ
                            )
                    );
                }
            }
        }
    }

    private void importBlockLightBorderCell(
            World world,
            int insideX,
            int insideY,
            int insideZ,
            int outsideX,
            int outsideY,
            int outsideZ,
            ArrayDeque<LightNode> lightQueue,
            Set<Chunk> changedChunks
    ) {
        if (!world.allowsBlockLight(
                insideX,
                insideY,
                insideZ
        )) {
            return;
        }

        int importedLight =
                world.getBlockLight(
                        outsideX,
                        outsideY,
                        outsideZ
                ) - 1;

        if (importedLight <= 0) {
            return;
        }

        int existingLight =
                world.getBlockLight(
                        insideX,
                        insideY,
                        insideZ
                );

        if (importedLight <= existingLight) {
            return;
        }

        world.setBlockLight(
                insideX,
                insideY,
                insideZ,
                importedLight
        );

        Chunk changedChunk =
                world.getChunkAtWorldBlock(
                        insideX,
                        insideY,
                        insideZ
                );

        if (changedChunk != null) {
            changedChunks.add(
                    changedChunk
            );
        }

        lightQueue.addLast(
                new LightNode(
                        insideX,
                        insideY,
                        insideZ
                )
        );
    }

    private void importRegionBorderBlockLight(
            World world,
            int minimumWorldX,
            int maximumWorldX,
            int minimumWorldZ,
            int maximumWorldZ,
            ArrayDeque<LightNode> lightQueue,
            Set<Chunk> changedChunks
    ) {
        for (
                int worldX = minimumWorldX;
                worldX <= maximumWorldX;
                worldX++
        ) {
            for (
                    int worldY =
                    WorldGenerationSettings.MIN_WORLD_Y;
                    worldY <=
                            WorldGenerationSettings.MAX_WORLD_Y;
                    worldY++
            ) {
                importBlockLightBorderCell(
                        world,
                        worldX,
                        worldY,
                        minimumWorldZ,
                        worldX,
                        worldY,
                        minimumWorldZ - 1,
                        lightQueue,
                        changedChunks
                );

                importBlockLightBorderCell(
                        world,
                        worldX,
                        worldY,
                        maximumWorldZ,
                        worldX,
                        worldY,
                        maximumWorldZ + 1,
                        lightQueue,
                        changedChunks
                );
            }
        }

        for (
                int worldZ = minimumWorldZ;
                worldZ <= maximumWorldZ;
                worldZ++
        ) {
            for (
                    int worldY =
                    WorldGenerationSettings.MIN_WORLD_Y;
                    worldY <=
                            WorldGenerationSettings.MAX_WORLD_Y;
                    worldY++
            ) {
                importBlockLightBorderCell(
                        world,
                        minimumWorldX,
                        worldY,
                        worldZ,
                        minimumWorldX - 1,
                        worldY,
                        worldZ,
                        lightQueue,
                        changedChunks
                );

                importBlockLightBorderCell(
                        world,
                        maximumWorldX,
                        worldY,
                        worldZ,
                        maximumWorldX + 1,
                        worldY,
                        worldZ,
                        lightQueue,
                        changedChunks
                );
            }
        }
    }

    private void propagateRegionBlockLight(
            World world,
            int minimumChunkX,
            int maximumChunkX,
            int minimumChunkZ,
            int maximumChunkZ,
            ArrayDeque<LightNode> lightQueue,
            Set<Chunk> changedChunks
    ) {
        int minimumWorldX =
                minimumChunkX *
                        Chunk.SIZE;

        int maximumWorldX =
                (
                        maximumChunkX + 1
                ) * Chunk.SIZE - 1;

        int minimumWorldZ =
                minimumChunkZ *
                        Chunk.SIZE;

        int maximumWorldZ =
                (
                        maximumChunkZ + 1
                ) * Chunk.SIZE - 1;

        importRegionBorderBlockLight(
                world,
                minimumWorldX,
                maximumWorldX,
                minimumWorldZ,
                maximumWorldZ,
                lightQueue,
                changedChunks
        );

        while (!lightQueue.isEmpty()) {
            LightNode current =
                    lightQueue.removeFirst();

            int currentLight =
                    world.getBlockLight(
                            current.worldX(),
                            current.worldY(),
                            current.worldZ()
                    );

            if (currentLight <= 1) {
                continue;
            }

            int nextLight =
                    currentLight - 1;

            for (int[] direction : DIRECTIONS) {
                int neighborX =
                        current.worldX() +
                                direction[0];

                int neighborY =
                        current.worldY() +
                                direction[1];

                int neighborZ =
                        current.worldZ() +
                                direction[2];

                if (
                        neighborX < minimumWorldX ||
                                neighborX > maximumWorldX ||
                                neighborZ < minimumWorldZ ||
                                neighborZ > maximumWorldZ ||
                                neighborY <
                                        WorldGenerationSettings.MIN_WORLD_Y ||
                                neighborY >
                                        WorldGenerationSettings.MAX_WORLD_Y
                ) {
                    continue;
                }

                if (!world.allowsBlockLight(
                        neighborX,
                        neighborY,
                        neighborZ
                )) {
                    continue;
                }

                int existingLight =
                        world.getBlockLight(
                                neighborX,
                                neighborY,
                                neighborZ
                        );

                if (nextLight <= existingLight) {
                    continue;
                }

                world.setBlockLight(
                        neighborX,
                        neighborY,
                        neighborZ,
                        nextLight
                );

                Chunk changedChunk =
                        world.getChunkAtWorldBlock(
                                neighborX,
                                neighborY,
                                neighborZ
                        );

                if (changedChunk != null) {
                    changedChunks.add(
                            changedChunk
                    );
                }

                lightQueue.addLast(
                        new LightNode(
                                neighborX,
                                neighborY,
                                neighborZ
                        )
                );
            }
        }
    }

    private Set<Chunk> relightBlockRegion(
            World world,
            int minimumChunkX,
            int maximumChunkX,
            int minimumChunkZ,
            int maximumChunkZ
    ) {
        Set<Chunk> changedChunks =
                new HashSet<>();

        ArrayDeque<LightNode> lightQueue =
                new ArrayDeque<>();

        clearRegionBlockLight(
                world,
                minimumChunkX,
                maximumChunkX,
                minimumChunkZ,
                maximumChunkZ,
                changedChunks
        );

        seedBlockLightSources(
                world,
                minimumChunkX,
                maximumChunkX,
                minimumChunkZ,
                maximumChunkZ,
                lightQueue,
                changedChunks
        );

        propagateRegionBlockLight(
                world,
                minimumChunkX,
                maximumChunkX,
                minimumChunkZ,
                maximumChunkZ,
                lightQueue,
                changedChunks
        );

        return changedChunks;
    }

    private Set<Chunk> relightRegion(
            World world,
            int minimumChunkX,
            int maximumChunkX,
            int minimumChunkZ,
            int maximumChunkZ
    ) {
        Set<Chunk> changedChunks =
                new HashSet<>();

        /*
         * First erase all skylight in the affected loaded chunks.
         *
         * We need to remove old light before recalculating, or an
         * enclosed building would retain its previous sunlight.
         */
        clearRegionSkyLight(
                world,
                minimumChunkX,
                maximumChunkX,
                minimumChunkZ,
                maximumChunkZ,
                changedChunks
        );

        /*
         * Recreate direct sunlight by scanning downward through
         * the actual blocks currently in the world.
         */
        generateDirectSkyLightForRegion(
                world,
                minimumChunkX,
                maximumChunkX,
                minimumChunkZ,
                maximumChunkZ,
                changedChunks
        );

        /*
         * Spread direct sunlight sideways and downward through
         * openings, caves, windows, and doorways.
         */
        propagateRegionSkyLight(
                world,
                minimumChunkX,
                maximumChunkX,
                minimumChunkZ,
                maximumChunkZ,
                changedChunks
        );

        return changedChunks;
    }

    private void clearRegionSkyLight(
            World world,
            int minimumChunkX,
            int maximumChunkX,
            int minimumChunkZ,
            int maximumChunkZ,
            Set<Chunk> changedChunks
    ) {
        for (
                int chunkX = minimumChunkX;
                chunkX <= maximumChunkX;
                chunkX++
        ) {
            for (
                    int chunkZ = minimumChunkZ;
                    chunkZ <= maximumChunkZ;
                    chunkZ++
            ) {
                for (
                        int chunkY =
                        WorldGenerationSettings.MIN_CHUNK_Y;
                        chunkY <=
                                WorldGenerationSettings.MAX_CHUNK_Y;
                        chunkY++
                ) {
                    Chunk chunk =
                            world.getChunk(
                                    chunkX,
                                    chunkY,
                                    chunkZ
                            );

                    if (
                            chunk == null ||
                                    !chunk.hasTerrain()
                    ) {
                        continue;
                    }

                    boolean changed =
                            false;

                    for (
                            int localX = 0;
                            localX < Chunk.SIZE;
                            localX++
                    ) {
                        for (
                                int localY = 0;
                                localY < Chunk.SIZE;
                                localY++
                        ) {
                            for (
                                    int localZ = 0;
                                    localZ < Chunk.SIZE;
                                    localZ++
                            ) {
                                if (
                                        chunk.getSkyLight(
                                                localX,
                                                localY,
                                                localZ
                                        ) == 0
                                ) {
                                    continue;
                                }

                                chunk.setSkyLight(
                                        localX,
                                        localY,
                                        localZ,
                                        0
                                );

                                changed =
                                        true;
                            }
                        }
                    }

                    if (changed) {
                        changedChunks.add(
                                chunk
                        );
                    }
                }
            }
        }
    }

    private void generateDirectSkyLightForRegion(
            World world,
            int minimumChunkX,
            int maximumChunkX,
            int minimumChunkZ,
            int maximumChunkZ,
            Set<Chunk> changedChunks
    ) {
        int minimumWorldX =
                minimumChunkX *
                        Chunk.SIZE;

        int maximumWorldX =
                (
                        maximumChunkX + 1
                ) * Chunk.SIZE - 1;

        int minimumWorldZ =
                minimumChunkZ *
                        Chunk.SIZE;

        int maximumWorldZ =
                (
                        maximumChunkZ + 1
                ) * Chunk.SIZE - 1;

        for (
                int worldX = minimumWorldX;
                worldX <= maximumWorldX;
                worldX++
        ) {
            for (
                    int worldZ = minimumWorldZ;
                    worldZ <= maximumWorldZ;
                    worldZ++
            ) {
                boolean sunlightBlocked =
                        false;

                for (
                        int worldY =
                        WorldGenerationSettings.MAX_WORLD_Y;
                        worldY >=
                                WorldGenerationSettings.MIN_WORLD_Y;
                        worldY--
                ) {
                    Chunk chunk =
                            world.getChunkAtWorldBlock(
                                    worldX,
                                    worldY,
                                    worldZ
                            );

                    /*
                     * Missing chunks are skipped rather than created.
                     */
                    if (
                            chunk == null ||
                                    !chunk.hasTerrain()
                    ) {
                        continue;
                    }

                    BlockType block =
                            world.getBlock(
                                    worldX,
                                    worldY,
                                    worldZ
                            );

                    if (
                            block != null &&
                                    block.isOpaque()
                    ) {
                        sunlightBlocked =
                                true;

                        continue;
                    }

                    if (sunlightBlocked) {
                        continue;
                    }

                    world.setSkyLight(
                            worldX,
                            worldY,
                            worldZ,
                            15
                    );

                    changedChunks.add(
                            chunk
                    );
                }
            }
        }
    }

    private void propagateRegionSkyLight(
            World world,
            int minimumChunkX,
            int maximumChunkX,
            int minimumChunkZ,
            int maximumChunkZ,
            Set<Chunk> changedChunks
    ) {
        ArrayDeque<LightNode> lightQueue =
                new ArrayDeque<>();

        int minimumWorldX =
                minimumChunkX *
                        Chunk.SIZE;

        int maximumWorldX =
                (
                        maximumChunkX + 1
                ) * Chunk.SIZE - 1;

        int minimumWorldZ =
                minimumChunkZ *
                        Chunk.SIZE;

        int maximumWorldZ =
                (
                        maximumChunkZ + 1
                ) * Chunk.SIZE - 1;

        /*
         * Seed the queue with every direct-sunlight cell inside
         * the recalculated region.
         */
        for (
                int worldX = minimumWorldX;
                worldX <= maximumWorldX;
                worldX++
        ) {
            for (
                    int worldY =
                    WorldGenerationSettings.MIN_WORLD_Y;
                    worldY <=
                            WorldGenerationSettings.MAX_WORLD_Y;
                    worldY++
            ) {
                for (
                        int worldZ = minimumWorldZ;
                        worldZ <= maximumWorldZ;
                        worldZ++
                ) {
                    if (
                            world.getSkyLight(
                                    worldX,
                                    worldY,
                                    worldZ
                            ) != 15
                    ) {
                        continue;
                    }

                    lightQueue.addLast(
                            new LightNode(
                                    worldX,
                                    worldY,
                                    worldZ
                            )
                    );
                }
            }
        }

        /*
         * Existing light immediately outside the region may still
         * illuminate cells near its border.
         */
        importRegionBorderLight(
                world,
                minimumWorldX,
                maximumWorldX,
                minimumWorldZ,
                maximumWorldZ,
                lightQueue,
                changedChunks
        );

        while (!lightQueue.isEmpty()) {
            LightNode current =
                    lightQueue.removeFirst();

            int currentLight =
                    world.getSkyLight(
                            current.worldX(),
                            current.worldY(),
                            current.worldZ()
                    );

            if (currentLight <= 1) {
                continue;
            }

            int nextLight =
                    currentLight - 1;

            for (int[] direction : DIRECTIONS) {
                int neighborX =
                        current.worldX() +
                                direction[0];

                int neighborY =
                        current.worldY() +
                                direction[1];

                int neighborZ =
                        current.worldZ() +
                                direction[2];

                /*
                 * Keep this recalculation inside the buffered region.
                 */
                if (
                        neighborX < minimumWorldX ||
                                neighborX > maximumWorldX ||
                                neighborZ < minimumWorldZ ||
                                neighborZ > maximumWorldZ ||
                                neighborY <
                                        WorldGenerationSettings.MIN_WORLD_Y ||
                                neighborY >
                                        WorldGenerationSettings.MAX_WORLD_Y
                ) {
                    continue;
                }

                if (!world.allowsSkyLight(
                        neighborX,
                        neighborY,
                        neighborZ
                )) {
                    continue;
                }

                int existingLight =
                        world.getSkyLight(
                                neighborX,
                                neighborY,
                                neighborZ
                        );

                if (nextLight <= existingLight) {
                    continue;
                }

                world.setSkyLight(
                        neighborX,
                        neighborY,
                        neighborZ,
                        nextLight
                );

                Chunk changedChunk =
                        world.getChunkAtWorldBlock(
                                neighborX,
                                neighborY,
                                neighborZ
                        );

                if (changedChunk != null) {
                    changedChunks.add(
                            changedChunk
                    );
                }

                lightQueue.addLast(
                        new LightNode(
                                neighborX,
                                neighborY,
                                neighborZ
                        )
                );
            }
        }
    }

    private void importRegionBorderLight(
            World world,
            int minimumWorldX,
            int maximumWorldX,
            int minimumWorldZ,
            int maximumWorldZ,
            ArrayDeque<LightNode> lightQueue,
            Set<Chunk> changedChunks
    ) {
        for (
                int worldX = minimumWorldX;
                worldX <= maximumWorldX;
                worldX++
        ) {
            for (
                    int worldY =
                    WorldGenerationSettings.MIN_WORLD_Y;
                    worldY <=
                            WorldGenerationSettings.MAX_WORLD_Y;
                    worldY++
            ) {
                importBorderCell(
                        world,
                        worldX,
                        worldY,
                        minimumWorldZ,
                        worldX,
                        worldY,
                        minimumWorldZ - 1,
                        lightQueue,
                        changedChunks
                );

                importBorderCell(
                        world,
                        worldX,
                        worldY,
                        maximumWorldZ,
                        worldX,
                        worldY,
                        maximumWorldZ + 1,
                        lightQueue,
                        changedChunks
                );
            }
        }

        for (
                int worldZ = minimumWorldZ;
                worldZ <= maximumWorldZ;
                worldZ++
        ) {
            for (
                    int worldY =
                    WorldGenerationSettings.MIN_WORLD_Y;
                    worldY <=
                            WorldGenerationSettings.MAX_WORLD_Y;
                    worldY++
            ) {
                importBorderCell(
                        world,
                        minimumWorldX,
                        worldY,
                        worldZ,
                        minimumWorldX - 1,
                        worldY,
                        worldZ,
                        lightQueue,
                        changedChunks
                );

                importBorderCell(
                        world,
                        maximumWorldX,
                        worldY,
                        worldZ,
                        maximumWorldX + 1,
                        worldY,
                        worldZ,
                        lightQueue,
                        changedChunks
                );
            }
        }
    }
    private void importBorderCell(
            World world,
            int insideX,
            int insideY,
            int insideZ,
            int outsideX,
            int outsideY,
            int outsideZ,
            ArrayDeque<LightNode> lightQueue,
            Set<Chunk> changedChunks
    ) {
        if (!world.allowsSkyLight(
                insideX,
                insideY,
                insideZ
        )) {
            return;
        }

        int importedLight =
                world.getSkyLight(
                        outsideX,
                        outsideY,
                        outsideZ
                ) - 1;

        if (importedLight <= 0) {
            return;
        }

        int existingLight =
                world.getSkyLight(
                        insideX,
                        insideY,
                        insideZ
                );

        if (importedLight <= existingLight) {
            return;
        }

        world.setSkyLight(
                insideX,
                insideY,
                insideZ,
                importedLight
        );

        Chunk changedChunk =
                world.getChunkAtWorldBlock(
                        insideX,
                        insideY,
                        insideZ
                );

        if (changedChunk != null) {
            changedChunks.add(
                    changedChunk
            );
        }

        lightQueue.addLast(
                new LightNode(
                        insideX,
                        insideY,
                        insideZ
                )
        );
    }

    public Set<Chunk> generateBlockLight(
            World world,
            Chunk sourceChunk
    ) {
        int chunkX =
                sourceChunk.getChunkX();

        int chunkZ =
                sourceChunk.getChunkZ();

        return relightBlockRegion(
                world,
                chunkX - RELIGHT_CHUNK_RADIUS,
                chunkX + RELIGHT_CHUNK_RADIUS,
                chunkZ - RELIGHT_CHUNK_RADIUS,
                chunkZ + RELIGHT_CHUNK_RADIUS
        );
    }

    public Set<Chunk> initializeBlockLight(
            World world,
            Chunk sourceChunk
    ) {
        Set<Chunk> changedChunks =
                new HashSet<>();

        ArrayDeque<LightNode> lightQueue =
                new ArrayDeque<>();

        int originX =
                sourceChunk.getWorldOriginX();

        int originY =
                sourceChunk.getWorldOriginY();

        int originZ =
                sourceChunk.getWorldOriginZ();

        /*
         * Find light-emitting blocks only inside the newly
         * loaded chunk. Do not clear existing neighboring light.
         */
        for (
                int localX = 0;
                localX < Chunk.SIZE;
                localX++
        ) {
            for (
                    int localY = 0;
                    localY < Chunk.SIZE;
                    localY++
            ) {
                for (
                        int localZ = 0;
                        localZ < Chunk.SIZE;
                        localZ++
                ) {
                    BlockType block =
                            sourceChunk.getBlock(
                                    localX,
                                    localY,
                                    localZ
                            );

                    if (block == null) {
                        continue;
                    }

                    int emittedLight =
                            block.getEmittedLight();

                    if (emittedLight <= 0) {
                        continue;
                    }

                    int worldX =
                            originX + localX;

                    int worldY =
                            originY + localY;

                    int worldZ =
                            originZ + localZ;

                    world.setBlockLight(
                            worldX,
                            worldY,
                            worldZ,
                            emittedLight
                    );

                    lightQueue.addLast(
                            new LightNode(
                                    worldX,
                                    worldY,
                                    worldZ
                            )
                    );

                    changedChunks.add(
                            sourceChunk
                    );
                }
            }
        }

        /*
         * Spread sources from this chunk through nearby loaded
         * chunks without erasing their existing light first.
         */
        propagateRegionBlockLight(
                world,
                sourceChunk.getChunkX() -
                        RELIGHT_CHUNK_RADIUS,
                sourceChunk.getChunkX() +
                        RELIGHT_CHUNK_RADIUS,
                sourceChunk.getChunkZ() -
                        RELIGHT_CHUNK_RADIUS,
                sourceChunk.getChunkZ() +
                        RELIGHT_CHUNK_RADIUS,
                lightQueue,
                changedChunks
        );

        return changedChunks;
    }


}