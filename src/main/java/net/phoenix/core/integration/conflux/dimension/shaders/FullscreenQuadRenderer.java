package net.phoenix.core.integration.conflux.dimension.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class FullscreenQuadRenderer {

    private static final int VERTEX_COUNT = 6;
    private int vertexArrayObject = -1;
    private int vertexBufferObject = -1;

    private static final float[] QUAD_VERTICES = {

            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f,

            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 0.0f, 0.0f, 1.0f
    };

    public void init() {
        if (vertexArrayObject != -1) return;

        try {

            vertexArrayObject = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vertexArrayObject);

            vertexBufferObject = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBufferObject);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, QUAD_VERTICES, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);
        } catch (Exception e) {
            System.err.println("Failed to initialize fullscreen quad: " + e.getMessage());
        }
    }

    public void render(PoseStack poseStack) {
        if (vertexArrayObject == -1) {
            init();
        }

        try {
            RenderSystem.assertOnRenderThread();

            GL30.glBindVertexArray(vertexArrayObject);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, VERTEX_COUNT);
            GL30.glBindVertexArray(0);

        } catch (Exception e) {
            System.err.println("Error rendering fullscreen quad: " + e.getMessage());
        }
    }

    public void renderWithDimensions(PoseStack poseStack, int width, int height) {
        render(poseStack);
    }

    public void cleanup() {
        if (vertexBufferObject != -1) {
            GL15.glDeleteBuffers(vertexBufferObject);
            vertexBufferObject = -1;
        }

        if (vertexArrayObject != -1) {
            GL30.glDeleteVertexArrays(vertexArrayObject);
            vertexArrayObject = -1;
        }
    }

    public boolean isInitialized() {
        return vertexBufferObject != -1 && vertexArrayObject != -1;
    }
}
