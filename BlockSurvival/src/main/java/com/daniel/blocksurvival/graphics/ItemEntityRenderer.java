package com.daniel.blocksurvival.graphics;

import com.daniel.blocksurvival.Camera;
import com.daniel.blocksurvival.entity.Entity;
import com.daniel.blocksurvival.entity.ItemEntity;
import com.daniel.blocksurvival.world.BlockType;
import com.daniel.blocksurvival.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class ItemEntityRenderer {

    private static final float ITEM_SCALE =
            0.25f;

    private final Texture atlasTexture;

    private final Shader shader;

    private final BlockMeshBuilder meshBuilder =
            new BlockMeshBuilder();

    /*
     * One reusable GPU mesh per block type.
     *
     * Breaking fifty stone blocks should still use only one
     * stone mesh rather than uploading fifty identical cubes.
     */
    private final Map<BlockType, Mesh> meshCache =
            new EnumMap<>(
                    BlockType.class
            );

    public ItemEntityRenderer(
            Texture atlasTexture
    ) {
        this.atlasTexture =
                atlasTexture;

        String vertexShaderSource = """
                #version 330 core

                layout (location = 0) in vec3 position;
                layout (location = 1) in vec2 textureCoordinate;
                layout (location = 2) in float ambientOcclusion;
                layout (location = 3) in float material;
                layout (location = 4) in float bendWeight;
                layout (location = 5) in float skyLight;
                layout (location = 6) in float blockLight;

                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;

                out vec2 fragmentTextureCoordinate;
                out vec3 fragmentWorldPosition;
                out float fragmentAO;
                out float fragmentSkyLight;
                out float fragmentBlockLight;

                void main() {
                    vec4 worldPosition =
                            modelMatrix *
                            vec4(
                                    position,
                                    1.0
                            );

                    gl_Position =
                            projectionMatrix *
                            viewMatrix *
                            worldPosition;

                    fragmentTextureCoordinate =
                            textureCoordinate;

                    fragmentWorldPosition =
                            worldPosition.xyz;

                    fragmentAO =
                            ambientOcclusion;

                    fragmentSkyLight =
                            skyLight;

                    fragmentBlockLight =
                            blockLight;
                }
                """;

        String fragmentShaderSource = """
                #version 330 core

                in vec2 fragmentTextureCoordinate;
                in vec3 fragmentWorldPosition;
                in float fragmentAO;
                in float fragmentSkyLight;
                in float fragmentBlockLight;

                uniform sampler2D blockTexture;

                uniform float sunBrightness;

                /*
                 * Lighting sampled from the item's current
                 * world location each frame.
                 */
                uniform float entitySkyLight;
                uniform float entityBlockLight;

                out vec4 finalColor;

                void main() {
                    vec4 textureColor =
                            texture(
                                    blockTexture,
                                    fragmentTextureCoordinate
                            );

                    if (textureColor.a < 0.5) {
                        discard;
                    }

                    /*
                     * Derive a normal from the transformed world
                     * positions, matching the world shader's approach.
                     */
                    vec3 positionChangeX =
                            dFdx(
                                    fragmentWorldPosition
                            );

                    vec3 positionChangeY =
                            dFdy(
                                    fragmentWorldPosition
                            );

                    vec3 normal =
                            normalize(
                                    cross(
                                            positionChangeX,
                                            positionChangeY
                                    )
                            );

                    if (!gl_FrontFacing) {
                        normal =
                                -normal;
                    }

                    vec3 lightDirection =
                            normalize(
                                    vec3(
                                            -0.6,
                                            1.0,
                                            0.4
                                    )
                            );

                    float directionalLight =
                            max(
                                    dot(
                                            normal,
                                            lightDirection
                                    ),
                                    0.0
                            );

                    float faceBrightness =
                            0.45 +
                            directionalLight *
                            0.55;

                    /*
                     * The uniform values are sampled at the item's
                     * current position. The vertex values remain as
                     * a fallback for emissive block models.
                     */
                    float sky =
                            max(
                                    fragmentSkyLight,
                                    entitySkyLight
                            );

                    float block =
                            max(
                                    fragmentBlockLight,
                                    entityBlockLight
                            );

                    float finalLight =
                            max(
                                    sky *
                                    sunBrightness,
                                    block
                            );

                    /*
                     * Avoid making an item completely invisible in
                     * total darkness during this first version.
                     */
                    finalLight =
                            max(
                                    finalLight,
                                    0.06
                            );

                    vec3 litColor =
                            textureColor.rgb *
                            faceBrightness *
                            fragmentAO *
                            finalLight;

                    finalColor =
                            vec4(
                                    litColor,
                                    textureColor.a
                            );
                }
                """;

        shader =
                new Shader(
                        vertexShaderSource,
                        fragmentShaderSource
                );

        shader.bind();

        shader.setInt(
                "blockTexture",
                0
        );

        shader.unbind();
    }

    public void render(
            List<Entity> entities,
            Matrix4f projectionMatrix,
            Matrix4f viewMatrix,
            Camera camera,
            World world,
            float sunBrightness
    ) {
        if (entities.isEmpty()) {
            return;
        }

        shader.bind();

        glActiveTexture(
                GL_TEXTURE0
        );

        atlasTexture.bind();

        shader.setMatrix4(
                "projectionMatrix",
                projectionMatrix
        );

        shader.setMatrix4(
                "viewMatrix",
                viewMatrix
        );

        shader.setFloat(
                "sunBrightness",
                sunBrightness
        );

        for (Entity entity : entities) {
            if (!(entity instanceof ItemEntity item)) {
                continue;
            }

            renderItem(
                    item,
                    world
            );
        }

        atlasTexture.unbind();

        shader.unbind();
    }

    private void renderItem(
            ItemEntity item,
            World world
    ) {
        Mesh mesh =
                getOrCreateMesh(
                        item.getBlockType()
                );

        if (mesh == null) {
            return;
        }

        Vector3f position =
                item.getPosition();

        /*
         * A tiny vertical offset prevents the cube from
         * visually clipping into the surface after settling.
         */
        float visualHeightOffset =
                0.02f;

        float rotationRadians =
                (float) Math.toRadians(
                        item.getRotation()
                );

        float bobHeight =
                item.isGrounded()
                        ? (float) Math.sin(
                        item.getAge() *
                                3.0f
                ) *
                        0.04f
                        : 0.0f;

        float itemScale =
                switch (
                        item.getBlockType()
                                .getModel()
                        ) {
                    case CROSS ->
                            0.40f;

                    case TORCH ->
                            0.60f;

                    case CUBE ->
                            ITEM_SCALE;
                };

        Matrix4f modelMatrix =
                new Matrix4f()
                        .translate(
                                position.x,
                                position.y +
                                        visualHeightOffset +
                                        bobHeight,
                                position.z
                        )
                        /*
                         * Primarily spin around Y, with a slight
                         * fixed tilt so the top remains visible.
                         */
                        .rotateY(
                                rotationRadians
                        )
                        .rotateX(
                                (float) Math.toRadians(
                                        18.0f
                                )
                        )
                        .scale(
                                itemScale
                        );

        shader.setMatrix4(
                "modelMatrix",
                modelMatrix
        );

        int blockX =
                (int) Math.floor(
                        position.x +
                                0.5f
                );

        int blockY =
                (int) Math.floor(
                        position.y +
                                0.5f
                );

        int blockZ =
                (int) Math.floor(
                        position.z +
                                0.5f
                );

        float skyLight =
                world.getSkyLight(
                        blockX,
                        blockY,
                        blockZ
                ) /
                        15.0f;

        float blockLight =
                world.getBlockLight(
                        blockX,
                        blockY,
                        blockZ
                ) /
                        15.0f;

        /*
         * Emissive items should illuminate themselves even if
         * the surrounding sampled block light is stale or dark.
         */
        blockLight =
                Math.max(
                        blockLight,
                        item.getBlockType()
                                .getEmittedLight() /
                                15.0f
                );

        shader.setFloat(
                "entitySkyLight",
                skyLight
        );

        shader.setFloat(
                "entityBlockLight",
                blockLight
        );

        mesh.render();
    }

    private Mesh getOrCreateMesh(
            BlockType blockType
    ) {
        Mesh existingMesh =
                meshCache.get(
                        blockType
                );

        if (existingMesh != null) {
            return existingMesh;
        }

        MeshData meshData =
                meshBuilder.buildBlock(
                        blockType
                );

        if (
                meshData == null ||
                        meshData.isEmpty()
        ) {
            return null;
        }

        Mesh newMesh =
                new Mesh(
                        meshData.vertices(),
                        meshData.indices()
                );

        meshCache.put(
                blockType,
                newMesh
        );

        return newMesh;
    }

    public void destroy() {
        for (Mesh mesh : meshCache.values()) {
            mesh.destroy();
        }

        meshCache.clear();

        shader.destroy();
    }
}