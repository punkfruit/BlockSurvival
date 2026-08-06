package com.daniel.blocksurvival.graphics;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;

public class TextRenderer {

    private static final int ATLAS_COLUMNS =
            26;

    private static final int ATLAS_ROWS =
            3;

    /*
     * Every character lives inside a 16×16 tile.
     *
     * The normalized UV dimensions are based on the number
     * of columns and rows rather than the raw pixel size.
     */
    private static final float GLYPH_U_SIZE =
            1.0f /
                    ATLAS_COLUMNS;

    private static final float GLYPH_V_SIZE =
            1.0f /
                    ATLAS_ROWS;

    /*
     * How far the cursor advances after drawing one glyph.
     *
     * The visible letters do not use all 16 pixels of their
     * tile, so advancing slightly less than a full tile keeps
     * words from looking overly spread apart.
     */
    private static final float CHARACTER_ADVANCE =
            12.0f;

    private static final float SPACE_ADVANCE =
            7.0f;

    private final Texture fontTexture;

    private final Shader shader;

    private final int vertexArrayId;
    private final int vertexBufferId;
    private final int elementBufferId;

    private final Map<Character, Glyph>
            glyphs =
            new HashMap<>();

    public TextRenderer(
            Texture fontTexture
    ) {
        if (fontTexture == null) {
            throw new IllegalArgumentException(
                    "TextRenderer requires a font texture."
            );
        }

        this.fontTexture =
                fontTexture;

        registerGlyphs();

        shader =
                createShader();

        vertexArrayId =
                glGenVertexArrays();

        vertexBufferId =
                glGenBuffers();

        elementBufferId =
                glGenBuffers();

        glBindVertexArray(
                vertexArrayId
        );

        glBindBuffer(
                GL_ARRAY_BUFFER,
                vertexBufferId
        );

        /*
         * Four vertices:
         *
         * x, y, u, v
         */
        glBufferData(
                GL_ARRAY_BUFFER,
                4L *
                        4L *
                        Float.BYTES,
                GL_DYNAMIC_DRAW
        );

        int[] indices = {
                0, 1, 2,
                2, 3, 0
        };

        glBindBuffer(
                GL_ELEMENT_ARRAY_BUFFER,
                elementBufferId
        );

        glBufferData(
                GL_ELEMENT_ARRAY_BUFFER,
                indices,
                GL_STATIC_DRAW
        );

        int stride =
                4 *
                        Float.BYTES;

        /*
         * Screen position.
         */
        glVertexAttribPointer(
                0,
                2,
                GL_FLOAT,
                false,
                stride,
                0
        );

        glEnableVertexAttribArray(
                0
        );

        /*
         * Glyph texture coordinates.
         */
        glVertexAttribPointer(
                1,
                2,
                GL_FLOAT,
                false,
                stride,
                2L *
                        Float.BYTES
        );

        glEnableVertexAttribArray(
                1
        );

        glBindVertexArray(
                0
        );

        glBindBuffer(
                GL_ARRAY_BUFFER,
                0
        );

        shader.bind();

        shader.setInt(
                "fontTexture",
                0
        );

        shader.unbind();
    }

