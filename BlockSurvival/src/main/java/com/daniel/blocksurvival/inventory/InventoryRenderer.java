package com.daniel.blocksurvival.inventory;

import com.daniel.blocksurvival.graphics.InventoryUiPainter;
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


    private final TextRenderer textRenderer;
    private final InventoryUiPainter painter;

    private int selectedGridX =
            0;

    private int selectedGridY =
            0;

    private ItemStack movingStack;

    public boolean isMovingItem(){
        return movingStack != null;
    }
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

        this.textRenderer =
                textRenderer;

        this.painter =
                new InventoryUiPainter(
                        atlasTexture,
                        textRenderer
                );
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

        painter.begin(
                framebufferWidth,
                framebufferHeight
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

        painter.end();
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

        float selectionRed =
                isMovingItem()
                        ? 0.30f
                        : 0.96f;

        float selectionGreen =
                isMovingItem()
                        ? 0.90f
                        : 0.78f;

        float selectionBlue =
                isMovingItem()
                        ? 1.0f
                        : 0.20f;

        /*
         * Top.
         */
        drawRectangle(
                selectedX,
                selectedY,
                selectedWidth,
                borderThickness,
                selectionRed,
                selectionGreen,
                selectionBlue,
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
                selectionRed,
                selectionGreen,
                selectionBlue,
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
                selectionRed,
                selectionGreen,
                selectionBlue,
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
                selectionRed,
                selectionGreen,
                selectionBlue,
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
        painter.drawGrid(
                inventory,
                startX,
                startY,
                cellSize,
                cellGap
        );
    }


    private void drawItems(
            Inventory inventory,
            float startX,
            float startY,
            float cellSize,
            float cellGap
    ) {
        painter.drawItems(
                inventory,
                startX,
                startY,
                cellSize,
                cellGap
        );
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
        painter.drawRectangle(
                x,
                y,
                width,
                height,
                red,
                green,
                blue,
                alpha
        );
    }

    private void drawTexturedRectangle(
            float x,
            float y,
            float width,
            float height,
            AtlasTile tile
    ) {
        painter.drawTexturedRectangle(
                x,
                y,
                width,
                height,
                tile
        );
    }

    public void toggleMoveSelectedItem(
            Inventory inventory
    ){
        //enter while already moving: finish the move
        if(movingStack != null){
            movingStack =
                    null;

            return;
        }

        ItemStack selectedStack =
                inventory.getStackAt(
                        selectedGridX,
                        selectedGridY
                );

        if(selectedStack == null){
            return;
        }

        movingStack = selectedStack;

        //snap cursor to item anchor
        selectedGridX =
                selectedStack.getGridX();

        selectedGridY =
                selectedStack.getGridY();
    }

    public void moveHeldItem(
            int movementX,
            int movementY,
            Inventory inventory
    ) {
        if (movingStack == null) {
            return;
        }

        int newX =
                movingStack.getGridX() +
                        movementX;

        int newY =
                movingStack.getGridY() +
                        movementY;

        boolean moved =
                inventory.moveStack(
                        movingStack,
                        newX,
                        newY,
                        movingStack.isRotated()
                );

        if (!moved) {
            return;
        }

        selectedGridX =
                movingStack.getGridX();

        selectedGridY =
                movingStack.getGridY();
    }

    public void rotateHeldItem(
            Inventory inventory
    ) {
        if (movingStack == null) {
            return;
        }

        ItemDefinition definition =
                movingStack.getDefinition();

        if (!definition.rotatable()) {
            return;
        }

        boolean newRotation =
                !movingStack.isRotated();

        inventory.moveStack(
                movingStack,
                movingStack.getGridX(),
                movingStack.getGridY(),
                newRotation
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
        painter.destroy();
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

    public ItemDefinition getSelectedItem(
            Inventory inventory
    ) {
        ItemStack selectedStack =
                getStackAtSelectedCell(
                        inventory
                );

        if (selectedStack == null) {
            return null;
        }

        return selectedStack.getDefinition();
    }
}