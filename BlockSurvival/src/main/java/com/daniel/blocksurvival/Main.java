package com.daniel.blocksurvival;

import com.daniel.blocksurvival.graphics.BlockOutlineRenderer;
import com.daniel.blocksurvival.world.BlockType;
import com.daniel.blocksurvival.world.World;
import com.daniel.blocksurvival.graphics.Mesh;
import com.daniel.blocksurvival.graphics.ChunkMeshBuilder;
import com.daniel.blocksurvival.world.ChunkManager;
import com.daniel.blocksurvival.world.TerrainGenerator;
import com.daniel.blocksurvival.world.RaycastResult;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

import com.daniel.blocksurvival.world.Chunk;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import com.daniel.blocksurvival.graphics.Texture;

import org.lwjgl.system.MemoryStack;

public class Main {

    private long window;


    private final ChunkMeshBuilder chunkMeshBuilder =
            new ChunkMeshBuilder();

    private final TerrainGenerator terrainGenerator =
            new TerrainGenerator(33333);

    private boolean removeBlockRequested = false;

    private boolean breakBlockRequested = false;

    private boolean placeBlockRequested = false;

    private Texture atlasTexture;
    private int shaderProgramId;
    private int mvpUniformLocation;

    private BlockOutlineRenderer outlineRenderer;

    private RaycastResult currentRaycast;


    private int framebufferWidth = 1280;
    private int framebufferHeight = 720;

    private final World world = new World();

    private final Map<Chunk, Mesh> chunkMeshes =
            new HashMap<>();

    private static final int RENDER_DISTANCE = 2;

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

        updateLoadedChunks();

        createShaders();

        outlineRenderer = new BlockOutlineRenderer();