    private void registerGlyphs() {
        /*
         * Row 0: uppercase A–Z.
         */
        for (
                int column = 0;
                column < 26;
                column++
        ) {
            registerGlyph(
                    (char) (
                            'A' +
                                    column
                    ),
                    column,
                    0
            );
        }

        /*
         * Row 1: lowercase a–z.
         */
        for (
                int column = 0;
                column < 26;
                column++
        ) {
            registerGlyph(
                    (char) (
                            'a' +
                                    column
                    ),
                    column,
                    1
            );
        }

        /*
         * Row 2 begins with digits 0–9.
         */
        for (
                int column = 0;
                column < 10;
                column++
        ) {
            registerGlyph(
                    (char) (
                            '0' +
                                    column
                    ),
                    column,
                    2
            );
        }

        /*
         * Punctuation visible in your third row.
         */
        registerGlyph(
                ':',
                10,
                2
        );

        registerGlyph(
                '/',
                11,
                2
        );

        registerGlyph(
                '-',
                12,
                2
        );

        registerGlyph(
                ',',
                13,
                2
        );

        registerGlyph(
                '.',
                14,
                2
        );

        registerGlyph(
                '?',
                15,
                2
        );

        /*
         * Column 16 is your large multiplication-style X.
         *
         * Register it as lowercase x so inventory information
         * can display strings such as "SIZE: 2 x 2".
         *
         * Ordinary uppercase X remains available from row 0.
         */
        registerGlyph(
                '×',
                16,
                2
        );

        registerGlyph(
                '!',
                17,
                2
        );

        registerGlyph(
                '(',
                18,
                2
        );

        registerGlyph(
                ')',
                19,
                2
        );

        registerGlyph(
                '#',
                20,
                2
        );

        registerGlyph(
                '^',
                21,
                2
        );

        registerGlyph(
                '+',
                22,
                2
        );

        registerGlyph(
                '&',
                23,
                2
        );

        registerGlyph(
                '[',
                24,
                2
        );

        registerGlyph(
                ']',
                25,
                2
        );
    }

    private void registerGlyph(
            char character,
            int column,
            int row
    ) {
        float minimumU =
                column *
                        GLYPH_U_SIZE;

        float minimumV =
                row *
                        GLYPH_V_SIZE;

        float maximumU =
                minimumU +
                        GLYPH_U_SIZE;

        float maximumV =
                minimumV +
                        GLYPH_V_SIZE;

        glyphs.put(
                character,
                new Glyph(
                        minimumU,
                        minimumV,
                        maximumU,
                        maximumV
                )
        );
    }

    public void drawText(
            String text,
            float x,
            float y,
            float scale,
            int screenWidth,
            int screenHeight
    ) {
        drawText(
                text,
                x,
                y,
                scale,
                screenWidth,
                screenHeight,
                new Vector4f(
                        1.0f,
                        1.0f,
                        1.0f,
                        1.0f
                ),
                true
        );
    }

    public void drawText(
            String text,
            float x,
            float y,
            float scale,
            int screenWidth,
            int screenHeight,
            Vector4f color,
            boolean drawShadow
    ) {
        if (
                text == null ||
                        text.isEmpty()
        ) {
            return;
        }

        glDisable(
                GL_DEPTH_TEST
        );

        glEnable(
                GL_BLEND
        );

        glBlendFunc(
                GL_SRC_ALPHA,
                GL_ONE_MINUS_SRC_ALPHA
        );

        shader.bind();

        Matrix4f projectionMatrix =
                new Matrix4f()
                        .ortho2D(
                                0.0f,
                                screenWidth,
                                screenHeight,
                                0.0f
                        );

        shader.setMatrix4(
                "projectionMatrix",
                projectionMatrix
        );

        glActiveTexture(
                GL_TEXTURE0
        );

        fontTexture.bind();

        /*
         * Draw the complete string once in dark gray,
         * offset slightly down and right.
         */
        if (drawShadow) {
            drawTextInternal(
                    text,
                    x +
                            scale,
                    y +
                            scale,
                    scale,
                    new Vector4f(
                            0.0f,
                            0.0f,
                            0.0f,
                            color.w *
                                    0.85f
                    )
            );
        }

        drawTextInternal(
                text,
                x,
                y,
                scale,
                color
        );

        fontTexture.unbind();

        shader.unbind();

        glDisable(
                GL_BLEND
        );

        glEnable(
                GL_DEPTH_TEST
        );
    }

