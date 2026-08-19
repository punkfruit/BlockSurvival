package com.daniel.blocksurvival.graphics;

import com.daniel.blocksurvival.inventory.Inventory;
import com.daniel.blocksurvival.inventory.ItemDefinition;
import com.daniel.blocksurvival.inventory.ItemStack;
import com.daniel.blocksurvival.world.AtlasTile;
import com.daniel.blocksurvival.world.BlockType;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class InventoryUiPainter {

    private static final int FLOATS_PER_RECTANGLE =
            24;

    private final Shader shader;

    private final Texture atlasTexture;

    private final TextRenderer textRenderer;

    private final int vertexArrayId;

    private final int vertexBufferId;

    public InventoryUiPainter(
            Texture atlasTexture,
            TextRenderer textRenderer
    ) {
        this.atlasTexture =
                atlasTexture;

        this.textRenderer =
                textRenderer;

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
                
                uniform vec4 rectangleColor;
                uniform sampler2D atlasTexture;
                uniform int useTexture;
                
                out vec4 finalColor;
                
                void main() {
                    if (useTexture == 1) {
                        vec4 textureColor =
                                texture(
                                        atlasTexture,
                                        fragmentTextureCoordinate
                                );
                
                        if (textureColor.a < 0.1) {
                            discard;
                        }
                
                        finalColor =
                                textureColor;
                    }
                    else {
                        finalColor =
                                rectangleColor;
                    }
                }
                """;

        shader =
                new Shader(
                        vertexShaderSource,
                        fragmentShaderSource
                );

        vertexArrayId =
                glGenVertexArrays();

        vertexBufferId =
                glGenBuffers();

        glBindVertexArray(
                vertexArrayId
        );

        glBindBuffer(
                GL_ARRAY_BUFFER,
                vertexBufferId
        );

        glBufferData(
                GL_ARRAY_BUFFER,
                FLOATS_PER_RECTANGLE *
                        Float.BYTES,
                GL_DYNAMIC_DRAW
        );

        glEnableVertexAttribArray(
                0
        );

        glVertexAttribPointer(
                0,
                2,
                GL_FLOAT,
                false,
                4 * Float.BYTES,
                0
        );

        glEnableVertexAttribArray(
                1
        );

        glVertexAttribPointer(
                1,
                2,
                GL_FLOAT,
                false,
                4 * Float.BYTES,
                2L * Float.BYTES
        );

        shader.bind();

        shader.setInt(
                "atlasTexture",
                0
        );

        shader.unbind();
    }

    public void begin(
            int framebufferWidth,
            int framebufferHeight
    ) {
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
                                framebufferWidth,
                                framebufferHeight,
                                0.0f
                        );

        shader.setMatrix4(
                "projectionMatrix",
                projectionMatrix
        );

        glActiveTexture(
                GL_TEXTURE0
        );

        atlasTexture.bind();
    }

    public void end() {
        atlasTexture.unbind();

        shader.unbind();

        glDisable(
                GL_BLEND
        );

        glEnable(
                GL_DEPTH_TEST
        );
    }

    public void drawRectangle(
            float x,
            float y,
            float width,
            float height,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        float right =
                x +
                        width;

        float bottom =
                y +
                        height;

        float[] vertices = {
                x, y, 0.0f, 0.0f,
                x, bottom, 0.0f, 1.0f,
                right, bottom, 1.0f, 1.0f,

                right, bottom, 1.0f, 1.0f,
                right, y, 1.0f, 0.0f,
                x, y, 0.0f, 0.0f
        };

        shader.setInt(
                "useTexture",
                0
        );

        shader.setVector4(
                "rectangleColor",
                new Vector4f(
                        red,
                        green,
                        blue,
                        alpha
                )
        );

        drawVertices(
                vertices
        );
    }

    private void drawVertices(
            float[] vertices
    ) {
        glBindVertexArray(
                vertexArrayId
        );

        glBindBuffer(
                GL_ARRAY_BUFFER,
                vertexBufferId
        );

        glBufferSubData(
                GL_ARRAY_BUFFER,
                0,
                vertices
        );

        glDrawArrays(
                GL_TRIANGLES,
                0,
                6
        );

        glBindBuffer(
                GL_ARRAY_BUFFER,
                0
        );

        glBindVertexArray(
                0
        );
    }

    public void drawTexturedRectangle(
            float x,
            float y,
            float width,
            float height,
            AtlasTile tile
    ) {
        float right =
                x +
                        width;

        float bottom =
                y +
                        height;

        float tileSize =
                BlockType.getTileSize();

        float minimumU =
                tile.column() *
                        tileSize;

        float minimumV =
                tile.row() *
                        tileSize;

        float maximumU =
                minimumU +
                        tileSize;

        float maximumV =
                minimumV +
                        tileSize;

        float[] vertices = {
                x, y, minimumU, minimumV,
                x, bottom, minimumU, maximumV,
                right, bottom, maximumU, maximumV,

                right, bottom, maximumU, maximumV,
                right, y, maximumU, minimumV,
                x, y, minimumU, minimumV
        };

        shader.setInt(
                "useTexture",
                1
        );

        drawVertices(
                vertices
        );
    }

    public void drawCell(
            float x,
            float y,
            float cellSize
    ) {
        drawRectangle(
                x,
                y,
                cellSize,
                cellSize,
                0.32f,
                0.34f,
                0.36f,
                1.0f
        );

        float border =
                Math.max(
                        2.0f,
                        cellSize *
                                0.045f
                );

        drawRectangle(
                x + border,
                y + border,
                cellSize -
                        border *
                                2.0f,
                cellSize -
                        border *
                                2.0f,
                0.15f,
                0.16f,
                0.17f,
                1.0f
        );
    }

    public void drawGrid(
            Inventory inventory,
            float startX,
            float startY,
            float cellSize,
            float cellGap
    ) {
        for (
                int gridY = 0;
                gridY < inventory.getHeight();
                gridY++
        ) {
            for (
                    int gridX = 0;
                    gridX < inventory.getWidth();
                    gridX++
            ) {
                float x =
                        startX +
                                gridX *
                                        (
                                                cellSize +
                                                        cellGap
                                        );

                float y =
                        startY +
                                gridY *
                                        (
                                                cellSize +
                                                        cellGap
                                        );

                drawCell(
                        x,
                        y,
                        cellSize
                );
            }
        }
    }

    public void drawItems(
            Inventory inventory,
            float startX,
            float startY,
            float cellSize,
            float cellGap
    ) {
        for (
                ItemStack stack :
                inventory.getStacks()
        ) {
            ItemDefinition definition =
                    stack.getDefinition();

            int itemWidth =
                    definition.getPlacedWidth(
                            stack.isRotated()
                    );

            int itemHeight =
                    definition.getPlacedHeight(
                            stack.isRotated()
                    );

            float itemX =
                    startX +
                            stack.getGridX() *
                                    (
                                            cellSize +
                                                    cellGap
                                    );

            float itemY =
                    startY +
                            stack.getGridY() *
                                    (
                                            cellSize +
                                                    cellGap
                                    );

            float itemPixelWidth =
                    itemWidth *
                            cellSize +
                            (
                                    itemWidth -
                                            1
                            ) *
                                    cellGap;

            float itemPixelHeight =
                    itemHeight *
                            cellSize +
                            (
                                    itemHeight -
                                            1
                            ) *
                                    cellGap;

            float border =
                    Math.max(
                            4.0f,
                            cellSize *
                                    0.075f
                    );

            /*
             * Generic dark item backing.
             */
            drawRectangle(
                    itemX,
                    itemY,
                    itemPixelWidth,
                    itemPixelHeight,
                    0.20f,
                    0.21f,
                    0.22f,
                    1.0f
            );

            drawTexturedRectangle(
                    itemX + border,
                    itemY + border,
                    itemPixelWidth -
                            border *
                                    2.0f,
                    itemPixelHeight -
                            border *
                                    2.0f,
                    definition.inventoryIcon()
            );
        }
    }

    public void drawSelection(
            float x,
            float y,
            float width,
            float height,
            float cellSize,
            boolean moving
    ) {
        float borderThickness =
                Math.max(
                        3.0f,
                        cellSize *
                                0.055f
                );

        float red =
                moving
                        ? 0.30f
                        : 0.96f;

        float green =
                moving
                        ? 0.90f
                        : 0.78f;

        float blue =
                moving
                        ? 1.0f
                        : 0.20f;

        drawRectangle(
                x,
                y,
                width,
                borderThickness,
                red,
                green,
                blue,
                1.0f
        );

        drawRectangle(
                x,
                y +
                        height -
                        borderThickness,
                width,
                borderThickness,
                red,
                green,
                blue,
                1.0f
        );

        drawRectangle(
                x,
                y,
                borderThickness,
                height,
                red,
                green,
                blue,
                1.0f
        );

        drawRectangle(
                x +
                        width -
                        borderThickness,
                y,
                borderThickness,
                height,
                red,
                green,
                blue,
                1.0f
        );
    }

    public TextRenderer getTextRenderer() {
        return textRenderer;
    }

    public void drawQuantities(
            Inventory inventory,
            float startX,
            float startY,
            float cellSize,
            float cellGap,
            int framebufferWidth,
            int framebufferHeight
    ) {
        for (ItemStack stack : inventory.getStacks()) {
            if (stack.getQuantity() <= 1) {
                continue;
            }

            ItemDefinition definition =
                    stack.getDefinition();

            int itemWidth =
                    definition.getPlacedWidth(
                            stack.isRotated()
                    );

            int itemHeight =
                    definition.getPlacedHeight(
                            stack.isRotated()
                    );

            float itemX =
                    startX +
                            stack.getGridX() *
                                    (
                                            cellSize +
                                                    cellGap
                                    );

            float itemY =
                    startY +
                            stack.getGridY() *
                                    (
                                            cellSize +
                                                    cellGap
                                    );

            float itemPixelWidth =
                    itemWidth *
                            cellSize +
                            (
                                    itemWidth -
                                            1
                            ) *
                                    cellGap;

            float itemPixelHeight =
                    itemHeight *
                            cellSize +
                            (
                                    itemHeight -
                                            1
                            ) *
                                    cellGap;

            String quantityText =
                    Integer.toString(
                            stack.getQuantity()
                    );

            float scale =
                    1.1f;

            float textWidth =
                    textRenderer.measureText(
                            quantityText,
                            scale
                    );

            float padding =
                    Math.max(
                            4.0f,
                            cellSize *
                                    0.07f
                    );

            float quantityX =
                    itemX +
                            itemPixelWidth -
                            padding -
                            textWidth;

            float quantityY =
                    itemY +
                            itemPixelHeight -
                            padding -
                            16.0f *
                                    scale;

            textRenderer.drawText(
                    quantityText,
                    quantityX,
                    quantityY,
                    scale,
                    framebufferWidth,
                    framebufferHeight
            );
        }
    }

    public void destroy() {
        glDeleteBuffers(
                vertexBufferId
        );

        glDeleteVertexArrays(
                vertexArrayId
        );

        shader.destroy();
    }
}