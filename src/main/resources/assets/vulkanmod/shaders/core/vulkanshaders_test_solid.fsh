#version 450

layout(location = 0) in vec4 v_Color;
layout(location = 0) out vec4 outColor;

void main() {
    // Slight green tint to see it's our shader
    outColor = vec4(v_Color.rgb * vec3(0.8, 1.2, 0.8), v_Color.a);
}
