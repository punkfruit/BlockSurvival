package com.daniel.blocksurvival.graphics;

import org.joml.Vector3f;

public class Sky {

    /*
     * 0.00 = midnight
     * 0.25 = sunrise
     * 0.50 = noon
     * 0.75 = sunset
     * 1.00 = midnight again
     */
    private float worldTime =
            0.25f;

    /*
     * Keep this short while testing.
     *
     * Later, something like 1200 seconds would create
     * a twenty-minute full day.
     */
    private float dayLengthSeconds =
            20.0f;

    private static final Vector3f MIDNIGHT_COLOR =
            new Vector3f(
                    0.015f,
                    0.025f,
                    0.08f
            );

    private static final Vector3f SUNRISE_COLOR =
            new Vector3f(
                    0.95f,
                    0.48f,
                    0.20f
            );

    private static final Vector3f NOON_COLOR =
            new Vector3f(
                    0.35f,
                    0.65f,
                    0.90f
            );

    private static final Vector3f SUNSET_COLOR =
            new Vector3f(
                    0.72f,
                    0.25f,
                    0.18f
            );

    private static final Vector3f MIDNIGHT_ZENITH =
            new Vector3f(
                    0.008f,
                    0.012f,
                    0.045f
            );

    private static final Vector3f MIDNIGHT_HORIZON =
            new Vector3f(
                    0.025f,
                    0.035f,
                    0.085f
            );

    private static final Vector3f SUNRISE_ZENITH =
            new Vector3f(
                    0.16f,
                    0.22f,
                    0.40f
            );

    private static final Vector3f SUNRISE_HORIZON =
            new Vector3f(
                    0.72f,
                    0.50f,
                    0.34f
            );

    private static final Vector3f NOON_ZENITH =
            new Vector3f(
                    0.18f,
                    0.47f,
                    0.82f
            );

    private static final Vector3f NOON_HORIZON =
            new Vector3f(
                    0.62f,
                    0.76f,
                    0.88f
            );

    private static final Vector3f SUNSET_ZENITH =
            new Vector3f(
                    0.18f,
                    0.16f,
                    0.32f
            );

    private static final Vector3f SUNSET_HORIZON =
            new Vector3f(
                    0.68f,
                    0.40f,
                    0.30f
            );

    public void update(
            float deltaTime
    ) {
        worldTime +=
                deltaTime /
                        dayLengthSeconds;

        /*
         * Usually deltaTime is tiny, but using a loop keeps
         * this correct even after a large frame pause.
         */
        while (worldTime >= 1.0f) {
            worldTime -= 1.0f;
        }
    }

    public float getWorldTime() {
        return worldTime;
    }

    public float getSunBrightness() {
        float sunAngle =
                worldTime *
                        (float) Math.PI *
                        2.0f;

        float sunHeight =
                (float) Math.sin(
                        sunAngle -
                                (float) Math.PI /
                                        2.0f
                );

        /*
         * Shift the curve upward so the world begins brightening
         * before the sun is fully above the horizon.
         */
        float adjustedSunHeight =
                sunHeight +
                        0.92f;

        /*
         * Rescale into a useful 0–1 daylight range.
         */
        float daylight =
                adjustedSunHeight /
                        1.22f;

        daylight =
                Math.max(
                        0.0f,
                        Math.min(
                                1.0f,
                                daylight
                        )
                );

        /*
         * Smooth the transition near sunrise and sunset.
         */
        daylight =
                daylight *
                        daylight *
                        (
                                3.0f -
                                        2.0f *
                                                daylight
                        );

        float minimumNightLight =
                0.08f;

        return minimumNightLight +
                daylight *
                        (
                                1.0f -
                                        minimumNightLight
                        );
    }

