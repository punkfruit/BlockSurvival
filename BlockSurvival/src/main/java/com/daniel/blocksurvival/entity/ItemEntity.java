package com.daniel.blocksurvival.entity;

import com.daniel.blocksurvival.world.BlockModel;
import com.daniel.blocksurvival.world.BlockType;
import com.daniel.blocksurvival.world.World;
import com.daniel.blocksurvival.inventory.ItemCollector;
import com.daniel.blocksurvival.inventory.ItemDefinition;
import com.daniel.blocksurvival.inventory.Items;
import org.joml.Vector3f;

public class ItemEntity
        extends Entity {

    @Override
    public EntityCategory getCategory() {
        return EntityCategory.ITEM;
    }

    @Override
    public EntityType getType() {
        return EntityType.DROPPED_ITEM;
    }

    private static final float GRAVITY =
            -18.0f;

    private static final float TERMINAL_VELOCITY =
            -18.0f;

    /*
     * The dropped cube will eventually be about one quarter
     * of a block wide.
     */
    private static final float HALF_SIZE =
            0.125f;

    private static final float PICKUP_RADIUS =
            1.25f;

    private static final float PICKUP_DELAY_SECONDS =
            3f;//0.40f default

    private static final float DESPAWN_SECONDS =
            300.0f;

    private final BlockType blockType;

    private float age;

    private float rotation;

    private boolean grounded;
    private boolean hasBounced;

    private static final float ATTRACTION_RADIUS =
            2.5f;

    private static final float ATTRACTION_ACCELERATION =
            24.0f;

    private static final float MAX_ATTRACTION_SPEED =
            8.0f;

    public ItemEntity(
            float x,
            float y,
            float z,
            BlockType blockType
    ) {
        super(
                x,
                y,
                z
        );

        if (blockType == null) {
            throw new IllegalArgumentException(
                    "An item entity requires a block type."
            );
        }

        this.blockType =
                blockType;

        /*
         * Give the item a small initial hop.
         */
        velocity.y =
                2.5f;

        /*
         * Add a little deterministic sideways movement.
         *
         * We can randomize this more elegantly later.
         */
        velocity.x =
                0.55f;

        velocity.z =
                -0.35f;
    }

    @Override
    public void update(
            World world,
            Vector3f playerPosition,
            ItemCollector itemCollector,
            float deltaTime
    ) {
        if (removed) {
            return;
        }

        age +=
                deltaTime;

        if (
                age >=
                        DESPAWN_SECONDS
        ) {
            remove();
            return;
        }

        /*
         * Rotate continuously for the future renderer.
         */
        float rotationSpeed =
                grounded
                        ? 45.0f
                        : 140.0f;

        rotation +=
                rotationSpeed *
                        deltaTime;

        if (rotation >= 360.0f) {
            rotation -=
                    360.0f;
        }

        applyAttraction(
                playerPosition,
                deltaTime
        );

        applyPhysics(
                world,
                deltaTime
        );

        attemptPickup(
                playerPosition,
                itemCollector
        );

        /*
        System.out.println(
                blockType +
                        " position: " +
                        position.x + ", " +
                        position.y + ", " +
                        position.z +
                        " grounded: " +
                        grounded
        );

         */
    }

    private void applyPhysics(
            World world,
            float deltaTime
    ) {
        if (!grounded) {
            velocity.y +=
                    GRAVITY *
                            deltaTime;

            if (
                    velocity.y <
                            TERMINAL_VELOCITY
            ) {
                velocity.y =
                        TERMINAL_VELOCITY;
            }
        }

        /*
         * Air resistance and ground friction.
         */
        float horizontalDrag =
                grounded
                        ? 0.82f
                        : 0.98f;

        velocity.x *=
                (float) Math.pow(
                        horizontalDrag,
                        deltaTime * 60.0f
                );

        velocity.z *=
                (float) Math.pow(
                        horizontalDrag,
                        deltaTime * 60.0f
                );

        moveAlongX(
                world,
                velocity.x *
                        deltaTime
        );

        moveAlongZ(
                world,
                velocity.z *
                        deltaTime
        );

        moveVertically(
                world,
                velocity.y *
                        deltaTime
        );
    }

    private void applyAttraction(
            Vector3f playerPosition,
            float deltaTime
    ) {
        if (
                playerPosition == null ||
                        age <
                                PICKUP_DELAY_SECONDS
        ) {
            return;
        }

        Vector3f directionToPlayer =
                new Vector3f(
                        playerPosition
                ).sub(
                        position
                );

        float distanceSquared =
                directionToPlayer.lengthSquared();

        if (
                distanceSquared >
                        ATTRACTION_RADIUS *
                                ATTRACTION_RADIUS
        ) {
            return;
        }

        /*
         * Avoid normalizing an effectively zero-length vector.
         */
        if (distanceSquared < 0.0001f) {
            return;
        }

        directionToPlayer.normalize();

        /*
         * Accelerate the item toward the player's body center.
         */
        velocity.fma(
                ATTRACTION_ACCELERATION *
                        deltaTime,
                directionToPlayer
        );

        /*
         * Once attraction begins, allow the item to lift away
         * from the floor.
         */
        grounded =
                false;

        /*
         * Prevent the item from becoming a tiny railgun slug.
         */
        float speedSquared =
                velocity.lengthSquared();

        if (
                speedSquared >
                        MAX_ATTRACTION_SPEED *
                                MAX_ATTRACTION_SPEED
        ) {
            velocity.normalize()
                    .mul(
                            MAX_ATTRACTION_SPEED
                    );
        }
    }

    private void moveAlongX(
            World world,
            float amount
    ) {
        float remaining =
                amount;

        while (
                Math.abs(
                        remaining
                ) > 0.0001f
        ) {
            float step =
                    Math.min(
                            Math.abs(
                                    remaining
                            ),
                            0.05f
                    ) *
                            Math.signum(
                                    remaining
                            );

            float nextX =
                    position.x +
                            step;

            if (
                    canOccupy(
                            world,
                            nextX,
                            position.y,
                            position.z
                    )
            ) {
                position.x =
                        nextX;

                remaining -=
                        step;
            }
            else {
                velocity.x *=
                        -0.20f;

                break;
            }
        }
    }

    private void moveAlongZ(
            World world,
            float amount
    ) {
        float remaining =
                amount;

        while (
                Math.abs(
                        remaining
                ) > 0.0001f
        ) {
            float step =
                    Math.min(
                            Math.abs(
                                    remaining
                            ),
                            0.05f
                    ) *
                            Math.signum(
                                    remaining
                            );

            float nextZ =
                    position.z +
                            step;

            if (
                    canOccupy(
                            world,
                            position.x,
                            position.y,
                            nextZ
                    )
            ) {
                position.z =
                        nextZ;

                remaining -=
                        step;
            }
            else {
                velocity.z *=
                        -0.20f;

                break;
            }
        }
    }

    private void moveVertically(
            World world,
            float amount
    ) {
        if (amount == 0.0f) {
            return;
        }

        grounded =
                false;

        float remaining =
                amount;

        while (
                Math.abs(
                        remaining
                ) > 0.0001f
        ) {
            float step =
                    Math.min(
                            Math.abs(
                                    remaining
                            ),
                            0.04f
                    ) *
                            Math.signum(
                                    remaining
                            );

            float nextY =
                    position.y +
                            step;

            if (
                    canOccupy(
                            world,
                            position.x,
                            nextY,
                            position.z
                    )
            ) {
                position.y =
                        nextY;

                remaining -=
                        step;
            }
            else {
                if (amount < 0.0f) {
                    /*
                     * Allow one noticeable landing bounce.
                     * Later collisions make the item settle completely.
                     */
                    if (
                            !hasBounced &&
                                    velocity.y < -2.0f
                    ) {
                        velocity.y =
                                -velocity.y *
                                        0.22f;

                        hasBounced =
                                true;

                        grounded =
                                false;
                    }
                    else {
                        velocity.y =
                                0.0f;

                        grounded =
                                true;
                    }
                }
                else {
                    velocity.y =
                            0.0f;
                }

                break;
            }
        }
    }

    private boolean canOccupy(
            World world,
            float x,
            float y,
            float z
    ) {
        float minimumX =
                x -
                        HALF_SIZE;

        float maximumX =
                x +
                        HALF_SIZE;

        float minimumY =
                y -
                        HALF_SIZE;

        float maximumY =
                y +
                        HALF_SIZE;

        float minimumZ =
                z -
                        HALF_SIZE;

        float maximumZ =
                z +
                        HALF_SIZE;

        /*
         * Blocks are centered on whole-number coordinates,
         * with their boundaries at coordinate ± 0.5.
         */
        int minimumBlockX =
                (int) Math.floor(
                        minimumX +
                                0.5f
                );

        int maximumBlockX =
                (int) Math.floor(
                        maximumX +
                                0.5f
                );

        int minimumBlockY =
                (int) Math.floor(
                        minimumY +
                                0.5f
                );

        int maximumBlockY =
                (int) Math.floor(
                        maximumY +
                                0.5f
                );

        int minimumBlockZ =
                (int) Math.floor(
                        minimumZ +
                                0.5f
                );

        int maximumBlockZ =
                (int) Math.floor(
                        maximumZ +
                                0.5f
                );

        for (
                int blockX =
                minimumBlockX;
                blockX <=
                        maximumBlockX;
                blockX++
        ) {
            for (
                    int blockY =
                    minimumBlockY;
                    blockY <=
                            maximumBlockY;
                    blockY++
            ) {
                for (
                        int blockZ =
                        minimumBlockZ;
                        blockZ <=
                                maximumBlockZ;
                        blockZ++
                ) {
                    BlockType block =
                            world.getBlock(
                                    blockX,
                                    blockY,
                                    blockZ
                            );

                    if (
                            isCollidable(
                                    block
                            )
                    ) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isCollidable(
            BlockType block
    ) {
        return block != null &&
                block !=
                        BlockType.WATER &&
                block.getModel() ==
                        BlockModel.CUBE;
    }

    private void attemptPickup(
            Vector3f playerPosition,
            ItemCollector itemCollector
    ) {
        if (
                playerPosition == null ||
                        itemCollector == null ||
                        age < PICKUP_DELAY_SECONDS
        ) {
            return;
        }

        float distanceSquared =
                position.distanceSquared(
                        playerPosition
                );

        if (
                distanceSquared >
                        PICKUP_RADIUS *
                                PICKUP_RADIUS
        ) {
            return;
        }

        ItemDefinition definition =
                Items.fromBlock(
                        blockType
                );

        if (definition == null) {
            System.err.println(
                    "No item definition exists for " +
                            blockType
            );

            return;
        }

        int quantityNotAccepted =
                itemCollector.collect(
                        definition,
                        1
                );

        /*
         * The entire dropped stack fit.
         */
        if (quantityNotAccepted == 0) {
            System.out.println(
                    "Picked up " +
                            definition.displayName()
            );

            remove();
            return;
        }

        /*
         * The inventory had no room.
         *
         * Leave the entity in the world rather than deleting it.
         */
        System.out.println(
                "No inventory space for " +
                        definition.displayName()
        );
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public float getRotation() {
        return rotation;
    }

    public float getAge() {
        return age;
    }

    public boolean isGrounded() {
        return grounded;
    }
}