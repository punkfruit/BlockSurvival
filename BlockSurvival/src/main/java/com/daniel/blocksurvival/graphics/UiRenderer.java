package com.daniel.blocksurvival.graphics;

import com.daniel.blocksurvival.Hotbar;
import com.daniel.blocksurvival.inventory.ItemDefinition;
import com.daniel.blocksurvival.world.AtlasTile;
import com.daniel.blocksurvival.world.BlockType;
import com.daniel.blocksurvival.inventory.Inventory;
import com.daniel.blocksurvival.inventory.ItemDefinition;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL33.*;

public class UiRenderer {

    private final Shader shader;
    private final Texture atlasTexture;
    private final TextRenderer textRenderer;

    private final int vao;
    private final int vbo;
    private final int ebo;

    private static final int SLOT_SIZE = 52;
    private static final int SLOT_GAP = 4;
    private static final int ICON_PADDING = 8;
    private static final int BOTTOM_MARGIN = 24;

    public UiRenderer(Texture atlasTexture, TextRenderer textRenderer) {
        this.atlasTexture = atlasTexture;
        this.textRenderer = textRenderer;

        shader = createShader();

        /*
         * Every UI quad uses four vertices and six indices.
         *
         * Each vertex contains:
         *
         * x, y, u, v
         */
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(
                GL_ARRAY_BUFFER,
                vbo
        );

        /*
         * The vertex data changes every time we draw a quad.
         */
        glBufferData(
                GL_ARRAY_BUFFER,
                4L * 4L * Float.BYTES,
                GL_DYNAMIC_DRAW
        );

        int[] indices = {
                0, 1, 2,
                2, 3, 0
        };

        glBindBuffer(
                GL_ELEMENT_ARRAY_BUFFER,
                ebo
        );

        glBufferData(
                GL_ELEMENT_ARRAY_BUFFER,
                indices,
                GL_STATIC_DRAW
        );

        int stride =
                4 * Float.BYTES;

        /*
         * Attribute 0: screen position.
         */
        glVertexAttribPointer(
                0,
                2,
                GL_FLOAT,
                false,
                stride,
                0
        );

        glEnableVertexAttribArray(0);

        /*
         * Attribute 1: texture coordinate.
         */
        glVertexAttribPointer(
                1,
                2,
                GL_FLOAT,
                false,
                stride,
                2L * Float.BYTES
        );

        glEnableVertexAttribArray(1);

        glBindVertexArray(0);

        shader.bind();

        shader.setInt(
                "uiTexture",
                0
        );

        shader.unbind();
    }

