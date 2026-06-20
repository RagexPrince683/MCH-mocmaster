#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;

//制作者 TV90
void main() {
    vec4 c = texture2D(DiffuseSampler, texCoord);
    float yellowness = (c.r + c.g) * 0.5 - c.b;
    const float threshold = 0.2;
    float isYellow = step(threshold, yellowness);
    vec3 enhanced = clamp(c.rgb + vec3(0.1, 0.1, -0.05), 0.0, 1.0);
    vec3 result = mix(c.rgb, enhanced, isYellow);
    gl_FragColor = vec4(result, c.a);
}
