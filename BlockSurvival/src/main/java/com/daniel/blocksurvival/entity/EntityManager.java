package com.daniel.blocksurvival.entity;

import com.daniel.blocksurvival.world.World;
import org.joml.Vector3f;
import com.daniel.blocksurvival.inventory.ItemCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntityManager {

    private final List<Entity> entities =
            new ArrayList<>();

    /*
     * Entities spawned during an update are placed here first.
     *
     * This prevents ConcurrentModificationException if an
     * entity eventually spawns another entity while the manager
     * is iterating over the main list.
     */
    private final List<Entity> pendingEntities =
            new ArrayList<>();

    private boolean updating;

    private static final float ITEM_MERGE_RADIUS =
            0.75f;

    private static final float ITEM_MERGE_RADIUS_SQUARED =
            ITEM_MERGE_RADIUS *
                    ITEM_MERGE_RADIUS;

    public void spawn(
            Entity entity
    ) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "Cannot spawn a null entity."
            );
        }

        if (updating) {
            pendingEntities.add(
                    entity
            );
        }
        else {
            entities.add(
                    entity
            );
        }
    }

    public void update(
            World world,
            Vector3f playerPosition,
            ItemCollector itemCollector,
            float deltaTime
    ) {
        updating =
                true;

        for (Entity entity : entities) {
            if (entity.isRemoved()) {
                continue;
            }

            entity.update(
                    world,
                    playerPosition,
                    itemCollector,
                    deltaTime
            );
        }

        updating =
                false;

        /*
         * Combine compatible item stacks before removing entities
         * that were consumed by another stack.
         */
        mergeNearbyItems();

        entities.removeIf(
                Entity::isRemoved
        );

        if (!pendingEntities.isEmpty()) {
            entities.addAll(
                    pendingEntities
            );

            pendingEntities.clear();
        }
    }

    /*
     * The renderer only needs to read this list.
     *
     * Returning an unmodifiable view prevents outside code
     * from adding or removing entities behind the manager's back.
     */
    public List<Entity> getEntities() {
        return Collections.unmodifiableList(
                entities
        );
    }

    public List<Entity> getEntitiesByCategory(
            EntityCategory category
    ) {
        List<Entity> matches =
                new ArrayList<>();

        for (Entity entity : entities) {
            if (
                    entity.getCategory() ==
                            category
            ) {
                matches.add(
                        entity
                );
            }
        }

        return matches;
    }

    public int getEntityCount() {
        return entities.size();
    }

    public void clear() {
        entities.clear();
        pendingEntities.clear();
    }

    private void mergeNearbyItems() {
        int entityCount =
                entities.size();

        for (
                int firstIndex = 0;
                firstIndex < entityCount;
                firstIndex++
        ) {
            Entity firstEntity =
                    entities.get(
                            firstIndex
                    );

            if (
                    !(firstEntity instanceof ItemEntity firstItem) ||
                            firstItem.isRemoved()
            ) {
                continue;
            }

            for (
                    int secondIndex =
                    firstIndex + 1;
                    secondIndex < entityCount;
                    secondIndex++
            ) {
                Entity secondEntity =
                        entities.get(
                                secondIndex
                        );

                if (
                        !(secondEntity instanceof ItemEntity secondItem) ||
                                secondItem.isRemoved()
                ) {
                    continue;
                }

                if (
                        !firstItem.canMergeWith(
                                secondItem
                        )
                ) {
                    continue;
                }

                float distanceSquared =
                        firstItem.distanceSquaredTo(
                                secondItem
                        );

                if (
                        distanceSquared >
                                ITEM_MERGE_RADIUS_SQUARED
                ) {
                    continue;
                }

                firstItem.mergeFrom(
                        secondItem
                );
            }
        }
    }
}