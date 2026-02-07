package com.projectilehighlighter.util;

import com.projectilehighlighter.ProjectileHighlighterConfig;

import java.awt.Color;

/**
 * Resolves deterministic default colors for projectile IDs.
 */
public final class ProjectileColorUtil
{
    private ProjectileColorUtil()
    {
        // Utility class
    }

    public static Color getDefaultColorForProjectile(int projectileId, ProjectileHighlighterConfig config)
    {
        if (config.defaultColorMode() == ProjectileHighlighterConfig.DefaultColorMode.FIXED)
        {
            return config.defaultColor();
        }

        return getSeededRandomColor(projectileId, config.defaultColor().getAlpha());
    }

    private static Color getSeededRandomColor(int projectileId, int alpha)
    {
        int hash = mix(projectileId);

        float hue = ((hash & 0x7FFFFFFF) / (float) Integer.MAX_VALUE);
        float saturation = 0.70f + (((hash >>> 8) & 0xFF) / 255f) * 0.25f;
        float brightness = 0.72f + (((hash >>> 16) & 0xFF) / 255f) * 0.22f;

        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return new Color(red, green, blue, alpha);
    }

    private static int mix(int value)
    {
        int x = value;
        x ^= (x >>> 16);
        x *= 0x7FEB352D;
        x ^= (x >>> 15);
        x *= 0x846CA68B;
        x ^= (x >>> 16);
        return x;
    }
}
