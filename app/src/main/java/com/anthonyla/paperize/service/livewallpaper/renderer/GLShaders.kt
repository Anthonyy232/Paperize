package com.anthonyla.paperize.service.livewallpaper.renderer

/**
 * GLSL shader programs for live wallpaper rendering.
 * Includes vertex shaders and fragment shaders for two-pass blur and color effects.
 */
object GLShaders {

    /**
     * Simple vertex shader that passes through positions and texture coordinates.
     * Used for all rendering passes.
     */
    const val VERTEX_SHADER = """
        uniform mat4 u_mvpMatrix;
        attribute vec4 a_position;
        attribute vec2 a_texCoord;
        varying vec2 v_texCoord;

        void main() {
            gl_Position = u_mvpMatrix * a_position;
            v_texCoord = a_texCoord;
        }
    """

    /**
     * Simple fragment shader for basic texture rendering without effects.
     * Used for testing or when no effects are enabled.
     */
    const val SIMPLE_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D u_texture;
        uniform float u_alpha;
        varying vec2 v_texCoord;

        void main() {
            vec4 color = texture2D(u_texture, v_texCoord);
            gl_FragColor = vec4(color.rgb, color.a * u_alpha);
        }
    """

    /**
     * Fragment shader for horizontal Gaussian blur pass.
     * Uses 17-tap kernel (half-integer steps) to eliminate banding/ghosting at high blur radii.
     * Weights are pre-computed for sigma ~= 1.815 sampled at {0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4} steps.
     * Branchless: when blurRadius is 0, all offsets are 0 so all samples collapse to center.
     */
    const val BLUR_HORIZONTAL_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D u_texture;
        uniform vec2 u_resolution;
        uniform float u_blurRadius;
        varying vec2 v_texCoord;

        void main() {
            vec2 pixelSize = 1.0 / u_resolution;
            float r = u_blurRadius;

            // 17-tap Gaussian (half-integer steps: 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0)
            // Doubles sampling density vs 9-tap to eliminate aliasing at high blur radii
            vec4 color = texture2D(u_texture, v_texCoord) * 0.1120;

            color += (texture2D(u_texture, v_texCoord + vec2(-0.5 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 0.5 * pixelSize.x * r, 0.0))) * 0.1078;
            color += (texture2D(u_texture, v_texCoord + vec2(-1.0 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 1.0 * pixelSize.x * r, 0.0))) * 0.0962;
            color += (texture2D(u_texture, v_texCoord + vec2(-1.5 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 1.5 * pixelSize.x * r, 0.0))) * 0.0796;
            color += (texture2D(u_texture, v_texCoord + vec2(-2.0 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 2.0 * pixelSize.x * r, 0.0))) * 0.0610;
            color += (texture2D(u_texture, v_texCoord + vec2(-2.5 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 2.5 * pixelSize.x * r, 0.0))) * 0.0434;
            color += (texture2D(u_texture, v_texCoord + vec2(-3.0 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 3.0 * pixelSize.x * r, 0.0))) * 0.0286;
            color += (texture2D(u_texture, v_texCoord + vec2(-3.5 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 3.5 * pixelSize.x * r, 0.0))) * 0.0175;
            color += (texture2D(u_texture, v_texCoord + vec2(-4.0 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 4.0 * pixelSize.x * r, 0.0))) * 0.0099;

            gl_FragColor = color;
        }
    """

    /**
     * Fragment shader for vertical Gaussian blur pass.
     * Uses 17-tap kernel matching the horizontal pass.
     */
    const val BLUR_VERTICAL_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D u_texture;
        uniform vec2 u_resolution;
        uniform float u_blurRadius;
        varying vec2 v_texCoord;

        void main() {
            vec2 pixelSize = 1.0 / u_resolution;
            float r = u_blurRadius;

            // 17-tap Gaussian (half-integer steps: 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0)
            vec4 color = texture2D(u_texture, v_texCoord) * 0.1120;

            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -0.5 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  0.5 * pixelSize.y * r))) * 0.1078;
            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -1.0 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  1.0 * pixelSize.y * r))) * 0.0962;
            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -1.5 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  1.5 * pixelSize.y * r))) * 0.0796;
            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -2.0 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  2.0 * pixelSize.y * r))) * 0.0610;
            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -2.5 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  2.5 * pixelSize.y * r))) * 0.0434;
            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -3.0 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  3.0 * pixelSize.y * r))) * 0.0286;
            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -3.5 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  3.5 * pixelSize.y * r))) * 0.0175;
            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -4.0 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  4.0 * pixelSize.y * r))) * 0.0099;

            gl_FragColor = color;
        }
    """

    /**
     * Fragment shader for color effects (darken, vignette, grayscale).
     * Applied after blur passes. Uses branchless math for optimal GPU performance.
     */
    const val EFFECTS_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D u_texture;
        uniform float u_alpha;
        uniform float u_darkenFactor;
        uniform float u_vignetteFactor;
        uniform float u_grayscaleFactor;
        uniform float u_adaptiveBrightnessFactor;
        varying vec2 v_texCoord;
 
        void main() {
            vec4 color = texture2D(u_texture, v_texCoord);
 
            // 1. Apply darken (branchless - when factor is 0, multiplier is 1.0)
            color.rgb *= (1.0 - u_darkenFactor);
 
            // 2. Apply vignette — matches CPU vignetteBitmap gradient stops:
            //    [0% dark at center] → [10% dark at 70% of radius] → [80% dark at edge]
            //    radius is normalized to 0.5 UV (image edge along shorter axis)
            vec2 vignetteCenter = v_texCoord - 0.5;
            float dist = length(vignetteCenter);
            float t = clamp(dist / 0.5, 0.0, 1.0);
            // Inner segment: 0% → 10% over [0, 0.7]
            float innerDark = smoothstep(0.0, 0.7, t) * 0.1;
            // Outer segment: 10% → 80% over [0.7, 1.0]
            float outerDark = smoothstep(0.7, 1.0, t) * 0.7;
            float darkAmount = innerDark + outerDark;
            color.rgb *= (1.0 - darkAmount * u_vignetteFactor);
 
            // 3. Apply grayscale (branchless - mix handles factor 0 correctly)
            // ITU-R BT.709 standard luminance calculation
            float gray = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
            color.rgb = mix(color.rgb, vec3(gray), u_grayscaleFactor);
 
            // 4. Apply adaptive brightness multiplier
            color.rgb *= u_adaptiveBrightnessFactor;

            gl_FragColor = vec4(color.rgb, color.a * u_alpha);
        }
    """

    /**
     * Solid color fragment shader for debugging and UI overlays.
     */
    const val COLOR_FRAGMENT_SHADER = """
        precision mediump float;
        uniform vec4 u_color;

        void main() {
            gl_FragColor = u_color;
        }
    """
}
