package com.daniel.blocksurvival.world.noise;

public class ValueNoise3D {

    private final int seed;

    /*
     * Each random noise value is placed at a point on a
     * three-dimensional grid.
     *
     * A larger grid size creates broader, smoother shapes.
     * A smaller grid size creates tighter, busier shapes.
     */
    private static final int GRID_SIZE = 16;

    public ValueNoise3D(int seed) {
        this.seed = seed;
    }

    /*
     * Smoothly changes a value from 0 through 1.
     *
     * Without this, interpolation would create more obvious
     * straight transitions between grid points.
     */
    private float smooth(float value) {
        return value * value * (3.0f - 2.0f * value);
    }

    /*
     * Linearly interpolate between two values.
     */
    private float lerp(
            float start,
            float end,
            float amount
    ) {
        return start +
                (end - start) * amount;
    }

    /*
     * Produce a deterministic random value for one point on
     * the three-dimensional noise grid.
     *
     * The same X, Y, Z coordinates and seed will always
     * produce the same result.
     */
    private float randomValue(
            int x,
            int y,
            int z
    ) {
        int hash =
                x * 374761393 +
                        y * 668265263 +
                        z * 214748364 +
                        seed * 982451653;

        hash =
                (hash ^ (hash >> 13))
                        * 1274126177;

        hash ^=
                hash >> 16;

        return
                (hash & 0x7fffffff)
                        / (float) Integer.MAX_VALUE;
    }

    public float sample(
            float worldX,
            float worldY,
            float worldZ
    ) {
        /*
         * Determine which noise-grid cell contains this
         * world position.
         */
        int gridX =
                (int) Math.floor(
                        worldX / GRID_SIZE
                );

        int gridY =
                (int) Math.floor(
                        worldY / GRID_SIZE
                );

        int gridZ =
                (int) Math.floor(
                        worldZ / GRID_SIZE
                );

        /*
         * Find the position inside the current grid cell.
         *
         * Each value ranges from 0.0 to 1.0.
         */
        float localX =
                (worldX - gridX * GRID_SIZE)
                        / GRID_SIZE;

        float localY =
                (worldY - gridY * GRID_SIZE)
                        / GRID_SIZE;

        float localZ =
                (worldZ - gridZ * GRID_SIZE)
                        / GRID_SIZE;

        localX = smooth(localX);
        localY = smooth(localY);
        localZ = smooth(localZ);

        /*
         * Sample the four corners on the lower Y layer.
         */
        float lowerFrontLeft =
                randomValue(
                        gridX,
                        gridY,
                        gridZ
                );

        float lowerFrontRight =
                randomValue(
                        gridX + 1,
                        gridY,
                        gridZ
                );

        float lowerBackLeft =
                randomValue(
                        gridX,
                        gridY,
                        gridZ + 1
                );

        float lowerBackRight =
                randomValue(
                        gridX + 1,
                        gridY,
                        gridZ + 1
                );

        /*
         * Sample the four corners on the upper Y layer.
         */
        float upperFrontLeft =
                randomValue(
                        gridX,
                        gridY + 1,
                        gridZ
                );

        float upperFrontRight =
                randomValue(
                        gridX + 1,
                        gridY + 1,
                        gridZ
                );

        float upperBackLeft =
                randomValue(
                        gridX,
                        gridY + 1,
                        gridZ + 1
                );

        float upperBackRight =
                randomValue(
                        gridX + 1,
                        gridY + 1,
                        gridZ + 1
                );

        /*
         * Interpolate across X on the lower layer.
         */
        float lowerFront =
                lerp(
                        lowerFrontLeft,
                        lowerFrontRight,
                        localX
                );

        float lowerBack =
                lerp(
                        lowerBackLeft,
                        lowerBackRight,
                        localX
                );

        /*
         * Interpolate across X on the upper layer.
         */
        float upperFront =
                lerp(
                        upperFrontLeft,
                        upperFrontRight,
                        localX
                );

        float upperBack =
                lerp(
                        upperBackLeft,
                        upperBackRight,
                        localX
                );

        /*
         * Interpolate across Z to produce one value for each
         * Y layer.
         */
        float lowerValue =
                lerp(
                        lowerFront,
                        lowerBack,
                        localZ
                );

        float upperValue =
                lerp(
                        upperFront,
                        upperBack,
                        localZ
                );

        /*
         * Finally, interpolate between the lower and upper
         * layers across Y.
         */
        return lerp(
                lowerValue,
                upperValue,
                localY
        );
    }

    public float sampleOctaves(
            float worldX,
            float worldY,
            float worldZ,
            int octaveCount,
            float persistence
    ) {
        float total = 0.0f;
        float amplitude = 1.0f;
        float frequency = 1.0f;
        float maximumValue = 0.0f;

        for (
                int octave = 0;
                octave < octaveCount;
                octave++
        ) {
            float noiseValue =
                    sample(
                            worldX * frequency,
                            worldY * frequency,
                            worldZ * frequency
                    );

            total += noiseValue * amplitude;
            maximumValue += amplitude;

            frequency *= 2.0f;
            amplitude *= persistence;
        }

        return total / maximumValue;
    }
}