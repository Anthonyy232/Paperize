package com.anthonyla.paperize.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Design Tokens
 *
 * Spacing, shapes, and layout constants following Material 3 Expressive guidelines.
 * Expressive design emphasizes generous spacing, larger corner radii, and comfortable
 * layouts that feel open and inviting.
 *
 * All spacing values use a 4dp base unit for consistency and visual harmony.
 */
object AppSpacing {
    /**
     * Extra small spacing (6.dp)
     * Increased from 4dp for better breathing room
     * Used for: Text line gaps, tight groupings within components
     */
    val extraSmall = 6.dp

    /**
     * Small spacing (12.dp)
     * Increased from 8dp for expressive comfort
     * Used for: Form fields, list items, content within cards, component internal padding
     */
    val small = 12.dp

    /**
     * Medium spacing (16.dp)
     * Increased from 12dp for clear visual separation
     * Used for: Section breaks, moderate content separation, card internal padding
     */
    val medium = 16.dp

    /**
     * Large spacing (20.dp)
     * Increased from 16dp for prominent separation
     * Used for: Screen edges, card padding, major section separation
     */
    val large = 20.dp

    /**
     * Extra large spacing (28.dp)
     * Increased from 24dp for expressive hierarchy
     * Used for: Screen-level separation, major feature divisions, section headers
     */
    val extraLarge = 28.dp

    /**
     * Huge spacing (36.dp)
     * New addition for expressive design
     * Used for: Major screen divisions, empty state spacing, hero sections
     */
    val huge = 36.dp

    // Grid-specific spacing
    /**
     * Grid spacing (8.dp)
     * Increased from 4dp for better grid item separation
     * Used for: Spacing between grid items in LazyGrids
     */
    val gridSpacing = 8.dp

    /**
     * Grid padding (12.dp)
     * Increased from 4dp for comfortable grid container padding
     * Used for: Content padding around grid containers
     */
    val gridPadding = 12.dp
}

/**
 * Material 3 Expressive Shape Tokens
 *
 * Larger corner radii create softer, more approachable UI elements.
 * These values align with the ExpressiveShapes used in the theme.
 */
object AppShapes {
    /**
     * Primary card shape (20.dp corners)
     * Increased from 16dp for expressive softness
     * Used for: Main content cards, feature cards, dialog containers
     */
    val cardShape = RoundedCornerShape(20.dp)

    /**
     * Medium card shape (16.dp corners)
     * Replaces smallCardShape with more generous rounding
     * Used for: Secondary cards, nested cards, list items with rounded corners
     */
    val mediumCardShape = RoundedCornerShape(16.dp)

    /**
     * Small card shape (12.dp corners)
     * Used for: Compact cards, chips, small UI elements
     */
    val smallCardShape = RoundedCornerShape(12.dp)

    /**
     * Image shape (12.dp corners)
     * Increased from 8dp for softer image presentation
     * Used for: Images within cards, thumbnails, media elements
     */
    val imageShape = RoundedCornerShape(12.dp)

    /**
     * Extra large shape (24.dp corners)
     * For large, prominent UI elements
     * Used for: Full-width cards, bottom sheets, large dialogs
     */
    val extraLargeShape = RoundedCornerShape(24.dp)
}

/**
 * Grid layout constants for responsive, comfortable layouts
 */
object AppGrid {
    /**
     * Minimum size for grid items (160.dp)
     * Increased from 150dp for more comfortable touch targets
     * Used with GridCells.Adaptive for responsive grid layouts
     */
    val itemMinSize = 160.dp

    /**
     * Preferred aspect ratio for grid items (1f for square)
     * Maintains consistent proportions across different screen sizes
     */
    const val itemAspectRatio = 1f
}

/**
 * Elevation tokens for layering and depth
 * Material 3 Expressive uses subtle elevation for visual hierarchy
 */
object AppElevation {
    /**
     * Level 0 (0.dp) - No elevation
     * Used for: Background surfaces, base layer
     */
    val level0 = 0.dp

    /**
     * Level 1 (1.dp) - Minimal elevation
     * Used for: Cards at rest, subtle layering
     */
    val level1 = 1.dp

    /**
     * Level 2 (3.dp) - Low elevation
     * Used for: Cards with interaction, FABs at rest
     */
    val level2 = 3.dp

    /**
     * Level 3 (6.dp) - Medium elevation
     * Used for: Elevated cards, FABs on hover/press, dialogs
     */
    val level3 = 6.dp

    /**
     * Level 4 (8.dp) - High elevation
     * Used for: Navigation drawer, menus, prominent dialogs
     */
    val level4 = 8.dp

    /**
     * Level 5 (12.dp) - Very high elevation
     * Used for: Modal bottom sheets, tooltips
     */
    val level5 = 12.dp

