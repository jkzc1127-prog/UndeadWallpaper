package org.maocide.undeadwallpaper.features

/**
 * 扩展版 OpenGL ES 片段着色器：
 * 在原有亮度 / 对比度 / 饱和度基础上，新增：
 * 1. 实时模糊 (uBlurRadius, uTexelSize)
 * 2. 边缘暗角 (uVignetteIntensity, uVignetteSoftness)
 * 3. 多视频/昼夜切换过渡透明度 (uFadeAlpha)
 * 4. 三击屏幕纯黑模式透明度 (uBlackoutAlpha)
 * 5. 陀螺仪双向视差偏移 (uParallaxOffset)
 */
object EnhancedWallpaperShader {

    const val FRAGMENT_SHADER_OES: String = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;

        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;

        uniform float uBrightness;
        uniform float uContrast;
        uniform float uSaturation;

        uniform vec2 uParallaxOffset;
        uniform float uBlurRadius;
        uniform vec2 uTexelSize;
        uniform float uVignetteIntensity;
        uniform float uVignetteSoftness;
        uniform float uFadeAlpha;
        uniform float uBlackoutAlpha;

        vec3 sampleBlurred(vec2 uv) {
            if (uBlurRadius <= 0.05) {
                return texture2D(sTexture, uv).rgb;
            }
            vec2 step = uTexelSize * uBlurRadius;
            vec3 sum = texture2D(sTexture, uv).rgb * 0.227027;
            sum += texture2D(sTexture, clamp(uv + vec2( step.x,  0.0),    0.0, 1.0)).rgb * 0.121622;
            sum += texture2D(sTexture, clamp(uv + vec2(-step.x,  0.0),    0.0, 1.0)).rgb * 0.121622;
            sum += texture2D(sTexture, clamp(uv + vec2( 0.0,     step.y), 0.0, 1.0)).rgb * 0.121622;
            sum += texture2D(sTexture, clamp(uv + vec2( 0.0,    -step.y), 0.0, 1.0)).rgb * 0.121622;
            vec2 diag = step * 0.7071;
            sum += texture2D(sTexture, clamp(uv + vec2( diag.x,  diag.y), 0.0, 1.0)).rgb * 0.071621;
            sum += texture2D(sTexture, clamp(uv + vec2(-diag.x,  diag.y), 0.0, 1.0)).rgb * 0.071621;
            sum += texture2D(sTexture, clamp(uv + vec2( diag.x, -diag.y), 0.0, 1.0)).rgb * 0.071621;
            sum += texture2D(sTexture, clamp(uv + vec2(-diag.x, -diag.y), 0.0, 1.0)).rgb * 0.071621;
            return sum;
        }

        void main() {
            float totalAlpha = clamp(uFadeAlpha, 0.0, 1.0) * clamp(uBlackoutAlpha, 0.0, 1.0);
            // 纯黑状态下跳过纹理采样，节省 GPU 带宽
            if (totalAlpha <= 0.001) {
                gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
                return;
            }

            vec2 uv = clamp(vTextureCoord + uParallaxOffset, 0.0, 1.0);
            vec3 color = sampleBlurred(uv);

            color = (color - 0.5) * uContrast + 0.5 + uBrightness;
            float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
            color = mix(vec3(luma), color, uSaturation);

            if (uVignetteIntensity > 0.001) {
                vec2 centered = vTextureCoord - 0.5;
                float dist = length(centered) * 1.4142;
                float vig = smoothstep(1.0, max(0.05, 1.0 - uVignetteSoftness), dist);
                color *= mix(1.0, vig, uVignetteIntensity);
            }

            color *= totalAlpha;
            gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
        }
    """.trimIndent()
}
