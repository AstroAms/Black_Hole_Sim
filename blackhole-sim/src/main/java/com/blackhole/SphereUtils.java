package com.blackhole;

import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class SphereUtils {
    public static int[] createSphereVAO(float radius, int stacks, int slices) {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        FloatBuffer vertices = MemoryUtil.memAllocFloat((stacks+1)*(slices+1)*3);
        IntBuffer indices = MemoryUtil.memAllocInt(stacks*slices*6);

        for(int i=0;i<=stacks;i++){
            float phi = (float)Math.PI * i / stacks;
            for(int j=0;j<=slices;j++){
                float theta = 2f * (float)Math.PI * j / slices;
                float x = radius*(float)(Math.sin(phi)*Math.cos(theta));
                float y = radius*(float)Math.cos(phi);
                float z = radius*(float)(Math.sin(phi)*Math.sin(theta));
                vertices.put(x).put(y).put(z);
            }
        }
        vertices.flip();

        for(int i=0;i<stacks;i++){
            for(int j=0;j<slices;j++){
                int first = i*(slices+1)+j;
                int second = first + slices + 1;
                indices.put(first).put(second).put(first+1);
                indices.put(second).put(second+1).put(first+1);
            }
        }
        indices.flip();

        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glVertexAttribPointer(0,3,GL_FLOAT,false,3*Float.BYTES,0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
        MemoryUtil.memFree(vertices);
        MemoryUtil.memFree(indices);

        return new int[]{vao, indices.capacity()};
    }
}
