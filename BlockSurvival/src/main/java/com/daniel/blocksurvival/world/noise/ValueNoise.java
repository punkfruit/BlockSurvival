package com.daniel.blocksurvival.world.noise;

import java.util.Random;

public class ValueNoise {

    private final int seed;

    private static final int GRID_SIZE = 16;



    private float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    public ValueNoise(int seed) {
        this.seed = seed;
    }

    private float randomValue(int x, int z) {

        long combinedSeed =
                x * 341873128712L
                        + z * 132897987541L
                        + seed;

        Random random =
                new Random(combinedSeed);

        return random.nextFloat();
    }

    private float lerp(
            float a,
            float b,
            float t
    ) {
        return a + (b - a) * t;
    }

    public float sampleOctaves(
            float worldX,
            float worldZ,
            int octaveCount,
            float persistence
    ) {
        float total = 0.0f;
        float amplitude = 1.0f;
        float frequency = 1.0f;
        float maximumValue = 0.0f;

        for (int octave = 0; octave < octaveCount; octave++) {
            float noiseValue =
                    sample(
                            worldX * frequency,
                            worldZ * frequency
                    );

            total += noiseValue * amplitude;
            maximumValue += amplitude;

            frequency *= 2.0f;
            amplitude *= persistence;
        }

        return total / maximumValue;
    }

    public float sample(
            float worldX,
            float worldZ
    ) {
        int gridX =
                (int)Math.floor(worldX / GRID_SIZE);

        int gridZ =
                (int)Math.floor(worldZ / GRID_SIZE);

        float localX =
                (worldX - gridX * GRID_SIZE)
                        / GRID_SIZE;

        float localZ =
                (worldZ - gridZ * GRID_SIZE)
                        / GRID_SIZE;

        localX = smooth(localX);
        localZ = smooth(localZ);

        float topLeft =
                randomValue(
                        gridX,
                        gridZ
                );
        float topRight =
                randomValue(
                        gridX + 1,
                        gridZ
                );
        float bottomLeft =
                randomValue(
                        gridX,
                        gridZ + 1
                );
        float bottomRight =
                randomValue(
                        gridX + 1,
                        gridZ + 1
                );
        float top =
                lerp(
                        topLeft,
                        topRight,
                        localX
                );

        float bottom =
                lerp(
                        bottomLeft,
                        bottomRight,
                        localX
                );
        return lerp(
                top,
                bottom,
                localZ
        );
    }


}