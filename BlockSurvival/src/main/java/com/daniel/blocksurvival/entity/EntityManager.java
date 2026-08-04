package com.daniel.blocksurvival.entity;

import com.daniel.blocksurvival.world.World;
import org.joml.Vector3f;

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
                    deltaTime
            );
        }

        updating =
                false;

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
}