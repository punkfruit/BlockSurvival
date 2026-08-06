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
import com.daniel.blocksurvival.graphics.TextRenderer;
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
    private final TextRenderer textRenderer;

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
            Texture atlasTexture,
            TextRenderer textRenderer
    ) {
        if (atlasTexture == null) {
            throw new IllegalArgumentException(
                    "InventoryRenderer requires an atlas texture."
            );
        }

        if (textRenderer == null) {
            throw new IllegalArgumentException(
                    "InventoryRenderer requires a text renderer."
            );
        }

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

        float informationHeight =
                cellSize *
                        1.45f;

        float informationGap =
                cellSize *
                        0.22f;

        float panelHeight =
                gridHeight +
                        informationGap +
                        informationHeight +
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

        float informationX =
                gridX;

        float informationY =
                gridY +
                        gridHeight +
                        informationGap;

        drawInformationBoxBackground(
                informationX,
                informationY,
                gridWidth,
                informationHeight,
                cellSize
        );

        /*
         * Text must be last because TextRenderer changes OpenGL state.
         */
        drawItemQuantities(
                inventory,
                gridX,
                gridY,
                cellSize,
                cellGap,
                framebufferWidth,
                framebufferHeight
        );

        drawInformationText(
                inventory,
                informationX,
                informationY,
                cellSize,
                framebufferWidth,
                framebufferHeight
        );
    }

    private void drawInformationBoxBackground(
            float x,
            float y,
            float width,
            float height,
            float cellSize
    ) {
        drawRectangle(
                x,
                y,
                width,
                height,
                0.31f,
                0.33f,
                0.35f,
                1.0f
        );

        float border =
                Math.max(
                        2.0f,
                        cellSize *
                                0.04f
                );

        drawRectangle(
                x + border,
                y + border,
                width -
                        border *
                                2.0f,
                height -
                        border *
                                2.0f,
                0.11f,
                0.12f,
                0.13f,
                0.98f
        );
    }

    private void drawInformationText(
            Inventory inventory,
            float x,
            float y,
            float cellSize,
            int framebufferWidth,
            int framebufferHeight
    ) {
        ItemStack selectedStack =
                getStackAtSelectedCell(
                        inventory
                );

        if (selectedStack == null) {
            textRenderer.drawText(
                    "EMPTY",
                    x +
                            cellSize *
                                    0.16f,
                    y +
                            cellSize *
                                    0.18f,
                    1.0f,
                    framebufferWidth,
                    framebufferHeight
            );

            return;
        }

        ItemDefinition definition =
                selectedStack.getDefinition();

        int placedWidth =
                definition.getPlacedWidth(
                        selectedStack.isRotated()
                );

        int placedHeight =
                definition.getPlacedHeight(
                        selectedStack.isRotated()
                );

        float textX =
                x +
                        cellSize *
                                0.16f;

        float firstLineY =
                y +
                        cellSize *
                                0.10f;

        float lineSpacing =
                cellSize *
                        0.32f;

        float nameScale =
                1.15f;

        float detailScale =
                0.85f;

        textRenderer.drawText(
                definition.displayName()
                        .toUpperCase(),
                textX,
                firstLineY,
                nameScale,
                framebufferWidth,
                framebufferHeight
        );

        textRenderer.drawText(
                "SIZE: " +
                        placedWidth +
                        " × " +
                        placedHeight,
                textX,
                firstLineY +
                        lineSpacing,
                detailScale,
                framebufferWidth,
                framebufferHeight
        );

        textRenderer.drawText(
                "STACK: " +
                        selectedStack.getQuantity() +
                        " / " +
                        definition.maximumStackSize(),
                textX,
                firstLineY +
                        lineSpacing *
                                2.0f,
                detailScale,
                framebufferWidth,
                framebufferHeight
        );

        String itemTypeText =
                definition.placedBlock() == null
                        ? "INVENTORY ITEM"
                        : "PLACEABLE BLOCK";

        textRenderer.drawText(
                itemTypeText,
                textX,
                firstLineY +
                        lineSpacing *
                                3.0f,
                detailScale,
                framebufferWidth,
                framebufferHeight
        );
    }

    private void drawItemQuantities(
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

            float itemBorder =
                    Math.max(
                            4.0f,
                            cellSize *
                                    0.075f
                    );

            String quantityText =
                    Integer.toString(
                            stack.getQuantity()
                    );

            /*
             * 1.58 was quite large relative to these cells.
             * Start here and tune freely.
             */
            float quantityScale =
                    1.5f;

            float textWidth =
                    textRenderer.measureText(
                            quantityText,
                            quantityScale
                    );

            float quantityX =
                    itemX +
                            itemPixelWidth -
                            itemBorder -
                            textWidth;

            float quantityY =
                    itemY +
                            itemPixelHeight -
                            itemBorder -
                            16.0f *
                                    quantityScale;

            textRenderer.drawText(
                    quantityText,
                    quantityX,
                    quantityY,
                    quantityScale,
                    framebufferWidth,
                    framebufferHeight
            );
        }
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