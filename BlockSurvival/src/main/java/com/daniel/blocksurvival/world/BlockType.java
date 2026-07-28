package com.daniel.blocksurvival.world;

public enum BlockType {
    GRASS(0.0f, 0.5f),
    DIRT(0.5f, 0.5f),
    STONE(0.0f, 0.0f),
    SAND(0.5f, 0.0f);

    private final float atlasX;
    private final float atlasY;

    BlockType(float atlasX, float atlasY) {
        this.atlasX = atlasX;
        this.atlasY = atlasY;
    }

    public float getAtlasX() {
        return atlasX;
    }

    public float getAtlasY() {
        return atlasY;
    }
}