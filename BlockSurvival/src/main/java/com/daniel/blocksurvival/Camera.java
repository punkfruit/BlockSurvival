package com.daniel.blocksurvival;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.daniel.blocksurvival.world.BlockType;
import com.daniel.blocksurvival.world.World;
import com.daniel.blocksurvival.world.BlockModel;

public class Camera {

    private final Vector3f position;

    private final Vector3f front =
            new Vector3f(0.0f, 0.0f, -1.0f);

    private final Vector3f up =
            new Vector3f(0.0f, 1.0f, 0.0f);

    private float yaw = -90.0f;
    private float pitch = 0.0f;

    private float verticalVelocity = 0.0f;

    private final Vector3f requestedMovement =
            new Vector3f();

    private boolean grounded = false;
    private boolean jumpRequested = false;
    private boolean inWater = false;

    private static final float PLAYER_RADIUS = 0.3f;
    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float EYE_HEIGHT = 1.6f;

    private static final float GRAVITY = -25.0f;
    private static final float JUMP_STRENGTH = 9.0f;
    private static final float WATER_GRAVITY = -4.0f;

    private static final float WATER_MOVE_MULTIPLIER = 0.45f;

    private static final float SWIM_UP_SPEED = 4.5f;

    private static final float MAX_WATER_SINK_SPEED = -2.0f;

    public Camera(Vector3f startingPosition) {
        position = new Vector3f(startingPosition);
    }

    public Matrix4f createViewMatrix() {
        Vector3f target = new Vector3f(position)
                .add(front);

        return new Matrix4f().lookAt(
                position,
                target,
                up
        );
    }

    public void updatePhysics(
            World world,
            float deltaTime
    ) {
        /*
         * Check the player's full collision box rather than
         * only checking the block containing the camera.
         */
        inWater = overlapsWater(world);

        if (inWater) {
            /*
             * Water slows horizontal movement.
             */
            requestedMovement.mul(
                    WATER_MOVE_MULTIPLIER
            );

            /*
             * Holding Space requests upward swimming.
             */
            if (jumpRequested) {
                verticalVelocity =
                        SWIM_UP_SPEED;
            } else {
                /*
                 * Water still pulls the player downward,
                 * but much more gently than normal gravity.
                 */
                verticalVelocity +=
                        WATER_GRAVITY *
                                deltaTime;

                if (
                        verticalVelocity <
                                MAX_WATER_SINK_SPEED
                ) {
                    verticalVelocity =
                            MAX_WATER_SINK_SPEED;
                }
            }

            grounded = false;

        } else {
            /*
             * Ordinary ground jumping.
             */
            if (jumpRequested && grounded) {
                verticalVelocity =
                        JUMP_STRENGTH;

                grounded = false;
            }

            /*
             * Ordinary gravity.
             */
            verticalVelocity +=
                    GRAVITY *
                            deltaTime;
        }

        jumpRequested = false;

        /*
         * Resolve horizontal movement one axis at a time.
         */
        moveHorizontal(
                world,
                requestedMovement.x,
                requestedMovement.z
        );

        /*
         * Resolve falling, jumping, or swimming.
         */
        moveVertical(
                world,
                verticalVelocity *
                        deltaTime
        );

        /*
         * Input only lasts for one frame.
         */
        requestedMovement.zero();
    }

    private void moveHorizontal(
            World world,
            float moveX,
            float moveZ
    ) {
        moveAlongX(world, moveX);
        moveAlongZ(world, moveZ);
    }

    private void moveAlongX(
            World world,
            float amount
    ) {
        if (amount == 0.0f) {
            return;
        }

        /*
         * Break large movements into small steps so the
         * player cannot pass straight through thin walls.
         */
        float remaining = amount;

        while (Math.abs(remaining) > 0.0001f) {
            float step =
                    Math.min(
                            Math.abs(remaining),
                            0.1f
                    ) * Math.signum(remaining);

            float nextX = position.x + step;

            if (canOccupy(
                    world,
                    nextX,
                    position.y,
                    position.z
            )) {
                position.x = nextX;
                remaining -= step;
            } else {
                break;
            }
        }
    }

    private void moveAlongZ(
            World world,
            float amount
    ) {
        if (amount == 0.0f) {
            return;
        }

        float remaining = amount;

        while (Math.abs(remaining) > 0.0001f) {
            float step =
                    Math.min(
                            Math.abs(remaining),
                            0.1f
                    ) * Math.signum(remaining);

            float nextZ = position.z + step;

            if (canOccupy(
                    world,
                    position.x,
                    position.y,
                    nextZ
            )) {
                position.z = nextZ;
                remaining -= step;
            } else {
                break;
            }
        }
    }

