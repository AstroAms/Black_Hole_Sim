package com.blackhole;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    public int programId;

    public ShaderProgram(String vertexSrc, String fragmentSrc) {
        int vertex = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertex, vertexSrc);
        glCompileShader(vertex);

        if (glGetShaderi(vertex, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException(glGetShaderInfoLog(vertex));
        }

        int fragment = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragment, fragmentSrc);
        glCompileShader(fragment);

        if (glGetShaderi(fragment, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException(glGetShaderInfoLog(fragment));
        }

        programId = glCreateProgram();
        glAttachShader(programId, vertex);
        glAttachShader(programId, fragment);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException(glGetProgramInfoLog(programId));
        }

        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    public void use() {
        glUseProgram(programId);
    }
}
