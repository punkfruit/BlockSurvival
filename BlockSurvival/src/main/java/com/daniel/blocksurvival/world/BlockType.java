package com.daniel.blocksurvival.world;

public enum BlockType {
    GRASS(0, 0, BlockModel.CUBE, true),
    DIRT(1, 0, BlockModel.CUBE, true),
    STONE(2, 0, BlockModel.CUBE, true),
    SAND(3, 0, BlockModel.CUBE, true),
    WOOD(0, 1, BlockModel.CUBE, true),
    LEAVES(1, 1, BlockModel.CUBE, true),
    SNOW(0, 0, BlockModel.CUBE, true),
    CACTUS(0, 0, BlockModel.CUBE, true),

    FLOWER(2, 1, BlockModel.CROSS, false);

    private final float atlasX;
    private final float atlasY;
    private final BlockModel model;
    private final boolean opaque;

    private static final int TILES_PER_ROW = 32;

    private static final float TILE_SIZE =
            1.0f / TILES_PER_ROW;

    BlockType(
            int atlasColumn,
            int atlasRow,
            BlockModel model,
            boolean opaque
    ) {
        this.atlasX =
                atlasColumn * TILE_SIZE;

        this.atlasY =
                atlasRow * TILE_SIZE;

        this.model = model;
        this.opaque = opaque;
    }

    public BlockModel getModel() {
        return model;
    }

    public boolean isOpaque() {
        return opaque;
    }

    public float getAtlasX() {
        return atlasX;
    }

    public float getAtlasY() {
        return atlasY;
    }

    public static float getTileSize() {
        return TILE_SIZE;
    }
}