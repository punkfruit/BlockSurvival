package com.daniel.blocksurvival.world;

import com.daniel.blocksurvival.graphics.ChunkMeshData;

/*
 * A chunk whose blocks and CPU-side mesh data
 * have finished processing on the worker thread.
 */
public record CompletedChunk(
        Chunk chunk,
        ChunkMeshData meshData
) {
}