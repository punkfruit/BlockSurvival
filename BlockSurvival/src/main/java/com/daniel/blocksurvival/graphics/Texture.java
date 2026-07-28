package com.daniel.blocksurvival.graphics;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture {

    private final int id;

    public Texture(String filePath) {
        id = load(filePath);
    }

    private int load(String filePath) {

        stbi_set_flip_vertically_on_load(true);

        try (MemoryStack stack = MemoryStack.stackPush()) {

            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer image = stbi_load(
                    filePath,
                    width,
                    height,
                    channels,
                    4
            );

            if (image == null) {
                throw new RuntimeException(
                        "Could not load texture:\n"
                                + filePath
                                + "\n"
                                + stbi_failure_reason()
                );
            }

            int texture = glGenTextures();

            glBindTexture(GL_TEXTURE_2D, texture);

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
                    image
            );

            glGenerateMipmap(GL_TEXTURE_2D);

            stbi_image_free(image);

            glBindTexture(GL_TEXTURE_2D, 0);

            return texture;
        }
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void destroy() {
        glDeleteTextures(id);
    }
}