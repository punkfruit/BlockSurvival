package com.daniel.blocksurvival.inventory;

import com.daniel.blocksurvival.graphics.Shader;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import com.daniel.blocksurvival.graphics.Texture;
import com.daniel.blocksurvival.world.AtlasTile;
import com.daniel.blocksurvival.world.BlockModel;
import com.daniel.blocksurvival.world.BlockType;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class InventoryRenderer {

    private boolean visible;

    private final Shader shader;

    private final int vertexArrayId;
    private final int vertexBufferId;
    private final Texture atlasTexture;

    private int selectedGridX =
            0;

    private int selectedGridY =
            0;

    /*
     * One rectangle:
     *
     * 2 position floats
     * × 6 vertices
     */
    private static final int FLOATS_PER_RECTANGLE =
            24;

    public InventoryRenderer(
            Texture atlasTexture
    ) {
        this.atlasTexture =
                atlasTexture;
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

        /*
         * 0 = solid colored rectangle
         * 1 = textured rectangle
         */
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

        /*
         * Allocate enough space for one rectangle.
         *
         * GL_DYNAMIC_DRAW means the coordinates will change
         * frequently while the buffer itself is reused.
         */
        glBufferData(
                GL_ARRAY_BUFFER,
                FLOATS_PER_RECTANGLE *
                        Float.BYTES,
                GL_DYNAMIC_DRAW
        );

        /*
         * Position: x, y
         */
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

        /*
         * Texture coordinates: u, v
         */
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

    public void toggle() {
        visible =
                !visible;
    }

    public void setVisible(
            boolean visible
    ) {
        this.visible =
                visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void render(
            Inventory inventory,
            int framebufferWidth,
            int framebufferHeight
    ) {
        if (!visible) {
            return;
        }

        /*
         * Draw UI over the 3D world regardless of depth.
         */
        glDisable(
                GL_DEPTH_TEST
        );

        /*
         * Required for transparent overlays and panels.
         */
        glEnable(
                GL_BLEND
        );

        glBlendFunc(
                GL_SRC_ALPHA,
                GL_ONE_MINUS_SRC_ALPHA
        );

        shader.bind();

        glActiveTexture(
                GL_TEXTURE0
        );

        atlasTexture.bind();

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

        drawBackground(
                framebufferWidth,
                framebufferHeight
        );

        drawInventoryPanel(
                inventory,
                framebufferWidth,
                framebufferHeight
        );

        atlasTexture.unbind();
        shader.unbind();

        glDisable(
                GL_BLEND
        );

        glEnable(
                GL_DEPTH_TEST
        );
    }

    private void drawBackground(
            int framebufferWidth,
            int framebufferHeight
    ) {
        drawRectangle(
                0.0f,
                0.0f,
                framebufferWidth,
                framebufferHeight,

                0.0f,
                0.0f,
                0.0f,
                0.58f
        );
    }

    private void drawInventoryPanel(
            Inventory inventory,
            int framebufferWidth,
            int framebufferHeight
    ) {
        /*
         * Keep cells square and scale them slightly with
         * the framebuffer, while enforcing sensible limits.
         */
        float cellSize =
                Math.min(
                        framebufferWidth,
                        framebufferHeight
                ) *
                        0.095f;

        cellSize =
                Math.max(
                        54.0f,
                        Math.min(
                                cellSize,
                                90.0f
                        )
                );

        float cellGap =
                Math.max(
                        4.0f,
                        cellSize *
                                0.07f
                );

        float panelPadding =
                cellSize *
                        0.38f;

        float gridWidth =
                inventory.getWidth() *
                        cellSize +
                        (
                                inventory.getWidth() -
                                        1
                        ) *
                                cellGap;

        float gridHeight =
                inventory.getHeight() *
                        cellSize +
                        (
                                inventory.getHeight() -
                                        1
                        ) *
                                cellGap;

        float panelWidth =
                gridWidth +
                        panelPadding *
                                2.0f;

        float panelHeight =
                gridHeight +
                        panelPadding *
                                2.0f;

        float panelX =
                (
                        framebufferWidth -
                                panelWidth
                ) /
                        2.0f;

        float panelY =
                (
                        framebufferHeight -
                                panelHeight
                ) /
                        2.0f;

        /*
         * Soft shadow behind the panel.
         */
        drawRectangle(
                panelX + 10.0f,
                panelY + 12.0f,
                panelWidth,
                panelHeight,

                0.0f,
                0.0f,
                0.0f,
                0.45f
        );

        /*
         * Main inventory panel.
         */
        drawRectangle(
                panelX,
                panelY,
                panelWidth,
                panelHeight,

                0.09f,
                0.10f,
                0.11f,
                0.96f
        );

        float gridX =
                panelX +
                        panelPadding;

        float gridY =
                panelY +
                        panelPadding;

        drawGrid(
                inventory,
                gridX,
                gridY,
                cellSize,
                cellGap
        );

        drawItems(
                inventory,
                gridX,
                gridY,
                cellSize,
                cellGap
        );
        drawSelection(
                inventory,
                gridX,
                gridY,
                cellSize,
                cellGap
        );
    }

    private void drawSelection(
            Inventory inventory,
            float startX,
            float startY,
            float cellSize,
            float cellGap
    ) {
        ItemStack selectedStack =
                getStackAtSelectedCell(
                        inventory
                );

        int selectionGridX =
                selectedGridX;

        int selectionGridY =
                selectedGridY;

        int selectionWidth =
                1;

        int selectionHeight =
                1;

        if (selectedStack != null) {
            ItemDefinition definition =
                    selectedStack.getDefinition();

            selectionGridX =
                    selectedStack.getGridX();

            selectionGridY =
                    selectedStack.getGridY();

            selectionWidth =
                    definition.getPlacedWidth(
                            selectedStack.isRotated()
                    );

            selectionHeight =
                    definition.getPlacedHeight(
                            selectedStack.isRotated()
                    );
        }
        float selectedX =
                startX +
                        selectionGridX *
                                (
                                        cellSize +
                                                cellGap
                                );

        float selectedY =
                startY +
                        selectionGridY *
                                (
                                        cellSize +
                                                cellGap
                                );

        float selectedWidth =
                selectionWidth *
                        cellSize +
                        (
                                selectionWidth -
                                        1
                        ) *
                                cellGap;

        float selectedHeight =
                selectionHeight *
                        cellSize +
                        (
                                selectionHeight -
                                        1
                        ) *
                                cellGap;

        float borderThickness =
                Math.max(
                        3.0f,
                        cellSize *
                                0.055f
                );

        /*
         * Top.
         */
        drawRectangle(
                selectedX,
                selectedY,
                selectedWidth,
                borderThickness,
                0.96f,
                0.78f,
                0.20f,
                1.0f
        );

        /*
         * Bottom.
         */
        drawRectangle(
                selectedX,
                selectedY +
                        selectedHeight -
                        borderThickness,
                selectedWidth,
                borderThickness,
                0.96f,
                0.78f,
                0.20f,
                1.0f
        );

        /*
         * Left.
         */
        drawRectangle(
                selectedX,
                selectedY,
                borderThickness,
                selectedHeight,
                0.96f,
                0.78f,
                0.20f,
                1.0f
        );

        /*
         * Right.
         */
        drawRectangle(
                selectedX +
                        selectedWidth -
                        borderThickness,
                selectedY,
                borderThickness,
                selectedHeight,
                0.96f,
                0.78f,
                0.20f,
                1.0f
        );
    }

    private void drawGrid(
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

                /*
                 * Slightly lighter outer cell.
                 */
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

                /*
                 * Dark inner area creates a simple border.
                 */
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
        }
    }


    private void drawItems(
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

            /*
             * Convert the inventory grid coordinates into
             * framebuffer pixel coordinates.
             */
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

            /*
             * Include the gaps between cells inside a
             * multi-cell item.
             *
             * A 2-cell-wide item covers:
             *
             * cell + gap + cell
             */
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

            ItemColor color =
                    createItemColor(
                            definition.id()
                    );

            /*
             * Dark outer border.
             */
            drawRectangle(
                    itemX,
                    itemY,
                    itemPixelWidth,
                    itemPixelHeight,

                    color.red() *
                            0.55f,
                    color.green() *
                            0.55f,
                    color.blue() *
                            0.55f,
                    1.0f
            );

            /*
             * Bright inner item area.
             */
            float itemBorder =
                    Math.max(
                            4.0f,
                            cellSize *
                                    0.075f
                    );



            AtlasTile iconTile =
                    definition.inventoryIcon();

            drawTexturedRectangle(
                    itemX + itemBorder,
                    itemY + itemBorder,
                    itemPixelWidth -
                            itemBorder *
                                    2.0f,
                    itemPixelHeight -
                            itemBorder *
                                    2.0f,
                    iconTile
            );
            if (stack.getQuantity() > 1) {
                drawNumber(
                        stack.getQuantity(),
                        itemX +
                                itemPixelWidth -
                                itemBorder -
                                3.0f,
                        itemY +
                                itemPixelHeight -
                                itemBorder -
                                3.0f,
                        cellSize *
                                0.055f
                );
            }
        }
    }

    private void drawNumber(
            int number,
            float rightX,
            float bottomY,
            float pixelSize
    ) {
        String text =
                Integer.toString(
                        Math.max(
                                0,
                                number
                        )
                );

        float digitWidth =
                pixelSize *
                        3.0f;

        float digitGap =
                pixelSize;

        float totalWidth =
                text.length() *
                        digitWidth +
                        (
                                text.length() -
                                        1
                        ) *
                                digitGap;

        float startX =
                rightX -
                        totalWidth;

        /*
         * Draw a small dark shadow first so the number stays
         * readable over bright textures such as snow or glowstone.
         */
        for (
                int index = 0;
                index < text.length();
                index++
        ) {
            int digit =
                    text.charAt(index) -
                            '0';

            float digitX =
                    startX +
                            index *
                                    (
                                            digitWidth +
                                                    digitGap
                                    );

            drawDigit(
                    digit,
                    digitX +
                            pixelSize *
                                    0.55f,
                    bottomY -
                            pixelSize *
                                    5.0f +
                            pixelSize *
                                    0.55f,
                    pixelSize,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.85f
            );
        }

        /*
         * Bright foreground digits.
         */
        for (
                int index = 0;
                index < text.length();
                index++
        ) {
            int digit =
                    text.charAt(index) -
                            '0';

            float digitX =
                    startX +
                            index *
                                    (
                                            digitWidth +
                                                    digitGap
                                    );

            drawDigit(
                    digit,
                    digitX,
                    bottomY -
                            pixelSize *
                                    5.0f,
                    pixelSize,
                    1.0f,
                    1.0f,
                    1.0f,
                    1.0f
            );
        }
    }

    private void drawDigit(
            int digit,
            float x,
            float y,
            float pixelSize,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        /*
         * Each digit is a 3×5 bitmap.
         */
        String[] rows =
                switch (digit) {
                    case 0 -> new String[] {
                            "111",
                            "101",
                            "101",
                            "101",
                            "111"
                    };

                    case 1 -> new String[] {
                            "010",
                            "110",
                            "010",
                            "010",
                            "111"
                    };

                    case 2 -> new String[] {
                            "111",
                            "001",
                            "111",
                            "100",
                            "111"
                    };

                    case 3 -> new String[] {
                            "111",
                            "001",
                            "111",
                            "001",
                            "111"
                    };

                    case 4 -> new String[] {
                            "101",
                            "101",
                            "111",
                            "001",
                            "001"
                    };

                    case 5 -> new String[] {
                            "111",
                            "100",
                            "111",
                            "001",
                            "111"
                    };

                    case 6 -> new String[] {
                            "111",
                            "100",
                            "111",
                            "101",
                            "111"
                    };

                    case 7 -> new String[] {
                            "111",
                            "001",
                            "010",
                            "010",
                            "010"
                    };

                    case 8 -> new String[] {
                            "111",
                            "101",
                            "111",
                            "101",
                            "111"
                    };

                    case 9 -> new String[] {
                            "111",
                            "101",
                            "111",
                            "001",
                            "111"
                    };

                    default -> new String[] {
                            "000",
                            "000",
                            "000",
                            "000",
                            "000"
                    };
                };

        for (
                int row = 0;
                row < rows.length;
                row++
        ) {
            for (
                    int column = 0;
                    column < 3;
                    column++
            ) {
                if (
                        rows[row].charAt(
                                column
                        ) != '1'
                ) {
                    continue;
                }

                drawRectangle(
                        x +
                                column *
                                        pixelSize,
                        y +
                                row *
                                        pixelSize,
                        pixelSize,
                        pixelSize,
                        red,
                        green,
                        blue,
                        alpha
                );
            }
        }
    }

    private ItemColor createItemColor(
            String itemId
    ) {
        /*
         * String.hashCode() is deterministic, so a given
         * item ID receives the same color every time.
         */
        int hash =
                itemId.hashCode();

        float red =
                0.35f +
                        (
                                (
                                        hash >>> 16
                                ) &
                                        0xFF
                        ) /
                                255.0f *
                                0.45f;

        float green =
                0.35f +
                        (
                                (
                                        hash >>> 8
                                ) &
                                        0xFF
                        ) /
                                255.0f *
                                0.45f;

        float blue =
                0.35f +
                        (
                                hash &
                                        0xFF
                        ) /
                                255.0f *
                                0.45f;

        return new ItemColor(
                red,
                green,
                blue
        );
    }

    private void drawRectangle(
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
                x,     y,      0.0f, 0.0f,
                x,     bottom, 0.0f, 1.0f,
                right, bottom, 1.0f, 1.0f,

                right, bottom, 1.0f, 1.0f,
                right, y,      1.0f, 0.0f,
                x,     y,      0.0f, 0.0f
        };

        shader.setInt(
                "useTexture",
                0
        );

        shader.setVector4(
                "rectangleColor",
                new org.joml.Vector4f(
                        red,
                        green,
                        blue,
                        alpha
                )
        );

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

    private void drawTexturedRectangle(
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
                x,     y,      minimumU, minimumV,
                x,     bottom, minimumU, maximumV,
                right, bottom, maximumU, maximumV,

                right, bottom, maximumU, maximumV,
                right, y,      maximumU, minimumV,
                x,     y,      minimumU, minimumV
        };

        shader.setInt(
                "useTexture",
                1
        );

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

    public void moveSelection(
            int movementX,
            int movementY,
            Inventory inventory
    ) {
        ItemStack currentStack =
                getStackAtSelectedCell(
                        inventory
                );

        int nextGridX =
                selectedGridX +
                        movementX;

        int nextGridY =
                selectedGridY +
                        movementY;

        /*
         * When currently selecting a multi-cell item,
         * move from the edge of that item instead of from
         * its anchor cell.
         */
        if (currentStack != null) {
            ItemDefinition definition =
                    currentStack.getDefinition();

            int itemWidth =
                    definition.getPlacedWidth(
                            currentStack.isRotated()
                    );

            int itemHeight =
                    definition.getPlacedHeight(
                            currentStack.isRotated()
                    );

            if (movementX > 0) {
                nextGridX =
                        currentStack.getGridX() +
                                itemWidth;
            }
            else if (movementX < 0) {
                nextGridX =
                        currentStack.getGridX() -
                                1;
            }

            if (movementY > 0) {
                nextGridY =
                        currentStack.getGridY() +
                                itemHeight;
            }
            else if (movementY < 0) {
                nextGridY =
                        currentStack.getGridY() -
                                1;
            }
        }

        selectedGridX =
                Math.floorMod(
                        nextGridX,
                        inventory.getWidth()
                );

        selectedGridY =
                Math.floorMod(
                        nextGridY,
                        inventory.getHeight()
                );

        /*
         * If the new cell belongs to another item,
         * snap to that item's anchor.
         */
        ItemStack selectedStack =
                getStackAtSelectedCell(
                        inventory
                );

        if (selectedStack != null) {
            selectedGridX =
                    selectedStack.getGridX();

            selectedGridY =
                    selectedStack.getGridY();
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
    private record ItemColor(
            float red,
            float green,
            float blue
    ) {
    }

    private ItemStack getStackAtSelectedCell(
            Inventory inventory
    ) {
        for (ItemStack stack : inventory.getStacks()) {
            ItemDefinition definition =
                    stack.getDefinition();

            int width =
                    definition.getPlacedWidth(
                            stack.isRotated()
                    );

            int height =
                    definition.getPlacedHeight(
                            stack.isRotated()
                    );

            boolean insideStack =
                    selectedGridX >=
                            stack.getGridX() &&
                            selectedGridX <
                                    stack.getGridX() +
                                            width &&
                            selectedGridY >=
                                    stack.getGridY() &&
                            selectedGridY <
                                    stack.getGridY() +
                                            height;

            if (insideStack) {
                return stack;
            }
        }

        return null;
    }
}