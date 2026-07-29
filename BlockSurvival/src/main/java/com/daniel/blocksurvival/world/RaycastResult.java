package com.daniel.blocksurvival.world;

public record RaycastResult(
        int hitX,
        int hitY,
        int hitZ,
        int placementX,
        int placementY,
        int placementZ
) {
}