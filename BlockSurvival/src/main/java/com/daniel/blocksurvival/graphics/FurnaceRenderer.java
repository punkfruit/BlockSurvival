package com.daniel.blocksurvival.graphics;

import com.daniel.blocksurvival.inventory.Inventory;
import com.daniel.blocksurvival.inventory.ItemDefinition;
import com.daniel.blocksurvival.inventory.ItemStack;
import com.daniel.blocksurvival.machine.FurnaceScreen;
import com.daniel.blocksurvival.machine.PrimitiveFurnace;

public class FurnaceRenderer {

    private final InventoryUiPainter painter;
    private final TextRenderer textRenderer;

    public FurnaceRenderer(
            Texture atlasTexture,
            TextRenderer textRenderer
    ) {
        if (atlasTexture == null) {
            throw new IllegalArgumentException(
                    "FurnaceRenderer requires an atlas texture."
            );
        }

        if (textRenderer == null) {
            throw new IllegalArgumentException(
                    "FurnaceRenderer requires a text renderer."
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

    public void render(
            FurnaceScreen screen,
            Inventory playerInventory,
            int framebufferWidth,
            int framebufferHeight
    ) {
        if (
                screen == null ||
                        !screen.isOpen()
        ) {
            return;
        }

        PrimitiveFurnace furnace =
                screen.getFurnace();

        if (furnace == null) {
            return;
        }

        painter.begin(
                framebufferWidth,
                framebufferHeight
        );

        /*
         * Darkened world behind the UI.
         */
        painter.drawRectangle(
                0.0f,
                0.0f,
                framebufferWidth,
                framebufferHeight,
                0.0f,
                0.0f,
                0.0f,
                0.72f
        );

        float panelWidth =
                Math.min(
                        framebufferWidth * 0.72f,
                        900.0f
                );

        float panelHeight =
                Math.min(
                        framebufferHeight * 0.82f,
                        760.0f
                );

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
         * Outer panel.
         */
        painter.drawRectangle(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                0.18f,
                0.19f,
                0.20f,
                0.98f
        );

        float inset =
                8.0f;

        painter.drawRectangle(
                panelX + inset,
                panelY + inset,
                panelWidth - inset * 2.0f,
                panelHeight - inset * 2.0f,
                0.08f,
                0.09f,
                0.10f,
                1.0f
        );

        float cellSize =
                Math.min(
                        78.0f,
                        panelWidth /
                                10.0f
                );

        float cellGap =
                cellSize *
                        0.08f;

        /*
         * MACHINE INVENTORIES
         */

        float machineY =
                panelY +
                        panelHeight *
                                0.20f;

        float inputX =
                panelX +
                        panelWidth *
                                0.18f;

        float fuelX =
                panelX +
                        panelWidth *
                                0.50f;

        float outputX =
                panelX +
                        panelWidth *
                                0.68f;

        drawInventory(
                furnace.getInputInventory(),
                inputX,
                machineY,
                cellSize,
                cellGap
        );

        drawInventory(
                furnace.getFuelInventory(),
                fuelX,
                machineY,
                cellSize,
                cellGap
        );

        drawInventory(
                furnace.getOutputInventory(),
                outputX,
                machineY,
                cellSize,
                cellGap
        );

        /*
         * PLAYER INVENTORY
         */

        float playerGridWidth =
                playerInventory.getWidth() *
                        cellSize +
                        (
                                playerInventory.getWidth() -
                                        1
                        ) *
                                cellGap;

        float playerX =
                panelX +
                        (
                                panelWidth -
                                        playerGridWidth
                        ) /
                                2.0f;

        float playerY =
                panelY +
                        panelHeight *
                                0.56f;

        drawInventory(
                playerInventory,
                playerX,
                playerY,
                cellSize,
                cellGap
        );

        /*
         * Selection border.
         */
        drawSelection(
                screen,
                playerInventory,
                inputX,
                fuelX,
                outputX,
                machineY,
                playerX,
                playerY,
                cellSize,
                cellGap
        );

        /*
         * Text is rendered after the painter so it can use
         * the font renderer's own OpenGL state safely.
         */
        painter.end();

        painter.drawQuantities(
                furnace.getInputInventory(),
                inputX,
                machineY,
                cellSize,
                cellGap,
                framebufferWidth,
                framebufferHeight
        );

        painter.drawQuantities(
                furnace.getFuelInventory(),
                fuelX,
                machineY,
                cellSize,
                cellGap,
                framebufferWidth,
                framebufferHeight
        );

        painter.drawQuantities(
                furnace.getOutputInventory(),
                outputX,
                machineY,
                cellSize,
                cellGap,
                framebufferWidth,
                framebufferHeight
        );

        painter.drawQuantities(
                playerInventory,
                playerX,
                playerY,
                cellSize,
                cellGap,
                framebufferWidth,
                framebufferHeight
        );

        drawLabels(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                inputX,
                fuelX,
                outputX,
                machineY,
                playerX,
                playerY,
                cellSize,
                framebufferWidth,
                framebufferHeight
        );
    }

    private void drawInventory(
            Inventory inventory,
            float x,
            float y,
            float cellSize,
            float cellGap
    ) {
        painter.drawGrid(
                inventory,
                x,
                y,
                cellSize,
                cellGap
        );

        painter.drawItems(
                inventory,
                x,
                y,
                cellSize,
                cellGap
        );
    }

    private void drawSelection(
            FurnaceScreen screen,
            Inventory playerInventory,
            float inputX,
            float fuelX,
            float outputX,
            float machineY,
            float playerX,
            float playerY,
            float cellSize,
            float cellGap
    ) {
        int section =
                screen.getSelectedSection();

        float selectionX;
        float selectionY;

        if (section == 0) {
            selectionX =
                    playerX +
                            screen.getPlayerX() *
                                    (
                                            cellSize +
                                                    cellGap
                                    );

            selectionY =
                    playerY +
                            screen.getPlayerY() *
                                    (
                                            cellSize +
                                                    cellGap
                                    );
        } else {
            selectionY =
                    machineY;

            float sectionX =
                    switch (section) {
                        case 1 -> inputX;
                        case 2 -> fuelX;
                        case 3 -> outputX;

                        default -> throw new IllegalStateException(
                                "Unknown furnace section: " +
                                        section
                        );
                    };

            selectionX =
                    sectionX +
                            screen.getMachineSlot() *
                                    (
                                            cellSize +
                                                    cellGap
                                    );
        }

        painter.drawSelection(
                selectionX,
                selectionY,
                cellSize,
                cellSize,
                cellSize,
                false
        );
    }

    private void drawLabels(
            float panelX,
            float panelY,
            float panelWidth,
            float panelHeight,
            float inputX,
            float fuelX,
            float outputX,
            float machineY,
            float playerX,
            float playerY,
            float cellSize,
            int framebufferWidth,
            int framebufferHeight
    ) {
        float titleScale =
                1.5f;

        float labelScale =
                1.0f;

        /*
         * Title.
         */
        String title =
                "PRIMITIVE FURNACE";

        float titleWidth =
                textRenderer.measureText(
                        title,
                        titleScale
                );

        textRenderer.drawText(
                title,
                panelX +
                        (
                                panelWidth -
                                        titleWidth
                        ) /
                                2.0f,
                panelY +
                        30.0f,
                titleScale,
                framebufferWidth,
                framebufferHeight
        );

        /*
         * Machine inventory labels.
         */
        float machineLabelY =
                machineY -
                        28.0f;

        textRenderer.drawText(
                "INPUT",
                inputX,
                machineLabelY,
                labelScale,
                framebufferWidth,
                framebufferHeight
        );

        textRenderer.drawText(
                "FUEL",
                fuelX,
                machineLabelY,
                labelScale,
                framebufferWidth,
                framebufferHeight
        );

        textRenderer.drawText(
                "OUTPUT",
                outputX,
                machineLabelY,
                labelScale,
                framebufferWidth,
                framebufferHeight
        );

        /*
         * Player inventory label.
         */
        textRenderer.drawText(
                "PLAYER",
                playerX,
                playerY -
                        28.0f,
                labelScale,
                framebufferWidth,
                framebufferHeight
        );

        String controls =
                "ENTER: TRANSFER   ESC: CLOSE";

        float controlsScale =
                0.8f;

        float controlsWidth =
                textRenderer.measureText(
                        controls,
                        controlsScale
                );

        textRenderer.drawText(
                controls,
                panelX +
                        (
                                panelWidth -
                                        controlsWidth
                        ) /
                                2.0f,
                panelY +
                        panelHeight -
                        35.0f,
                controlsScale,
                framebufferWidth,
                framebufferHeight
        );
    }



    public void destroy() {
        painter.destroy();
    }
}