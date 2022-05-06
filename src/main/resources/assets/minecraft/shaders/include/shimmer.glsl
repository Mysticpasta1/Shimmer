#version 150

//shimmer light
struct Light {
    vec4 color;
    vec3 position;
    float radius;
};

layout (std140) uniform Lights {
    Light lights[2048];     //  16  8 * 2048
};

layout (std140) uniform Env {
    int lightCount;
    vec3 camPos;
};

vec4 color_light(vec3 pos, vec4 vertex_color) {
    vec3 lightColor = vec3(0., 0., 0.);
    vec3 fragPos = pos + camPos;
    for (int i = 0; i < lightCount; i++) {
        Light l = lights[i];
        float radius = pow(l.radius, 2);
        vec3 poss = vec3(0.,0.,0.);
        float intensity = pow(max(0., 1. - distance(l.position, fragPos) / l.radius), 2);
        lightColor += l.color.rgb * l.color.a * intensity;
    }

    vec3 lcolor_2 = clamp(lightColor.rgb, 0.0f, 1.0f);

    // blend
    return vec4(vertex_color.rgb + lcolor_2, 1.0);
    //    return vec4(max(vertex_color.rgb, lcolor_2), 1.0);
}
