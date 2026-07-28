package com.daniel.blocksurvival.world;

public class World {

    private final ChunkManager chunkManager =
            new ChunkManager();

    public void setBlock(
            int worldX,
            int worldY,
            int worldZ,
            BlockType type
    ) {
        chunkManager.setBlock(
                worldX,
                worldY,
                worldZ,
                type
        );
    }

    public BlockType getBlock(
            int worldX,
            int worldY,
            int worldZ
    ) {
        return chunkManager.getBlock(
                worldX,
                worldY,
                worldZ
        );
    }

    public boolean hasBlock(
            int worldX,
            int worldY,
            int worldZ
    ) {
        return chunkManager.hasBlock(
                worldX,
                worldY,
                worldZ
        );
    }

    public Iterable<Chunk> getChunks() {
        return chunkManager.getChunks();
    }

    public int getChunkCount() {
        return chunkManager.getChunkCount();
    }

    public Chunk getChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        return chunkManager.getChunk(
                chunkX,
                chunkY,
                chunkZ
        );
    }

    public Chunk getChunkAtWorldBlock(
            int worldX,
            int worldY,
            int worldZ
    ) {
        int chunkX =
                Math.floorDiv(worldX, Chunk.SIZE);

        int chunkY =
                Math.floorDiv(worldY, Chunk.SIZE);

        int chunkZ =
                Math.floorDiv(worldZ, Chunk.SIZE);

        return chunkManager.getChunk(
                chunkX,
                chunkY,
                chunkZ
        );
    }

    public Chunk getOrCreateChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        return chunkManager.getOrCreateChunk(
                chunkX,
                chunkY,
                chunkZ
        );
    }

    public void removeChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        chunkManager.removeChunk(
                chunkX,
                chunkY,
                chunkZ
        );
    }
}