package com.daniel.blocksurvival.graphics;

import org.joml.Matrix4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.*;

public class BlockOutlineRenderer {

    private final int vao;
    private final int vbo;
    private final int ebo;

    private final int shaderProgram;

    private final int projectionLocation;
    private final int viewLocation;
    private final int blockPositionLocation;

    public BlockOutlineRenderer() {

        /*
         * Eight corners of a cube centered at 0, 0, 0.
         *
         * The cube is slightly larger than one block,
         * which prevents it from flickering against
         * the block surface.
         */
        float size = .502f;

        float[] vertices = {
                // Bottom four corners
                -size, -size, -size, // 0
                size, -size, -size, // 1
                size, -size,  size, // 2
                -size, -size,  size, // 3

                // Top four corners
                -size,  size, -size, // 4
                size,  size, -size, // 5
                size,  size,  size, // 6
                -size,  size,  size  // 7
        };

        /*
         * Each pair of indices creates one line.
         *
         * 4 bottom edges
         * 4 top edges
         * 4 vertical edges
         */
        int[] indices = {
                // Bottom square
                0, 1,
                1, 2,
                2, 3,
                3, 0,

                // Top square
                4, 5,
                5, 6,
                6, 7,
                7, 4,

                // Vertical edges
                0, 4,
                1, 5,
                2, 6,
                3, 7
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        FloatBuffer vertexBuffer =
                memAllocFloat(vertices.length);

        vertexBuffer.put(vertices).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(
                GL_ARRAY_BUFFER,
                vertexBuffer,
                GL_STATIC_DRAW
        );

        memFree(vertexBuffer);

        IntBuffer indexBuffer =
                memAllocInt(indices.length);

        indexBuffer.put(indices).flip();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(
                GL_ELEMENT_ARRAY_BUFFER,
                indexBuffer,
                GL_STATIC_DRAW
        );

        memFree(indexBuffer);

        /*
         * Position attribute:
         *
         * location 0
         * three floats per vertex
         */
        glVertexAttribPointer(
                0,
                3,
                GL_FLOAT,
                false,
                3 * Float.BYTES,
                0
        );

        glEnableVertexAttribArray(0);

        glBindVertexArray(0);

        shaderProgram = createShaderProgram();

        projectionLocation =
                glGetUniformLocation(
                        shaderProgram,
                        "projection"
                );

        viewLocation =
                glGetUniformLocation(
                        shaderProgram,
                        "view"
                );

        blockPositionLocation =
                glGetUniformLocation(
                        shaderProgram,
                        "blockPosition"
                );
    }

    public void render(
            int blockX,
            int blockY,
            int blockZ,
            Matrix4f projection,
            Matrix4f view
    ) {
        glUseProgram(shaderProgram);

        uploadMatrix(
                projectionLocation,
                projection
        );

        uploadMatrix(
                viewLocation,
                view
        );

        /*
         * Your blocks are centered directly on their
         * integer world coordinates, so the outline
         * moves directly to blockX, blockY, blockZ.
         */
        glUniform3f(
                blockPositionLocation,
                blockX,
                blockY,
                blockZ
        );

        /*
         * Keep depth testing enabled so lines behind
         * the block remain hidden.
         */
        glEnable(GL_DEPTH_TEST);

        glBindVertexArray(vao);

        glDrawElements(
                GL_LINES,
                24,
                GL_UNSIGNED_INT,
                0
        );



        glBindVertexArray(0);
        glUseProgram(0);
    }

    private void uploadMatrix(
            int location,
            Matrix4f matrix
    ) {
        FloatBuffer buffer =
                memAllocFloat(16);

        matrix.get(buffer);

        glUniformMatrix4fv(
                location,
                false,
                buffer
        );

        memFree(buffer);
    }

    private int createShaderProgram() {

        String vertexShaderSource = """
                #version 330 core

                layout (location = 0) in vec3 position;

                uniform mat4 projection;
                uniform mat4 view;
                uniform vec3 blockPosition;

                void main() {
                    vec3 worldPosition =
                        position + blockPosition;

                    gl_Position =
                        projection
                        * view
                        * vec4(worldPosition, 1.0);
                }
                """;

        String fragmentShaderSource = """
                #version 330 core

                out vec4 fragmentColor;

                void main() {
                    fragmentColor =
                        vec4(0.0, 0.0, 0.0, 1.0);
                }
                """;

        int vertexShader = compileShader(
                GL_VERTEX_SHADER,
                vertexShaderSource
        );

        int fragmentShader = compileShader(
                GL_FRAGMENT_SHADER,
                fragmentShaderSource
        );

        int program = glCreateProgram();

        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);

        glLinkProgram(program);

        if (glGetProgrami(
                program,
                GL_LINK_STATUS
        ) == GL_FALSE) {
            throw new RuntimeException(
                    "Failed to link block outline shader:\n"
                            + glGetProgramInfoLog(program)
            );
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private int compileShader(
            int shaderType,
            String source
    ) {
        int shader = glCreateShader(shaderType);

        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(
                shader,
                GL_COMPILE_STATUS
        ) == GL_FALSE) {
            throw new RuntimeException(
                    "Failed to compile block outline shader:\n"
                            + glGetShaderInfoLog(shader)
            );
        }

        return shader;
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteProgram(shaderProgram);
    }
}