    /**
     * Component Elevation (10.dp) - Custom elevation for interactive components
     * Used for: Settings switches, interactive cards with tonal elevation
     * Note: Custom value chosen for Material 3 Expressive design to provide
     * stronger visual distinction while maintaining accessibility
     */
    val componentElevation = 10.dp

    /**
     * Selection elevation (5.dp) - Elevation for selected items
     * Used for: Selected folders, wallpapers, and other selectable items
     */
    val selectionElevation = 5.dp
}

/**
 * Icon size tokens for consistent icon sizing throughout the app
 */
object AppIconSizes {
    /**
     * Small icon (24.dp)
     * Used for: Toolbar icons, inline icons, compact UI elements
     */
    val small = 24.dp

    /**
     * Medium icon (32.dp)
     * Used for: List item icons, secondary actions
     */
    val medium = 32.dp

    /**
     * Large icon (48.dp)
     * Used for: Primary action buttons, prominent icons
     */
    val large = 48.dp

    /**
     * Extra large icon (56.dp)
     * Used for: Album covers, feature icons
     */
    val extraLarge = 56.dp

    /**
     * Huge icon (64.dp)
     * Used for: Empty state icons, onboarding icons
     */
    val huge = 64.dp

    /**
     * Massive icon (80.dp)
     * Used for: Large empty state icons, hero icons
     */
    val massive = 80.dp
}

/**
 * Border width tokens for consistent borders
 */
object AppBorderWidths {
    /**
     * Thin border (1.dp)
     * Used for: Subtle dividers, secondary borders
     */
    val thin = 1.dp

    /**
     * Medium border (2.dp)
     * Used for: Default borders, card outlines
     */
    val medium = 2.dp

    /**
     * Thick border (3.dp)
     * Used for: Selection indicators, prominent borders
     */
    val thick = 3.dp
}

/**
 * Corner radius tokens for consistent rounded corners
 */
object AppRadii {
    /**
     * Tiny radius (4.dp)
     * Used for: Small UI elements, middle items in lists
     */
    val tiny = 4.dp

    /**
     * Small radius (12.dp)
     * Used for: Compact cards, chips, images
     */
    val small = 12.dp

    /**
     * Medium radius (16.dp)
     * Used for: Standard cards, dialogs, buttons
     */
    val medium = 16.dp

    /**
     * Large radius (20.dp)
     * Used for: Primary cards, prominent UI elements
     */
    val large = 20.dp

    /**
     * Extra large radius (32.dp)
     * Used for: Pressed states, prominent buttons
     */
    val extraLarge = 32.dp

    /**
     * Huge radius (36.dp)
     * Used for: Large pressed states, hero cards
     */
    val huge = 36.dp

    /**
     * Massive radius (40.dp)
     * Used for: Maximum rounding, special effects
     */
    val massive = 40.dp
}

/**
 * Alpha/opacity tokens for consistent transparency
 */
object AppAlpha {
    /**
     * Very subtle opacity (0.1f)
     * Used for: Very light overlays, subtle backgrounds
     */
    const val verySubtle = 0.1f

    /**
     * Subtle opacity (0.15f)
     * Used for: Light borders, faint overlays
     */
    const val subtle = 0.15f

    /**
     * Light opacity (0.25f)
     * Used for: Disabled states, subtle icons
     */
    const val light = 0.25f

    /**
     * Medium opacity (0.3f)
     * Used for: Secondary icons, medium overlays
     */
    const val medium = 0.3f

    /**
     * Semi-medium opacity (0.4f)
     * Used for: Empty state icons, inactive elements
     */
    const val semiMedium = 0.4f

    /**
     * Selected opacity (0.7f)
     * Used for: Selected items, active indicators
     */
    const val selected = 0.7f

    /**
     * Prominent opacity (0.8f)
     * Used for: Important text, primary overlays
     */
    const val prominent = 0.8f

    /**
     * Full opacity (1.0f)
     * Used for: Fully visible elements
     */
    const val full = 1.0f
}

/**
 * Elevation delta tokens for animated elevation changes
 */
object AppElevationDeltas {
    /**
     * Pressed elevation increase (2.dp)
     * Amount to increase elevation when an element is pressed
     */
    val pressedIncrease = 2.dp
}

/**
 * Maximum width constraints for responsive layouts
 */
object AppMaxWidths {
    /**
     * Maximum content width (600.dp)
     * Material 3 guideline for optimal reading width
     * Used for: Content containers on large screens
     */
    val contentMaxWidth = 600.dp
}

/**
 * Scale factor tokens for sizing elements relative to containers
 */
object AppScaleFactor {
    /**
     * Medium icon scale (0.45f)
     * Used for: Icons that take 45% of container size
     */
    const val iconMedium = 0.45f

    /**
     * Large icon scale (0.5f)
     * Used for: Icons that take 50% of container size
     */
    const val iconLarge = 0.5f

    /**
     * Threshold (0.5f)
     * Used for: Conditional logic based on 50% threshold
     */
    const val threshold = 0.5f
}
