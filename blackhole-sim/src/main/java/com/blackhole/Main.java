package com.blackhole;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private long window;
    private int width = 1280, height = 720;

    private int raymarchProgram;
    private int gridProgram;
    private int screenQuadVAO;
    private int funnelVAO, funnelVertexCount;
    private int starsTexture;

    // Simulation parameters
    private float bhRadius = 1.0f;
    private boolean enableLensing = true;
    private boolean enableDisk = true;
    private boolean enableDoppler = true;
    private boolean enableGrid = false;
    private boolean enableStars = true;
    private boolean showFunnelMesh = false;
    private boolean isPaused = false;
    private float diskBrightness = 1.25f;
    private float simTime = 0.0f;

    // Camera parameters
    private float camDistance = 14.0f;
    private float camAzimuth = 0.2f;
    private float camElevation = 1.40f; // ~80 degrees (cinematic near-equatorial inclination)
    private float fov = (float) Math.toRadians(55.0);

    // Mouse tracking
    private boolean leftMouseDown = false;
    private double lastMouseX = 0, lastMouseY = 0;

    // Projection & view matrices for 3D overlay
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f model = new Matrix4f();

    public void run() throws Exception {
        init();
        loop();

        // Cleanup
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() throws Exception {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "Black Hole Relativistic Simulation", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create GLFW window");

        // Center window on screen
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            glfwSetWindowPos(window, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable VSync
        glfwShowWindow(window);
        GL.createCapabilities();

        setupCallbacks();

        // Compile shaders
        raymarchProgram = ShaderUtils.createShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");
        gridProgram = ShaderUtils.createShaderProgram("/shaders/grid_vertex.glsl", "/shaders/grid_fragment.glsl");

        // Create geometry
        screenQuadVAO = GridUtils.createScreenQuadVAO();
        int[] funnelResult = GridUtils.createFlammFunnelVAO(bhRadius, 20.0f, 48, 30);
        funnelVAO = funnelResult[0];
        funnelVertexCount = funnelResult[1];

        // Load background stars
        starsTexture = TextureUtils.loadTexture("/textures/stars.png");

        System.out.println("=================================================");
        System.out.println(" Black Hole Relativistic Simulation Initialized ");
        System.out.println(" OpenGL Version: " + glGetString(GL_VERSION));
        System.out.println(" Controls:");
        System.out.println("  - Left Mouse Drag : Orbit camera 360 degrees");
        System.out.println("  - Scroll Wheel    : Zoom in / out");
        System.out.println("  - [G]             : Toggle Spacetime Grid shader");
        System.out.println("  - [M]             : Toggle 3D Spacetime Funnel Mesh");
        System.out.println("  - [D]             : Toggle Accretion Disk");
        System.out.println("  - [L]             : Toggle Relativistic Lensing");
        System.out.println("  - [B]             : Toggle Doppler Beaming");
        System.out.println("  - [S]             : Toggle Starfield Background");
        System.out.println("  - [SPACE]         : Pause / Resume simulation");
        System.out.println("  - [1] - [4]       : Preset Camera Views");
        System.out.println("  - [R]             : Reset Camera View");
        System.out.println("  - [UP] / [DOWN]   : Adjust Black Hole mass/radius");
        System.out.println("  - [LEFT] / [RIGHT]: Adjust Disk Glow Intensity");
        System.out.println("=================================================");
    }

    private void setupCallbacks() {
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            if (w > 0 && h > 0) {
                width = w;
                height = h;
                glViewport(0, 0, width, height);
            }
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW_PRESS) {
                    leftMouseDown = true;
                    double[] xpos = new double[1], ypos = new double[1];
                    glfwGetCursorPos(win, xpos, ypos);
                    lastMouseX = xpos[0];
                    lastMouseY = ypos[0];
                } else if (action == GLFW_RELEASE) {
                    leftMouseDown = false;
                }
            }
        });

        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (leftMouseDown) {
                float dx = (float) (xpos - lastMouseX);
                float dy = (float) (ypos - lastMouseY);

                camAzimuth += dx * 0.006f;
                camElevation += dy * 0.006f;

                // Clamp elevation to avoid gimbal flip
                camElevation = Math.max(0.015f, Math.min((float) Math.PI - 0.015f, camElevation));

                lastMouseX = xpos;
                lastMouseY = ypos;
            }
        });

        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            camDistance -= (float) yoffset * 0.8f;
            camDistance = Math.max(2.2f * bhRadius, Math.min(50.0f, camDistance));
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                switch (key) {
                    case GLFW_KEY_G -> enableGrid = !enableGrid;
                    case GLFW_KEY_M -> showFunnelMesh = !showFunnelMesh;
                    case GLFW_KEY_D -> enableDisk = !enableDisk;
                    case GLFW_KEY_L -> enableLensing = !enableLensing;
                    case GLFW_KEY_B -> enableDoppler = !enableDoppler;
                    case GLFW_KEY_S -> enableStars = !enableStars;
                    case GLFW_KEY_SPACE -> isPaused = !isPaused;
                    case GLFW_KEY_R -> {
                        camDistance = 14.0f;
                        camAzimuth = 0.2f;
                        camElevation = 1.40f;
                    }
                    case GLFW_KEY_1 -> {
                        // Preset 1: Equatorial view (looking edge-on at disk)
                        camDistance = 13.0f;
                        camAzimuth = 0.0f;
                        camElevation = (float) Math.PI * 0.5f - 0.01f;
                    }
                    case GLFW_KEY_2 -> {
                        // Preset 2: 30-degree cinematic inclination (Interstellar style)
                        camDistance = 14.0f;
                        camAzimuth = 0.4f;
                        camElevation = 1.25f;
                    }
                    case GLFW_KEY_3 -> {
                        // Preset 3: Polar view (top-down into the vortex)
                        camDistance = 18.0f;
                        camAzimuth = 0.0f;
                        camElevation = 0.05f;
                    }
                    case GLFW_KEY_4 -> {
                        // Preset 4: Close-up on the photon ring
                        camDistance = 4.2f * bhRadius;
                        camAzimuth = 0.3f;
                        camElevation = 1.35f;
                    }
                    case GLFW_KEY_UP -> {
                        bhRadius = Math.min(3.0f, bhRadius + 0.1f);
                        rebuildFunnel();
                    }
                    case GLFW_KEY_DOWN -> {
                        bhRadius = Math.max(0.4f, bhRadius - 0.1f);
                        rebuildFunnel();
                    }
                    case GLFW_KEY_RIGHT -> diskBrightness = Math.min(3.0f, diskBrightness + 0.15f);
                    case GLFW_KEY_LEFT -> diskBrightness = Math.max(0.2f, diskBrightness - 0.15f);
                    case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(win, true);
                }
            }
        });
    }

    private void rebuildFunnel() {
        glDeleteVertexArrays(funnelVAO);
        int[] result = GridUtils.createFlammFunnelVAO(bhRadius, 20.0f, 48, 30);
        funnelVAO = result[0];
        funnelVertexCount = result[1];
    }

    private void loop() {
        double lastFrameTime = glfwGetTime();
        double lastTitleUpdate = lastFrameTime;
        int frameCounter = 0;

        while (!glfwWindowShouldClose(window)) {
            double currentFrameTime = glfwGetTime();
            float deltaTime = (float) (currentFrameTime - lastFrameTime);
            lastFrameTime = currentFrameTime;

            if (!isPaused) {
                simTime += deltaTime;
            }

            // Update FPS & Status in Title Bar
            frameCounter++;
            if (currentFrameTime - lastTitleUpdate >= 0.25) {
                double fps = frameCounter / (currentFrameTime - lastTitleUpdate);
                String title = String.format(
                    "Black Hole Simulation [FPS: %.0f] | Lensing: %s | Disk: %s | Doppler: %s | Grid: %s | 3D-Mesh: %s",
                    fps,
                    enableLensing ? "ON" : "OFF",
                    enableDisk ? "ON" : "OFF",
                    enableDoppler ? "ON" : "OFF",
                    enableGrid ? "ON" : "OFF",
                    showFunnelMesh ? "ON" : "OFF"
                );
                glfwSetWindowTitle(window, title);
                frameCounter = 0;
                lastTitleUpdate = currentFrameTime;
            }

            // Calculate camera position and orthonormal vectors
            float camX = camDistance * (float) (Math.sin(camElevation) * Math.sin(camAzimuth));
            float camY = camDistance * (float) Math.cos(camElevation);
            float camZ = camDistance * (float) (Math.sin(camElevation) * Math.cos(camAzimuth));
            Vector3f camPos = new Vector3f(camX, camY, camZ);

            Vector3f target = new Vector3f(0, 0, 0);
            Vector3f camForward = new Vector3f(target).sub(camPos).normalize();
            Vector3f worldUp = new Vector3f(0, 1, 0);
            Vector3f camRight = new Vector3f(camForward).cross(worldUp).normalize();
            Vector3f camUp = new Vector3f(camRight).cross(camForward).normalize();

            // Clear Screen
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // ================= 1. RENDER RAY-TRACED BLACK HOLE =================
            glDisable(GL_DEPTH_TEST);
            glUseProgram(raymarchProgram);

            // Bind background stars texture to unit 0
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, starsTexture);
            glUniform1i(glGetUniformLocation(raymarchProgram, "uStarsTexture"), 0);

            // Set Raymarch Uniforms
            glUniform2f(glGetUniformLocation(raymarchProgram, "uResolution"), (float) width, (float) height);
            glUniform1f(glGetUniformLocation(raymarchProgram, "uTime"), simTime);
            glUniform3f(glGetUniformLocation(raymarchProgram, "uCamPos"), camPos.x, camPos.y, camPos.z);
            glUniform3f(glGetUniformLocation(raymarchProgram, "uCamForward"), camForward.x, camForward.y, camForward.z);
            glUniform3f(glGetUniformLocation(raymarchProgram, "uCamUp"), camUp.x, camUp.y, camUp.z);
            glUniform3f(glGetUniformLocation(raymarchProgram, "uCamRight"), camRight.x, camRight.y, camRight.z);
            glUniform1f(glGetUniformLocation(raymarchProgram, "uFov"), fov);
            glUniform1f(glGetUniformLocation(raymarchProgram, "uBhRadius"), bhRadius);
            glUniform1i(glGetUniformLocation(raymarchProgram, "uEnableLensing"), enableLensing ? 1 : 0);
            glUniform1i(glGetUniformLocation(raymarchProgram, "uEnableDisk"), enableDisk ? 1 : 0);
            glUniform1i(glGetUniformLocation(raymarchProgram, "uEnableDoppler"), enableDoppler ? 1 : 0);
            glUniform1i(glGetUniformLocation(raymarchProgram, "uEnableGrid"), enableGrid ? 1 : 0);
            glUniform1i(glGetUniformLocation(raymarchProgram, "uEnableStars"), enableStars ? 1 : 0);
            glUniform1f(glGetUniformLocation(raymarchProgram, "uDiskBrightness"), diskBrightness);

            // Draw full-screen quad
            glBindVertexArray(screenQuadVAO);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            glBindVertexArray(0);

            // ================= 2. OPTIONAL 3D SPACETIME FUNNEL MESH =================
            if (showFunnelMesh) {
                glEnable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                glUseProgram(gridProgram);

                projection.identity().perspective(fov, width / (float) height, 0.1f, 100.0f);
                view.identity().lookAt(camPos, target, camUp);
                model.identity();

                float[] matBuffer = new float[16];
                glUniformMatrix4fv(glGetUniformLocation(gridProgram, "uProjection"), false, projection.get(matBuffer));
                glUniformMatrix4fv(glGetUniformLocation(gridProgram, "uView"), false, view.get(matBuffer));
                glUniformMatrix4fv(glGetUniformLocation(gridProgram, "uModel"), false, model.get(matBuffer));
                glUniform1f(glGetUniformLocation(gridProgram, "uBhRadius"), bhRadius);

                glBindVertexArray(funnelVAO);
                glDrawArrays(GL_LINES, 0, funnelVertexCount);
                glBindVertexArray(0);

                glDisable(GL_BLEND);
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run();
    }
}
