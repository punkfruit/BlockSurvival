package com.daniel.blocksurvival;

import com.daniel.blocksurvival.graphics.*;
import com.daniel.blocksurvival.world.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Main {

    private long window;
    private final Hotbar hotbar =
            new Hotbar();


    private final ChunkMeshBuilder chunkMeshBuilder =
            new ChunkMeshBuilder();

    private final TerrainGenerator terrainGenerator =
            new TerrainGenerator(44444);

    private boolean removeBlockRequested = false;

    private boolean breakBlockRequested = false;

    private boolean placeBlockRequested = false;

    private Texture atlasTexture;
    private Shader worldShader;
    private WorldRenderer worldRenderer;
    private BlockOutlineRenderer outlineRenderer;
    private UiRenderer uiRenderer;

    private RaycastResult currentRaycast;


    private int framebufferWidth = 1280;
    private int framebufferHeight = 720;

    private final World world = new World();

    private final SaveManager saveManager =
            new SaveManager("World1");

    private final Map<Chunk, ChunkRenderData> chunkMeshes =
            new HashMap<>();

    private static final int RENDER_DISTANCE = 4;

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
                            key == GLFW_KEY_R &&
                                    action == GLFW_PRESS
                    ) {
                        removeBlockRequested = true;
                    }

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
        );

        glfwSetMouseButtonCallback(
                window,
                (windowHandle, button, action, mods) -> {
                    if (
                            button == GLFW_MOUSE_BUTTON_LEFT &&
                                    action == GLFW_PRESS
                    ) {
                        breakBlockRequested = true;
                    }

                    if (
                            button == GLFW_MOUSE_BUTTON_RIGHT &&
                                    action == GLFW_PRESS
                    ) {
                        placeBlockRequested = true;
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

                    camera.rotate(
                            horizontalOffset,
                            verticalOffset
                    );
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
                0.35f,
                0.65f,
                0.90f,
                1.0f
        );

        loadPlayer();

        updateLoadedChunks();

        createShaders();

        outlineRenderer = new BlockOutlineRenderer();

        atlasTexture = new Texture(
                "src/main/resources/textures/block_atlas.png"
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

        System.out.println(
                "Player entered chunk: " +
                        playerChunkX + ", " +
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
        for (
                int chunkX =
                playerChunkX - RENDER_DISTANCE;
                chunkX <=
                        playerChunkX + RENDER_DISTANCE;
                chunkX++
        ) {
            for (
                    int chunkZ =
                    playerChunkZ - RENDER_DISTANCE;
                    chunkZ <=
                            playerChunkZ + RENDER_DISTANCE;
                    chunkZ++
            ) {
                for (
                        int chunkY =
                        WorldGenerationSettings.MIN_CHUNK_Y;
                        chunkY <=
                                WorldGenerationSettings.MAX_CHUNK_Y;
                        chunkY++
                ) {
                    Chunk chunk =
                            world.getChunk(
                                    chunkX,
                                    chunkY,
                                    chunkZ
                            );

                    if (chunk == null) {
                        chunk =
                                world.getOrCreateChunk(
                                        chunkX,
                                        chunkY,
                                        chunkZ
                                );
                    }

                    /*
                     * A structure from another chunk may have already
                     * caused this chunk object to be created.
                     *
                     * Therefore, existence alone does not prove that
                     * the chunk was properly loaded or generated.
                     */
                    if (chunk.isGenerated()) {
                        continue;
                    }

                    boolean loadedFromDisk =
                            saveManager.loadChunk(
                                    chunk
                            );

                    if (loadedFromDisk) {
                        chunk.setGenerated(true);

                        continue;
                    }

                    terrainGenerator.generateChunk(
                            world,
                            chunk
                    );

                    System.out.println(
                            "Generated chunk: " +
                                    chunkX + ", " +
                                    chunkY + ", " +
                                    chunkZ
                    );
                }
            }
        }

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

            if (
                    distanceX > RENDER_DISTANCE ||
                            distanceZ > RENDER_DISTANCE
            ) {
                chunksToUnload.add(chunk);
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

            System.out.println(
                    "Unloaded chunk: " +
                            chunk.getChunkX() + ", " +
                            chunk.getChunkZ()
            );
        }

        /*
         * Rebuild the active area.
         *
         * This ensures faces along newly loaded or unloaded
         * chunk borders are updated correctly.
         */
        rebuildAllChunkMeshes();
    }


    private void rebuildAllChunkMeshes() {
        /*
         * Destroy every old GPU mesh.
         */
        for (ChunkRenderData renderData : chunkMeshes.values()) {
            renderData.destroy();
        }

        chunkMeshes.clear();

        /*
         * Build one mesh for every loaded chunk.
         */
        for (Chunk chunk : world.getChunks()) {
            ChunkRenderData renderData =
                    chunkMeshBuilder.build(
                            world,
                            chunk
                    );

            chunkMeshes.put(chunk, renderData);
        }

        System.out.println(
                "Loaded chunks: " +
                        world.getChunkCount()
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

    private void rebuildChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        System.out.println(
                "Rebuilding chunk: " +
                        chunkX + ", " +
                        chunkY + ", " +
                        chunkZ
        );
        Chunk chunk =
                world.getChunk(
                        chunkX,
                        chunkY,
                        chunkZ
                );

        if (chunk == null) {
            return;
        }

        ChunkRenderData oldMesh =
                chunkMeshes.remove(chunk);

        if (oldMesh != null) {
            oldMesh.destroy();
        }

        if (chunk.isEmpty()) {
            return;
        }

        ChunkRenderData newMesh =
                chunkMeshBuilder.build(
                        world,
                        chunk
                );

        chunkMeshes.put(
                chunk,
                newMesh
        );
    }



    private void createShaders() {
        String vertexShaderSource = """
        #version 330 core

        layout (location = 0) in vec3 position;
        layout (location = 1) in vec2 textureCoordinate;
        layout (location = 2) in float ambientOcclusion;
        layout (location = 3) in float material;
        layout (location = 4) in float bendWeight;

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
        
        uniform sampler2D blockTexture;
        uniform vec3 cameraPosition;
        uniform vec3 fogColor;
        uniform float fogStart;
        uniform float fogEnd;

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
        
            vec3 litColor =
                    textureColor.rgb *
                    brightness *
                    fragmentAO;
        
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

        System.out.println(
                "Breaking block at: " +
                        blockX + ", " +
                        blockY + ", " +
                        blockZ
        );

        world.setBlock(
                blockX,
                blockY,
                blockZ,
                null
        );

        rebuildChunksAroundBlock(
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

        world.setBlock(
                placementX,
                placementY,
                placementZ,
                selectedBlock
        );

        rebuildChunksAroundBlock(
                placementX,
                placementY,
                placementZ
        );

        currentRaycast = calculateRaycast();
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


    private void gameLoop() {
        while (!glfwWindowShouldClose(window)) {
            float currentFrameTime = (float) glfwGetTime();

            deltaTime =
                    currentFrameTime -
                            previousFrameTime;

            previousFrameTime = currentFrameTime;

            processInput();
            camera.updatePhysics(world, deltaTime);
            currentRaycast = calculateRaycast();
            updateLoadedChunks();
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

                rebuildChunksAroundBlock(
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

            glClear(
                    GL_COLOR_BUFFER_BIT |
                            GL_DEPTH_BUFFER_BIT
            );

            /*
             * The cube now remains still at the world origin.
             */



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
        savePlayer();
        saveDirtyChunks();

        if (worldShader != null) {
            worldShader.destroy();
        }

        for (ChunkRenderData renderData : chunkMeshes.values()) {
            renderData.destroy();
        }

        chunkMeshes.clear();

        atlasTexture.destroy();

        if (uiRenderer != null) {
            uiRenderer.cleanup();
        }
        glfwDestroyWindow(window);
        glfwTerminate();

        if (outlineRenderer != null) {
            outlineRenderer.cleanup();
        }

        GLFWErrorCallback callback = glfwSetErrorCallback(null);

        if (callback != null) {
            callback.free();
        }
    }
}