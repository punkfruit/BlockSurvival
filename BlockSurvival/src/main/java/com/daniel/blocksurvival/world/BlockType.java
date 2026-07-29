package com.daniel.blocksurvival.world;

public enum BlockType {
    GRASS(new AtlasTile(0, 0), new AtlasTile(0,1),new AtlasTile(1,0), BlockModel.CUBE, true),
    DIRT(new AtlasTile(1, 0), BlockModel.CUBE, true),
    STONE(new AtlasTile(2, 0), BlockModel.CUBE, true),
    SAND(new AtlasTile(3, 0), BlockModel.CUBE, true),
    WOOD(new AtlasTile(4, 1), new AtlasTile(4,0),new AtlasTile(4,1), BlockModel.CUBE, true),
    LEAVES(new AtlasTile(1, 1), BlockModel.CUBE, true),
    SNOW(new AtlasTile(3, 1), new AtlasTile(3,1),new AtlasTile(3,1), BlockModel.CUBE, true),
    CACTUS(new AtlasTile(0, 0), new AtlasTile(0,0),new AtlasTile(0,0), BlockModel.CUBE, true),
    FLOWER(new AtlasTile(2, 1), BlockModel.CROSS, false);

    private final AtlasTile topTexture;
    private final AtlasTile sideTexture;
    private final AtlasTile bottomTexture;
    private BlockModel model;
    private final boolean opaque;

    private static final int TILES_PER_ROW = 32;

    private static final float TILE_SIZE =
            1.0f / TILES_PER_ROW;

    BlockType(
            AtlasTile topTexture,
            AtlasTile sideTexture,
            AtlasTile bottomTexture,
            BlockModel model,
            boolean opaque
    ) {
        this.topTexture = topTexture;
        this.sideTexture = sideTexture;
        this.bottomTexture = bottomTexture;

        this.model = model;
        this.opaque = opaque;
    }

    BlockType(
            AtlasTile texture,
            BlockModel model,
            boolean opaque

    ) {
        this(
                texture,
                texture,
                texture,
                model,
                opaque
        );
    }



    public BlockModel getModel() {
        return model;
    }

    public boolean isOpaque() {
        return opaque;
    }

    public AtlasTile getTopTexture() {
        return topTexture;
    }

    public AtlasTile getSideTexture() {
        return sideTexture;
    }

    public AtlasTile getBottomTexture() {
        return bottomTexture;
    }

    public static float getTileSize() {
        return TILE_SIZE;
    }

    public AtlasTile getTextureForFace(
            BlockFace face
    ) {
        return switch (face) {
            case TOP -> topTexture;
            case BOTTOM -> bottomTexture;

            case NORTH,
                 SOUTH,
                 EAST,
                 WEST -> sideTexture;
        };
    }
}