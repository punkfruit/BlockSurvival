package com.daniel.blocksurvival.world;

import java.util.HashMap;
import java.util.Map;

public class ChunkManager {

    private final Map<String, Chunk> chunks =
            new HashMap<>();

    public void addChunk(Chunk chunk) {
        String key = createChunkKey(
                chunk.getChunkX(),
                chunk.getChunkY(),
                chunk.getChunkZ()
        );

        chunks.put(key, chunk);
    }

    public Chunk getChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        String key = createChunkKey(
                chunkX,
                chunkY,
                chunkZ
        );

        return chunks.get(key);
    }

    public boolean hasChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        return getChunk(
                chunkX,
                chunkY,
                chunkZ
        ) != null;
    }

    public void removeChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        chunks.remove(
                createChunkKey(
                        chunkX,
                        chunkY,
                        chunkZ
                )
        );
    }

    public Iterable<Chunk> getChunks() {
        return chunks.values();
    }

    public int getChunkCount() {
        return chunks.size();
    }

    private String createChunkKey(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        return chunkX + "," +
                chunkY + "," +
                chunkZ;
    }

    public void setBlock(
            int worldX,
            int worldY,
            int worldZ,
            BlockType type
    ) {
        setBlockInternal(
                worldX,
                worldY,
                worldZ,
                type,
                true
        );
    }

    public void setGeneratedBlock(
            int worldX,
            int worldY,
            int worldZ,
            BlockType type
    ) {
        setBlockInternal(
                worldX,
                worldY,
                worldZ,
                type,
                false
        );
    }

    private void setBlockInternal(
            int worldX,
            int worldY,
            int worldZ,
            BlockType type,
            boolean markDirty
    ) {
        int chunkX =
                Math.floorDiv(
                        worldX,
                        Chunk.SIZE
                );

        int chunkY =
                Math.floorDiv(
                        worldY,
                        Chunk.SIZE
                );

        int chunkZ =
                Math.floorDiv(
                        worldZ,
                        Chunk.SIZE
                );

        int localX =
                Math.floorMod(
                        worldX,
                        Chunk.SIZE
                );

        int localY =
                Math.floorMod(
                        worldY,
                        Chunk.SIZE
                );

        int localZ =
                Math.floorMod(
                        worldZ,
                        Chunk.SIZE
                );

        Chunk chunk =
                getChunk(
                        chunkX,
                        chunkY,
                        chunkZ
                );

        if (chunk == null) {
            chunk =
                    new Chunk(
                            chunkX,
                            chunkY,
                            chunkZ
                    );

            addChunk(
                    chunk
            );
        }

        if (markDirty) {
            chunk.setBlock(
                    localX,
                    localY,
                    localZ,
                    type
            );
        }
        else {
            chunk.setGeneratedBlock(
                    localX,
                    localY,
                    localZ,
                    type
            );
        }
    }

    public BlockType getBlock(
            int worldX,
            int worldY,
            int worldZ
    ) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE);
        int chunkY = Math.floorDiv(worldY, Chunk.SIZE);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE);

        int localX = Math.floorMod(worldX, Chunk.SIZE);
        int localY = Math.floorMod(worldY, Chunk.SIZE);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE);

        Chunk chunk = getChunk(
                chunkX,
                chunkY,
                chunkZ
        );

        if (chunk == null) {
            return null;
        }

        return chunk.getBlock(
                localX,
                localY,
                localZ
        );
    }

    public boolean hasBlock(
            int worldX,
            int worldY,
            int worldZ
    ) {
        return getBlock(
                worldX,
                worldY,
                worldZ
        ) != null;
    }

    public Chunk getOrCreateChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        Chunk chunk =
                getChunk(
                        chunkX,
                        chunkY,
                        chunkZ
                );

        if (chunk == null) {
            chunk = new Chunk(
                    chunkX,
                    chunkY,
                    chunkZ
            );

            addChunk(chunk);
        }

        return chunk;
    }
}