    private void moveVertical(
            World world,
            float amount
    ) {
        if (amount == 0.0f) {
            return;
        }

        grounded = false;

        float remaining = amount;

        while (Math.abs(remaining) > 0.0001f) {
            float step =
                    Math.min(
                            Math.abs(remaining),
                            0.05f
                    ) * Math.signum(remaining);

            float nextY = position.y + step;

            if (canOccupy(
                    world,
                    position.x,
                    nextY,
                    position.z
            )) {
                position.y = nextY;
                remaining -= step;
            } else {
                /*
                 * A downward collision means we landed.
                 * An upward collision means we hit a ceiling.
                 */
                if (amount < 0.0f) {
                    grounded = true;
                }

                verticalVelocity = 0.0f;
                break;
            }
        }
    }

    private boolean canOccupy(
            World world,
            float cameraX,
            float cameraY,
            float cameraZ
    ) {
        float minimumX =
                cameraX - PLAYER_RADIUS;

        float maximumX =
                cameraX + PLAYER_RADIUS;

        float feetY =
                cameraY - EYE_HEIGHT;

        float minimumY =
                feetY;

        float maximumY =
                feetY + PLAYER_HEIGHT;

        float minimumZ =
                cameraZ - PLAYER_RADIUS;

        float maximumZ =
                cameraZ + PLAYER_RADIUS;

        /*
         * Blocks are centered on integer coordinates, so
         * their boundaries occur at coordinate +/- 0.5.
         */
        int minimumBlockX =
                (int) Math.floor(minimumX + 0.5f);

        int maximumBlockX =
                (int) Math.floor(maximumX + 0.5f);

        int minimumBlockY =
                (int) Math.floor(minimumY + 0.5f);

        int maximumBlockY =
                (int) Math.floor(maximumY + 0.5f);

        int minimumBlockZ =
                (int) Math.floor(minimumZ + 0.5f);

        int maximumBlockZ =
                (int) Math.floor(maximumZ + 0.5f);

        for (
                int blockX = minimumBlockX;
                blockX <= maximumBlockX;
                blockX++
        ) {
            for (
                    int blockY = minimumBlockY;
                    blockY <= maximumBlockY;
                    blockY++
            ) {
                for (
                        int blockZ = minimumBlockZ;
                        blockZ <= maximumBlockZ;
                        blockZ++
                ) {
                    BlockType block =
                            world.getBlock(
                                    blockX,
                                    blockY,
                                    blockZ
                            );

                    if (isCollidable(block)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean overlapsWater(
            World world
    ) {
        /*
         * Player collision box.
         */
        float playerMinimumX =
                position.x - PLAYER_RADIUS;

        float playerMaximumX =
                position.x + PLAYER_RADIUS;

        float playerFeetY =
                position.y - EYE_HEIGHT;

        float playerMinimumY =
                playerFeetY;

        float playerMaximumY =
                playerFeetY + PLAYER_HEIGHT;

        float playerMinimumZ =
                position.z - PLAYER_RADIUS;

        float playerMaximumZ =
                position.z + PLAYER_RADIUS;

        /*
         * Find every block coordinate touched by the
         * player's collision box.
         */
        int minimumBlockX =
                (int) Math.floor(
                        playerMinimumX + 0.5f
                );

        int maximumBlockX =
                (int) Math.floor(
                        playerMaximumX + 0.5f
                );

        int minimumBlockY =
                (int) Math.floor(
                        playerMinimumY + 0.5f
                );

        int maximumBlockY =
                (int) Math.floor(
                        playerMaximumY + 0.5f
                );

        int minimumBlockZ =
                (int) Math.floor(
                        playerMinimumZ + 0.5f
                );

        int maximumBlockZ =
                (int) Math.floor(
                        playerMaximumZ + 0.5f
                );

        for (
                int blockX = minimumBlockX;
                blockX <= maximumBlockX;
                blockX++
        ) {
            for (
                    int blockY = minimumBlockY;
                    blockY <= maximumBlockY;
                    blockY++
            ) {
                for (
                        int blockZ = minimumBlockZ;
                        blockZ <= maximumBlockZ;
                        blockZ++
                ) {
                    BlockType block =
                            world.getBlock(
                                    blockX,
                                    blockY,
                                    blockZ
                            );

                    if (block != BlockType.WATER) {
                        continue;
                    }

                    /*
                     * Water uses its configurable top height.
                     */
                    float waterMinimumX =
                            blockX - 0.5f;

                    float waterMaximumX =
                            blockX + 0.5f;

                    float waterMinimumY =
                            blockY - 0.5f;

                    float waterMaximumY =
                            blockY +
                                    block.getTopOffset();

                    float waterMinimumZ =
                            blockZ - 0.5f;

                    float waterMaximumZ =
                            blockZ + 0.5f;

                    boolean overlaps =
                            playerMaximumX >
                                    waterMinimumX &&
                                    playerMinimumX <
                                            waterMaximumX &&

                                    playerMaximumY >
                                            waterMinimumY &&
                                    playerMinimumY <
                                            waterMaximumY &&

                                    playerMaximumZ >
                                            waterMinimumZ &&
                                    playerMinimumZ <
                                            waterMaximumZ;

                    if (overlaps) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    private boolean isCollidable(BlockType block) {
        return block != null &&
                block != BlockType.WATER &&
                block.getModel() == BlockModel.CUBE;
    }

    private void move(
            World world,
            float dx,
            float dz
    ) {
        // we'll fill this in next
    }

    public void moveForward(float amount) {
        Vector3f direction =
                getHorizontalForward();

        requestedMovement.add(
                direction.mul(amount)
        );
    }

    public void moveBackward(float amount) {
        Vector3f direction =
                getHorizontalForward()
                        .negate();

        requestedMovement.add(
                direction.mul(amount)
        );
    }

    public void moveLeft(float amount) {
        Vector3f direction =
                calculateRightDirection()
                        .negate();

        direction.y = 0.0f;
        direction.normalize();

        requestedMovement.add(
                direction.mul(amount)
        );
    }

    public void moveRight(float amount) {
        Vector3f direction =
                calculateRightDirection();

        direction.y = 0.0f;
        direction.normalize();

        requestedMovement.add(
                direction.mul(amount)
        );
    }

    public void jump() {
        jumpRequested = true;
    }

    public void moveUp(float amount) {
        position.y += amount;
    }

    public void moveDown(float amount) {
        position.y -= amount;
    }

    public void rotate(
            float horizontalOffset,
            float verticalOffset
    ) {
        yaw += horizontalOffset;
        pitch += verticalOffset;

        if (pitch > 89.0f) {
            pitch = 89.0f;
        }

        if (pitch < -89.0f) {
            pitch = -89.0f;
        }

        Vector3f newDirection = new Vector3f();

        newDirection.x =
                (float) (
                        Math.cos(Math.toRadians(yaw))
                                * Math.cos(Math.toRadians(pitch))
                );

        newDirection.y =
                (float) Math.sin(
                        Math.toRadians(pitch)
                );

        newDirection.z =
                (float) (
                        Math.sin(Math.toRadians(yaw))
                                * Math.cos(Math.toRadians(pitch))
                );

        front.set(newDirection).normalize();
    }

    private Vector3f getHorizontalForward() {
        Vector3f direction =
                new Vector3f(front);

        direction.y = 0.0f;

        /*
         * Avoid normalizing a nearly empty vector when
         * looking almost perfectly upward or downward.
         */
        if (direction.lengthSquared() > 0.0001f) {
            direction.normalize();
        }

        return direction;
    }

    private Vector3f calculateRightDirection() {
        return new Vector3f(front)
                .cross(up)
                .normalize();
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public boolean isInWater() {
        return inWater;
    }

    public boolean isCameraUnderwater(
            World world
    ) {
        /*
         * position is the camera's actual eye position.
         */
        float cameraX = position.x;
        float cameraY = position.y;
        float cameraZ = position.z;

        /*
         * Blocks are centered on integer coordinates.
         */
        int blockX =
                (int) Math.floor(
                        cameraX + 0.5f
                );

        int blockY =
                (int) Math.floor(
                        cameraY + 0.5f
                );

        int blockZ =
                (int) Math.floor(
                        cameraZ + 0.5f
                );

        BlockType block =
                world.getBlock(
                        blockX,
                        blockY,
                        blockZ
                );

        if (block != BlockType.WATER) {
            return false;
        }

        /*
         * Check against the actual lowered water surface.
         */
        float waterSurfaceY =
                blockY +
                        block.getTopOffset();

        return cameraY <
                waterSurfaceY;
    }

    public Vector3f getFront() {
        return new Vector3f(front);
    }

    public boolean overlapsBlock(
            int blockX,
            int blockY,
            int blockZ
    ) {
        float playerMinX =
                position.x - PLAYER_RADIUS;

        float playerMaxX =
                position.x + PLAYER_RADIUS;

        float playerMinY =
                position.y - EYE_HEIGHT;

        float playerMaxY =
                playerMinY + PLAYER_HEIGHT;

        float playerMinZ =
                position.z - PLAYER_RADIUS;

        float playerMaxZ =
                position.z + PLAYER_RADIUS;

        /*
         * Blocks are centered on integer coordinates,
         * so each block extends 0.5 in every direction.
         */
        float blockMinX = blockX - 0.5f;
        float blockMaxX = blockX + 0.5f;

        float blockMinY = blockY - 0.5f;
        float blockMaxY = blockY + 0.5f;

        float blockMinZ = blockZ - 0.5f;
        float blockMaxZ = blockZ + 0.5f;

        return playerMaxX > blockMinX &&
                playerMinX < blockMaxX &&

                playerMaxY > blockMinY &&
                playerMinY < blockMaxY &&

                playerMaxZ > blockMinZ &&
                playerMinZ < blockMaxZ;
    }
}