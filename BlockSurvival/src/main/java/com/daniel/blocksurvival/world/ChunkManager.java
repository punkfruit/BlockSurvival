package com.daniel.blocksurvival.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChunkManager {

    private final Map<ChunkPosition, Chunk> chunks =
            new HashMap<>();

    public synchronized void addChunk(Chunk chunk) {
        ChunkPosition position =
                createChunkPosition(
                        chunk.getChunkX(),
                        chunk.getChunkY(),
                        chunk.getChunkZ()
                );

        chunks.put(
                position,
                chunk
        );

    }

    public synchronized Chunk getChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        ChunkPosition position =
                createChunkPosition(
                        chunkX,
                        chunkY,
                        chunkZ
                );

        return chunks.get(
                position
        );
    }

    public synchronized boolean hasChunk(
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

    public synchronized void removeChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        chunks.remove(
                createChunkPosition(
                        chunkX,
                        chunkY,
                        chunkZ
                )
        );
    }

    public synchronized Iterable<Chunk> getChunks() {
        return new ArrayList<>(
                chunks.values()
        );
    }

    public synchronized int getChunkCount() {
        return chunks.size();
    }

    private ChunkPosition createChunkPosition(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        return new ChunkPosition(
                chunkX,
                chunkY,
                chunkZ
        );
    }

    public synchronized void setBlock(
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

    public synchronized void setGeneratedBlock(
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

    public synchronized BlockType getBlock(
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

    public synchronized boolean hasBlock(
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

    public synchronized Chunk getOrCreateChunk(
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

    public synchronized BlockDirection getBlockDirection(
            int worldX,
            int worldY,
            int worldZ
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

        Chunk chunk =
                getChunk(
                        chunkX,
                        chunkY,
                        chunkZ
                );

        if (chunk == null) {
            return BlockDirection.UP;
        }

        return chunk.getBlockDirection(
                Math.floorMod(
                        worldX,
                        Chunk.SIZE
                ),
                Math.floorMod(
                        worldY,
                        Chunk.SIZE
                ),
                Math.floorMod(
                        worldZ,
                        Chunk.SIZE
                )
        );
    }

    public synchronized void setBlockDirection(
            int worldX,
            int worldY,
            int worldZ,
            BlockDirection direction
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

        Chunk chunk =
                getChunk(
                        chunkX,
                        chunkY,
                        chunkZ
                );

        if (chunk == null) {
            return;
        }

        chunk.setBlockDirection(
                Math.floorMod(
                        worldX,
                        Chunk.SIZE
                ),
                Math.floorMod(
                        worldY,
                        Chunk.SIZE
                ),
                Math.floorMod(
                        worldZ,
                        Chunk.SIZE
                ),
                direction
        );
    }
}