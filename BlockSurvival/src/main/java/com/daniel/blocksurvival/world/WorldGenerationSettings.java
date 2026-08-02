package com.daniel.blocksurvival.world;

public final class WorldGenerationSettings {

    /*
     * Chunk -4 contains world Y positions -64 through -49.
     */
    public static final int MIN_CHUNK_Y = -4;

    /*
     * Chunk 1 contains world Y positions 16 through 31.
     *
     * The terrain itself currently stays below this, but trees
     * can extend above chunk Y = 0.
     */
    public static final int MAX_CHUNK_Y = 1;

    public static final int MIN_WORLD_Y =
            MIN_CHUNK_Y * Chunk.SIZE;

    public static final int MAX_WORLD_Y =
            (
                    MAX_CHUNK_Y + 1
            ) * Chunk.SIZE - 1;

    private WorldGenerationSettings() {
    }
}