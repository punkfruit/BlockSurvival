package com.daniel.blocksurvival.world;

/*
 * Represents where a chunk currently is in its lifecycle.
 *
 * Right now several of these states behave identically.
 * Later, background generation and meshing will transition
 * through them automatically.
 */
public enum ChunkState {

    /*
     * Chunk object exists but contains no generated terrain.
     */
    UNGENERATED,

    /*
     * Waiting for a worker thread to begin generation.
     */
    QUEUED,

    /*
     * Terrain generation is currently running.
     */
    GENERATING,

    /*
     * Blocks have been generated.
     *
     * No render mesh exists yet.
     */
    GENERATED,

    /*
     * CPU mesh generation is running.
     */
    MESHING,

    /*
     * Mesh data has been uploaded to the GPU.
     *
     * Ready to render.
     */
    READY
}