    private void drawTextInternal(
            String text,
            float x,
            float y,
            float scale,
            Vector4f color
    ) {
        float cursorX =
                x;

        float cursorY =
                y;

        shader.setVector4(
                "textColor",
                color
        );

        for (
                int index = 0;
                index < text.length();
                index++
        ) {
            char character =
                    text.charAt(
                            index
                    );

            if (character == '\n') {
                cursorX =
                        x;

                cursorY +=
                        16.0f *
                                scale;

                continue;
            }

            if (character == ' ') {
                cursorX +=
                        SPACE_ADVANCE *
                                scale;

                continue;
            }

            Glyph glyph =
                    glyphs.get(
                            character
                    );

            /*
             * Missing characters become question marks.
             */
            if (glyph == null) {
                glyph =
                        glyphs.get(
                                '?'
                        );
            }

            drawGlyph(
                    glyph,
                    cursorX,
                    cursorY,
                    16.0f *
                            scale,
                    16.0f *
                            scale
            );

            cursorX +=
                    CHARACTER_ADVANCE *
                            scale;
        }
    }

    private void drawGlyph(
            Glyph glyph,
            float x,
            float y,
            float width,
            float height
    ) {
        float right =
                x +
                        width;

        float bottom =
                y +
                        height;

        float[] vertices = {
                /*
                 * Position       UV
                 */
                x,     y,
                glyph.minimumU(),
                glyph.minimumV(),

                x,     bottom,
                glyph.minimumU(),
                glyph.maximumV(),

                right, bottom,
                glyph.maximumU(),
                glyph.maximumV(),

                right, y,
                glyph.maximumU(),
                glyph.minimumV()
        };

        glBindBuffer(
                GL_ARRAY_BUFFER,
                vertexBufferId
        );

        glBufferSubData(
                GL_ARRAY_BUFFER,
                0,
                vertices
        );

        glBindVertexArray(
                vertexArrayId
        );

        glDrawElements(
                GL_TRIANGLES,
                6,
                GL_UNSIGNED_INT,
                0
        );

        glBindVertexArray(
                0
        );
    }

    public float measureText(
            String text,
            float scale
    ) {
        if (
                text == null ||
                        text.isEmpty()
        ) {
            return 0.0f;
        }

        float currentLineWidth =
                0.0f;

        float maximumLineWidth =
                0.0f;

        for (
                int index = 0;
                index < text.length();
                index++
        ) {
            char character =
                    text.charAt(
                            index
                    );

            if (character == '\n') {
                maximumLineWidth =
                        Math.max(
                                maximumLineWidth,
                                currentLineWidth
                        );

                currentLineWidth =
                        0.0f;

                continue;
            }

            currentLineWidth +=
                    (
                            character == ' '
                                    ? SPACE_ADVANCE
                                    : CHARACTER_ADVANCE
                    ) *
                            scale;
        }

        return Math.max(
                maximumLineWidth,
                currentLineWidth
        );
    }

    private Shader createShader() {
        String vertexShaderSource = """
                #version 330 core

                layout (location = 0) in vec2 position;
                layout (location = 1) in vec2 textureCoordinate;

                uniform mat4 projectionMatrix;

                out vec2 fragmentTextureCoordinate;

                void main() {
                    gl_Position =
                            projectionMatrix *
                            vec4(
                                    position,
                                    0.0,
                                    1.0
                            );

                    fragmentTextureCoordinate =
                            textureCoordinate;
                }
                """;

        String fragmentShaderSource = """
                #version 330 core

                in vec2 fragmentTextureCoordinate;

                uniform sampler2D fontTexture;
                uniform vec4 textColor;

                out vec4 finalColor;

                void main() {
                    vec4 glyphColor =
                            texture(
                                    fontTexture,
                                    fragmentTextureCoordinate
                            );

                    if (glyphColor.a < 0.01) {
                        discard;
                    }

                    /*
                     * The atlas is primarily white, so this lets
                     * the caller tint text any desired color.
                     */
                    finalColor =
                            vec4(
                                    textColor.rgb,
                                    textColor.a *
                                            glyphColor.a
                            );
                }
                """;

        return new Shader(
                vertexShaderSource,
                fragmentShaderSource
        );
    }

    public void destroy() {
        shader.destroy();

        glDeleteBuffers(
                vertexBufferId
        );

        glDeleteBuffers(
                elementBufferId
        );

        glDeleteVertexArrays(
                vertexArrayId
        );
    }

    private record Glyph(
            float minimumU,
            float minimumV,
            float maximumU,
            float maximumV
    ) {
    }
}