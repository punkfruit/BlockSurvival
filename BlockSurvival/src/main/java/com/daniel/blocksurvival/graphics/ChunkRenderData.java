package com.daniel.blocksurvival.graphics;

public class ChunkRenderData {

    private final Mesh opaqueMesh;
    private final Mesh transparentMesh;

    public ChunkRenderData(
            Mesh opaqueMesh,
            Mesh transparentMesh
    ) {
        this.opaqueMesh = opaqueMesh;
        this.transparentMesh = transparentMesh;
    }

    public Mesh getOpaqueMesh() {
        return opaqueMesh;
    }

    public Mesh getTransparentMesh() {
        return transparentMesh;
    }

    public void destroy() {
        if (opaqueMesh != null) {
            opaqueMesh.destroy();
        }

        if (transparentMesh != null) {
            transparentMesh.destroy();
        }
    }

    public static ChunkRenderData fromMeshData(
            ChunkMeshData meshData
    ) {
        if (meshData == null) {
            return new ChunkRenderData(
                    null,
                    null
            );
        }

        Mesh opaqueMesh =
                createMesh(
                        meshData.opaqueMeshData()
                );

        Mesh transparentMesh =
                createMesh(
                        meshData.transparentMeshData()
                );

        return new ChunkRenderData(
                opaqueMesh,
                transparentMesh
        );
    }

    private static Mesh createMesh(
            MeshData meshData
    ) {
        if (
                meshData == null ||
                        meshData.isEmpty()
        ) {
            return null;
        }

        return new Mesh(
                meshData.vertices(),
                meshData.indices()
        );
    }
}