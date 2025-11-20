package com.anthonyla.paperize.presentation.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Shapes
 *
 * Expressive uses larger, more pronounced corner radii to create softer, more approachable
 * and dynamic UI elements. This follows the M3 Expressive shape system which emphasizes
 * rounded corners for a friendlier, more engaging aesthetic.
 *
 * Key differences from baseline Material 3:
 * - extraSmall: 8dp (was 4dp) - Subtle rounding for tight spaces
 * - small: 12dp (was 8dp) - Cards, chips, and small containers
 * - medium: 16dp (was 12dp) - Standard cards and containers
 * - large: 24dp (was 16dp) - Large cards, bottom sheets
 * - extraLarge: 32dp (was 28dp) - Full-screen dialogs, large surfaces
 *
 * These increased radii create a more modern, expressive feel while maintaining
 * the established Material 3 shape token hierarchy.
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
