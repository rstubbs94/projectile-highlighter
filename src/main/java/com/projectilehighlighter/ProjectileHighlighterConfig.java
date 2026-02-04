package com.projectilehighlighter;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import java.awt.Color;

@ConfigGroup("projectilehighlighter")
public interface ProjectileHighlighterConfig extends Config
{
    // ==================== General Section ====================
    @ConfigSection(
        name = "General",
        description = "General plugin settings",
        position = 0
    )
    String generalSection = "general";

    @ConfigItem(
        keyName = "enabled",
        name = "Enable Highlighting",
        description = "Enable or disable projectile highlighting",
        section = generalSection,
        position = 0
    )
    default boolean enabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "debugMode",
        name = "Debug Mode",
        description = "Shows projectile IDs on the overlay to help identify projectiles",
        section = generalSection,
        position = 1
    )
    default boolean debugMode()
    {
        return false;
    }

    @ConfigItem(
        keyName = "highlightAll",
        name = "Highlight All Projectiles",
        description = "Highlight all projectiles (useful with debug mode to see everything)",
        section = generalSection,
        position = 2
    )
    default boolean highlightAll()
    {
        return false;
    }

    // ==================== Overlay Section ====================
    @ConfigSection(
        name = "Overlay Settings",
        description = "Customize the projectile overlay appearance",
        position = 1
    )
    String overlaySection = "overlay";

    @ConfigItem(
        keyName = "overlayStyle",
        name = "Overlay Style",
        description = "The style of overlay to draw on projectiles",
        section = overlaySection,
        position = 0
    )
    default OverlayStyle overlayStyle()
    {
        return OverlayStyle.HULL;
    }

    @Alpha
    @ConfigItem(
        keyName = "defaultColor",
        name = "Default Color",
        description = "Default color for highlighted projectiles",
        section = overlaySection,
        position = 1
    )
    default Color defaultColor()
    {
        return new Color(255, 0, 0, 150);
    }

    @Range(
        min = 1,
        max = 10
    )
    @ConfigItem(
        keyName = "outlineWidth",
        name = "Outline Width",
        description = "Width of the outline for hull/outline styles",
        section = overlaySection,
        position = 2
    )
    default int outlineWidth()
    {
        return 2;
    }

	@Range(
		min = 0,
		max = 255
	)
	@ConfigItem(
		keyName = "fillOpacity",
		name = "Fill Opacity",
		description = "Opacity of the fill color (0-255)",
		section = overlaySection,
		position = 3
	)
	default int fillOpacity()
	{
		return 50;
	}

	@Range(
		min = 8,
		max = 32
	)
	@ConfigItem(
		keyName = "debugTextSize",
		name = "Debug Text Size",
		description = "Font size for the projectile ID text that appears in debug mode",
		section = overlaySection,
		position = 4
	)
	default int debugTextSize()
	{
		return 12;
	}

	@Range(
		min = 10,
		max = 80
	)
	@ConfigItem(
		keyName = "circleDiameter",
		name = "Circle Diameter",
		description = "Pixel diameter for outline and filled styles",
		section = overlaySection,
		position = 5
	)
	default int circleDiameter()
	{
		return 20;
	}

    // Overlay style enum
    enum OverlayStyle
    {
        HULL("Filled Outline"),
        OUTLINE("Outline"),
        FILLED("Filled"),
        TILE("Tile");

        private final String name;

        OverlayStyle(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }
}
