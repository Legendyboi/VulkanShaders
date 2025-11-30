#version 450

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec4 a_Color;

layout(location = 0) out vec4 v_Color;

layout(push_constant) uniform PushConstants {
    mat4 u_ModelViewProj;
} pc;

void main() {
    gl_Position = pc.u_ModelViewProj * vec4(a_Position, 1.0);
    v_Color = a_Color;
}
