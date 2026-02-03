package com.projectilehighlighter;

import com.projectilehighlighter.util.GroupStorage;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.util.Map;

public class ProjectileHighlighterOverlay extends Overlay
{

    private final Client client;
    private final ProjectileHighlighterPlugin plugin;
    private final ProjectileHighlighterConfig config;

    @Setter
    private GroupStorage groupStorage;

    @Inject
    public ProjectileHighlighterOverlay(Client client, ProjectileHighlighterPlugin plugin, ProjectileHighlighterConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.enabled())
        {
            return null;
        }

        Map<Projectile, ProjectileHighlighterPlugin.TrackedProjectileInfo> trackedProjectiles = plugin.getTrackedProjectiles();

        for (Map.Entry<Projectile, ProjectileHighlighterPlugin.TrackedProjectileInfo> entry : trackedProjectiles.entrySet())
        {
            Projectile projectile = entry.getKey();
            ProjectileHighlighterPlugin.TrackedProjectileInfo info = entry.getValue();

            // Skip expired projectiles
            if (projectile.getRemainingCycles() <= 0)
            {
                continue;
            }

            renderProjectile(graphics, projectile, info);
        }

        return null;
    }

    private void renderProjectile(Graphics2D graphics, Projectile projectile, ProjectileHighlighterPlugin.TrackedProjectileInfo info)
    {
        int x = (int) projectile.getX();
        int y = (int) projectile.getY();
        int z = projectile.getHeight();

        LocalPoint projectilePoint = new LocalPoint(x, y);

        // Get screen point from 3D coordinates
        Point screenPoint = Perspective.localToCanvas(client, projectilePoint, projectile.getFloor(), z);

        if (screenPoint == null)
        {
            return;
        }

        Color color = info.getColor();
        ProjectileHighlighterConfig.OverlayStyle style = info.getOverlayStyle();
		int size = Math.max(10, Math.min(80, config.circleDiameter()));

        // Set up graphics
        graphics.setStroke(new BasicStroke(config.outlineWidth()));

        switch (style)
        {
            case HULL:
            case OUTLINE:
                renderOutline(graphics, screenPoint, size, color, style == ProjectileHighlighterConfig.OverlayStyle.HULL);
                break;
            case FILLED:
                renderFilled(graphics, screenPoint, size, color);
                break;
            case TILE:
                renderTile(graphics, projectile, color);
                break;
        }

        // Draw projectile ID in debug mode
        if (config.debugMode())
        {
            renderDebugText(graphics, screenPoint, projectile.getId(), size);
        }
    }

	private void renderDebugText(Graphics2D graphics, Point point, int projectileId, int size)
	{
		String idText = String.valueOf(projectileId);
		Font originalFont = graphics.getFont();
		int textSize = config.debugTextSize();
		if (textSize < 8)
		{
			textSize = 8;
		}
		else if (textSize > 32)
		{
			textSize = 32;
		}
		graphics.setFont(originalFont.deriveFont(Font.BOLD, (float) textSize));

        int textWidth = graphics.getFontMetrics().stringWidth(idText);
        int textX = point.getX() - textWidth / 2;
        int textY = point.getY() - size - 5;

        // Draw text shadow for visibility
        graphics.setColor(Color.BLACK);
        graphics.drawString(idText, textX + 1, textY + 1);
        graphics.setColor(Color.WHITE);
        graphics.drawString(idText, textX, textY);

        graphics.setFont(originalFont);
    }

    private void renderOutline(Graphics2D graphics, Point point, int size, Color color, boolean fill)
    {
        int halfSize = size / 2;
        int x = point.getX() - halfSize;
        int y = point.getY() - halfSize;

        Ellipse2D.Double ellipse = new Ellipse2D.Double(x, y, size, size);

        if (fill)
        {
            // Use the alpha from the color itself (set by the entry)
            Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.min(color.getAlpha(), config.fillOpacity()));
            graphics.setColor(fillColor);
            graphics.fill(ellipse);
        }

        // Draw outline with full opacity
        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
        graphics.draw(ellipse);
    }

    private void renderFilled(Graphics2D graphics, Point point, int size, Color color)
    {
        int halfSize = size / 2;
        int x = point.getX() - halfSize;
        int y = point.getY() - halfSize;

        Ellipse2D.Double ellipse = new Ellipse2D.Double(x, y, size, size);

        graphics.setColor(color);
        graphics.fill(ellipse);

        // Draw border with full opacity
        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
        graphics.draw(ellipse);
    }

    private void renderTile(Graphics2D graphics, Projectile projectile, Color color)
    {
        int x = (int) projectile.getX();
        int y = (int) projectile.getY();

        LocalPoint projectilePoint = new LocalPoint(x, y);

        Polygon tilePoly = Perspective.getCanvasTilePoly(client, projectilePoint);

        if (tilePoly == null)
        {
            return;
        }

        // Fill the tile
        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),
            Math.min(color.getAlpha(), config.fillOpacity()));
        graphics.setColor(fillColor);
        graphics.fill(tilePoly);

        // Draw tile outline with full opacity
        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
        graphics.draw(tilePoly);
    }
}
