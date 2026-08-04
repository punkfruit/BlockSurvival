package com.daniel.blocksurvival.inventory;

import com.daniel.blocksurvival.world.BlockType;

import java.util.EnumMap;
import java.util.Map;

public final class Items {

    private static final Map<BlockType, ItemDefinition>
            BLOCK_ITEMS =
            new EnumMap<>(
                    BlockType.class
            );

    static {
        for (BlockType blockType : BlockType.values()) {
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