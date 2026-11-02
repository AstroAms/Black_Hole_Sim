package com.blackhole;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class TextureUtils {

    public static int loadTexture(String path) throws Exception {
        int textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            InputStream is = TextureUtils.class.getResourceAsStream(path);
            if(is==null) throw new RuntimeException("Texture not found: "+path);

            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();

            ByteBuffer image = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4);
            if(image==null) throw new RuntimeException("Failed to load: "+STBImage.stbi_failure_reason());

            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,w.get(0),h.get(0),0,GL_RGBA,GL_UNSIGNED_BYTE,image);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);

            STBImage.stbi_image_free(image);
            MemoryUtil.memFree(buffer);
        }

        return textureID;
    }
}
