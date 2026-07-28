package com.daniel.blocksurvival;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    private final Vector3f position;

    private final Vector3f front =
            new Vector3f(0.0f, 0.0f, -1.0f);

    private final Vector3f up =
            new Vector3f(0.0f, 1.0f, 0.0f);

    private float yaw = -90.0f;
    private float pitch = 0.0f;

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

    public void moveForward(float amount) {
        position.add(
                new Vector3f(front).mul(amount)
        );
    }

    public void moveBackward(float amount) {
        position.sub(
                new Vector3f(front).mul(amount)
        );
    }

    public void moveLeft(float amount) {
        Vector3f right = calculateRightDirection();

        position.sub(
                right.mul(amount)
        );
    }

    public void moveRight(float amount) {
        Vector3f right = calculateRightDirection();

        position.add(
                right.mul(amount)
        );
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

    private Vector3f calculateRightDirection() {
        return new Vector3f(front)
                .cross(up)
                .normalize();
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getFront() {
        return new Vector3f(front);
    }
}