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
}