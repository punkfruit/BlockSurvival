package com.daniel.blocksurvival.world;

public enum BlockType {
    GRASS(new AtlasTile(0, 0), new AtlasTile(0,1),new AtlasTile(1,0), BlockModel.CUBE, true, 0.5f, 0),
    DIRT(new AtlasTile(1, 0), BlockModel.CUBE, true),
    STONE(new AtlasTile(2, 0), BlockModel.CUBE, true),
    SAND(new AtlasTile(3, 0), BlockModel.CUBE, true),
    WOOD(new AtlasTile(4, 1), new AtlasTile(4,0),new AtlasTile(4,1), BlockModel.CUBE, true, 0.5f, 0),
    LEAVES(new AtlasTile(1, 1), BlockModel.CUBE, false),
    SNOW(new AtlasTile(3, 1), new AtlasTile(3,1),new AtlasTile(3,1), BlockModel.CUBE, true, 0.5f, 0),
    CACTUS(new AtlasTile(0, 0), new AtlasTile(0,0),new AtlasTile(0,0), BlockModel.CUBE, true, 0.5f, 0),
    FLOWER(new AtlasTile(2, 1), BlockModel.CROSS, false),
    GLOWSTONE(new AtlasTile(4, 2), new AtlasTile(4,2),new AtlasTile(4,2), BlockModel.CUBE, true, 0.5f, 15),
    TORCH(new AtlasTile(5, 2), new AtlasTile(5, 2), new AtlasTile(5, 2), BlockModel.TORCH, false, 0.5f, 14),
    WATER(new AtlasTile(5, 0), BlockModel.CUBE, false, 0.4f),

    /*
     * IMPORTANT:
     * New block types currently need to be appended to the end
     * because chunk saves use BlockType.ordinal().
     */
    IRON_ORE(
            new AtlasTile(
                    5,
                    1
            ),
            BlockModel.CUBE,
            true
    ),

    COAL_ORE(
            new AtlasTile(
                    6,
                    1
            ),
            BlockModel.CUBE,
            true
    );

    private final AtlasTile topTexture;
    private final AtlasTile sideTexture;
    private final AtlasTile bottomTexture;
    private BlockModel model;
    private final boolean opaque;
    private final float topOffset;
    private final int emittedLight;
    private static final float FULL_BLOCK_TOP =
            0.5f;

    private static final int TILES_PER_ROW = 32;

    private static final float TILE_SIZE =
            1.0f / TILES_PER_ROW;

    BlockType(
            AtlasTile topTexture,
            AtlasTile sideTexture,
            AtlasTile bottomTexture,
            BlockModel model,
            boolean opaque,
            float topOffset,
            int emittedLight
    ) {
        this.topTexture = topTexture;
        this.sideTexture = sideTexture;
        this.bottomTexture = bottomTexture;
        this.model = model;
        this.opaque = opaque;
        this.topOffset = topOffset;
        this.emittedLight = emittedLight;
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
                opaque,
                FULL_BLOCK_TOP,
                0
        );
    }

    BlockType(
            AtlasTile texture,
            BlockModel model,
            boolean opaque,
            float topOffset
    ) {
        this(
                texture,
                texture,
                texture,
                model,
                opaque,
                topOffset,
                0
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
    public float getTopOffset() {
        return topOffset;
    }

    public int getEmittedLight() {
        return emittedLight;
    }
}