package com.daniel.blocksurvival.graphics;

/*
 * Identifies special rendering behavior for vertices.
 *
 * These numeric IDs are sent to the GPU as part of
 * each vertex and interpreted by the shaders.
 */
public enum RenderMaterial {

    DEFAULT(0.0f),
    WATER(1.0f),
    FOLIAGE(2.0f),
    LEAVES(3.0f);

    private final float id;

    RenderMaterial(float id) {
        this.id = id;
    }

    public float getId() {
        return id;
    }
}