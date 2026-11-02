package com.blackhole;

import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class GridUtils {

    /**
     * Creates a Schwarzschild spacetime curvature funnel (Flamm's paraboloid)
     * y(r) = -2.0 * sqrt(rs * (r - rs))
     */
    public static int[] createFlammFunnelVAO(float rs, float maxRadius, int radialSegments, int ringSegments) {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // Lines for rings + lines for radial spokes
        int totalSegments = ringSegments * radialSegments + radialSegments * ringSegments;
        int vertexCount = totalSegments * 2;
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertexCount * 3);

        // 1. Concentric circular rings
        for (int i = 0; i < ringSegments; i++) {
            float t1 = (float) i / (ringSegments - 1);
            // Quadratic distribution for more rings near the horizon
            float r = rs + (maxRadius - rs) * (t1 * t1);
            float y = -2.0f * (float) Math.sqrt(Math.max(0.0f, rs * (r - rs)));

            for (int j = 0; j < radialSegments; j++) {
                float theta1 = 2.0f * (float) Math.PI * j / radialSegments;
                float theta2 = 2.0f * (float) Math.PI * (j + 1) / radialSegments;

                float x1 = r * (float) Math.cos(theta1);
                float z1 = r * (float) Math.sin(theta1);

                float x2 = r * (float) Math.cos(theta2);
                float z2 = r * (float) Math.sin(theta2);

                buffer.put(x1).put(y).put(z1);
                buffer.put(x2).put(y).put(z2);
            }
        }

        // 2. Radial spokes
        for (int j = 0; j < radialSegments; j++) {
            float theta = 2.0f * (float) Math.PI * j / radialSegments;
            float cosT = (float) Math.cos(theta);
            float sinT = (float) Math.sin(theta);

            for (int i = 0; i < ringSegments - 1; i++) {
                float t1 = (float) i / (ringSegments - 1);
                float t2 = (float) (i + 1) / (ringSegments - 1);

                float r1 = rs + (maxRadius - rs) * (t1 * t1);
                float r2 = rs + (maxRadius - rs) * (t2 * t2);

                float y1 = -2.0f * (float) Math.sqrt(Math.max(0.0f, rs * (r1 - rs)));
                float y2 = -2.0f * (float) Math.sqrt(Math.max(0.0f, rs * (r2 - rs)));

                buffer.put(r1 * cosT).put(y1).put(r1 * sinT);
                buffer.put(r2 * cosT).put(y2).put(r2 * sinT);
            }
        }

        buffer.flip();

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
        MemoryUtil.memFree(buffer);

        return new int[]{vao, vertexCount};
    }

    /**
     * Creates a simple full-screen quad VAO for fragment-shader ray tracing
     */
    public static int createScreenQuadVAO() {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        float[] quadVertices = {
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        };

        FloatBuffer buffer = MemoryUtil.memAllocFloat(quadVertices.length);
        buffer.put(quadVertices).flip();

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
        MemoryUtil.memFree(buffer);

        return vao;
    }
}
