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
        attribute vec4 a_position;
        attribute vec2 a_texCoord;
        varying vec2 v_texCoord;

        void main() {
            gl_Position = a_position;
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
     * Uses 9-tap kernel for good quality and performance.
     */
    const val BLUR_HORIZONTAL_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D u_texture;
        uniform vec2 u_resolution;
        uniform float u_blurRadius;
        varying vec2 v_texCoord;

        void main() {
            if (u_blurRadius < 0.01) {
                // No blur, pass through
                gl_FragColor = texture2D(u_texture, v_texCoord);
                return;
            }

            vec2 pixelSize = 1.0 / u_resolution;
            vec4 color = vec4(0.0);

            // 9-tap Gaussian weights (sigma ~= 2.0) - ES 2.0 compatible
            float weights[9];
            weights[0] = 0.0204;
            weights[1] = 0.0577;
            weights[2] = 0.1215;
            weights[3] = 0.1899;
            weights[4] = 0.2210;
            weights[5] = 0.1899;
            weights[6] = 0.1215;
            weights[7] = 0.0577;
            weights[8] = 0.0204;

            // Horizontal blur
            for (int i = -4; i <= 4; i++) {
                vec2 offset = vec2(float(i) * pixelSize.x * u_blurRadius, 0.0);
                color += texture2D(u_texture, v_texCoord + offset) * weights[i + 4];
            }

            gl_FragColor = color;
        }
    """

    /**
     * Fragment shader for vertical Gaussian blur pass.
     * Uses 9-tap kernel matching horizontal pass.
     */
    const val BLUR_VERTICAL_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D u_texture;
        uniform vec2 u_resolution;
        uniform float u_blurRadius;
        varying vec2 v_texCoord;

        void main() {
            vec2 pixelSize = 1.0 / u_resolution;
            vec4 color = vec4(0.0);

            // 9-tap Gaussian weights (sigma ~= 2.0) - ES 2.0 compatible
            float weights[9];
            weights[0] = 0.0204;
            weights[1] = 0.0577;
            weights[2] = 0.1215;
            weights[3] = 0.1899;
            weights[4] = 0.2210;
            weights[5] = 0.1899;
            weights[6] = 0.1215;
            weights[7] = 0.0577;
            weights[8] = 0.0204;

            // Vertical blur
            for (int i = -4; i <= 4; i++) {
                vec2 offset = vec2(0.0, float(i) * pixelSize.y * u_blurRadius);
                color += texture2D(u_texture, v_texCoord + offset) * weights[i + 4];
            }

            gl_FragColor = color;
        }
    """

    /**
     * Fragment shader for color effects (darken, vignette, grayscale).
     * Applied after blur passes.
     */
    const val EFFECTS_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D u_texture;
        uniform float u_alpha;
        uniform float u_darkenFactor;
        uniform float u_vignetteFactor;
        uniform float u_grayscaleEnabled;
        varying vec2 v_texCoord;

        void main() {
            vec4 color = texture2D(u_texture, v_texCoord);

            // 1. Apply darken (brightness adjustment)
            if (u_darkenFactor < 0.99) {
                color.rgb *= u_darkenFactor;
            }

            // 2. Apply vignette (radial gradient)
            if (u_vignetteFactor > 0.01) {
                vec2 center = v_texCoord - 0.5;
                float dist = length(center);
                float vignette = smoothstep(0.7, 0.2, dist * (1.0 + u_vignetteFactor));
                color.rgb *= vignette;
            }

            // 3. Apply grayscale (luminance calculation)
            if (u_grayscaleEnabled > 0.5) {
                // ITU-R BT.709 standard
                float gray = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
                color.rgb = vec3(gray);
            }

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