        atlasTexture = new Texture(
                "src/main/resources/textures/block_atlas.png"
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
                Chunk chunk =
                        world.getChunk(
                                chunkX,
                                0,
                                chunkZ
                        );

                if (chunk == null) {
                    chunk =
                            world.getOrCreateChunk(
                                    chunkX,
                                    0,
                                    chunkZ
                            );

                    terrainGenerator.generateChunk(
                            world,
                            chunk
                    );

                    System.out.println(
                            "Generated chunk: " +
                                    chunkX + ", " +
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
            Mesh mesh =
                    chunkMeshes.remove(chunk);

            if (mesh != null) {
                mesh.destroy();
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
        for (Mesh mesh : chunkMeshes.values()) {
            mesh.destroy();
        }

        chunkMeshes.clear();

        /*
         * Build one mesh for every loaded chunk.
         */
        for (Chunk chunk : world.getChunks()) {
            Mesh mesh =
                    chunkMeshBuilder.build(
                            world,
                            chunk
                    );

            chunkMeshes.put(chunk, mesh);
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

        Mesh oldMesh =
                chunkMeshes.remove(chunk);

        if (oldMesh != null) {
            oldMesh.destroy();
        }

        if (chunk.isEmpty()) {
            return;
        }

        Mesh newMesh =
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

        uniform mat4 mvpMatrix;

        out vec2 fragmentTextureCoordinate;
        out vec3 fragmentWorldPosition;
        out float fragmentAO;

        void main() {
            gl_Position =
                    mvpMatrix *
                    vec4(position, 1.0);

            fragmentTextureCoordinate =
                    textureCoordinate;
            
            fragmentAO = ambientOcclusion;

            fragmentWorldPosition =
                    position;
        }
        """;

        String fragmentShaderSource = """
        #version 330 core

        in vec2 fragmentTextureCoordinate;
        in vec3 fragmentWorldPosition;
        in float fragmentAO;

        uniform sampler2D blockTexture;

        out vec4 finalColor;

        void main() {
            vec4 textureColor =
                    texture(
                            blockTexture,
                            fragmentTextureCoordinate
                    );

            if (textureColor.a < 0.5) {
                discard;
            }

            /*
             * Calculate the direction the current face points.
             *
             * dFdx and dFdy measure how the world position changes
             * across neighboring pixels.
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

            /*
             * Cross-model plants are visible from both sides.
             * Flip the normal when viewing the back side so it
             * receives sensible lighting too.
             */
            if (!gl_FrontFacing) {
                normal = -normal;
            }

            /*
             * Direction pointing toward the sun.
             */
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

            /*
             * Ambient light prevents faces pointing away from
             * the sun from becoming completely black.
             */
            float ambientLight = 0.45;

            float brightness =
                    ambientLight +
                    sunlight * 0.55;

            finalColor =
                    vec4(
                            textureColor.rgb *
                            brightness *
                            fragmentAO,
                            textureColor.a
                    );
        }
        """;

        int vertexShaderId = compileShader(
                GL_VERTEX_SHADER,
                vertexShaderSource
        );

        int fragmentShaderId = compileShader(
                GL_FRAGMENT_SHADER,
                fragmentShaderSource
        );

        shaderProgramId = glCreateProgram();

        glAttachShader(shaderProgramId, vertexShaderId);
        glAttachShader(shaderProgramId, fragmentShaderId);

        glLinkProgram(shaderProgramId);

        if (glGetProgrami(shaderProgramId, GL_LINK_STATUS) == GL_FALSE) {
            String error = glGetProgramInfoLog(shaderProgramId);

            throw new RuntimeException(
                    "Could not link shader program:\n" + error
            );
        }

        /*
         * The finished program contains copies of the compiled shaders,
         * so the individual shader objects can now be deleted.
         */
        glDetachShader(shaderProgramId, vertexShaderId);
        glDetachShader(shaderProgramId, fragmentShaderId);

        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
        mvpUniformLocation = glGetUniformLocation(
                shaderProgramId,
                "mvpMatrix"
        );



        int textureUniformLocation = glGetUniformLocation(
                shaderProgramId,
                "blockTexture"
        );

        if (textureUniformLocation == -1) {
            throw new RuntimeException(
                    "Could not find the blockTexture shader uniform."
            );
        }

        glUseProgram(shaderProgramId);

        /*
         * Texture unit zero will contain our block texture.
         */
        glUniform1i(textureUniformLocation, 0);

        glUseProgram(0);

        if (mvpUniformLocation == -1) {
            throw new RuntimeException(
                    "Could not find the mvpMatrix shader uniform."
            );
        }
    }

    private int compileShader(int shaderType, String source) {
        int shaderId = glCreateShader(shaderType);

        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(shaderId);

            throw new RuntimeException(
                    "Could not compile shader:\n" + error
            );
        }

        return shaderId;
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
                "Placing block at: " +
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

        world.setBlock(
                placementX,
                placementY,
                placementZ,
                BlockType.GRASS
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
            glUseProgram(shaderProgramId);

            glActiveTexture(GL_TEXTURE0);
            atlasTexture.bind();

            Matrix4f mvpMatrix =
                    new Matrix4f(projectionMatrix)
                            .mul(viewMatrix);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer matrixBuffer =
                        stack.mallocFloat(16);

                mvpMatrix.get(matrixBuffer);

                glUniformMatrix4fv(
                        mvpUniformLocation,
                        false,
                        matrixBuffer
                );
            }

            for (Mesh mesh : chunkMeshes.values()) {
                mesh.render();
            }

            atlasTexture.unbind();
            glUseProgram(0);

            if (currentRaycast != null) {
                outlineRenderer.render(
                        currentRaycast.hitX(),
                        currentRaycast.hitY(),
                        currentRaycast.hitZ(),
                        projectionMatrix,
                        viewMatrix
                );
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        glDeleteProgram(shaderProgramId);

        for (Mesh mesh : chunkMeshes.values()) {
            mesh.destroy();
        }

        chunkMeshes.clear();

        atlasTexture.destroy();

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