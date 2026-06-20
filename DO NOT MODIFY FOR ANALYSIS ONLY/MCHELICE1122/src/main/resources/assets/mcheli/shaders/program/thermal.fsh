#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

//制作者 TV90
void main() {
    vec4 centerColor = texture2D(DiffuseSampler, texCoord);
    float brightness = (centerColor.r + centerColor.g + centerColor.b) / 3.0;

    float maxChannel = max(max(centerColor.r, centerColor.g), centerColor.b);
    float minChannel = min(min(centerColor.r, centerColor.g), centerColor.b);
    float saturation = maxChannel - minChannel;
    
    float blueDominance = centerColor.b / max(0.001, max(centerColor.r, centerColor.g));
    bool isSkyLike = blueDominance > 1.1 && brightness > 0.5;
    
    bool isGrayLike = saturation < 0.5;
    bool isBrightEnough = brightness > 0.5;
    
    if (isGrayLike && isBrightEnough && !isSkyLike) {
        float whiteLevel = clamp((brightness - 0.5) / 0.65, 0.0, 1.0);
        whiteLevel = pow(whiteLevel, 0.5) * 1.2;
        gl_FragColor = vec4(vec3(min(whiteLevel, 1.0)), centerColor.a);
    } else {
        float gray = dot(centerColor.rgb, vec3(0.299, 0.587, 0.114));
        if (isSkyLike) {
            gray = gray * 0.5;
        } else {
            gray = gray * 0.25;
        }
        gl_FragColor = vec4(vec3(gray), centerColor.a);
    }
}