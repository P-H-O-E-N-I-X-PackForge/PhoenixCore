package net.phoenix.core.integration.conflux.dimension.shaders;

import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.Map;

public class CompiledShaderProgram {

    private final int programId;
    private final String name;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    public CompiledShaderProgram(int programId, String name) {
        this.programId = programId;
        this.name = name;
    }

    private int getUniformLocation(String uniformName) {
        return uniformLocations.computeIfAbsent(uniformName, name -> {
            int location = GL20.glGetUniformLocation(programId, uniformName);
            if (location == -1) {
                System.err.println("[PhoenixCore] Uniform not found: " + uniformName + " in shader " + this.name);
            }
            return location;
        });
    }

    public void use() {
        GL20.glUseProgram(programId);
    }

    public boolean isValid() {
        return programId > 0 && GL20.glIsProgram(programId);
    }

    public void setUniform1f(String uniformName, float value) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            use();
            GL20.glUniform1f(location, value);
        }
    }

    public void setUniform2f(String uniformName, float x, float y) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            use();
            GL20.glUniform2f(location, x, y);
        }
    }

    public void setUniform3f(String uniformName, float x, float y, float z) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            use();
            GL20.glUniform3f(location, x, y, z);
        }
    }

    public void setUniform4f(String uniformName, float x, float y, float z, float w) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            use();
            GL20.glUniform4f(location, x, y, z, w);
        }
    }

    public void setUniform1i(String uniformName, int value) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            use();
            GL20.glUniform1i(location, value);
        }
    }

    public void setUniform2i(String uniformName, int x, int y) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            use();
            GL20.glUniform2i(location, x, y);
        }
    }

    public void delete() {
        if (isValid()) {
            GL20.glDeleteProgram(programId);
        }
    }

    public int getProgramId() {
        return programId;
    }

    public String getName() {
        return name;
    }
}
