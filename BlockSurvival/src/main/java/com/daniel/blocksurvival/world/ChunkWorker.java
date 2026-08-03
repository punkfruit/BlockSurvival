package com.daniel.blocksurvival.world;

import com.daniel.blocksurvival.graphics.ChunkMeshBuilder;
import com.daniel.blocksurvival.graphics.ChunkMeshData;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ChunkWorker {

    private static final int GENERATION_WORKER_COUNT = 2;

    /*
     * Chunks waiting for terrain generation or loading.
     */
    private final PriorityBlockingQueue<ChunkRequest>
            pendingChunks =
            new PriorityBlockingQueue<>();

    /*
     * Prevent the same coordinate from being queued repeatedly.
     */
    private final Set<ChunkPosition> queuedPositions =
            ConcurrentHashMap.newKeySet();

    /*
     * Prevent two generation workers from modifying different
     * vertical chunks in the same horizontal column at once.
     *
     * This matters because surface structures can extend upward
     * from chunk Y = 0 into chunk Y = 1.
     */
    private final ConcurrentHashMap<
            ChunkColumnPosition,
            Object
            > columnLocks =
            new ConcurrentHashMap<>();

    /*
     * Gives equal-priority requests a stable order.
     */
    private final AtomicLong requestSequence =
            new AtomicLong();

    private volatile int focusChunkX;
    private volatile int focusChunkZ;
    private volatile int activeRenderDistance;

    /*
     * Chunks whose block data exists but still need
     * CPU-side mesh generation.
     */
    private final BlockingQueue<Chunk> urgentMeshChunks =
            new LinkedBlockingQueue<>();

    private final BlockingQueue<Chunk> generatedChunks =
            new LinkedBlockingQueue<>();

    /*
     * Prevent the same chunk from appearing in the mesh queue
     * more than once simultaneously.
     */

    /*
     * Records chunks whose lighting changed.
     *
     * If lighting changes while a chunk is already meshing,
     * this flag survives until that mesh has been uploaded,
     * after which another remesh can be scheduled.
     */
    private final Set<ChunkPosition> remeshRequested =
            ConcurrentHashMap.newKeySet();

    /*
     * Finished CPU mesh data waiting for the main thread
     * to upload it to OpenGL.
     */
    private final ConcurrentLinkedQueue<CompletedChunk>
            completedChunks =
            new ConcurrentLinkedQueue<>();

    private final World world;
    private final TerrainGenerator terrainGenerator;

    private final LightEngine lightEngine;
    private final SaveManager saveManager;

    /*
     * Used only by the mesh thread.
     *
     * ChunkMeshBuilder contains temporary mutable lists,
     * so it must not be shared between multiple mesh threads.
     */
    private final ChunkMeshBuilder meshBuilder =
            new ChunkMeshBuilder();

    private final Thread[] generationThreads;

    private final Thread meshThread;

    private volatile boolean running = true;

    private final Set<ChunkPosition> queuedMeshes =
            ConcurrentHashMap.newKeySet();

    public ChunkWorker(
            World world,
            TerrainGenerator terrainGenerator,
            LightEngine lightEngine,
            SaveManager saveManager
    ) {
        this.world = world;
        this.terrainGenerator = terrainGenerator;
        this.lightEngine = lightEngine;
        this.saveManager = saveManager;

        generationThreads =
                new Thread[
                        GENERATION_WORKER_COUNT
                        ];

        for (
                int workerIndex = 0;
                workerIndex <
                        GENERATION_WORKER_COUNT;
                workerIndex++
        ) {
            Thread generationThread =
                    new Thread(
                            this::runGenerationWorker,
                            "Chunk Generation Worker "
                                    + (workerIndex + 1)
                    );

            generationThread.setDaemon(
                    true
            );

            generationThreads[
                    workerIndex
                    ] = generationThread;
        }

        meshThread =
                new Thread(
                        this::runMeshWorker,
                        "Chunk Mesh Worker"
                );

        meshThread.setDaemon(
                true
        );

        for (
                Thread generationThread :
                generationThreads
        ) {
            generationThread.start();
        }

        meshThread.start();
    }

    public void queueChunk(
            int chunkX,
            int chunkY,
            int chunkZ,
            int priority
    ) {
        /*
         * If the chunk already exists and has terrain,
         * no generation request is needed.
         */
        Chunk existingChunk =
                world.getChunk(
                        chunkX,
                        chunkY,
                        chunkZ
                );

        if (
                existingChunk != null &&
                        existingChunk.getState() !=
                                ChunkState.UNGENERATED
        ) {
            return;
        }

        ChunkPosition position =
                new ChunkPosition(
                        chunkX,
                        chunkY,
                        chunkZ
                );

        /*
         * add() returns false if this coordinate was
         * already waiting in the queue.
         */
        if (!queuedPositions.add(position)) {
            return;
        }

        pendingChunks.offer(
                new ChunkRequest(
                        chunkX,
                        chunkY,
                        chunkZ,
                        priority,
                        requestSequence.getAndIncrement()
                )
        );
    }

    public void updateFocus(
            int playerChunkX,
            int playerChunkZ,
            int renderDistance
    ) {
        focusChunkX =
                playerChunkX;

        focusChunkZ =
                playerChunkZ;

        activeRenderDistance =
                renderDistance;

        /*
         * Old waiting jobs were prioritized around the previous
         * player position.
         *
         * Clear them and allow Main to submit the currently
         * relevant area again in fresh priority order.
         */
        pendingChunks.clear();
        queuedPositions.clear();
    }

    public CompletedChunk pollCompletedChunk() {
        return completedChunks.poll();
    }

    /*
     * First pipeline stage:
     *
     * saved file / terrain generation -> block data
     */
    private void runGenerationWorker() {
        while (running) {
            try {
                ChunkRequest request =
                        pendingChunks.poll(
                                100,
                                TimeUnit.MILLISECONDS
                        );

                if (request == null) {
                    continue;
                }

                ChunkPosition position =
                        new ChunkPosition(
                                request.chunkX(),
                                request.chunkY(),
                                request.chunkZ()
                        );

                /*
                 * This request is no longer waiting.
                 */
                queuedPositions.remove(
                        position
                );

                /*
                 * Skip the request if the player has moved far enough
                 * away that the coordinate is no longer relevant.
                 */
                int distanceX =
                        Math.abs(
                                request.chunkX() -
                                        focusChunkX
                        );

                int distanceZ =
                        Math.abs(
                                request.chunkZ() -
                                        focusChunkZ
                        );

                if (
                        distanceX > activeRenderDistance ||
                                distanceZ > activeRenderDistance
                ) {
                    continue;
                }

                Chunk chunk =
                        world.getOrCreateChunk(
                                request.chunkX(),
                                request.chunkY(),
                                request.chunkZ()
                        );

                synchronized (chunk) {
                    if (
                            chunk.getState() !=
                                    ChunkState.UNGENERATED
                    ) {
                        continue;
                    }

                    chunk.setState(
                            ChunkState.QUEUED
                    );
                }

                generateChunkSafely(
                        chunk
                );
            }
            catch (InterruptedException exception) {
                if (!running) {
                    return;
                }
            }
            catch (Exception exception) {
                System.err.println(
                        "Unexpected generation-worker error."
                );

                exception.printStackTrace();
            }
        }
    }

    private ChunkPosition getChunkPosition(
            Chunk chunk
    ) {
        return new ChunkPosition(
                chunk.getChunkX(),
                chunk.getChunkY(),
                chunk.getChunkZ()
        );
    }

    private void queueMesh(
            Chunk chunk
    ) {
        ChunkPosition position =
                getChunkPosition(
                        chunk
                );

        /*
         * Only one waiting mesh job per coordinate.
         */
        if (!queuedMeshes.add(position)) {
            return;
        }

        generatedChunks.offer(
                chunk
        );
    }

    private void queueUrgentMesh(
            Chunk chunk
    ) {
        ChunkPosition position =
                getChunkPosition(
                        chunk
                );

        if (!queuedMeshes.add(position)) {
            return;
        }

        urgentMeshChunks.offer(
                chunk
        );
    }



    public void onChunkUploaded(
            Chunk chunk
    ) {
        ChunkPosition position =
                getChunkPosition(
                        chunk
                );

        /*
         * remove() both checks and consumes the pending request.
         */
        if (remeshRequested.remove(position)) {
            queueUrgentMesh(
                    chunk
            );
        }


    }

    /*
     * Second pipeline stage:
     *
     * block data -> CPU-side vertices and indices
     */
    private void runMeshWorker() {
        while (running) {
            try {
                Chunk chunk =
                        urgentMeshChunks.poll();

                if (chunk == null) {
                    chunk =
                            generatedChunks.poll(
                                    100,
                                    TimeUnit.MILLISECONDS
                            );
                }

                if (chunk == null) {
                    continue;
                }

                meshChunk(
                        chunk
                );
            }
            catch (InterruptedException exception) {
                if (!running) {
                    return;
                }
            }
            catch (Exception exception) {
                System.err.println(
                        "Unexpected mesh-worker error."
                );

                exception.printStackTrace();
            }
        }
    }

    private void generateChunkSafely(
            Chunk chunk
    ) {
        ChunkColumnPosition columnPosition =
                new ChunkColumnPosition(
                        chunk.getChunkX(),
                        chunk.getChunkZ()
                );

        /*
         * Every vertical chunk in the same X/Z column receives
         * the same lock object.
         */
        Object columnLock =
                columnLocks.computeIfAbsent(
                        columnPosition,
                        ignored ->
                                new Object()
                );

        /*
         * Different columns may generate simultaneously.
         *
         * Chunks within this same column must wait for one another.
         */
        synchronized (columnLock) {
            generateChunk(
                    chunk
            );
        }
    }

    private void generateChunk(
            Chunk chunk
    ) {
        try {
            chunk.setState(
                    ChunkState.GENERATING
            );

            /*
            System.out.println(
                    Thread.currentThread().getName()
                            + " generating chunk "
                            + chunk.getChunkX()
                            + ", "
                            + chunk.getChunkY()
                            + ", "
                            + chunk.getChunkZ()
            );

             */

            boolean loadedFromDisk =
                    saveManager.loadChunk(
                            chunk
                    );

            if (!loadedFromDisk) {
                terrainGenerator.generateChunk(
                        world,
                        chunk
                );
            }

            /*
             * Terrain is complete, so lighting may now traverse this chunk.
             */
            chunk.setState(
                    ChunkState.GENERATED
            );



            Set<Chunk> skyChangedChunks =
                    lightEngine.generateSkyLight(
                            world,
                            chunk
                    );

            Set<Chunk> blockChangedChunks =
                    lightEngine.initializeBlockLight(
                            world,
                            chunk
                    );

            Set<Chunk> lightChangedChunks =
                    new HashSet<>();

            lightChangedChunks.addAll(
                    skyChangedChunks
            );

            lightChangedChunks.addAll(
                    blockChangedChunks
            );



            for (
                    Chunk changedChunk :
                    lightChangedChunks
            ) {
                requestRemesh(
                        changedChunk
                );
            }

            queueMesh(
                    chunk
            );
        }
        catch (Exception exception) {
            /*
             * Generation did not finish, so the chunk may be
             * requested again later.
             */
            chunk.setState(
                    ChunkState.UNGENERATED
            );

            System.err.println(
                    "Failed to generate chunk: "
                            + chunk.getChunkX()
                            + ", "
                            + chunk.getChunkY()
                            + ", "
                            + chunk.getChunkZ()
            );

            exception.printStackTrace();
        }
    }

    private void meshChunk(
            Chunk chunk
    ) {
        ChunkPosition position =
                getChunkPosition(
                        chunk
                );

        queuedMeshes.remove(
                position
        );

        try {
            synchronized (chunk) {
                if (
                        chunk.getState() !=
                                ChunkState.GENERATED &&
                                chunk.getState() !=
                                        ChunkState.READY
                ) {
                    return;
                }



                chunk.setState(
                        ChunkState.MESHING
                );
            }

            ChunkMeshData meshData =
                    meshBuilder.build(
                            world,
                            chunk
                    );

            completedChunks.offer(
                    new CompletedChunk(
                            chunk,
                            meshData
                    )
            );
        }
        catch (Exception exception) {
            chunk.setState(
                    ChunkState.GENERATED
            );

            System.err.println(
                    "Failed to mesh chunk: "
                            + chunk.getChunkX()
                            + ", "
                            + chunk.getChunkY()
                            + ", "
                            + chunk.getChunkZ()
            );

            exception.printStackTrace();
        }
    }

    public void shutdown() {
        running = false;

        for (
                Thread generationThread :
                generationThreads
        ) {
            generationThread.interrupt();
        }

        meshThread.interrupt();

        for (
                Thread generationThread :
                generationThreads
        ) {
            joinThread(
                    generationThread
            );
        }

        joinThread(
                meshThread
        );
    }

    private void joinThread(
            Thread thread
    ) {
        try {
            thread.join(
                    2000
            );
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public void requestRemesh(
            Chunk chunk
    ) {
        if (
                chunk == null ||
                        !chunk.hasTerrain()
        ) {
            return;
        }

        ChunkPosition position =
                getChunkPosition(
                        chunk
                );

        /*
         * Remember that this chunk needs a newer mesh.
         *
         * This flag remains present if the chunk is currently
         * meshing, so the request is not lost.
         */
        remeshRequested.add(
                position
        );

        /*
         * READY chunks can be queued immediately.
         *
         * If the chunk is already MESHING, onChunkUploaded()
         * will notice the retained request and queue it afterward.
         *
         * If it is only GENERATED, its initial mesh should already
         * be waiting in the ordinary mesh queue.
         */
        if (
                chunk.getState() ==
                        ChunkState.READY
        ) {
            queueUrgentMesh(
                    chunk
            );
        }


    }
}