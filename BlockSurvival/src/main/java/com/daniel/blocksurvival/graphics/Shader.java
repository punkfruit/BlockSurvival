package com.daniel.blocksurvival.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class Shader {

    private final int programId;

    private final Map<String, Integer> uniformLocations =
            new HashMap<>();

    public Shader(
            String vertexSource,
            String fragmentSource
    ) {
        int vertexShaderId =
                compileShader(
                        GL_VERTEX_SHADER,
                        vertexSource
                );

        int fragmentShaderId =
                compileShader(
                        GL_FRAGMENT_SHADER,
                        fragmentSource
                );

        programId =
                glCreateProgram();

        glAttachShader(
                programId,
                vertexShaderId
        );

        glAttachShader(
                programId,
                fragmentShaderId
        );

        glLinkProgram(programId);

        if (
                glGetProgrami(
                        programId,
                        GL_LINK_STATUS
                ) == GL_FALSE
        ) {
            String error =
                    glGetProgramInfoLog(
                            programId
                    );

            throw new RuntimeException(
                    "Could not link shader program:\n" +
                            error
            );
        }

        glDetachShader(
                programId,
                vertexShaderId
        );

        glDetachShader(
                programId,
                fragmentShaderId
        );

        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }

    private int compileShader(
            int shaderType,
            String source
    ) {
        int shaderId =
                glCreateShader(shaderType);

        glShaderSource(
                shaderId,
                source
        );

        glCompileShader(shaderId);

        if (
                glGetShaderi(
                        shaderId,
                        GL_COMPILE_STATUS
                ) == GL_FALSE
        ) {
            String error =
                    glGetShaderInfoLog(
                            shaderId
                    );

            throw new RuntimeException(
                    "Could not compile shader:\n" +
                            error
            );
        }

        return shaderId;
    }

    private int getUniformLocation(
            String name
    ) {
        Integer cachedLocation =
                uniformLocations.get(name);

        if (cachedLocation != null) {
            return cachedLocation;
        }

        int location =
                glGetUniformLocation(
                        programId,
                        name
                );

        if (location == -1) {
            throw new RuntimeException(
                    "Unknown shader uniform: " +
                            name
            );
        }

        uniformLocations.put(
                name,
                location
        );

        return location;
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void setMatrix4(
            String name,
            Matrix4f matrix
    ) {
        try (
                MemoryStack stack =
                        MemoryStack.stackPush()
        ) {
            FloatBuffer buffer =
                    stack.mallocFloat(16);

            matrix.get(buffer);

            glUniformMatrix4fv(
                    getUniformLocation(name),
                    false,
                    buffer
            );
        }
    }

    public void setVector3(
            String name,
            Vector3f value
    ) {
        glUniform3f(
                getUniformLocation(name),
                value.x,
                value.y,
                value.z
        );
    }

    public void setVector4(
            String name,
            Vector4f value
    ) {
        glUniform4f(
                getUniformLocation(name),
                value.x,
                value.y,
                value.z,
                value.w
        );
    }

    public void setFloat(
            String name,
            float value
    ) {
        glUniform1f(
                getUniformLocation(name),
                value
        );
    }

    public void setInt(
            String name,
            int value
    ) {
        glUniform1i(
                getUniformLocation(name),
                value
        );
    }

    public void destroy() {
        glDeleteProgram(programId);
    }
}