    public Vector3f getSkyColor() {
        if (worldTime < 0.25f) {
            float progress =
                    worldTime /
                            0.25f;

            return interpolateColor(
                    MIDNIGHT_COLOR,
                    SUNRISE_COLOR,
                    smoothStep(progress)
            );
        }

        if (worldTime < 0.50f) {
            float progress =
                    (
                            worldTime -
                                    0.25f
                    ) /
                            0.25f;

            return interpolateColor(
                    SUNRISE_COLOR,
                    NOON_COLOR,
                    smoothStep(progress)
            );
        }

        if (worldTime < 0.75f) {
            float progress =
                    (
                            worldTime -
                                    0.50f
                    ) /
                            0.25f;

            return interpolateColor(
                    NOON_COLOR,
                    SUNSET_COLOR,
                    smoothStep(progress)
            );
        }

        float progress =
                (
                        worldTime -
                                0.75f
                ) /
                        0.25f;

        return interpolateColor(
                SUNSET_COLOR,
                MIDNIGHT_COLOR,
                smoothStep(progress)
        );
    }

    public Vector3f getZenithColor() {
        return getInterpolatedTimeColor(
                MIDNIGHT_ZENITH,
                SUNRISE_ZENITH,
                NOON_ZENITH,
                SUNSET_ZENITH
        );
    }

    public Vector3f getHorizonColor() {
        return getInterpolatedTimeColor(
                MIDNIGHT_HORIZON,
                SUNRISE_HORIZON,
                NOON_HORIZON,
                SUNSET_HORIZON
        );
    }

    private Vector3f getInterpolatedTimeColor(
            Vector3f midnight,
            Vector3f sunrise,
            Vector3f noon,
            Vector3f sunset
    ) {
        if (worldTime < 0.25f) {
            float progress =
                    smoothStep(
                            worldTime / 0.25f
                    );

            return interpolateColor(
                    midnight,
                    sunrise,
                    progress
            );
        }

        if (worldTime < 0.50f) {
            float progress =
                    smoothStep(
                            (
                                    worldTime -
                                            0.25f
                            ) / 0.25f
                    );

            return interpolateColor(
                    sunrise,
                    noon,
                    progress
            );
        }

        if (worldTime < 0.75f) {
            float progress =
                    smoothStep(
                            (
                                    worldTime -
                                            0.50f
                            ) / 0.25f
                    );

            return interpolateColor(
                    noon,
                    sunset,
                    progress
            );
        }

        float progress =
                smoothStep(
                        (
                                worldTime -
                                        0.75f
                        ) / 0.25f
                );

        return interpolateColor(
                sunset,
                midnight,
                progress
        );
    }

    private Vector3f interpolateColor(
            Vector3f start,
            Vector3f end,
            float progress
    ) {
        return new Vector3f(
                start
        ).lerp(
                end,
                progress
        );
    }

    public Vector3f getFogColor() {
        return new Vector3f(
                getHorizonColor()
        ).lerp(
                getZenithColor(),
                0.20f
        );
    }

    /*
     * Smooths the transition so each quarter of the day
     * eases in and out instead of changing at a constant rate.
     */
    private float smoothStep(
            float value
    ) {
        value =
                Math.max(
                        0.0f,
                        Math.min(
                                1.0f,
                                value
                        )
                );

        return value *
                value *
                (
                        3.0f -
                                2.0f *
                                        value
                );
    }

    public Vector3f getSunDirection() {
        /*
         * Our time convention:
         *
         * 0.00 = midnight
         * 0.25 = sunrise
         * 0.50 = noon
         * 0.75 = sunset
         */
        float angle =
                worldTime *
                        (float) Math.PI *
                        2.0f -
                        (float) Math.PI /
                                2.0f;

        float horizontalPosition =
                (float) Math.cos(
                        angle
                );

        float verticalPosition =
                (float) Math.sin(
                        angle
                );

        return new Vector3f(
                horizontalPosition,
                verticalPosition,
                0.25f
        ).normalize();
    }

    public Vector3f getMoonDirection() {
        return new Vector3f(
                getSunDirection()
        ).negate();
    }
}