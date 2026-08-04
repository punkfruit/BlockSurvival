package com.daniel.blocksurvival.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL33.*;

public class SkyRenderer {

    private final int vao;

    private final Shader shader;

    public SkyRenderer() {
        /*
         * Core-profile OpenGL requires a VAO to be bound,
         * even though this shader generates its own vertices.
         */
        vao =
                glGenVertexArrays();

        String vertexShaderSource = """
                #version 330 core
                
                out vec2 screenCoordinate;
                
                void main() {
                    vec2 positions[3] =
                            vec2[](
                                    vec2(-1.0, -1.0),
                                    vec2( 3.0, -1.0),
                                    vec2(-1.0,  3.0)
                            );
                
                    vec2 position =
                            positions[
                                    gl_VertexID
                            ];
                
                    gl_Position =
                            vec4(
                                    position,
                                    0.0,
                                    1.0
                            );
                
                    screenCoordinate =
                            position * 0.5 +
                            0.5;
                }
                """;

        String fragmentShaderSource = """
        #version 330 core

        in vec2 screenCoordinate;

        uniform mat4 inverseProjection;
        uniform mat4 inverseView;

        uniform vec3 horizonColor;
        uniform vec3 zenithColor;
        uniform vec3 sunDirection;
        uniform vec3 moonDirection;
        uniform float worldTime;

        out vec4 finalColor;
        
                /*
                 * Hand-authored 15×15 pixel sun.
                 *
                 * Each row defines the filled horizontal range.
                 * Returning false leaves that pixel transparent.
                 */
                bool isSunPixel(
                                ivec2 pixel
                        ) {
                            int x = pixel.x;
                            int y = pixel.y;
                
                            if (
                                    x < 0 ||
                                    x >= 15 ||
                                    y < 0 ||
                                    y >= 15
                            ) {
                                return false;
                            }
                
                            /*
                             * Hand-drawn 15×15 sun:
                             *
                             * ......###......
                             * ....#######....
                             * ...#########...
                             * ..###########..
                             * .#############.
                             * ##############.
                             * ###############
                             * ###############
                             * ###############
                             * .##############
                             * .#############.
                             * ..###########..
                             * ...##########..
                             * ....########....
                             * ......####.....
                             */
                
                            if (y == 0) {
                                return x >= 6 && x <= 8;
                            }
                
                            if (y == 1) {
                                return x >= 4 && x <= 10;
                            }
                
                            if (y == 2) {
                                return x >= 3 && x <= 11;
                            }
                
                            if (y == 3) {
                                return x >= 2 && x <= 12;
                            }
                
                            if (y == 4) {
                                return x >= 1 && x <= 13;
                            }
                
                            if (y == 5) {
                                return x >= 0 && x <= 13;
                            }
                
                            if (y >= 6 && y <= 8) {
                                return true;
                            }
                
                            if (y == 9) {
                                return x >= 1 && x <= 14;
                            }
                
                            if (y == 10) {
                                return x >= 1 && x <= 13;
                            }
                
                            if (y == 11) {
                                return x >= 2 && x <= 12;
                            }
                
                            if (y == 12) {
                                return x >= 3 && x <= 12;
                            }
                
                            if (y == 13) {
                                return x >= 4 && x <= 11;
                            }
                
                            if (y == 14) {
                                return x >= 6 && x <= 9;
                            }
                
                            return false;
                        }
                        
                bool isMoonPixel(
                                ivec2 pixel
                        ) {
                            int x = pixel.x;
                            int y = pixel.y;
                
                            if (
                                    x < 0 ||
                                    x >= 15 ||
                                    y < 0 ||
                                    y >= 15
                            ) {
                                return false;
                            }
                
                            /*
                             * Slightly calmer and rounder than the sun,
                             * but still hand-shaped.
                             *
                             * .....#####.....
                             * ...########....
                             * ..###########..
                             * .############..
                             * .#############.
                             * ##############.
                             * ###############
                             * ###############
                             * ##############.
                             * .#############.
                             * .############..
                             * ..###########..
                             * ...#########...
                             * ....#######....
                             * ......###......
                             */
                
                            if (y == 0) {
                                return x >= 5 && x <= 9;
                            }
                
                            if (y == 1) {
                                return x >= 3 && x <= 10;
                            }
                
                            if (y == 2) {
                                return x >= 2 && x <= 12;
                            }
                
                            if (y == 3) {
                                return x >= 1 && x <= 12;
                            }
                
                            if (y == 4) {
                                return x >= 1 && x <= 13;
                            }
                
                            if (y == 5) {
                                return x >= 0 && x <= 13;
                            }
                
                            if (y == 6 || y == 7) {
                                return true;
                            }
                
                            if (y == 8) {
                                return x >= 0 && x <= 13;
                            }
                
                            if (y == 9) {
                                return x >= 1 && x <= 13;
                            }
                
                            if (y == 10) {
                                return x >= 1 && x <= 12;
                            }
                
                            if (y == 11) {
                                return x >= 2 && x <= 12;
                            }
                
                            if (y == 12) {
                                return x >= 3 && x <= 11;
                            }
                
                            if (y == 13) {
                                return x >= 4 && x <= 10;
                            }
                
                            if (y == 14) {
                                return x >= 6 && x <= 8;
                            }
                
                            return false;
                        }

                bool isMoonDarkPixel(
                                ivec2 pixel
                        ) {
                            int x = pixel.x;
                            int y = pixel.y;
                
                            /*
                             * Large connected darker regions inspired by
                             * maria on the moon's surface.
                             */
                
                            if (y == 1) {
                                return x >= 3 && x <= 6;
                            }
                
                            if (y == 2) {
                                return x >= 2 && x <= 6;
                            }
                
                            if (y == 3) {
                                return x >= 2 && x <= 7;
                            }
                
                            if (y == 4) {
                                return x >= 3 && x <= 7;
                            }
                
                            if (y == 5) {
                                return
                                        (x >= 2 && x <= 6) ||
                                        (x >= 10 && x <= 12);
                            }
                
                            if (y == 6) {
                                return
                                        (x >= 2 && x <= 7) ||
                                        (x >= 9 && x <= 12);
                            }
                
                            if (y == 7) {
                                return
                                        (x >= 3 && x <= 6) ||
                                        (x >= 9 && x <= 12);
                            }
                
                            if (y == 8) {
                                return
                                        (x >= 4 && x <= 7) ||
                                        (x >= 9 && x <= 11);
                            }
                
                            if (y == 9) {
                                return
                                        (x >= 5 && x <= 8) ||
                                        x == 11;
                            }
                
                            if (y == 10) {
                                return
                                        (x >= 6 && x <= 9) ||
                                        x == 4;
                            }
                
                            if (y == 11) {
                                return
                                        (x >= 5 && x <= 8) ||
                                        (x >= 10 && x <= 11);
                            }
                
                            if (y == 12) {
                                return x >= 4 && x <= 7;
                            }
                
                            if (y == 13) {
                                return x >= 5 && x <= 7;
                            }
                
                            return false;
                        }

            float hash31(
                            vec3 value
                    ) {
                        value =
                                fract(
                                        value *
                                        0.1031
                                );
                
                        value +=
                                dot(
                                        value,
                                        value.yzx +
                                                33.33
                                );
                
                        return fract(
                                (
                                        value.x +
                                                value.y
                                ) *
                                value.z
                        );
                    }

        void main() {
            /*
             * Convert the 0–1 screen coordinate into
             * OpenGL clip coordinates from -1 to +1.
             */
            vec2 clipCoordinate =
                    screenCoordinate * 2.0 -
                    1.0;

            /*
             * Reconstruct the direction represented by this
             * screen pixel in camera/view space.
             */
            vec4 viewPosition =
                    inverseProjection *
                    vec4(
                            clipCoordinate,
                            1.0,
                            1.0
                    );

            vec3 viewDirection =
                    normalize(
                            viewPosition.xyz /
                            viewPosition.w
                    );

            /*
             * Rotate that direction into world space.
             */
            vec3 worldDirection =
                    normalize(
                            (
                                    inverseView *
                                    vec4(
                                            viewDirection,
                                            0.0
                                    )
                            ).xyz
                    );

            /*
             * Keep the horizon fixed to the world.
             *
             * Looking upward approaches the zenith color.
             * Looking toward or below the horizon uses the
             * horizon color.
             */
            float height =
                    clamp(
                            worldDirection.y,
                            0.0,
                            1.0
                    );

            float gradient =
                    pow(
                            height,
                            0.45
                    );

            vec3 skyColor =
                    mix(
                            horizonColor,
                            zenithColor,
                            gradient
                    );
            
            vec3 normalizedSunDirection =
                                      normalize(
                                              sunDirection
                                      );
            
            /*
                     * Stars fade in as daylight disappears.
                     *
                     * worldTime convention:
                     * 0.00 = midnight
                     * 0.50 = noon
                     */
                    float nightAmount =
                            1.0 -
                            smoothstep(
                                    0.08,
                                    0.32,
                                    max(
                                            normalizedSunDirection.y,
                                            0.0
                                    )
                            );

            /*
             * A dot product of 1 means this pixel points
             * directly toward the sun.
             */
             
             /*
                      * Quantize the sky direction into a coarse spherical grid.
                      * Each cell can either contain a star or remain empty.
                      */
                     vec3 starGrid =
                             floor(
                                     worldDirection *
                                             460.0  // 120.0 = larger stars, fewer cells, 180.0 = balanced, 260.0 = tiny dense stars
                             );
                
                     float starRandom =
                             hash31(
                                     starGrid
                             );
                
                     /*
                      * Only a small percentage of cells become stars.
                      */
                     float starExists =
                             step(
                                     0.999, //STAR AMOUNT
                                     starRandom
                             );
                
                     /*
                      * Give stars a few brightness levels.
                      */
                     float starBrightness =
                             mix(
                                     0.45,
                                     1.0,
                                     hash31(
                                             starGrid +
                                                     vec3(
                                                             17.0,
                                                             31.0,
                                                             47.0
                                                     )
                                     )
                             );
             
             float heroStar =
                             step(
                                     0.998,
                                     hash31(
                                             starGrid +
                                             vec3(
                                                     200.0
                                             )
                                     )
                             );
             
             /*
                      * Every star twinkles differently.
                      */
                
                     float twinkleStrength =
                             mix(
                                     0.05,
                                     0.55,
                                     hash31(
                                             starGrid +
                                             vec3(
                                                     71.0,
                                                     29.0,
                                                     13.0
                                             )
                                     )
                             );
                
                     float twinkleSpeed =
                             mix(
                                     0.5,
                                     3.0,
                                     hash31(
                                             starGrid +
                                             vec3(
                                                     11.0,
                                                     83.0,
                                                     47.0
                                             )
                                     )
                             );
                
                     float twinkleOffset =
                             hash31(
                                     starGrid +
                                     vec3(
                                             97.0,
                                             19.0,
                                             61.0
                                     )
                             ) *
                             6.283185;
                
                     float twinkle =
                             1.0 -
                             twinkleStrength +
                
                             twinkleStrength *
                
                             (
                                     0.5 +
                                     0.5 *
                                     sin(
                                             worldTime *
                                             120.0 *
                                             twinkleSpeed +
                                             twinkleOffset
                                     )
                             );
                     twinkle =
                                     mix(
                                             twinkle,
                                             twinkle * 2.0,
                                             heroStar
                                     );
                
                     /*
                      * Keep stars out of the lower sky.
                      */
                     float aboveHorizon =
                             smoothstep(
                                     0.02,
                                     0.18,
                                     worldDirection.y
                             );
                
                     float stars =
                             starExists *
                             starBrightness *
                             twinkle *
                             nightAmount *
                             aboveHorizon;
                             
                     float largeStar =
                                     step(
                                             0.9985,
                                             starRandom
                                     );
                
                             stars +=
                                     largeStar *
                                     0.55 *
                                     nightAmount *
                                     aboveHorizon;
                             
             float starColorSeed =
                             hash31(
                                     starGrid +
                                     vec3(400.0)
                             );
                
                     vec3 starColor =
                             mix(
                                     vec3(
                                             1.0,
                                             0.95,
                                             0.90
                                     ),
                                     vec3(
                                             0.82,
                                             0.90,
                                             1.0
                                     ),
                                     starColorSeed
                             );
                
                     
            
                
                              /*
                               * Build two perpendicular axes around the sun.
                               *
                               * These let us treat the area around the sun as a tiny
                               * two-dimensional canvas floating in the sky.
                               */
                              vec3 referenceUp =
                                      abs(
                                              normalizedSunDirection.y
                                      ) > 0.98
                                              ? vec3(0.0, 0.0, 1.0)
                                              : vec3(0.0, 1.0, 0.0);
                
                              vec3 sunRight =
                                      normalize(
                                              cross(
                                                      referenceUp,
                                                      normalizedSunDirection
                                              )
                                      );
                
                              vec3 sunUp =
                                      normalize(
                                              cross(
                                                      normalizedSunDirection,
                                                      sunRight
                                              )
                                      );
                
                              float sunAlignment =
                                      dot(
                                              worldDirection,
                                              normalizedSunDirection
                                      );
                
                              /*
                               * Project this sky pixel onto the sun's local canvas.
                               */
                              vec2 sunLocal =
                                      vec2(
                                              dot(
                                                      worldDirection,
                                                      sunRight
                                              ),
                                              dot(
                                                      worldDirection,
                                                      sunUp
                                              )
                                      );
                
                              /*
                               * Controls the sun's apparent angular size.
                               *
                               * Larger value = larger sun.
                               */
                              const float SUN_SIZE =
                                      0.055;
                
                              vec2 sunUV =
                                      sunLocal /
                                              SUN_SIZE *
                                              0.5 +
                                              0.5;
                
                              /*
                               * Convert the sun canvas into an actual 15×15 pixel grid.
                               */
                              ivec2 sunPixel =
                                      ivec2(
                                              floor(
                                                      sunUV *
                                                              15.0
                                              )
                                      );
                
                              bool insideSunCanvas =
                                      sunUV.x >= 0.0 &&
                                      sunUV.x < 1.0 &&
                                      sunUV.y >= 0.0 &&
                                      sunUV.y < 1.0;
                
                              /*
                               * Keep the atmospheric glow smooth, even though the
                               * central body is pixel art.
                               */
                              float sunGlow =
                                              smoothstep(
                                                      0.900,
                                                      0.998,
                                                      sunAlignment
                                              );
                
                              float sunDisk =
                                      insideSunCanvas &&
                                      isSunPixel(
                                              sunPixel
                                      )
                                              ? 1.0
                                              : 0.0;
                              
                              float sunVisibility =
                                              smoothstep(
                                                      -0.04,
                                                      0.04,
                                                      normalizedSunDirection.y
                                              );
                              
                              float sunDetail =
                                              mod(
                                                      float(
                                                              sunPixel.x * 3 +
                                                              sunPixel.y * 5
                                                      ),
                                                      7.0
                                              ) == 0.0
                                                      ? 1.0
                                                      : 0.0;
                
                              vec3 sunColor =
                                                        vec3(
                                                                1.0,
                                                                0.86,
                                                                0.54
                                                        );
                
                                                vec3 sunHighlight =
                                                        vec3(
                                                                1.0,
                                                                0.95,
                                                                0.72
                                                        );
                
                                                vec3 finalSunColor =
                                                        mix(
                                                                sunColor,
                                                                sunHighlight,
                                                                sunDetail * 0.20
                                                        );
                
                              /*
                                                 * Add the soft atmospheric halo first.
                                                 */
                                                skyColor +=
                                                                             sunColor *
                                                                             sunGlow *
                                                                             0.12 *
                                                                             sunVisibility;
                
                                                /*
                                                 * Then draw the solid pixel-art sun on top.
                                                 */
                                                skyColor =
                                                                          mix(
                                                                                  skyColor,
                                                                                  finalSunColor,
                                                                                  sunDisk *
                                                                                  sunVisibility
                                                                          );


                            vec3 normalizedMoonDirection =
                                            normalize(
                                                    moonDirection
                                            );
                
                                    vec3 moonReferenceUp =
                                            abs(
                                                    normalizedMoonDirection.y
                                            ) > 0.98
                                                    ? vec3(0.0, 0.0, 1.0)
                                                    : vec3(0.0, 1.0, 0.0);
                
                                    vec3 moonRight =
                                            normalize(
                                                    cross(
                                                            moonReferenceUp,
                                                            normalizedMoonDirection
                                                    )
                                            );
                
                                    vec3 moonUp =
                                            normalize(
                                                    cross(
                                                            normalizedMoonDirection,
                                                            moonRight
                                                    )
                                            );
                
                                    float moonAlignment =
                                            dot(
                                                    worldDirection,
                                                    normalizedMoonDirection
                                            );
                
                                    vec2 moonLocal =
                                            vec2(
                                                    dot(
                                                            worldDirection,
                                                            moonRight
                                                    ),
                                                    dot(
                                                            worldDirection,
                                                            moonUp
                                                    )
                                            );
                
                                    const float MOON_SIZE =
                                            0.050;
                
                                    vec2 moonUV =
                                            moonLocal /
                                                    MOON_SIZE *
                                                    0.5 +
                                                    0.5;
                
                                    ivec2 moonPixel =
                                            ivec2(
                                                    floor(
                                                            moonUV *
                                                                    15.0
                                                    )
                                            );
                
                                    bool insideMoonCanvas =
                                            moonUV.x >= 0.0 &&
                                            moonUV.x < 1.0 &&
                                            moonUV.y >= 0.0 &&
                                            moonUV.y < 1.0;
                
                                    float moonDisk =
                                            insideMoonCanvas &&
                                            isMoonPixel(
                                                    moonPixel
                                            )
                                                    ? 1.0
                                                    : 0.0;
                
                                    /*
                                     * A few deterministic darker crater pixels.
                                     */
                                    float darkPatch =
                                                                          isMoonDarkPixel(
                                                                                  moonPixel
                                                                          )
                                                                                  ? 1.0
                                                                                  : 0.0;
                
                                    vec3 moonColor =
                                            vec3(
                                                    0.64,
                                                    0.64,
                                                    0.62
                                            );
                
                                    vec3 craterColor =
                                            vec3(
                                                    0.34,
                                                    0.35,
                                                    0.36
                                            );
                
                                    vec3 finalMoonColor =
                                            mix(
                                                    moonColor,
                                                    craterColor,
                                                    darkPatch
                                            );
                
                                    float moonGlow =
                                            smoothstep(
                                                    0.982,
                                                    0.999,
                                                    moonAlignment
                                            );
                
                                    /*
                                     * Only show the moon while it is above the horizon.
                                     */
                                    float moonVisibility =
                                            smoothstep(
                                                    -0.04,
                                                    0.04,
                                                    normalizedMoonDirection.y
                                            );
                
                float celestialMask =
                                1.0 -
                                max(
                                        smoothstep(
                                                0.985,
                                                0.999,
                                                sunAlignment
                                        ),
                                        smoothstep(
                                                0.985,
                                                0.999,
                                                moonAlignment
                                        )
                                );
                                skyColor +=
                                     starColor *
                                     stars *
                                     celestialMask;
                                
                                
                
                                    skyColor +=
                                            moonColor *
                                            moonGlow *
                                            0.04 *
                                            moonVisibility;
                
                                    skyColor =
                                            mix(
                                                    skyColor,
                                                    finalMoonColor,
                                                    moonDisk *
                                                    moonVisibility
                                            );
            finalColor =
                    vec4(
                            skyColor,
                            1.0
                    );
        }
        """;

        shader =
                new Shader(
                        vertexShaderSource,
                        fragmentShaderSource
                );
    }

