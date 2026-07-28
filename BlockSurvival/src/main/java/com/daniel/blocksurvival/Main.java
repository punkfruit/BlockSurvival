package com.daniel.blocksurvival;

import java.nio.ByteBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import org.lwjgl.system.MemoryStack;

public class Main {

    private long window;

    private int vaoId;
    private int vboId;
    private int eboId;
    private int textureId;
    private int shaderProgramId;
    private int mvpUniformLocation;

    private int atlasOffsetUniformLocation;

    private int framebufferWidth = 1280;
    private int framebufferHeight = 720;

    private final Block[] blocks = createBlocks();

    private final Vector3f cameraPosition =
            new Vector3f(0.0f, 1.5f, 5.0f);

    private final Vector3f cameraFront =
            new Vector3f(0.0f, 0.0f, -1.0f);

    private final Vector3f cameraUp =
            new Vector3f(0.0f, 1.0f, 0.0f);

    private float yaw = -90.0f;
    private float pitch = 0.0f;

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

        glfwSetKeyCallback(window, (windowHandle, key, scanCode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(windowHandle, true);
            }
        });

        glfwSetCursorPosCallback(window, (windowHandle, mouseX, mouseY) -> {
            if (firstMouseMovement) {
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                firstMouseMovement = false;
            }

            float xOffset = (float) (mouseX - lastMouseX);
            float yOffset = (float) (lastMouseY - mouseY);

            lastMouseX = mouseX;
            lastMouseY = mouseY;

            float sensitivity = 0.1f;

            xOffset *= sensitivity;
            yOffset *= sensitivity;

            yaw += xOffset;
            pitch += yOffset;

            /*
             * Prevent the camera from flipping upside down.
             */
            if (pitch > 89.0f) {
                pitch = 89.0f;
            }

            if (pitch < -89.0f) {
                pitch = -89.0f;
            }

            Vector3f newDirection = new Vector3f();

            newDirection.x =
                    (float) (
                            Math.cos(Math.toRadians(yaw)) *
                                    Math.cos(Math.toRadians(pitch))
                    );

            newDirection.y =
                    (float) Math.sin(Math.toRadians(pitch));

            newDirection.z =
                    (float) (
                            Math.sin(Math.toRadians(yaw)) *
                                    Math.cos(Math.toRadians(pitch))
                    );

            cameraFront.set(newDirection).normalize();
        });

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

        createCube();
        createShaders();

        textureId = loadTexture(
                "src/main/resources/textures/block_atlas.png"
        );
    }

    private void createCube() {
        /*
         * Each vertex contains:
         *
         * x, y, z position
         * u, v texture coordinate
         */
        float[] vertices = {
                // FRONT FACE
                // Position                 // UV
                -0.5f,  0.5f,  0.5f,        0.0f, 1.0f,
                -0.5f, -0.5f,  0.5f,        0.0f, 0.0f,
                0.5f, -0.5f,  0.5f,        1.0f, 0.0f,
                0.5f,  0.5f,  0.5f,        1.0f, 1.0f,

                // BACK FACE
                0.5f,  0.5f, -0.5f,        0.0f, 1.0f,
                0.5f, -0.5f, -0.5f,        0.0f, 0.0f,
                -0.5f, -0.5f, -0.5f,        1.0f, 0.0f,
                -0.5f,  0.5f, -0.5f,        1.0f, 1.0f,

                // LEFT FACE
                -0.5f,  0.5f, -0.5f,        0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f,        0.0f, 0.0f,
                -0.5f, -0.5f,  0.5f,        1.0f, 0.0f,
                -0.5f,  0.5f,  0.5f,        1.0f, 1.0f,

                // RIGHT FACE
                0.5f,  0.5f,  0.5f,        0.0f, 1.0f,
                0.5f, -0.5f,  0.5f,        0.0f, 0.0f,
                0.5f, -0.5f, -0.5f,        1.0f, 0.0f,
                0.5f,  0.5f, -0.5f,        1.0f, 1.0f,

                // TOP FACE
                -0.5f,  0.5f, -0.5f,        0.0f, 1.0f,
                -0.5f,  0.5f,  0.5f,        0.0f, 0.0f,
                0.5f,  0.5f,  0.5f,        1.0f, 0.0f,
                0.5f,  0.5f, -0.5f,        1.0f, 1.0f,

                // BOTTOM FACE
                -0.5f, -0.5f,  0.5f,        0.0f, 1.0f,
                -0.5f, -0.5f, -0.5f,        0.0f, 0.0f,
                0.5f, -0.5f, -0.5f,        1.0f, 0.0f,
                0.5f, -0.5f,  0.5f,        1.0f, 1.0f
        };

        int[] indices = {
                0,  1,  2,   2,  3,  0,
                4,  5,  6,   6,  7,  4,
                8,  9, 10,  10, 11,  8,
                12, 13, 14,  14, 15, 12,
                16, 17, 18,  18, 19, 16,
                20, 21, 22,  22, 23, 20
        };

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        /*
         * Each vertex contains five floats:
         *
         * x, y, z, u, v
         */
        int numbersPerVertex = 5;
        int stride = numbersPerVertex * Float.BYTES;

        /*
         * Attribute 0: XYZ position
         */
        glVertexAttribPointer(
                0,
                3,
                GL_FLOAT,
                false,
                stride,
                0
        );

        glEnableVertexAttribArray(0);

        /*
         * Attribute 1: UV texture coordinates
         */
        glVertexAttribPointer(
                1,
                2,
                GL_FLOAT,
                false,
                stride,
                3L * Float.BYTES
        );

        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private int loadTexture(String filePath) {
        /*
         * OpenGL considers the bottom-left the image origin.
         * Most image formats consider the top-left the origin.
         */
        stbi_set_flip_vertically_on_load(true);

        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer imageData = stbi_load(
                    filePath,
                    width,
                    height,
                    channels,
                    4
            );

            if (imageData == null) {
                throw new RuntimeException(
                        "Could not load texture: "
                                + filePath
                                + "\nReason: "
                                + stbi_failure_reason()
                );
            }

            int newTextureId = glGenTextures();

            glBindTexture(GL_TEXTURE_2D, newTextureId);

            /*
             * Use nearest-neighbor filtering so pixel art
             * remains crisp rather than becoming blurry.
             */
            glTexParameteri(
                    GL_TEXTURE_2D,
                    GL_TEXTURE_MIN_FILTER,
                    GL_NEAREST
            );

            glTexParameteri(
                    GL_TEXTURE_2D,
                    GL_TEXTURE_MAG_FILTER,
                    GL_NEAREST
            );

            /*
             * Repeat the image if UV values leave the 0–1 range.
             */
            glTexParameteri(
                    GL_TEXTURE_2D,
                    GL_TEXTURE_WRAP_S,
                    GL_REPEAT
            );

            glTexParameteri(
                    GL_TEXTURE_2D,
                    GL_TEXTURE_WRAP_T,
                    GL_REPEAT
            );

            glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGBA8,
                    width.get(0),
                    height.get(0),
                    0,
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    imageData
            );

            glGenerateMipmap(GL_TEXTURE_2D);

            stbi_image_free(imageData);

            glBindTexture(GL_TEXTURE_2D, 0);

            return newTextureId;
        }
    }

    private void createShaders() {
        String vertexShaderSource = """
        #version 330 core

        layout (location = 0) in vec3 position;
        layout (location = 1) in vec2 textureCoordinate;

        uniform mat4 mvpMatrix;

        out vec2 fragmentTextureCoordinate;

        void main() {
            gl_Position = mvpMatrix * vec4(position, 1.0);

            fragmentTextureCoordinate =
                    textureCoordinate;
        }
        """;

        String fragmentShaderSource = """
        #version 330 core

        in vec2 fragmentTextureCoordinate;

        uniform sampler2D blockTexture;
        uniform vec2 atlasOffset;

        out vec4 finalColor;

        void main() {
            /*
             * The atlas is a 2 × 2 grid.
             *
             * Multiplying by 0.5 shrinks the UV coordinates
             * so they cover only one quarter of the image.
             */
            vec2 atlasCoordinate =
                    fragmentTextureCoordinate * 0.5
                    + atlasOffset;

            finalColor = texture(
                    blockTexture,
                    atlasCoordinate
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

        atlasOffsetUniformLocation = glGetUniformLocation(
                shaderProgramId,
                "atlasOffset"
        );

        if (atlasOffsetUniformLocation == -1) {
            throw new RuntimeException(
                    "Could not find the atlasOffset shader uniform."
            );
        }

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
        float cameraSpeed = 3.0f * deltaTime;

        /*
         * Forward.
         */
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            cameraPosition.add(
                    new Vector3f(cameraFront).mul(cameraSpeed)
            );
        }

        /*
         * Backward.
         */
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            cameraPosition.sub(
                    new Vector3f(cameraFront).mul(cameraSpeed)
            );
        }

        /*
         * Calculate the direction pointing to the camera's right.
         */
        Vector3f cameraRight = new Vector3f(cameraFront)
                .cross(cameraUp)
                .normalize();

        /*
         * Move left.
         */
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            cameraPosition.sub(
                    new Vector3f(cameraRight).mul(cameraSpeed)
            );
        }

        /*
         * Move right.
         */
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            cameraPosition.add(
                    new Vector3f(cameraRight).mul(cameraSpeed)
            );
        }

        /*
         * Move upward.
         */
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            cameraPosition.y += cameraSpeed;
        }

        /*
         * Move downward.
         */
        if (
                glfwGetKey(window, GLFW_KEY_LEFT_SHIFT)
                        == GLFW_PRESS
        ) {
            cameraPosition.y -= cameraSpeed;
        }
    }

    private Block[] createBlocks() {
        int worldSize = 7;
        int floorBlocks = worldSize * worldSize;
        int pillarBlocks = 4;

        Block[] blocks =
                new Block[floorBlocks + pillarBlocks];

        int index = 0;

        /*
         * Create the floor.
         */
        for (int x = 0; x < worldSize; x++) {
            for (int z = 0; z < worldSize; z++) {

                /*
                 * Make a simple pattern:
                 *
                 * Most blocks are grass.
                 * The outer edge is sand.
                 */
                boolean isEdge =
                        x == 0 ||
                                z == 0 ||
                                x == worldSize - 1 ||
                                z == worldSize - 1;

                int textureIndex;

                if (isEdge) {
                    textureIndex = 3; // Sand
                } else {
                    textureIndex = 0; // Grass
                }

                blocks[index] = new Block(
                        new Vector3f(
                                x - worldSize / 2,
                                -1.0f,
                                z - worldSize / 2
                        ),
                        textureIndex
                );

                index++;
            }
        }

        /*
         * Create a pillar.
         *
         * Bottom two blocks are stone.
         * Top two blocks are dirt.
         */
        for (int y = 0; y < pillarBlocks; y++) {
            int textureIndex;

            if (y < 2) {
                textureIndex = 2; // Stone
            } else {
                textureIndex = 1; // Dirt
            }

            blocks[index] = new Block(
                    new Vector3f(
                            1.0f,
                            y,
                            0.0f
                    ),
                    textureIndex
            );

            index++;
        }

        return blocks;
    }

    private static class Block {
        private final Vector3f position;
        private final int textureIndex;

        private Block(
                Vector3f position,
                int textureIndex
        ) {
            this.position = position;
            this.textureIndex = textureIndex;
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
            Vector3f cameraTarget = new Vector3f(cameraPosition)
                    .add(cameraFront);

            Matrix4f viewMatrix = new Matrix4f()
                    .lookAt(
                            cameraPosition,
                            cameraTarget,
                            cameraUp
                    );

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
                            100.0f
                    );

            /*
             * Combine the three transformations.
             *
             * The multiplication order matters.
             */
            glUseProgram(shaderProgramId);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureId);

            glBindVertexArray(vaoId);


            try (MemoryStack stack = stackPush()) {
                FloatBuffer matrixBuffer = stack.mallocFloat(16);

                for (Vector3f blockPosition : blockPositions) {
                    /*
                     * Move this copy of the cube to its block position.
                     */
                    Matrix4f modelMatrix = new Matrix4f()
                            .translate(blockPosition);

                    /*
                     * Combine projection, camera, and block position.
                     */
                    Matrix4f mvpMatrix =
                            new Matrix4f(projectionMatrix)
                                    .mul(viewMatrix)
                                    .mul(modelMatrix);

                    /*
                     * Reuse the same buffer for each block.
                     */
                    matrixBuffer.clear();
                    mvpMatrix.get(matrixBuffer);

                    glUniformMatrix4fv(
                            mvpUniformLocation,
                            false,
                            matrixBuffer
                    );

                    glDrawElements(
                            GL_TRIANGLES,
                            36,
                            GL_UNSIGNED_INT,
                            0
                    );
                }
            }

            glBindVertexArray(0);
            glUseProgram(0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        glDeleteProgram(shaderProgramId);

        glDeleteBuffers(eboId);
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);

        glDeleteTextures(textureId);

        glfwDestroyWindow(window);
        glfwTerminate();

        GLFWErrorCallback callback = glfwSetErrorCallback(null);

        if (callback != null) {
            callback.free();
        }
    }
}