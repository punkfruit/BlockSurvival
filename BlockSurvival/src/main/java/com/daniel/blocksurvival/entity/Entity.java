package com.daniel.blocksurvival.entity;

import com.daniel.blocksurvival.world.World;
import org.joml.Vector3f;

public abstract class Entity {



    protected final Vector3f position =
            new Vector3f();

    protected final Vector3f velocity =
            new Vector3f();

    protected boolean removed;

    protected Entity(
            float x,
            float y,
            float z
    ) {
        position.set(
                x,
                y,
                z
        );
    }

    public abstract void update(
            World world,
            Vector3f playerPosition,
            float deltaTime
    );

    public abstract EntityCategory getCategory();

    public abstract EntityType getType();

    public Vector3f getPosition() {
        return new Vector3f(
                position
        );
    }

    public Vector3f getVelocity() {
        return new Vector3f(
                velocity
        );
    }

    public boolean isRemoved() {
        return removed;
    }

    public void remove() {
        removed = true;
    }
}