package com.daniel.blocksurvival.world;

public record PlayerSaveData(
        float positionX,
        float positionY,
        float positionZ,
        float yaw,
        float pitch,
        int selectedHotbarSlot
) {
}