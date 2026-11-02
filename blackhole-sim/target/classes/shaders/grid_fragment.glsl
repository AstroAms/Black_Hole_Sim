#version 330 core
out vec4 FragColor;

in float vDepth;
in vec3 vWorldPos;

uniform float uBhRadius;

void main() {
    float r = length(vWorldPos.xz);
    // Glow cyan near horizon, fade to deep blue at outer edge
    float t = clamp((r - uBhRadius) / (12.0 * uBhRadius), 0.0, 1.0);
    vec3 color = mix(vec3(0.1, 0.9, 1.0), vec3(0.05, 0.25, 0.8), t);
    
    // Depth fade
    float alpha = mix(0.85, 0.2, t);
    FragColor = vec4(color, alpha);
}
