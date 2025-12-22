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
     * Uses 9-tap kernel with branchless design for optimal GPU performance.
     * Weights are pre-computed for sigma ~= 2.0.
     */
    const val BLUR_HORIZONTAL_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D u_texture;
        uniform vec2 u_resolution;
        uniform float u_blurRadius;
        varying vec2 v_texCoord;

        void main() {
            vec2 pixelSize = 1.0 / u_resolution;

            // Sample center pixel
            vec4 center = texture2D(u_texture, v_texCoord);

            // 9-tap Gaussian blur (branchless - when radius is 0, offsets are 0)
            vec4 color = center * 0.2210;

            // Symmetric pairs for efficiency
            float r = u_blurRadius;
            color += (texture2D(u_texture, v_texCoord + vec2(-4.0 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 4.0 * pixelSize.x * r, 0.0))) * 0.0204;
            color += (texture2D(u_texture, v_texCoord + vec2(-3.0 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 3.0 * pixelSize.x * r, 0.0))) * 0.0577;
            color += (texture2D(u_texture, v_texCoord + vec2(-2.0 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 2.0 * pixelSize.x * r, 0.0))) * 0.1215;
            color += (texture2D(u_texture, v_texCoord + vec2(-1.0 * pixelSize.x * r, 0.0)) +
                      texture2D(u_texture, v_texCoord + vec2( 1.0 * pixelSize.x * r, 0.0))) * 0.1899;

            gl_FragColor = color;
        }
    """

    /**
     * Fragment shader for vertical Gaussian blur pass.
     * Uses 9-tap kernel with branchless design matching horizontal pass.
     */
    const val BLUR_VERTICAL_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D u_texture;
        uniform vec2 u_resolution;
        uniform float u_blurRadius;
        varying vec2 v_texCoord;

        void main() {
            vec2 pixelSize = 1.0 / u_resolution;

            // Sample center pixel
            vec4 center = texture2D(u_texture, v_texCoord);

            // 9-tap Gaussian blur (branchless - when radius is 0, offsets are 0)
            vec4 color = center * 0.2210;

            // Symmetric pairs for efficiency
            float r = u_blurRadius;
            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -4.0 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  4.0 * pixelSize.y * r))) * 0.0204;
            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -3.0 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  3.0 * pixelSize.y * r))) * 0.0577;
            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -2.0 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  2.0 * pixelSize.y * r))) * 0.1215;
            color += (texture2D(u_texture, v_texCoord + vec2(0.0, -1.0 * pixelSize.y * r)) +
                      texture2D(u_texture, v_texCoord + vec2(0.0,  1.0 * pixelSize.y * r))) * 0.1899;

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
 
            // 2. Apply vignette (branchless using mix)
            // When vignetteFactor is 0, the vignette calculation still runs but
            // the smoothstep result approaches 1.0 everywhere, so color is unchanged
            vec2 center = v_texCoord - 0.5;
            float dist = length(center);
            float vignette = 1.0 - smoothstep(0.3, 0.9, dist * (1.0 + u_vignetteFactor * 2.0));
            // Mix between original (1.0) and vignette based on factor
            float vignetteMultiplier = mix(1.0, vignette, u_vignetteFactor);
            color.rgb *= vignetteMultiplier;
 
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
