package com.daniel.blocksurvival.inventory;

import com.daniel.blocksurvival.world.AtlasTile;
import com.daniel.blocksurvival.world.BlockModel;
import com.daniel.blocksurvival.world.BlockType;

import java.util.EnumMap;
import java.util.Map;

public final class Items {

    private static final Map<BlockType, ItemDefinition>
            BLOCK_ITEMS =
            new EnumMap<>(
                    BlockType.class
            );

    /*
     * First inventory-only item.
     *
     * placedBlock is null because this cannot be placed
     * directly into the voxel world.
     */
    public static final ItemDefinition MACHINE_CORE =
            new ItemDefinition(
                    "machine_core",
                    "Machine Core",
                    2,
                    2,
                    1,
                    true,
                    new AtlasTile(
                            4,
                            3
                    ),
                    null
            );

    static {
        for (BlockType blockType : BlockType.values()) {
            AtlasTile inventoryIcon =
                    chooseBlockInventoryIcon(
                            blockType
                    );

            BLOCK_ITEMS.put(
                    blockType,
                    new ItemDefinition(
                            "block." +
                                    blockType.name()
                                            .toLowerCase(),
                            formatName(
                                    blockType.name()
                            ),
                            1,
                            1,
                            8,
                            false,
                            inventoryIcon,
                            blockType
                    )
            );
        }
    }

    private Items() {
    }

    public static ItemDefinition fromBlock(
            BlockType blockType
    ) {
        return BLOCK_ITEMS.get(
                blockType
        );
    }

    private static AtlasTile chooseBlockInventoryIcon(
            BlockType blockType
    ) {
        return switch (blockType.getModel()) {
            case CUBE ->
                    blockType.getTopTexture();

            case CROSS, TORCH ->
                    blockType.getSideTexture();
        };
    }

    private static String formatName(
            String enumName
    ) {
        String lower =
                enumName.toLowerCase()
                        .replace(
                                '_',
                                ' '
                        );

        return Character.toUpperCase(
                lower.charAt(0)
        ) +
                lower.substring(1);
    }
}