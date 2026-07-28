package com.daniel.blocksurvival.world;

public class Chunk {

    /*
     * A chunk contains 16 × 16 × 16 block cells.
     */
    public static final int SIZE = 16;

    private final BlockType[][][] blocks;

    /*
     * These are chunk coordinates, not block coordinates.
     *
     * Chunk (0, 0, 0) contains world blocks 0–15.
     * Chunk (1, 0, 0) contains world blocks 16–31.
     */
    private final int chunkX;
    private final int chunkY;
    private final int chunkZ;

    public Chunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        blocks = new BlockType[SIZE][SIZE][SIZE];
    }

    public void setBlock(
            int localX,
            int localY,
            int localZ,
            BlockType type
    ) {
        if (!isInsideChunk(localX, localY, localZ)) {
            return;
        }

        blocks[localX][localY][localZ] = type;
    }

    public BlockType getBlock(
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
}