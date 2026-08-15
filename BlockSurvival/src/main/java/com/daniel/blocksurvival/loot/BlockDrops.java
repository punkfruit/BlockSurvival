package com.daniel.blocksurvival.loot;

import com.daniel.blocksurvival.inventory.ItemDefinition;
import com.daniel.blocksurvival.inventory.Items;
import com.daniel.blocksurvival.world.BlockType;

import java.util.List;

public final class BlockDrops {

    private BlockDrops() {
    }

    public static List<ItemDrop> getDrops(
            BlockType blockType
    ) {
        if (blockType == null) {
            return List.of();
        }

        return switch (blockType) {
            case IRON_ORE ->
                    List.of(
                            new ItemDrop(
                                    Items.RAW_IRON,
                                    1
                            )
                    );

            case COAL_ORE ->
                    List.of(
                            new ItemDrop(
                                    Items.COAL,
                                    1
                            )
                    );

            /*
             * Default behavior:
             *
             * Ordinary blocks simply drop themselves.
             */
            default -> {
                ItemDefinition blockItem =
                        Items.fromBlock(
                                blockType
                        );

                if (blockItem == null) {
                    yield List.of();
                }

                yield List.of(
                        new ItemDrop(
                                blockItem,
                                1
                        )
                );
            }
        };
    }
}