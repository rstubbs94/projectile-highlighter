package com.projectilehighlighter.model;

import com.projectilehighlighter.ProjectileHighlighterConfig.OverlayStyle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.Color;

/**
 * Represents a single projectile configuration within a group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectileEntry
{
    private int projectileId;
    private String customName;
    private int colorRgb;
    private int colorAlpha;
    private OverlayStyle overlayStyle;

    public Color getColor()
    {
        return new Color(
            (colorRgb >> 16) & 0xFF,
            (colorRgb >> 8) & 0xFF,
            colorRgb & 0xFF,
            colorAlpha
        );
    }

    public void setColor(Color color)
    {
        this.colorRgb = (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
        this.colorAlpha = color.getAlpha();
    }

    public static ProjectileEntry createDefault(int projectileId, Color defaultColor, OverlayStyle defaultStyle)
    {
        return ProjectileEntry.builder()
            .projectileId(projectileId)
            .customName(null)
            .colorRgb((defaultColor.getRed() << 16) | (defaultColor.getGreen() << 8) | defaultColor.getBlue())
            .colorAlpha(defaultColor.getAlpha())
            .overlayStyle(defaultStyle)
            .build();
    }
}
