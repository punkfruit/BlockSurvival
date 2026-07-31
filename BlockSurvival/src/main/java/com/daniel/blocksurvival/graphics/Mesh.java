package com.daniel.blocksurvival.graphics;

import static org.lwjgl.opengl.GL33.*;

public class Mesh {

    private final int vao;
    private final int vbo;
    private final int ebo;

    private final int indexCount;

    public Mesh(float[] vertices, int[] indices) {

        indexCount = indices.length;

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(
                GL_ARRAY_BUFFER,
                vertices,
                GL_STATIC_DRAW
        );

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(
                GL_ELEMENT_ARRAY_BUFFER,
                indices,
                GL_STATIC_DRAW
        );

        glVertexAttribPointer(
                0,
                3,
                GL_FLOAT,
                false,
                8 * Float.BYTES,
                0
        );

        glEnableVertexAttribArray(0);

        glVertexAttribPointer(
                1,
                2,
                GL_FLOAT,
                false,
                8 * Float.BYTES,
                3 * Float.BYTES
        );

        glEnableVertexAttribArray(1);

        glVertexAttribPointer(
                2,
                1,
                GL_FLOAT,
                false,
                8 * Float.BYTES,
                5 * Float.BYTES
        );

        glEnableVertexAttribArray(2);

        glVertexAttribPointer(
                3,
                1,
                GL_FLOAT,
                false,
                8 * Float.BYTES,
                6 * Float.BYTES
        );

        glEnableVertexAttribArray(3);

        glVertexAttribPointer(
                4,
                1,
                GL_FLOAT,
                false,
                8 * Float.BYTES,
                7 * Float.BYTES
        );

        glEnableVertexAttribArray(4);

        glBindVertexArray(0);
    }

    public void render() {

        glBindVertexArray(vao);

        glDrawElements(
                GL_TRIANGLES,
                indexCount,
                GL_UNSIGNED_INT,
                0
        );

        glBindVertexArray(0);
    }

    public void destroy() {

        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }
}