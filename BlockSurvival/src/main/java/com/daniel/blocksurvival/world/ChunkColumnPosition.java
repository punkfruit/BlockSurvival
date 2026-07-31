package com.daniel.blocksurvival.world;

/*
 * Identifies one vertical column of chunks.
 *
 * All chunks with the same X and Z share this column,
 * regardless of their Y coordinate.
 */
public record ChunkColumnPosition(
        int chunkX,
        int chunkZ
) {
}