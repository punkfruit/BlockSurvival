package com.daniel.blocksurvival.world;

public class Chunk {

    /*
     * A chunk contains 16 × 16 × 16 block cells.
     */
    public static final int SIZE = 16;

    private final BlockType[][][] blocks;
    /*
     * Light levels are stored separately from blocks.
     *
     * Both arrays store values from 0–15.
     *
     * Sky light comes from the sun.
     * Block light comes from torches, lava, etc.
     */
    private final byte[] skyLight;

    private final byte[] blockLight;

    /*
     * These are chunk coordinates, not block coordinates.
     *
     * Chunk (0, 0, 0) contains world blocks 0–15.
     * Chunk (1, 0, 0) contains world blocks 16–31.
     */
    private final int chunkX;
    private final int chunkY;
    private final int chunkZ;

    private boolean dirty = false;
    private volatile ChunkState state =
            ChunkState.UNGENERATED;

    public Chunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        blocks = new BlockType[SIZE][SIZE][SIZE];
        skyLight =
                new byte[
                        SIZE * SIZE * SIZE
                        ];

        blockLight =
                new byte[
                        SIZE * SIZE * SIZE
                        ];
    }

    public synchronized void setBlock(
            int localX,
            int localY,
            int localZ,
            BlockType type
    ) {
        setBlockInternal(
                localX,
                localY,
                localZ,
                type,
                true
        );
    }

    public synchronized void setGeneratedBlock(
            int localX,
            int localY,
            int localZ,
            BlockType type
    ) {
        setBlockInternal(
                localX,
                localY,
                localZ,
                type,
                false
        );
    }

    private void setBlockInternal(
            int localX,
            int localY,
            int localZ,
            BlockType type,
            boolean markDirty
    ) {
        if (!isInsideChunk(localX, localY, localZ)) {
            return;
        }

        if (blocks[localX][localY][localZ] == type) {
            return;
        }

        blocks[localX][localY][localZ] = type;

        if (markDirty) {
            dirty = true;
        }
    }

    public synchronized BlockType getBlock(
            int localX,
            int localY,
            int localZ
    ) {
        if (!isInsideChunk(localX, localY, localZ)) {
            return null;
        }

        return blocks[localX][localY][localZ];
    }

    public boolean hasBlock(
            int localX,
            int localY,
            int localZ
    ) {
        return getBlock(localX, localY, localZ) != null;
    }

    public boolean isInsideChunk(
            int localX,
            int localY,
            int localZ
    ) {
        return localX >= 0 &&
                localX < SIZE &&
                localY >= 0 &&
                localY < SIZE &&
                localZ >= 0 &&
                localZ < SIZE;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkY() {
        return chunkY;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getWorldOriginX() {
        return chunkX * SIZE;
    }

    public int getWorldOriginY() {
        return chunkY * SIZE;
    }

    public int getWorldOriginZ() {
        return chunkZ * SIZE;
    }

    public boolean isEmpty() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    if (blocks[x][y][z] != null) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }

    public ChunkState getState() {
        return state;
    }

    public void setState(
            ChunkState state
    ) {
        this.state = state;
    }

    /*
     * Converts a local block coordinate into the
     * corresponding position inside the light arrays.
     */
    private int lightIndex(
            int localX,
            int localY,
            int localZ
    ) {
        return
                localX +
                        localY * SIZE +
                        localZ * SIZE * SIZE;
    }

    public synchronized int getSkyLight(
            int localX,
            int localY,
            int localZ
    ) {
        if (!isInsideChunk(
                localX,
                localY,
                localZ
        )) {
            return 0;
        }

        return Byte.toUnsignedInt(
                skyLight[
                        lightIndex(
                                localX,
                                localY,
                                localZ
                        )
                        ]
        );
    }

    public synchronized void setSkyLight(
            int localX,
            int localY,
            int localZ,
            int lightLevel
    ) {
        if (!isInsideChunk(
                localX,
                localY,
                localZ
        )) {
            return;
        }

        lightLevel =
                Math.max(
                        0,
                        Math.min(
                                15,
                                lightLevel
                        )
                );

        skyLight[
                lightIndex(
                        localX,
                        localY,
                        localZ
                )
                ] =
                (byte) lightLevel;
    }

    public synchronized int getBlockLight(
            int localX,
            int localY,
            int localZ
    ) {
        if (!isInsideChunk(
                localX,
                localY,
                localZ
        )) {
            return 0;
        }

        return Byte.toUnsignedInt(
                blockLight[
                        lightIndex(
                                localX,
                                localY,
                                localZ
                        )
                        ]
        );
    }

    public synchronized void setBlockLight(
            int localX,
            int localY,
            int localZ,
            int lightLevel
    ) {
        if (!isInsideChunk(
                localX,
                localY,
                localZ
        )) {
            return;
        }

        lightLevel =
                Math.max(
                        0,
                        Math.min(
                                15,
                                lightLevel
                        )
                );

        blockLight[
                lightIndex(
                        localX,
                        localY,
                        localZ
                )
                ] =
                (byte) lightLevel;
    }

    public boolean hasTerrain() {
        return switch (state) {
            case GENERATED,
                 MESHING,
                 READY -> true;

            default -> false;
        };
    }
}