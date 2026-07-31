package com.daniel.blocksurvival.graphics;

/*
 * Plain CPU-side mesh data.
 *
 * This class contains no OpenGL objects or calls,
 * so it can safely be created on a background thread.
 */
public record MeshData(
        float[] vertices,
        int[] indices
) {
    public boolean isEmpty() {
        return indices == null ||
                indices.length == 0;
    }
}