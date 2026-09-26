package com.anthonyla.paperize.service.livewallpaper.renderer

object GLShaders {

    const val VERTEX_SHADER = """
        uniform mat4 u_mvpMatrix;
        attribute vec4 a_position;
        attribute vec2 a_texCoord;
        varying vec2 v_texCoord;
        varying vec2 v_imageCoord;

        void main() {
            gl_Position = u_mvpMatrix * a_position;
            v_texCoord = a_texCoord;
            // a_position spans the complete picture even when its texture is split
            // into tiles. Keep a global image coordinate so image-wide effects do
            // not restart at every tile boundary.
            v_imageCoord = vec2(a_position.x * 0.5 + 0.5, 0.5 - a_position.y * 0.5);
        }
    """

    /**
     * Fragment shader for directional Gaussian blur pass.
     * Uses 17-tap kernel (half-integer steps) to eliminate banding/ghosting at high blur radii.
     * Weights are pre-computed for sigma ~= 1.815 sampled at {0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4} steps.
     * Branchless: when blurRadius is 0, all offsets are 0 so all samples collapse to center.
     */
    const val BLUR_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D u_texture;
        uniform vec2 u_resolution;
        uniform vec2 u_direction;
        uniform float u_blurRadius;
        varying vec2 v_texCoord;

        void main() {
            vec2 stepSize = u_direction * u_blurRadius / u_resolution;

            vec4 color = texture2D(u_texture, v_texCoord) * 0.1120;

            color += (texture2D(u_texture, v_texCoord + (-0.5 * stepSize)) +
                      texture2D(u_texture, v_texCoord + (0.5 * stepSize))) * 0.1078;
            color += (texture2D(u_texture, v_texCoord + (-1.0 * stepSize)) +
                      texture2D(u_texture, v_texCoord + (1.0 * stepSize))) * 0.0962;
            color += (texture2D(u_texture, v_texCoord + (-1.5 * stepSize)) +
                      texture2D(u_texture, v_texCoord + (1.5 * stepSize))) * 0.0796;
            color += (texture2D(u_texture, v_texCoord + (-2.0 * stepSize)) +
                      texture2D(u_texture, v_texCoord + (2.0 * stepSize))) * 0.0610;
            color += (texture2D(u_texture, v_texCoord + (-2.5 * stepSize)) +
                      texture2D(u_texture, v_texCoord + (2.5 * stepSize))) * 0.0434;
            color += (texture2D(u_texture, v_texCoord + (-3.0 * stepSize)) +
                      texture2D(u_texture, v_texCoord + (3.0 * stepSize))) * 0.0286;
            color += (texture2D(u_texture, v_texCoord + (-3.5 * stepSize)) +
                      texture2D(u_texture, v_texCoord + (3.5 * stepSize))) * 0.0175;
            color += (texture2D(u_texture, v_texCoord + (-4.0 * stepSize)) +
                      texture2D(u_texture, v_texCoord + (4.0 * stepSize))) * 0.0099;

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
        varying vec2 v_imageCoord;
 
        void main() {
            vec4 color = texture2D(u_texture, v_texCoord);
 
            color.rgb *= (1.0 - u_darkenFactor);
 
            // 2. Apply vignette — matches CPU vignetteBitmap gradient stops:
            //    [0% dark at center] → [10% dark at 70% of radius] → [80% dark at edge]
            //    radius is normalized to 0.5 UV (image edge along shorter axis)
            vec2 vignetteCenter = v_imageCoord - 0.5;
            float dist = length(vignetteCenter);
            float t = clamp(dist / 0.5, 0.0, 1.0);
            float innerDark = smoothstep(0.0, 0.7, t) * 0.1;
            float outerDark = smoothstep(0.7, 1.0, t) * 0.7;
            float darkAmount = innerDark + outerDark;
            color.rgb *= (1.0 - darkAmount * u_vignetteFactor);
 
            // 3. Apply grayscale (branchless - mix handles factor 0 correctly)
            // ITU-R BT.709 standard luminance calculation
            float gray = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
            color.rgb = mix(color.rgb, vec3(gray), u_grayscaleFactor);
 
            color.rgb *= u_adaptiveBrightnessFactor;

            gl_FragColor = vec4(color.rgb, color.a * u_alpha);
        }
    """

}
