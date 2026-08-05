package com.daniel.blocksurvival;

import com.daniel.blocksurvival.entity.Entity;
import com.daniel.blocksurvival.entity.EntityManager;
import com.daniel.blocksurvival.entity.ItemEntity;
import com.daniel.blocksurvival.graphics.*;
import com.daniel.blocksurvival.inventory.Inventory;
import com.daniel.blocksurvival.inventory.InventoryRenderer;
import com.daniel.blocksurvival.inventory.ItemStack;
import com.daniel.blocksurvival.inventory.Items;
import com.daniel.blocksurvival.world.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Main {


    private long window;
    private final Hotbar hotbar =
            new Hotbar();




    private static final int WORLD_SEED = 33333;

    public final World world = new World(); //setting public temp

    private final SaveManager saveManager =
            new SaveManager("World1");

    private final TerrainGenerator terrainGenerator =
            new TerrainGenerator(WORLD_SEED);

    private final EntityManager entityManager =
            new EntityManager();

    //private static final float PICKUP_DELAY_SECONDS = 4.0f;
    private final Sky sky =
            new Sky();

    private final LightEngine lightEngine =
            new LightEngine(
                    terrainGenerator
            );

    private final ChunkWorker chunkWorker =
            new ChunkWorker(
                    world,
                    terrainGenerator,
                    lightEngine,
                    saveManager
            );

    private boolean removeBlockRequested = false;

    private boolean breakBlockRequested = false;

    private boolean placeBlockRequested = false;

    private Texture atlasTexture;
    private Shader worldShader;
    private WorldRenderer worldRenderer;
    private SkyRenderer skyRenderer;
    private BlockOutlineRenderer outlineRenderer;
    private UiRenderer uiRenderer;
    private ItemEntityRenderer itemEntityRenderer;

    private RaycastResult currentRaycast;


    private int framebufferWidth = 1280;
    private int framebufferHeight = 720;

    private final ChunkMeshBuilder immediateMeshBuilder =
            new ChunkMeshBuilder();


    private final Map<Chunk, ChunkRenderData> chunkMeshes =
            new HashMap<>();

    private static final int RENDER_DISTANCE = 10; //goal is 32 or something eventually!

    private static final int FULL_DEPTH_DISTANCE = 4;

    private static final int SHALLOW_DEPTH_DISTANCE = 8;

    private int lastPlayerChunkX =
            Integer.MIN_VALUE;

    private int lastPlayerChunkZ =
            Integer.MIN_VALUE;



    private final Camera camera =
            new Camera(
                    new Vector3f(0.0f, 12f, 5.0f)
            );

    private double lastMouseX = 640.0;
    private double lastMouseY = 360.0;
    private boolean firstMouseMovement = true;

    private boolean waitingForPlayerTerrain = true;

    private final Inventory playerInventory =
            new Inventory(
                    4,
                    3
            );
    private InventoryRenderer inventoryRenderer;

    private float deltaTime = 0.0f;
    private float previousFrameTime = 0.0f;



    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        System.out.println("Starting Block Survival...");

        initialize();
        gameLoop();
        cleanup();
    }

    private void initialize() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW.");
        }

        glfwDefaultWindowHints();

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        // Required by macOS for modern OpenGL.
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        int width = 1280;
        int height = 720;

        window = glfwCreateWindow(
                width,
                height,
                "Block Survival",
                0,
                0
        );

        if (window == 0) {
            throw new RuntimeException("Failed to create the game window.");
        }


        glfwSetKeyCallback(
                window,
                (windowHandle, key, scanCode, action, mods) -> {
                    if (
                            key == GLFW_KEY_ESCAPE &&
                                    action == GLFW_PRESS
                    ) {
                        glfwSetWindowShouldClose(
                                windowHandle,
                                true
                        );
                    }

                    if (
                            key == GLFW_KEY_TAB &&
                                    action == GLFW_PRESS
                    ) {
                        inventoryRenderer.toggle();

                        if (inventoryRenderer.isVisible()) {
                            breakBlockRequested =
                                    false;

                            placeBlockRequested =
                                    false;
                        }
                    }

                    if (
                            inventoryRenderer.isVisible() &&
                                    action == GLFW_PRESS
                    ) {
                        switch (key) {
                            case GLFW_KEY_LEFT,
                                 GLFW_KEY_A ->
                                    inventoryRenderer.moveSelection(
                                            -1,
                                            0,
                                            playerInventory
                                    );

                            case GLFW_KEY_RIGHT,
                                 GLFW_KEY_D ->
                                    inventoryRenderer.moveSelection(
                                            1,
                                            0,
                                            playerInventory
                                    );

                            case GLFW_KEY_UP,
                                 GLFW_KEY_W ->
                                    inventoryRenderer.moveSelection(
                                            0,
                                            -1,
                                            playerInventory
                                    );

                            case GLFW_KEY_DOWN,
                                 GLFW_KEY_S ->
                                    inventoryRenderer.moveSelection(
                                            0,
                                            1,
                                            playerInventory
                                    );
                        }
                    }

                    if (
                            action == GLFW_PRESS &&
                                    !inventoryRenderer.isVisible()
                    ) {
                        if (
                                key >= GLFW_KEY_1 &&
                                        key <= GLFW_KEY_9
                        ) {
                            if (action == GLFW_PRESS) {
                                if (
                                        key >= GLFW_KEY_1 &&
                                                key <= GLFW_KEY_9
                                ) {
                                    int slotNumber =
                                            key - GLFW_KEY_1 + 1;

                                    hotbar.selectSlot(
                                            slotNumber
                                    );

                                    System.out.println(
                                            "Selected hotbar slot " +
                                                    slotNumber +
                                                    ": " +
                                                    hotbar.getSelectedBlock()
                                    );
                                }
                            }
                        }
                    }


                }
        );

        glfwSetMouseButtonCallback(
                window,
                (windowHandle, button, action, mods) -> {
                    if (
                            inventoryRenderer.isVisible()
                    ) {
                        return;
                    }

                    if (
                            button == GLFW_MOUSE_BUTTON_LEFT &&
                                    action == GLFW_PRESS
                    ) {
                        breakBlockRequested =
                                true;
                    }

                    if (
                            button == GLFW_MOUSE_BUTTON_RIGHT &&
                                    action == GLFW_PRESS
                    ) {
                        placeBlockRequested =
                                true;
                    }
                }
        );

        glfwSetScrollCallback(
                window,
                (windowHandle, horizontalOffset, verticalOffset) -> {
                    hotbar.scroll(
                            verticalOffset
                    );

                    System.out.println(
                            "Selected hotbar slot " +
                                    (hotbar.getSelectedIndex() + 1) +
                                    ": " +
                                    hotbar.getSelectedBlock()
                    );
                }
        );

        glfwSetCursorPosCallback(
                window,
                (windowHandle, mouseX, mouseY) -> {
                    if (firstMouseMovement) {
                        lastMouseX = mouseX;
                        lastMouseY = mouseY;
                        firstMouseMovement = false;
                    }

                    float horizontalOffset =
                            (float) (mouseX - lastMouseX);

                    float verticalOffset =
                            (float) (lastMouseY - mouseY);

                    lastMouseX = mouseX;
                    lastMouseY = mouseY;

                    float sensitivity = 0.1f;

                    horizontalOffset *= sensitivity;
                    verticalOffset *= sensitivity;

                    if (!inventoryRenderer.isVisible()) { //UNSURE IF THIS IS THE RIGHT PLACE
                        camera.rotate(
                                horizontalOffset,
                                verticalOffset
                        );
                    }

                }
        );

        glfwMakeContextCurrent(window);

        glfwSetInputMode(
                window,
                GLFW_CURSOR,
                GLFW_CURSOR_DISABLED
        );

        // Enable vertical synchronization.
        glfwSwapInterval(1);

        glfwShowWindow(window);

        // OpenGL commands cannot be used before this line.
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);

        /*
         * On Retina displays, the framebuffer resolution may be larger
         * than the ordinary window size.
         */
        try (MemoryStack stack = stackPush()) {
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);

            glfwGetFramebufferSize(
                    window,
                    widthBuffer,
                    heightBuffer
            );

            framebufferWidth = widthBuffer.get(0);
            framebufferHeight = heightBuffer.get(0);

            glViewport(
                    0,
                    0,
                    framebufferWidth,
                    framebufferHeight
            );
        }

        glfwSetFramebufferSizeCallback(window, (windowHandle, newWidth, newHeight) -> {
            framebufferWidth = newWidth;
            framebufferHeight = newHeight;

            glViewport(0, 0, newWidth, newHeight);
        });

        glClearColor(
                0.0f,
                0.0f,
                0.0f,
                1.0f
        );

        loadPlayer();

        updateLoadedChunks();

        createShaders();

        outlineRenderer = new BlockOutlineRenderer();
        skyRenderer = new SkyRenderer();

        atlasTexture = new Texture(
                "src/main/resources/textures/block_atlas.png"
        );
        itemEntityRenderer =
                new ItemEntityRenderer(
                        atlasTexture
                );

        worldRenderer =
                new WorldRenderer(
                        worldShader,
                        atlasTexture
                );
        uiRenderer =
                new UiRenderer(
                        atlasTexture
                );
        inventoryRenderer =
                new InventoryRenderer(
                        atlasTexture
                );

        int remaining =
                playerInventory.collect(
                        Items.MACHINE_CORE,
                        1
                );

        System.out.println(
                "Machine Core remaining: " +
                        remaining
        );

    }


    private void updateLoadedChunks() {
        /*
         * Convert the camera's world position into a world block.
         */
        int playerBlockX =
                (int) Math.floor(
                        camera.getPosition().x
                );

        int playerBlockZ =
                (int) Math.floor(
                        camera.getPosition().z
                );

        /*
         * Convert the world block into a chunk coordinate.
         */
        int playerChunkX =
                Math.floorDiv(
                        playerBlockX,
                        Chunk.SIZE
                );

        int playerChunkZ =
                Math.floorDiv(
                        playerBlockZ,
                        Chunk.SIZE
                );

        /*
         * Do nothing unless the player crossed into another chunk.
         */
        if (
                playerChunkX == lastPlayerChunkX &&
                        playerChunkZ == lastPlayerChunkZ
        ) {
            return;
        }

        lastPlayerChunkX = playerChunkX;
        lastPlayerChunkZ = playerChunkZ;
        chunkWorker.updateFocus(
                playerChunkX,
                playerChunkZ,
                RENDER_DISTANCE
        );

        /*
        System.out.println(
                "Player entered chunk: " +
                        playerChunkX + ", " +
                        playerChunkZ
        );

         */


        /*
         * Queue the exact chunk containing the player first.
         *
         * This gives the worker the terrain directly around the
         * saved player position before the rest of the world.
         */
        int playerBlockY =
                (int) Math.floor(
                        camera.getPosition().y
                );

        int playerChunkY =
                Math.floorDiv(
                        playerBlockY,
                        Chunk.SIZE
                );

        queueChunksAroundPlayer(
                playerChunkX,
                playerChunkY,
                playerChunkZ
        );








        /*
         * Generate every missing chunk inside render distance.
         */
        /*
         * Load or generate every chunk column inside render distance.
         *
         * Each horizontal position now contains several vertical chunks.
         */


        /*
         * Find chunks that are now outside render distance.
         *
         * We collect them first because removing chunks while
         * iterating over world.getChunks() would cause trouble.
         */
        List<Chunk> chunksToUnload =
                new ArrayList<>();


        for (Chunk chunk : world.getChunks()) {
            int distanceX =
                    Math.abs(
                            chunk.getChunkX() -
                                    playerChunkX
                    );

            int distanceZ =
                    Math.abs(
                            chunk.getChunkZ() -
                                    playerChunkZ
                    );

            boolean outsideRenderDistance =
                    distanceX > RENDER_DISTANCE ||
                            distanceZ > RENDER_DISTANCE;
            int horizontalDistance =
                    Math.max(
                            distanceX,
                            distanceZ
                    );

            boolean unnecessaryVerticalChunk =
                    !shouldKeepVerticalChunk(
                            chunk.getChunkY(),
                            horizontalDistance
                    );

            boolean workerStillUsesChunk =
                    chunk.getState() ==
                            ChunkState.QUEUED ||
                            chunk.getState() ==
                                    ChunkState.GENERATING ||
                            chunk.getState() ==
                                    ChunkState.MESHING;

            if (
                    (
                            outsideRenderDistance ||
                                    unnecessaryVerticalChunk
                    ) &&
                            !workerStillUsesChunk
            ) {
                chunksToUnload.add(
                        chunk
                );
            }
        }

        /*
         * Destroy the GPU mesh and remove the chunk data.
         */
        for (Chunk chunk : chunksToUnload) {

            /*
             * Only write chunks that actually changed.
             */
            if (chunk.isDirty()) {
                saveManager.saveChunk(
                        chunk
                );
            }

            ChunkRenderData renderData =
                    chunkMeshes.remove(chunk);

            if (renderData != null) {
                renderData.destroy();
            }

            world.removeChunk(
                    chunk.getChunkX(),
                    chunk.getChunkY(),
                    chunk.getChunkZ()
            );

            /*
            System.out.println(
                    "Unloaded chunk: " +
                            chunk.getChunkX() + ", " +
                            chunk.getChunkZ()
            );

             */
        }



    }

    private void processCompletedChunks() {
        /*
         * Limit GPU uploads per frame so a large batch of
         * completed chunks cannot create a new hitch.
         */
        int maximumUploadsPerFrame = 6;
        int uploadedCount = 0;

        while (
                uploadedCount <
                        maximumUploadsPerFrame
        ) {
            CompletedChunk completed =
                    chunkWorker.pollCompletedChunk();

            if (completed == null) {
                break;
            }

            Chunk chunk =
                    completed.chunk();

            /*
             * The player may have moved far away while this
             * chunk was being generated.
             */
            Chunk currentChunk =
                    world.getChunk(
                            chunk.getChunkX(),
                            chunk.getChunkY(),
                            chunk.getChunkZ()
                    );

            if (currentChunk != chunk) {
                continue;
            }

            ChunkRenderData oldRenderData =
                    chunkMeshes.remove(
                            chunk
                    );

            if (oldRenderData != null) {
                oldRenderData.destroy();
            }

            ChunkRenderData renderData =
                    ChunkRenderData.fromMeshData(
                            completed.meshData()
                    );

            chunkMeshes.put(
                    chunk,
                    renderData
            );

            chunk.setState(
                    ChunkState.READY
            );
            chunkWorker.onChunkUploaded(
                    chunk
            );

            /*
             * Existing neighboring meshes may still contain
             * now-hidden border faces.
             *
             * For the first threaded version, only rebuild
             * neighbors that are already ready.
             */
            /*
            //killing fps :(
            rebuildReadyNeighborsOfChunk(
                    chunk
            );
            */

            uploadedCount++;
        }
    }

    private void rebuildReadyNeighborsOfChunk(
            Chunk chunk
    ) {
        rebuildReadyChunk(
                chunk.getChunkX() - 1,
                chunk.getChunkY(),
                chunk.getChunkZ()
        );

        rebuildReadyChunk(
                chunk.getChunkX() + 1,
                chunk.getChunkY(),
                chunk.getChunkZ()
        );

        rebuildReadyChunk(
                chunk.getChunkX(),
                chunk.getChunkY() - 1,
                chunk.getChunkZ()
        );

        rebuildReadyChunk(
                chunk.getChunkX(),
                chunk.getChunkY() + 1,
                chunk.getChunkZ()
        );

        rebuildReadyChunk(
                chunk.getChunkX(),
                chunk.getChunkY(),
                chunk.getChunkZ() - 1
        );

        rebuildReadyChunk(
                chunk.getChunkX(),
                chunk.getChunkY(),
                chunk.getChunkZ() + 1
        );
    }

    private void rebuildReadyChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        Chunk chunk =
                world.getChunk(
                        chunkX,
                        chunkY,
                        chunkZ
                );

        if (
                chunk == null ||
                        chunk.getState() !=
                                ChunkState.READY
        ) {
            return;
        }

        rebuildChunk(
                chunkX,
                chunkY,
                chunkZ
        );

        chunk.setState(
                ChunkState.READY
        );
    }

    private void rebuildNeighborsOfChunk(
            Chunk chunk
    ) {
        int chunkX =
                chunk.getChunkX();

        int chunkY =
                chunk.getChunkY();

        int chunkZ =
                chunk.getChunkZ();

        rebuildChunk(
                chunkX - 1,
                chunkY,
                chunkZ
        );

        rebuildChunk(
                chunkX + 1,
                chunkY,
                chunkZ
        );

        rebuildChunk(
                chunkX,
                chunkY - 1,
                chunkZ
        );

        rebuildChunk(
                chunkX,
                chunkY + 1,
                chunkZ
        );

        rebuildChunk(
                chunkX,
                chunkY,
                chunkZ - 1
        );

        rebuildChunk(
                chunkX,
                chunkY,
                chunkZ + 1
        );
    }




    private void rebuildChunksAroundBlock(
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

        int localX =
                Math.floorMod(worldX, Chunk.SIZE);

        int localY =
                Math.floorMod(worldY, Chunk.SIZE);

        int localZ =
                Math.floorMod(worldZ, Chunk.SIZE);

        /*
         * Always rebuild the chunk containing the edited block.
         */
        rebuildChunk(
                chunkX,
                chunkY,
                chunkZ
        );

        /*
         * Rebuild a neighboring chunk only when the changed
         * block touches that side of the current chunk.
         */

        if (localX == 0) {
            rebuildChunk(
                    chunkX - 1,
                    chunkY,
                    chunkZ
            );
        }

        if (localX == Chunk.SIZE - 1) {
            rebuildChunk(
                    chunkX + 1,
                    chunkY,
                    chunkZ
            );
        }

        if (localY == 0) {
            rebuildChunk(
                    chunkX,
                    chunkY - 1,
                    chunkZ
            );
        }

        if (localY == Chunk.SIZE - 1) {
            rebuildChunk(
                    chunkX,
                    chunkY + 1,
                    chunkZ
            );
        }

        if (localZ == 0) {
            rebuildChunk(
                    chunkX,
                    chunkY,
                    chunkZ - 1
            );
        }

        if (localZ == Chunk.SIZE - 1) {
            rebuildChunk(
                    chunkX,
                    chunkY,
                    chunkZ + 1
            );
        }
    }

    private void rebuildBoundaryNeighbors(
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

        if (localX == 0) {
            rebuildChunk(
                    chunkX - 1,
                    chunkY,
                    chunkZ
            );
        }

        if (localX == Chunk.SIZE - 1) {
            rebuildChunk(
                    chunkX + 1,
                    chunkY,
                    chunkZ
            );
        }

        if (localY == 0) {
            rebuildChunk(
                    chunkX,
                    chunkY - 1,
                    chunkZ
            );
        }

        if (localY == Chunk.SIZE - 1) {
            rebuildChunk(
                    chunkX,
                    chunkY + 1,
                    chunkZ
            );
        }

        if (localZ == 0) {
            rebuildChunk(
                    chunkX,
                    chunkY,
                    chunkZ - 1
            );
        }

        if (localZ == Chunk.SIZE - 1) {
            rebuildChunk(
                    chunkX,
                    chunkY,
                    chunkZ + 1
            );
        }
    }

    private void rebuildChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        Chunk chunk =
                world.getChunk(
                        chunkX,
                        chunkY,
                        chunkZ
                );

        if (
                chunk == null ||
                        !chunk.hasTerrain()
        ) {
            return;
        }

        /*
         * Keep the existing GPU mesh visible while a new
         * CPU-side mesh is built in the background.
         */
        chunkWorker.requestRemesh(
                chunk
        );
    }

    private boolean isPlayerTerrainLoaded() {
        Vector3f playerPosition =
                camera.getBodyCenterPosition();

        int blockX =
                (int) Math.floor(
                        playerPosition.x
                );

        int blockY =
                (int) Math.floor(
                        playerPosition.y
                );

        int blockZ =
                (int) Math.floor(
                        playerPosition.z
                );

        Chunk playerChunk =
                world.getChunkAtWorldBlock(
                        blockX,
                        blockY,
                        blockZ
                );

        return playerChunk != null &&
                playerChunk.hasTerrain();
    }

    private void queueChunkColumn(
            int chunkX,
            int chunkZ,
            int playerChunkY,
            int horizontalDistance
    ) {
        int surfacePriority =
                horizontalDistance * 100;
        /*
         * Near the player, generate the complete underground.
         */
        if (
                horizontalDistance <=
                        FULL_DEPTH_DISTANCE
        ) {
            /*
             * Start at the player's current vertical layer.
             */
            if (
                    playerChunkY >=
                            WorldGenerationSettings.MIN_CHUNK_Y &&
                            playerChunkY <=
                                    WorldGenerationSettings.MAX_CHUNK_Y
            ) {
                queueChunk(
                        chunkX,
                        playerChunkY,
                        chunkZ,
                        surfacePriority
                );
            }

            /*
             * Expand vertically away from the player.
             */
            int maximumVerticalDistance =
                    Math.max(
                            playerChunkY -
                                    WorldGenerationSettings.MIN_CHUNK_Y,
                            WorldGenerationSettings.MAX_CHUNK_Y -
                                    playerChunkY
                    );

            for (
                    int verticalDistance = 1;
                    verticalDistance <=
                            maximumVerticalDistance;
                    verticalDistance++
            ) {
                int chunkBelow =
                        playerChunkY -
                                verticalDistance;

                if (
                        chunkBelow >=
                                WorldGenerationSettings.MIN_CHUNK_Y
                ) {
                    queueChunk(
                            chunkX,
                            chunkBelow,
                            chunkZ,
                            surfacePriority +
                                    Math.abs(
                                            chunkBelow -
                                                    playerChunkY
                                    )
                    );
                }

                int chunkAbove =
                        playerChunkY +
                                verticalDistance;

                if (
                        chunkAbove <=
                                WorldGenerationSettings.MAX_CHUNK_Y
                ) {
                    queueChunk(
                            chunkX,
                            chunkAbove,
                            chunkZ,
                            surfacePriority +
                                    Math.abs(
                                            chunkAbove -
                                                    playerChunkY
                                    )
                    );
                }
            }

            return;
        }

        /*
         * At medium distance, keep the surface, the tree layer,
         * and one shallow underground layer.
         */
        if (
                horizontalDistance <=
                        SHALLOW_DEPTH_DISTANCE
        ) {
            queueChunk(
                    chunkX,
                    0,
                    chunkZ,
                    surfacePriority
            );

            queueChunk(
                    chunkX,
                    1,
                    chunkZ,
                    surfacePriority + 1
            );

            queueChunk(
                    chunkX,
                    -1,
                    chunkZ,
                    surfacePriority + 2
            );

            return;
        }

        /*
         * Far away, only generate visible surface terrain.
         */
        queueChunk(
                chunkX,
                0,
                chunkZ,
                surfacePriority
        );

        queueChunk(
                chunkX,
                1,
                chunkZ,
                surfacePriority + 1
        );
    }

    private void queueChunk(
            int chunkX,
            int chunkY,
            int chunkZ,
            int priority
    ) {
        chunkWorker.queueChunk(
                chunkX,
                chunkY,
                chunkZ,
                priority
        );
    }

    private void queueChunksAroundPlayer(
            int playerChunkX,
            int playerChunkY,
            int playerChunkZ
    ) {
        queueChunkColumn(
                playerChunkX,
                playerChunkZ,
                playerChunkY,
                0
        );

        for (
                int radius = 1;
                radius <= RENDER_DISTANCE;
                radius++
        ) {
            int minimumX =
                    playerChunkX - radius;

            int maximumX =
                    playerChunkX + radius;

            int minimumZ =
                    playerChunkZ - radius;

            int maximumZ =
                    playerChunkZ + radius;

            for (
                    int chunkX = minimumX;
                    chunkX <= maximumX;
                    chunkX++
            ) {
                queueChunkColumn(
                        chunkX,
                        minimumZ,
                        playerChunkY,
                        radius
                );

                queueChunkColumn(
                        chunkX,
                        maximumZ,
                        playerChunkY,
                        radius
                );
            }

            for (
                    int chunkZ = minimumZ + 1;
                    chunkZ < maximumZ;
                    chunkZ++
            ) {
                queueChunkColumn(
                        minimumX,
                        chunkZ,
                        playerChunkY,
                        radius
                );

                queueChunkColumn(
                        maximumX,
                        chunkZ,
                        playerChunkY,
                        radius
                );
            }
        }
    }

    private boolean shouldKeepVerticalChunk(
            int chunkY,
            int horizontalDistance
    ) {
        if (
                horizontalDistance <=
                        FULL_DEPTH_DISTANCE
        ) {
            return chunkY >=
                    WorldGenerationSettings.MIN_CHUNK_Y &&
                    chunkY <=
                            WorldGenerationSettings.MAX_CHUNK_Y;
        }

        if (
                horizontalDistance <=
                        SHALLOW_DEPTH_DISTANCE
        ) {
            return chunkY >= -1 &&
                    chunkY <= 1;
        }

        return chunkY >= 0 &&
                chunkY <= 1;
    }


    private void createShaders() {
        String vertexShaderSource = """
        #version 330 core

        layout (location = 0) in vec3 position;
        layout (location = 1) in vec2 textureCoordinate;
        layout (location = 2) in float ambientOcclusion;
        layout (location = 3) in float material;
        layout (location = 4) in float bendWeight;
        layout (location = 5) in float skyLight;
        layout (location = 6) in float blockLight;

        const float MATERIAL_DEFAULT = 0.0;
        const float MATERIAL_WATER = 1.0;
        const float MATERIAL_FOLIAGE = 2.0;
        const float MATERIAL_LEAVES = 3.0;
        
        

        uniform mat4 mvpMatrix;
        uniform float animationTime;


        out vec2 fragmentTextureCoordinate;
        out vec3 fragmentWorldPosition;
        out float fragmentAO;
        out float fragmentMaterial;
        out float fragmentSkyLight;
        out float fragmentBlockLight;
        
        

        void main() {
        
            vec3 animatedPosition =
                    position;

            /*
             * Animate foliage vertices while keeping their
             * bottom vertices anchored to the ground.
             */
            if (material == MATERIAL_FOLIAGE) {

                /*
                 * Creates a broad gust moving diagonally
                 * across the world.
                 */
                float travelingGust =
                        sin(
                                position.x * 0.32 +
                                position.z * 0.24 -
                                animationTime * 1.5
                        );

                /*
                 * Adds smaller local motion so the plants
                 * do not all move as one rigid wave.
                 */
                float localFlutter =
                        sin(
                                position.x * 1.7 -
                                position.z * 1.3 +
                                animationTime * 2.4
                        );

                float windStrength =
                        travelingGust * 0.075 +
                        localFlutter * 0.025;

                animatedPosition.x +=
                        windStrength *
                        bendWeight;

                animatedPosition.z +=
                        windStrength *
                        0.45 *
                        bendWeight;
            }
            
            /*
                     * Leaves move more subtly than flowers.
                     *
                     * This uses world position so the wind appears to roll
                     * continuously across an entire tree canopy.
                     */
                    else if (material == MATERIAL_LEAVES) {
                
                        /*
                         * A broad, slow-moving gust.
                         */
                        float canopyGust =
                                sin(
                                        position.x * 0.28 +
                                        position.z * 0.22 -
                                        animationTime * 0.9
                                );
                
                        /*
                         * Smaller and quicker motion layered over the gust.
                         */
                        float leafFlutter =
                                sin(
                                        position.x * 1.3 -
                                        position.z * 1.1 +
                                        position.y * 0.7 +
                                        animationTime * 2.1
                                );
                
                        float leafMovement =
                                canopyGust * 0.022 + //controls the larger canopy sway.
                                leafFlutter * 0.008; //controls the little flutter.
                
                        /*
                         * Mostly horizontal movement, with an extremely
                         * small vertical lift.
                         */
                        animatedPosition.x +=
                                leafMovement;
                
                        animatedPosition.z +=
                                leafMovement * 0.55;
                
                        animatedPosition.y +=
                                leafFlutter * 0.003;
                    }

            gl_Position =
                    mvpMatrix *
                    vec4(
                            animatedPosition,
                            1.0
                    );

            fragmentTextureCoordinate =
                    textureCoordinate;

            fragmentAO =
                    ambientOcclusion;

            fragmentMaterial =
                    material;

            /*
             * Lighting and fog must use the animated position,
             * not the original static position.
             */
            fragmentWorldPosition =
                    animatedPosition;
                    
             fragmentSkyLight =
                             skyLight;
                
                     fragmentBlockLight =
                             blockLight;
        }
        """;

        String fragmentShaderSource = """
        #version 330 core
        
        const float MATERIAL_DEFAULT = 0.0;
        const float MATERIAL_WATER = 1.0;
        const float MATERIAL_FOLIAGE = 2.0;
        const float MATERIAL_LEAVES = 3.0;
        
        in vec2 fragmentTextureCoordinate;
        in vec3 fragmentWorldPosition;
        in float fragmentAO;
        in float fragmentMaterial;
        in float fragmentSkyLight;
        in float fragmentBlockLight;
        
        uniform sampler2D blockTexture;
        uniform vec3 cameraPosition;
        uniform vec3 fogColor;
        uniform float fogStart;
        uniform float fogEnd;
        uniform float sunBrightness;

        uniform float animationTime;
        uniform float atlasTileSize;
        
        out vec4 finalColor;
        
        void main() {
            vec2 animatedTextureCoordinate =
        fragmentTextureCoordinate;

/*
 * Material 1.0 represents water.
 */
if (fragmentMaterial == MATERIAL_WATER) {
    /*
     * Determine which atlas tile this UV belongs to.
     *
     * The tiny subtraction prevents coordinates lying directly
     * on a tile's upper edge from being mistaken for the next tile.
     */
    vec2 tileOrigin =
            floor(
                    (
                            fragmentTextureCoordinate -
                            vec2(0.00001)
                    ) /
                    atlasTileSize
            ) *
            atlasTileSize;

    /*
     * Convert the atlas UV into coordinates ranging from
     * 0.0 to 1.0 inside this individual tile.
     */
    vec2 localTextureCoordinate =
            (
                    fragmentTextureCoordinate -
                    tileOrigin
            ) /
            atlasTileSize;

    /*
     * Scroll slowly in two directions.
     */
    vec2 waterMovement =
            vec2(
                    animationTime * 0.025,
                    animationTime * 0.012
            );

    /*
     * Add a small ripple so the movement is not merely
     * a perfectly straight conveyor belt.
     */
    float rippleX =
            sin(
                    localTextureCoordinate.y * 12.0 +
                    animationTime * 1.4
            ) * 0.012;

    float rippleY =
            cos(
                    localTextureCoordinate.x * 10.0 +
                    animationTime * 1.1
            ) * 0.008;

    localTextureCoordinate +=
            waterMovement +
            vec2(
                    rippleX,
                    rippleY
            );

    /*
     * Wrap inside this tile instead of drifting into
     * neighboring atlas textures.
     */
    localTextureCoordinate =
            fract(
                    localTextureCoordinate
            );

    animatedTextureCoordinate =
            tileOrigin +
            localTextureCoordinate *
            atlasTileSize;
}

vec4 textureColor =
        texture(
                blockTexture,
                animatedTextureCoordinate
        );
        
            if (textureColor.a < 0.5) {
                discard;
            }
        
            /*
             * Calculate the direction the current face points.
             */
            vec3 positionChangeX =
                    dFdx(fragmentWorldPosition);
        
            vec3 positionChangeY =
                    dFdy(fragmentWorldPosition);
        
            vec3 normal =
                    normalize(
                            cross(
                                    positionChangeX,
                                    positionChangeY
                            )
                    );
        
            if (!gl_FrontFacing) {
                normal = -normal;
            }
        
            vec3 sunDirection =
                    normalize(
                            vec3(
                                    -0.6,
                                    1.0,
                                    0.4
                            )
                    );
        
            float sunlight =
                    max(
                            dot(normal, sunDirection),
                            0.0
                    );
        
            float ambientLight = 0.45;
        
            float brightness =
                    ambientLight +
                    sunlight * 0.55;
        
            float finalLight =
        max(
                fragmentSkyLight *
                        sunBrightness,
                fragmentBlockLight
        );

vec3 litColor =
        textureColor.rgb *
        brightness *
        fragmentAO *
        finalLight;
        
            float distanceFromCamera =
                    length(
                            fragmentWorldPosition -
                            cameraPosition
                    );
        
            float fogFactor =
                    clamp(
                            (fogEnd - distanceFromCamera) /
                            (fogEnd - fogStart),
                            0.0,
                            1.0
                    );
        
            vec3 foggedColor =
                    mix(
                            fogColor,
                            litColor,
                            fogFactor
                    );
        
            finalColor =
                    vec4(
                            foggedColor,
                            textureColor.a
                    );
        }
        """;
        worldShader =
                new Shader(
                        vertexShaderSource,
                        fragmentShaderSource
                );

        worldShader.bind();

        worldShader.setInt(
                "blockTexture",
                0
        );

        worldShader.setFloat(
                "sunBrightness",
                1.0f
        );

        worldShader.unbind();
    }



    private void processInput() {
        float cameraSpeed = 9.0f * deltaTime;

        /*
         * Apply sprint before processing movement.
         */
        if (
                glfwGetKey(window, GLFW_KEY_LEFT_CONTROL)
                        == GLFW_PRESS
        ) {
            cameraSpeed *= 2.0f;
        }

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            camera.moveForward(cameraSpeed);
        }

        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            camera.moveBackward(cameraSpeed);
        }

        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            camera.moveLeft(cameraSpeed);
        }

        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            camera.moveRight(cameraSpeed);
        }

        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            camera.jump();
        }

    }


    private void breakTargetedBlock() {
        if (currentRaycast == null) {
            System.out.println("No block in range.");
            return;
        }



        int blockX = currentRaycast.hitX();
        int blockY = currentRaycast.hitY();
        int blockZ = currentRaycast.hitZ();

        BlockType oldBlock =
                world.getBlock(
                        blockX,
                        blockY,
                        blockZ
                );

        System.out.println(
                "Breaking block at: " +
                        blockX + ", " +
                        blockY + ", " +
                        blockZ
        );

        if (oldBlock != null) {
            entityManager.spawn(
                    new ItemEntity(
                            blockX,
                            blockY + 0.15f,
                            blockZ,
                            oldBlock
                    )
            );
        }


        world.setBlock(
                blockX,
                blockY,
                blockZ,
                null
        );

        rebuildEditedChunkImmediately(
                blockX,
                blockY,
                blockZ
        );

        Set<Chunk> lightChangedChunks =
                lightEngine.blockChanged(
                        world,
                        blockX,
                        blockY,
                        blockZ,
                        oldBlock,
                        null
                );

        for (
                Chunk changedChunk :
                lightChangedChunks
        ) {
            chunkWorker.requestRemesh(
                    changedChunk
            );
        }

        rebuildBoundaryNeighbors(
                blockX,
                blockY,
                blockZ
        );

        /*
         * Immediately update the target after removing
         * the block.
         */
        currentRaycast = calculateRaycast();
    }


    private void placeTargetedBlock() {
        if (currentRaycast == null) {
            System.out.println("No block in range.");
            return;
        }

        int placementX =
                currentRaycast.placementX();

        int placementY =
                currentRaycast.placementY();

        int placementZ =
                currentRaycast.placementZ();

        BlockType oldBlock =
                world.getBlock(
                        placementX,
                        placementY,
                        placementZ
                );

        System.out.println(
                "Placing " +
                        hotbar.getSelectedBlock() +
                        " at: " +
                        placementX + ", " +
                        placementY + ", " +
                        placementZ
        );

        if (camera.overlapsBlock(
                placementX,
                placementY,
                placementZ
        )) {
            System.out.println(
                    "Cannot place a block inside the player."
            );

            return;
        }

        BlockType selectedBlock =
                hotbar.getSelectedBlock();

        BlockDirection direction = null;
        if (selectedBlock == BlockType.TORCH) {
            int differenceX =
                    placementX -
                            currentRaycast.hitX();

            int differenceY =
                    placementY -
                            currentRaycast.hitY();

            int differenceZ =
                    placementZ -
                            currentRaycast.hitZ();

            /*
             * For now, a torch can only be placed on the top
             * face of a supporting block.
             */
            boolean placedOnTop =
                    differenceX == 0 &&
                            differenceY == 1 &&
                            differenceZ == 0;

            if (
                    differenceX == 0 &&
                            differenceY == 1 &&
                            differenceZ == 0
            ) {
                direction =
                        BlockDirection.UP;
            } else if (
                    differenceZ == -1
            ) {
                direction =
                        BlockDirection.NORTH;
            } else if (
                    differenceZ == 1
            ) {
                direction =
                        BlockDirection.SOUTH;
            } else if (
                    differenceX == 1
            ) {
                direction =
                        BlockDirection.EAST;
            } else if (
                    differenceX == -1
            ) {
                direction =
                        BlockDirection.WEST;
            } else {
                return;
            }

            int supportX =
                    placementX;

            int supportY =
                    placementY;

            int supportZ =
                    placementZ;

            switch (direction) {
                case UP ->
                        supportY -= 1;

                case NORTH ->
                        supportZ += 1;

                case SOUTH ->
                        supportZ -= 1;

                case EAST ->
                        supportX -= 1;

                case WEST ->
                        supportX += 1;
            }

            BlockType supportBlock =
                    world.getBlock(
                            supportX,
                            supportY,
                            supportZ
                    );

            if (
                    supportBlock == null ||
                            !supportBlock.isOpaque()
            ) {
                System.out.println(
                        "Torch requires a solid supporting block."
                );

                return;
            }
        }

        world.setBlock(
                placementX,
                placementY,
                placementZ,
                selectedBlock
        );

        world.setBlockDirection(
                placementX,
                placementY,
                placementZ,
                direction
        );

        rebuildEditedChunkImmediately(
                placementX,
                placementY,
                placementZ
        );

        Set<Chunk> lightChangedChunks =
                lightEngine.blockChanged(
                        world,
                        placementX,
                        placementY,
                        placementZ,
                        oldBlock,
                        selectedBlock
                );

        for (
                Chunk changedChunk :
                lightChangedChunks
        ) {
            chunkWorker.requestRemesh(
                    changedChunk
            );
        }

        rebuildBoundaryNeighbors(
                placementX,
                placementY,
                placementZ
        );

        currentRaycast = calculateRaycast();

        System.out.println(
                world.getBlockDirection(
                        placementX,
                        placementY,
                        placementZ
                )
        );
    }

    private RaycastResult calculateRaycast() {
        /*
         * Make copies so we do not accidentally modify
         * the camera's internal vectors.
         */
        Vector3f rayOrigin =
                new Vector3f(camera.getPosition());

        Vector3f rayDirection =
                new Vector3f(camera.getFront())
                        .normalize();

        float maximumDistance = 6.0f;
        float stepSize = 0.05f;

        int previousX = 0;
        int previousY = 0;
        int previousZ = 0;

        boolean hasPreviousCell = false;

        for (
                float distance = 0.0f;
                distance <= maximumDistance;
                distance += stepSize
        ) {
            /*
             * Find a point along the ray:
             *
             * origin + direction × distance
             */
            Vector3f currentPoint =
                    new Vector3f(rayDirection)
                            .mul(distance)
                            .add(rayOrigin);

            /*
             * Your blocks are centered on integer coordinates.
             *
             * Adding 0.5 before flooring finds the nearest
             * block center instead of treating integers as
             * block corners.
             */
            int blockX =
                    (int) Math.floor(
                            currentPoint.x + 0.5f
                    );

            int blockY =
                    (int) Math.floor(
                            currentPoint.y + 0.5f
                    );

            int blockZ =
                    (int) Math.floor(
                            currentPoint.z + 0.5f
                    );

            BlockType block =
                    world.getBlock(
                            blockX,
                            blockY,
                            blockZ
                    );

            if (block != null) {
                /*
                 * We found the first solid block.
                 */
                if (!hasPreviousCell) {
                    /*
                     * This would mean the ray began inside
                     * a solid block. We cannot safely place
                     * anything in front of it.
                     */
                    return null;
                }

                return new RaycastResult(
                        blockX,
                        blockY,
                        blockZ,
                        previousX,
                        previousY,
                        previousZ
                );
            }

            /*
             * This cell is empty, so remember it.
             * If the next cell is solid, this becomes
             * the placement position.
             */
            previousX = blockX;
            previousY = blockY;
            previousZ = blockZ;

            hasPreviousCell = true;
        }

        /*
         * Nothing was hit within six blocks.
         */
        return null;
    }

    private void updateTargetedBlock() {
        Vector3f rayPosition =
                new Vector3f(camera.getPosition())
                        .add(0.0f, 0.5f, 0.0f);

        Vector3f rayDirection =
                new Vector3f(camera.getFront())
                        .normalize();

        float maximumDistance = 6.0f;
        float stepSize = 0.05f;


        for (
                float distance = 0.0f;
                distance <= maximumDistance;
                distance += stepSize
        ) {
            Vector3f currentPoint =
                    new Vector3f(rayDirection)
                            .mul(distance)
                            .add(rayPosition);

            int blockX =
                    (int) Math.floor(
                            currentPoint.x + 0.5f
                    );

            int blockY =
                    (int) Math.floor(
                            currentPoint.y + 0.5f
                    );

            int blockZ =
                    (int) Math.floor(
                            currentPoint.z + 0.5f
                    );

            BlockType block =
                    world.getBlock(
                            blockX,
                            blockY,
                            blockZ
                    );


        }
    }

    /*
     * TODO:
     * Immediate rebuild currently uses the previous lighting state.
     *
     * In the future, lighting updates should run through a
     * double-buffered lighting pipeline so geometry and lighting
     * become visible simultaneously.
     */
    private void rebuildEditedChunkImmediately(
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
                world.getChunk(
                        chunkX,
                        chunkY,
                        chunkZ
                );

        if (
                chunk == null ||
                        !chunk.hasTerrain()
        ) {
            return;
        }

        /*
         * Build the replacement first.
         *
         * The current GPU mesh remains visible during this work,
         * so the chunk never disappears.
         */
        ChunkMeshData meshData =
                immediateMeshBuilder.build(
                        world,
                        chunk
                );

        ChunkRenderData replacement =
                ChunkRenderData.fromMeshData(
                        meshData
                );

        /*
         * Atomically replace the visible mesh.
         */
        ChunkRenderData previous =
                chunkMeshes.put(
                        chunk,
                        replacement
                );

        if (previous != null) {
            previous.destroy();
        }
    }


    private void gameLoop() {
        while (!glfwWindowShouldClose(window)) {
            float currentFrameTime = (float) glfwGetTime();

            deltaTime =
                    currentFrameTime -
                            previousFrameTime;

            previousFrameTime = currentFrameTime;

            /*
             * Queue chunks and collect completed worker results before
             * attempting to move the player.
             */
            updateLoadedChunks();
            processCompletedChunks();

            if (waitingForPlayerTerrain) {
                if (isPlayerTerrainLoaded()) {
                    waitingForPlayerTerrain = false;

                    System.out.println(
                            "Player terrain loaded."
                    );
                }
            }
            else {


                if(!inventoryRenderer.isVisible()){
                    camera.updatePhysics(
                            world,
                            deltaTime
                    );

                    processInput();
                    entityManager.update(
                            world,
                            camera.getBodyCenterPosition(),
                            playerInventory,
                            deltaTime
                    );

                    if (removeBlockRequested) {
                        /*
                         * Remove the block in the center of the floor.
                         * null represents empty space.
                         */
                        world.setBlock(
                                0,
                                0,
                                0,
                                null
                        );

                        rebuildBoundaryNeighbors(
                                0,
                                0,
                                0
                        );

                        removeBlockRequested = false;
                    }

                    if (placeBlockRequested) {
                        placeTargetedBlock();
                        placeBlockRequested = false;
                    }

                    if (breakBlockRequested) {
                        breakTargetedBlock();
                        breakBlockRequested = false;
                    }
                }


                currentRaycast =
                        calculateRaycast();
            }


            sky.update(
                    deltaTime
            );

            float sunBrightness =
                    sky.getSunBrightness();

            Vector3f skyColor =
                    sky.getSkyColor();

            glClearColor(
                    skyColor.x,
                    skyColor.y,
                    skyColor.z,
                    1.0f
            );


            glClear(
                    GL_COLOR_BUFFER_BIT |
                            GL_DEPTH_BUFFER_BIT
            );


            /*
             * The cube now remains still at the world origin.
             */

            worldShader.bind();

            worldShader.setFloat(
                    "sunBrightness",
                    sunBrightness
            );

            Vector3f fogColor =
                    sky.getFogColor();

            worldShader.setVector3(
                    "fogColor",
                    fogColor
            );

            worldShader.unbind();


            /*
             * VIEW:
             * Represents the camera.
             *
             * Moving the world backward by 3 units has the same
             * visual result as moving the camera forward.
             */
            Matrix4f viewMatrix =
                    camera.createViewMatrix();

            float aspectRatio =
                    (float) framebufferWidth /
                            (float) framebufferHeight;

            /*
             * PROJECTION:
             * Creates perspective, making distant objects appear smaller.
             */
            Matrix4f projectionMatrix = new Matrix4f()
                    .perspective(
                            (float) Math.toRadians(45.0),
                            aspectRatio,
                            0.1f,
                            500.0f
                    );

            skyRenderer.render(
                    sky,
                    projectionMatrix,
                    viewMatrix
            );

            /*
             * Combine the three transformations.
             *
             * The multiplication order matters.
             */

            worldRenderer.render(
                    projectionMatrix,
                    viewMatrix,
                    camera,
                    world,
                    chunkMeshes,
                    deltaTime
            );

            itemEntityRenderer.render(
                    entityManager.getEntities(),
                    projectionMatrix,
                    viewMatrix,
                    camera,
                    world,
                    sunBrightness
            );

            if (currentRaycast != null) {
                outlineRenderer.render(
                        currentRaycast.hitX(),
                        currentRaycast.hitY(),
                        currentRaycast.hitZ(),
                        projectionMatrix,
                        viewMatrix
                );
            }
            uiRenderer.render(
                    hotbar,
                    framebufferWidth,
                    framebufferHeight
            );
            inventoryRenderer.render(
                    playerInventory,
                    framebufferWidth,
                    framebufferHeight
            );

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
    private void saveDirtyChunks() {
        System.out.println(
                "Saving modified chunks..."
        );

        int savedChunkCount = 0;

        for (Chunk chunk : world.getChunks()) {
            if (!chunk.isDirty()) {
                continue;
            }

            saveManager.saveChunk(
                    chunk
            );

            savedChunkCount++;
        }

        System.out.println(
                "Saved " +
                        savedChunkCount +
                        " modified chunk(s)."
        );
    }

    private void savePlayer() {
        Vector3f playerPosition =
                camera.getPosition();

        PlayerSaveData playerData =
                new PlayerSaveData(
                        playerPosition.x,
                        playerPosition.y,
                        playerPosition.z,
                        camera.getYaw(),
                        camera.getPitch(),
                        hotbar.getSelectedIndex()
                );

        saveManager.savePlayer(
                playerData
        );
    }

    private void loadPlayer() {
        PlayerSaveData playerData =
                saveManager.loadPlayer();

        if (playerData == null) {
            System.out.println(
                    "No player save found. Using default spawn."
            );

            return;
        }

        camera.setPosition(
                playerData.positionX(),
                playerData.positionY(),
                playerData.positionZ()
        );

        camera.setRotation(
                playerData.yaw(),
                playerData.pitch()
        );

        hotbar.selectSlot(
                playerData.selectedHotbarSlot() + 1
        );

        System.out.println(
                "Player position: " +
                        playerData.positionX() + ", " +
                        playerData.positionY() + ", " +
                        playerData.positionZ()
        );
    }

    private void cleanup() {
        chunkWorker.shutdown();
        savePlayer();
        saveDirtyChunks();

        if (worldShader != null) {
            worldShader.destroy();
        }

        for (ChunkRenderData renderData : chunkMeshes.values()) {
            renderData.destroy();
        }

        chunkMeshes.clear();
        entityManager.clear();

        if (itemEntityRenderer != null) {
            itemEntityRenderer.destroy();
        }
        if (inventoryRenderer != null) {
            inventoryRenderer.destroy();
        }

        if (uiRenderer != null) {
            uiRenderer.cleanup();
        }

        if (skyRenderer != null) {
            skyRenderer.destroy();
        }

        if (outlineRenderer != null) {
            outlineRenderer.cleanup();
        }

        if (atlasTexture != null) {
            atlasTexture.destroy();
        }

        glfwDestroyWindow(window);
        glfwTerminate();

        GLFWErrorCallback callback = glfwSetErrorCallback(null);

        if (callback != null) {
            callback.free();
        }
    }

    private void printPlayerInventory() {
        System.out.println(
                "Inventory:"
        );

        if (
                playerInventory.getStacks()
                        .isEmpty()
        ) {
            System.out.println(
                    "  empty"
            );

            return;
        }

        for (
                ItemStack stack :
                playerInventory.getStacks()
        ) {
            System.out.println(
                    "  " +
                            stack.getDefinition()
                                    .displayName() +
                            " x" +
                            stack.getQuantity() +
                            " at [" +
                            stack.getGridX() +
                            ", " +
                            stack.getGridY() +
                            "]" +
                            (
                                    stack.isRotated()
                                            ? " rotated"
                                            : ""
                            )
            );
        }
    }
}