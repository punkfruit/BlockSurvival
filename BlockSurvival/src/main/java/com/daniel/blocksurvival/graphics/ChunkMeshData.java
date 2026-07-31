package com.daniel.blocksurvival.graphics;

/*
 * CPU-side geometry for one chunk.
 *
 * No VAOs, VBOs, or other OpenGL resources live here.
 */
public record ChunkMeshData(
        MeshData opaqueMeshData,
        MeshData transparentMeshData
) {
}