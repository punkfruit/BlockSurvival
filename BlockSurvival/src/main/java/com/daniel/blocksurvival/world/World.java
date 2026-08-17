package com.daniel.blocksurvival.world;

import com.daniel.blocksurvival.machine.Machine;
import com.daniel.blocksurvival.machine.MachineManager;

public class World {


    private final ChunkManager chunkManager =
            new ChunkManager();

    private final MachineManager machineManager =
            new MachineManager();

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

    public void setGeneratedBlock(
            int worldX,
            int worldY,
            int worldZ,
            BlockType type
    ) {
        chunkManager.setGeneratedBlock(
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

    public int getSkyLight(
            int worldX,
            int worldY,
            int worldZ
    ) {
        Chunk chunk =
                getChunkAtWorldBlock(
                        worldX,
                        worldY,
                        worldZ
                );

        if (chunk == null) {
            return 0;
        }

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

        return chunk.getSkyLight(
                localX,
                localY,
                localZ
        );
    }

    public int getBlockLight(
            int worldX,
            int worldY,
            int worldZ
    ) {
        Chunk chunk =
                getChunkAtWorldBlock(
                        worldX,
                        worldY,
                        worldZ
                );

        if (chunk == null) {
            return 0;
        }

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

        return chunk.getBlockLight(
                localX,
                localY,
                localZ
        );
    }

    public void setSkyLight(
            int worldX,
            int worldY,
            int worldZ,
            int lightLevel
    ) {
        Chunk chunk =
                getChunkAtWorldBlock(
                        worldX,
                        worldY,
                        worldZ
                );

        /*
         * Lighting should not create missing chunks.
         *
         * It may spread only through terrain that has actually
         * been loaded or generated.
         */
        if (chunk == null) {
            return;
        }

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

        chunk.setSkyLight(
                localX,
                localY,
                localZ,
                lightLevel
        );
    }

    public boolean allowsSkyLight(
            int worldX,
            int worldY,
            int worldZ
    ) {
        Chunk chunk =
                getChunkAtWorldBlock(
                        worldX,
                        worldY,
                        worldZ
                );

        /*
         * Missing chunks are not traversable yet.
         *
         * When they eventually load, we will relight the nearby
         * border so illumination can continue into them.
         */
        if (
                chunk == null ||
                        !chunk.hasTerrain()
        ) {
            return false;
        }

        BlockType block =
                getBlock(
                        worldX,
                        worldY,
                        worldZ
                );

        return block == null ||
                !block.isOpaque();
    }


    public void setBlockLight(
            int worldX,
            int worldY,
            int worldZ,
            int lightLevel
    ) {
        Chunk chunk =
                getChunkAtWorldBlock(
                        worldX,
                        worldY,
                        worldZ
                );

        if (chunk == null) {
            return;
        }

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

        chunk.setBlockLight(
                localX,
                localY,
                localZ,
                lightLevel
        );
    }
    public boolean allowsBlockLight(
            int worldX,
            int worldY,
            int worldZ
    ) {
        Chunk chunk =
                getChunkAtWorldBlock(
                        worldX,
                        worldY,
                        worldZ
                );

        if (
                chunk == null ||
                        !chunk.hasTerrain()
        ) {
            return false;
        }

        BlockType block =
                getBlock(
                        worldX,
                        worldY,
                        worldZ
                );

        return block == null ||
                !block.isOpaque();
    }

    public Machine getMachineAt(
            int worldX,
            int worldY,
            int worldZ
    ) {
        return machineManager.getMachineAt(
                worldX,
                worldY,
                worldZ
        );
    }

    public boolean registerMachine(
            Machine machine
    ) {
        return machineManager.register(
                machine
        );
    }

    public void removeMachine(
            Machine machine
    ) {
        machineManager.remove(
                machine
        );
    }

    public Iterable<Machine> getMachines() {
        return machineManager.getMachines();
    }

    public void loadMachines(
            Iterable<Machine> machines
    ) {
        for (
                Machine machine :
                machines
        ) {
            boolean registered =
                    machineManager.register(
                            machine
                    );

            if (!registered) {
                System.err.println(
                        "Could not restore machine at " +
                                machine.getAnchor()
                );
            }
        }
    }

    public BlockDirection getBlockDirection(
            int worldX,
            int worldY,
            int worldZ
    ) {
        return chunkManager.getBlockDirection(
                worldX,
                worldY,
                worldZ
        );
    }

    public void setBlockDirection(
            int worldX,
            int worldY,
            int worldZ,
            BlockDirection direction
    ) {
        chunkManager.setBlockDirection(
                worldX,
                worldY,
                worldZ,
                direction
        );
    }


}