package com.daniel.blocksurvival.inventory;

import com.daniel.blocksurvival.world.AtlasTile;
import com.daniel.blocksurvival.world.BlockModel;
import com.daniel.blocksurvival.world.BlockType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class Items {

    private static final Map<BlockType, ItemDefinition>
            BLOCK_ITEMS =
            new EnumMap<>(
                    BlockType.class
            );

    private static final Map<String, ItemDefinition>
            ITEMS_BY_ID =
            new HashMap<>();

    /*
     * First inventory-only item.
     *
     * placedBlock is null because this cannot be placed
     * directly into the voxel world.
     */
    public static final ItemDefinition MACHINE_CORE =
            register(
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
                    )
            );
    public static final ItemDefinition BASEBALL_BAT =
            register(
                    new ItemDefinition(
                            "baseball_bat",
                            "Baseball Bat",
                            1,
                            2,
                            1,
                            true,
                            new AtlasTile(
                                    0,
                                    4
                            ),
                            null
                    )
            );

    static {
        for (BlockType blockType : BlockType.values()) {
            AtlasTile inventoryIcon =
                    chooseBlockInventoryIcon(
                            blockType
                    );

            ItemDefinition definition =
                    register(
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

            BLOCK_ITEMS.put(
                    blockType,
                    definition
            );

        }
    }

    private Items() {
    }

    public static ItemDefinition getById(
            String id
    ) {
        return ITEMS_BY_ID.get(
                id
        );
    }
    public static ItemDefinition fromBlock(
            BlockType blockType
    ) {
        return BLOCK_ITEMS.get(
                blockType
        );
    }
    private static ItemDefinition register(
            ItemDefinition definition
    ) {
        ItemDefinition existing =
                ITEMS_BY_ID.put(
                        definition.id(),
                        definition
                );

        if (existing != null) {
            throw new IllegalStateException(
                    "Duplicate item ID: " +
                            definition.id()
            );
        }

        return definition;
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