    public void render(
            Sky sky,
            Matrix4f projectionMatrix,
            Matrix4f viewMatrix
    ) {
        /*
         * The sky must not write over or test against
         * the world's depth information.
         */
        glDisable(
                GL_DEPTH_TEST
        );

        glDepthMask(
                false
        );

        Matrix4f inverseProjection =
                new Matrix4f(
                        projectionMatrix
                ).invert();

        Matrix4f rotationOnlyView =
                new Matrix4f(
                        viewMatrix
                );

        /*
         * Remove camera translation.
         *
         * The sky should rotate with the camera but should never
         * move through space with it.
         */
        rotationOnlyView.m30(0.0f);
        rotationOnlyView.m31(0.0f);
        rotationOnlyView.m32(0.0f);

        Matrix4f inverseView =
                rotationOnlyView.invert();

        shader.bind();

        shader.setMatrix4(
                "inverseProjection",
                inverseProjection
        );

        shader.setMatrix4(
                "inverseView",
                inverseView
        );

        shader.setVector3(
                "horizonColor",
                sky.getHorizonColor()
        );

        shader.setVector3(
                "zenithColor",
                sky.getZenithColor()
        );

        shader.setVector3(
                "sunDirection",
                sky.getSunDirection()
        );

        shader.setVector3(
                "moonDirection",
                sky.getMoonDirection()
        );

        shader.setFloat(
                "worldTime",
                sky.getWorldTime()
        );

        glBindVertexArray(
                vao
        );

        glDrawArrays(
                GL_TRIANGLES,
                0,
                3
        );

        glBindVertexArray(
                0
        );

        shader.unbind();

        glDepthMask(
                true
        );

        glEnable(
                GL_DEPTH_TEST
        );
    }

    public void destroy() {
        shader.destroy();

        glDeleteVertexArrays(
                vao
        );
    }
}