    public void render(
            Hotbar hotbar,
            Inventory inventory,
            int screenWidth,
            int screenHeight
    ) {
        /*
         * Pixel coordinates:
         *
         * 0, 0 is the upper-left.
         * screenWidth, screenHeight is the lower-right.
         */
        Matrix4f projection =
                new Matrix4f().ortho(
                        0.0f,
                        screenWidth,
                        screenHeight,
                        0.0f,
                        -1.0f,
                        1.0f
                );

        /*
         * The UI should not be hidden behind world geometry.
         */
        glDisable(GL_DEPTH_TEST);

        /*
         * Needed for transparent atlas pixels and translucent
         * slot backgrounds.
         */
        glEnable(GL_BLEND);

        glBlendFunc(
                GL_SRC_ALPHA,
                GL_ONE_MINUS_SRC_ALPHA
        );

        shader.bind();

        shader.setMatrix4(
                "projectionMatrix",
                projection
        );

        int slotCount =
                hotbar.getSlotCount();

        int totalWidth =
                slotCount * SLOT_SIZE +
                        (slotCount - 1) * SLOT_GAP;

        float hotbarX =
                (screenWidth - totalWidth) / 2.0f;

        float hotbarY =
                screenHeight -
                        BOTTOM_MARGIN -
                        SLOT_SIZE;

        /*
         * Draw every slot.
         */
        for (
                int slotIndex = 0;
                slotIndex < slotCount;
                slotIndex++
        ) {
            float slotX =
                    hotbarX +
                            slotIndex *
                                    (SLOT_SIZE + SLOT_GAP);

            boolean selected =
                    slotIndex ==
                            hotbar.getSelectedIndex();

            drawSlot(
                    slotX,
                    hotbarY,
                    selected
            );

            ItemDefinition item =
                    hotbar.getItem(
                            slotIndex
                    );

            if (item != null) {
                int quantity =
                        inventory.getQuantity(
                                item
                        );

                boolean available =
                        quantity > 0;

                drawItemIcon(
                        item,
                        slotX + ICON_PADDING,
                        hotbarY + ICON_PADDING,
                        SLOT_SIZE -
                                ICON_PADDING * 2,
                        available
                );
            }


        }

        drawCrosshair(
                screenWidth,
                screenHeight
        );

        shader.unbind();

        drawHotbarQuantities(
                hotbar,
                inventory,
                hotbarX,
                hotbarY,
                screenWidth,
                screenHeight
        );

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    private void drawSlot(
            float x,
            float y,
            boolean selected
    ) {
        if (selected) {
            /*
             * Larger bright quad behind the selected slot.
             */
            drawColoredQuad(
                    x - 3.0f,
                    y - 3.0f,
                    SLOT_SIZE + 6.0f,
                    SLOT_SIZE + 6.0f,
                    new Vector4f(
                            0.95f,
                            0.95f,
                            0.95f,
                            1.0f
                    )
            );
        } else {
            /*
             * Thin muted border behind ordinary slots.
             */
            drawColoredQuad(
                    x - 1.0f,
                    y - 1.0f,
                    SLOT_SIZE + 2.0f,
                    SLOT_SIZE + 2.0f,
                    new Vector4f(
                            0.55f,
                            0.55f,
                            0.55f,
                            0.9f
                    )
            );
        }

        /*
         * Dark translucent slot interior.
         */
        drawColoredQuad(
                x,
                y,
                SLOT_SIZE,
                SLOT_SIZE,
                new Vector4f(
                        0.08f,
                        0.08f,
                        0.08f,
                        0.78f
                )
        );
    }

    private void drawItemIcon(
            ItemDefinition item,
            float x,
            float y,
            float size,
            boolean available
    ) {
        AtlasTile tile =
                item.inventoryIcon();

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

        Vector4f color =
                available
                        ? new Vector4f(
                        1.0f,
                        1.0f,
                        1.0f,
                        1.0f
                )
                        : new Vector4f(
                        0.28f,
                        0.28f,
                        0.28f,
                        0.55f
                );

        drawTexturedQuad(
                x,
                y,
                size,
                size,
                minimumU,
                minimumV,
                maximumU,
                maximumV,
                color
        );
    }

    private void drawCrosshair(
            int screenWidth,
            int screenHeight
    ) {
        float centerX =
                screenWidth / 2.0f;

        float centerY =
                screenHeight / 2.0f;

        Vector4f crosshairColor =
                new Vector4f(
                        1.0f,
                        1.0f,
                        1.0f,
                        0.9f
                );

        /*
         * Horizontal arm.
         */
        drawColoredQuad(
                centerX - 8.0f,
                centerY - 1.0f,
                16.0f,
                2.0f,
                crosshairColor
        );

        /*
         * Vertical arm.
         */
        drawColoredQuad(
                centerX - 1.0f,
                centerY - 8.0f,
                2.0f,
                16.0f,
                crosshairColor
        );
    }

    private void drawColoredQuad(
            float x,
            float y,
            float width,
            float height,
            Vector4f color
    ) {
        shader.setInt(
                "useTexture",
                0
        );

        shader.setVector4(
                "uiColor",
                color
        );

        /*
         * UV values do not matter when useTexture is false.
         */
        uploadAndDrawQuad(
                x,
                y,
                width,
                height,
                0.0f,
                0.0f,
                1.0f,
                1.0f
        );
    }

    private void drawTexturedQuad(
            float x,
            float y,
            float width,
            float height,
            float minimumU,
            float minimumV,
            float maximumU,
            float maximumV,
            Vector4f color
    ) {
        shader.setInt(
                "useTexture",
                1
        );

        shader.setVector4(
                "uiColor",
                color
        );

        glActiveTexture(
                GL_TEXTURE0
        );

        atlasTexture.bind();

        uploadAndDrawQuad(
                x,
                y,
                width,
                height,
                minimumU,
                minimumV,
                maximumU,
                maximumV
        );
    }

    private void drawHotbarQuantities(
            Hotbar hotbar,
            Inventory inventory,
            float hotbarX,
            float hotbarY,
            int screenWidth,
            int screenHeight
    ) {
        for (
                int slotIndex = 0;
                slotIndex < hotbar.getSlotCount();
                slotIndex++
        ) {
            ItemDefinition item =
                    hotbar.getItem(
                            slotIndex
                    );

            if (item == null) {
                continue;
            }

            int quantity =
                    inventory.getQuantity(
                            item
                    );

            if (quantity <= 0) {
                continue;
            }

            String quantityText =
                    Integer.toString(
                            quantity
                    );

            float scale =
                    0.70f;

            float textWidth =
                    textRenderer.measureText(
                            quantityText,
                            scale
                    );

            float slotX =
                    hotbarX +
                            slotIndex *
                                    (
                                            SLOT_SIZE +
                                                    SLOT_GAP
                                    );

            float textX =
                    slotX +
                            SLOT_SIZE -
                            4.0f -
                            textWidth;

            float textY =
                    hotbarY +
                            SLOT_SIZE -
                            4.0f -
                            16.0f *
                                    scale;

            textRenderer.drawText(
                    quantityText,
                    textX,
                    textY,
                    scale,
                    screenWidth,
                    screenHeight
            );
        }
    }

    private void uploadAndDrawQuad(
            float x,
            float y,
            float width,
            float height,
            float minimumU,
            float minimumV,
            float maximumU,
            float maximumV
    ) {
        float right =
                x + width;

        float bottom =
                y + height;

        float[] vertices = {
                /*
                 * Position       UV
                 */
                x,     y,      minimumU, minimumV,
                x,     bottom, minimumU, maximumV,
                right, bottom, maximumU, maximumV,
                right, y,      maximumU, minimumV
        };

        glBindBuffer(
                GL_ARRAY_BUFFER,
                vbo
        );

        glBufferSubData(
                GL_ARRAY_BUFFER,
                0,
                vertices
        );

        glBindVertexArray(vao);

        glDrawElements(
                GL_TRIANGLES,
                6,
                GL_UNSIGNED_INT,
                0
        );

        glBindVertexArray(0);
    }

    private Shader createShader() {
        String vertexSource = """
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

        String fragmentSource = """
                #version 330 core

                in vec2 fragmentTextureCoordinate;

                uniform sampler2D uiTexture;
                uniform vec4 uiColor;
                uniform int useTexture;

                out vec4 finalColor;

                void main() {
                    if (useTexture == 1) {
                        vec4 textureColor =
                                texture(
                                        uiTexture,
                                        fragmentTextureCoordinate
                                );

                        /*
                         * Remove fully transparent atlas pixels.
                         */
                        if (textureColor.a < 0.01) {
                            discard;
                        }

                        finalColor =
                                textureColor *
                                uiColor;
                    } else {
                        finalColor =
                                uiColor;
                    }
                }
                """;

        return new Shader(
                vertexSource,
                fragmentSource
        );
    }

    public void cleanup() {
        shader.destroy();

        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }
}