package com.daniel.blocksurvival.graphics;
import com.daniel.blocksurvival.Camera;
import com.daniel.blocksurvival.world.BlockType;
import com.daniel.blocksurvival.world.World;
import static org.lwjgl.opengl.GL11.*;
import com.daniel.blocksurvival.world.Chunk;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class WorldRenderer {

    private final Shader shader;

    private final Texture atlasTexture;
    private static final Vector3f SKY_FOG_COLOR =
            new Vector3f(
                    0.35f,
                    0.65f,
                    0.90f
            );

    private static final Vector3f WATER_FOG_COLOR =
            new Vector3f(
                    0.12f,
                    0.38f,
                    0.55f
            );

    private static final float SKY_FOG_START =
            20.0f;

    private static final float SKY_FOG_END =
            40.0f;

    private static final float WATER_FOG_START =
            0.0f;

    private static final float WATER_FOG_END =
            10.0f;

    private static final float FOG_TRANSITION_SPEED =
            3.0f;

    /*
     * 0.0 = sky fog
     * 1.0 = underwater fog
     */
    private float fogBlend = 0.0f;
    /*
     * Total elapsed animation time.
     *
     * We accumulate delta time rather than reading the system clock
     * so the renderer controls the animation consistently.
     */
    private float animationTime = 0.0f;

    private final Vector3f blendedFogColor =
            new Vector3f();

    public WorldRenderer(
            Shader shader,
            Texture atlasTexture
    ) {
        this.shader = shader;
        this.atlasTexture = atlasTexture;
    }

    public void render(
            Matrix4f projectionMatrix,
            Matrix4f viewMatrix,
            Camera camera,
            World world,
            Map<Chunk, ChunkRenderData> chunkMeshes,
            float deltaTime
    ) {
        shader.bind();

        animationTime += deltaTime;

        glActiveTexture(GL_TEXTURE0);
        atlasTexture.bind();
        shader.setFloat(
                "animationTime",
                animationTime
        );

        shader.setFloat(
                "atlasTileSize",
                BlockType.getTileSize()
        );

        shader.setVector3(
                "cameraPosition",
                camera.getPosition()
        );

        float targetFogBlend =
                camera.isCameraUnderwater(
                        world
                )
                        ? 1.0f
                        : 0.0f;

        /*
         * Move gradually toward the desired fog state.
         */
        float blendStep =
                FOG_TRANSITION_SPEED *
                        deltaTime;

        if (fogBlend < targetFogBlend) {
            fogBlend = Math.min(
                    fogBlend + blendStep,
                    targetFogBlend
            );
        } else if (fogBlend > targetFogBlend) {
            fogBlend = Math.max(
                    fogBlend - blendStep,
                    targetFogBlend
            );
        }

        /*
         * Blend the RGB color manually.
         *
         * At fogBlend 0.0, this produces SKY_FOG_COLOR.
         * At fogBlend 1.0, this produces WATER_FOG_COLOR.
         */
        blendedFogColor.set(
                SKY_FOG_COLOR.x +
                        (
                                WATER_FOG_COLOR.x -
                                        SKY_FOG_COLOR.x
                        ) * fogBlend,

                SKY_FOG_COLOR.y +
                        (
                                WATER_FOG_COLOR.y -
                                        SKY_FOG_COLOR.y
                        ) * fogBlend,

                SKY_FOG_COLOR.z +
                        (
                                WATER_FOG_COLOR.z -
                                        SKY_FOG_COLOR.z
                        ) * fogBlend
        );

        float blendedFogStart =
                SKY_FOG_START +
                        (
                                WATER_FOG_START -
                                        SKY_FOG_START
                        ) * fogBlend;

        float blendedFogEnd =
                SKY_FOG_END +
                        (
                                WATER_FOG_END -
                                        SKY_FOG_END
                        ) * fogBlend;

        shader.setVector3(
                "fogColor",
                blendedFogColor
        );

        shader.setFloat(
                "fogStart",
                blendedFogStart
        );

        shader.setFloat(
                "fogEnd",
                blendedFogEnd
        );

        Matrix4f mvpMatrix =
                new Matrix4f(projectionMatrix)
                        .mul(viewMatrix);

        shader.setMatrix4(
                "mvpMatrix",
                mvpMatrix
        );

        for (ChunkRenderData renderData : chunkMeshes.values()) {

            if (renderData.getOpaqueMesh() != null) {
                renderData.getOpaqueMesh().render();
            }
        }


        List<Map.Entry<Chunk, ChunkRenderData>>
                transparentChunks =
                new ArrayList<>(
                        chunkMeshes.entrySet()
                );

        transparentChunks.sort(
                (entryA, entryB) -> {
                    float distanceA =
                            distanceSquaredToChunk(
                                    camera,
                                    entryA.getKey()
                            );

                    float distanceB =
                            distanceSquaredToChunk(
                                    camera,
                                    entryB.getKey()
                            );

                    return Float.compare(
                            distanceB,
                            distanceA
                    );
                }
        );

        /*
         * Transparent pass.
         */
        glEnable(GL_BLEND);

        glBlendFunc(
                GL_SRC_ALPHA,
                GL_ONE_MINUS_SRC_ALPHA
        );

        for (
                Map.Entry<Chunk, ChunkRenderData> entry :
                transparentChunks
        ) {
            Mesh transparentMesh =
                    entry.getValue()
                            .getTransparentMesh();

            if (transparentMesh != null) {
                transparentMesh.render();
            }
        }

        glDisable(GL_BLEND);

        atlasTexture.unbind();
        shader.unbind();
    }

    private float distanceSquaredToChunk(
            Camera camera,
            Chunk chunk
    ) {
        /*
         * Find the approximate center of the chunk.
         */
        float centerX =
                chunk.getWorldOriginX() +
                        Chunk.SIZE / 2.0f;

        float centerY =
                chunk.getWorldOriginY() +
                        Chunk.SIZE / 2.0f;

        float centerZ =
                chunk.getWorldOriginZ() +
                        Chunk.SIZE / 2.0f;

        float differenceX =
                centerX -
                        camera.getPosition().x;

        float differenceY =
                centerY -
                        camera.getPosition().y;

        float differenceZ =
                centerZ -
                        camera.getPosition().z;

        /*
         * We do not need Math.sqrt().
         * Squared distances sort in the same order.
         */
        return differenceX * differenceX +
                differenceY * differenceY +
                differenceZ * differenceZ;
    }
}
