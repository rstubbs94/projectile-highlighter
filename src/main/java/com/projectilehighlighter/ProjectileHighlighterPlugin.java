package com.projectilehighlighter;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.projectilehighlighter.model.ProjectileEntry;
import com.projectilehighlighter.model.ProjectileGroup;
import com.projectilehighlighter.ui.ProjectileHighlighterPanel;
import com.projectilehighlighter.util.GroupStorage;
import com.projectilehighlighter.util.ProjectileColorUtil;
import com.projectilehighlighter.util.ProjectileNames;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.overlay.OverlayManager;

import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@PluginDescriptor(
    name = "Projectile Highlighter",
    description = "Highlights projectiles with customizable colored overlays for better visibility",
    tags = {"projectile", "highlight", "overlay", "pvm", "combat", "visibility"}
)
public class ProjectileHighlighterPlugin extends Plugin
{
    private static final int WORLD_DISTANCE_BIAS_TILES = 1;
    private static final int LOCAL_DISTANCE_BIAS_UNITS = 128;

    @Inject
    private Client client;

    @Inject
    private ProjectileHighlighterConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private Gson gson;

    @Inject
    private ProjectileHighlighterOverlay overlay;

    @Inject
    private ColorPickerManager colorPickerManager;

    // Sidebar panel
    private ProjectileHighlighterPanel panel;
    private NavigationButton navButton;

    @Getter
    private GroupStorage groupStorage;

    // Active tracking from enabled groups
    @Getter
    private final Map<Projectile, TrackedProjectileInfo> trackedProjectiles = new HashMap<>();

    // Track projectiles we've already processed this instance (for debug + recent list)
    private final Set<Projectile> processedProjectiles = new HashSet<>();

    @Override
    protected void startUp()
    {
        log.info("Projectile Highlighter started");

        // Initialize projectile names utility
        ProjectileNames.initialize();

        // Initialize group storage
        groupStorage = new GroupStorage(gson);
        groupStorage.setOnGroupsChangedCallback(this::onGroupsChanged);

        // Create sidebar panel
        panel = new ProjectileHighlighterPanel(groupStorage, config, colorPickerManager);

        // Create navigation button with icon
        navButton = NavigationButton.builder()
            .tooltip("Projectile Highlighter")
            .icon(loadIcon())
            .priority(7)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
        overlayManager.add(overlay);

        // Pass group storage to overlay
        overlay.setGroupStorage(groupStorage);
    }

    @Override
    protected void shutDown()
    {
        log.info("Projectile Highlighter stopped");
        clientToolbar.removeNavigation(navButton);
        overlayManager.remove(overlay);
        trackedProjectiles.clear();
        processedProjectiles.clear();
    }

    @Provides
    ProjectileHighlighterConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ProjectileHighlighterConfig.class);
    }

    /**
     * Load the panel icon from resources, with fallback to generated icon.
     */
    private BufferedImage loadIcon()
    {
        try
        {
            return ImageUtil.loadImageResource(getClass(), "panel_icon.png");
        }
        catch (Exception e)
        {
            log.debug("Could not load panel_icon.png, using generated icon");
            BufferedImage img = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setColor(new Color(200, 100, 50));
            g.fillOval(2, 2, 14, 14);
            g.setColor(Color.WHITE);
            g.drawOval(2, 2, 14, 14);
            g.dispose();
            return img;
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event)
    {
        if (!config.enabled())
        {
            return;
        }

        Projectile projectile = event.getProjectile();
        int projectileId = projectile.getId();

        // Process each projectile instance only once (for debug messages and recent list)
        if (!processedProjectiles.contains(projectile))
        {
            processedProjectiles.add(projectile);

            // Feed to panel for recent list
            if (panel != null)
            {
                panel.addRecentProjectile(projectileId, resolveSourceName(projectile));
            }
        }

        // Check if we should track this projectile for rendering
        if (!trackedProjectiles.containsKey(projectile))
        {
            TrackedProjectileInfo info = getTrackingInfo(projectileId);
            if (info != null)
            {
                trackedProjectiles.put(projectile, info);
                log.debug("Tracking projectile ID: {} with color: {}", projectileId, info.getColor());
            }
        }
    }

    /**
     * Determine if a projectile should be tracked and get its rendering info.
     */
    private TrackedProjectileInfo getTrackingInfo(int projectileId)
    {
        // Check if highlight all is enabled
        if (config.highlightAll())
        {
            return new TrackedProjectileInfo(
                ProjectileColorUtil.getDefaultColorForProjectile(projectileId, config),
                config.overlayStyle()
            );
        }

        // Check enabled groups for this projectile
        ProjectileEntry entry = groupStorage.getEnabledEntry(projectileId);
        if (entry != null)
        {
            return new TrackedProjectileInfo(entry.getColor(), entry.getOverlayStyle());
        }

        return null;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        // Clean up expired projectiles
        trackedProjectiles.entrySet().removeIf(entry ->
            entry.getKey().getRemainingCycles() <= 0
        );

        processedProjectiles.removeIf(projectile ->
            projectile.getRemainingCycles() <= 0
        );
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals("projectilehighlighter"))
        {
            return;
        }

        // Clear tracking when highlighting is disabled
        if (event.getKey().equals("enabled") && !config.enabled())
        {
            trackedProjectiles.clear();
            processedProjectiles.clear();
        }
		else if (event.getKey().equals("highlightAll")
			|| event.getKey().equals("defaultColorMode")
			|| event.getKey().equals("defaultColor")
			|| event.getKey().equals("overlayStyle"))
		{
			refreshTrackedProjectiles();
		}

    }

    /**
     * Called when groups change in storage.
     */
    private void onGroupsChanged(List<ProjectileGroup> groups)
    {
        log.debug("Groups changed, {} groups total", groups.size());
        refreshTrackedProjectiles();
    }

	private void refreshTrackedProjectiles()
	{
		if (groupStorage == null)
		{
			return;
		}

		trackedProjectiles.entrySet().removeIf(entry -> {
			int projectileId = entry.getKey().getId();
			TrackedProjectileInfo updated = getTrackingInfo(projectileId);
			if (updated == null)
			{
				return true;
			}

			entry.setValue(updated);
			return false;
		});
	}

    /**
     * Get a display name for an actor.
     */
    private String getActorName(Actor actor)
    {
        if (actor == null)
        {
            return null;
        }

        String name = null;
        if (actor instanceof NPC)
        {
            name = ((NPC) actor).getName();
        }
        else if (actor instanceof Player)
        {
            name = ((Player) actor).getName();
        }

        return sanitizeActorName(name);
    }

    private String sanitizeActorName(String name)
    {
        if (name == null)
        {
            return null;
        }

        String cleaned = name.replaceAll("<[^>]*>", "").trim();
        if (cleaned.isEmpty() || cleaned.equalsIgnoreCase("null"))
        {
            return null;
        }

        return cleaned;
    }

    /**
     * Resolve a source name for a projectile, with a proximity fallback.
     * Priority:
     * 1) Direct source actor from RuneLite API
     * 2) Closest actor to projectile source/spawn point
     * 3) Unknown Source
     */
    private String resolveSourceName(Projectile projectile)
    {
        String sourceName = getActorName(projectile.getSourceActor());
        if (sourceName != null && !sourceName.isEmpty())
        {
            return sourceName;
        }

        Actor targetActor = projectile.getTargetActor();
        Actor fallback = null;
        WorldPoint sourcePoint = projectile.getSourcePoint();
        if (sourcePoint != null)
        {
            fallback = findClosestActorToWorldPoint(sourcePoint, targetActor);
        }

        if (fallback == null)
        {
            LocalPoint sourceLocal = new LocalPoint(projectile.getX1(), projectile.getY1());
            fallback = findClosestActorToLocalPoint(sourceLocal, targetActor);
        }

        String fallbackName = getActorName(fallback);
        if (fallbackName != null && !fallbackName.isEmpty())
        {
            return fallbackName;
        }

        return "Unknown Source";
    }

    private Actor findClosestActorToWorldPoint(WorldPoint sourcePoint, Actor targetActor)
    {
        Candidate npcCandidate = findClosestNpcToWorldPoint(sourcePoint, targetActor);
        Candidate playerCandidate = findClosestPlayerToWorldPoint(sourcePoint, targetActor);
        return chooseBestFallbackActor(npcCandidate, playerCandidate, targetActor, WORLD_DISTANCE_BIAS_TILES);
    }

    private Actor findClosestActorToLocalPoint(LocalPoint sourceLocal, Actor targetActor)
    {
        Candidate npcCandidate = findClosestNpcToLocalPoint(sourceLocal, targetActor);
        Candidate playerCandidate = findClosestPlayerToLocalPoint(sourceLocal, targetActor);
        return chooseBestFallbackActor(npcCandidate, playerCandidate, targetActor, LOCAL_DISTANCE_BIAS_UNITS);
    }

    private int getDistanceToWorldPoint(WorldPoint sourcePoint, Actor actor)
    {
        if (actor == null)
        {
            return Integer.MAX_VALUE;
        }

        String actorName = getActorName(actor);
        if (actorName == null || actorName.isEmpty())
        {
            return Integer.MAX_VALUE;
        }

        WorldPoint actorPoint = actor.getWorldLocation();
        if (actorPoint == null || actorPoint.getPlane() != sourcePoint.getPlane())
        {
            return Integer.MAX_VALUE;
        }

        return actorPoint.distanceTo2D(sourcePoint);
    }

    private int getDistanceToLocalPoint(LocalPoint sourceLocal, Actor actor)
    {
        if (actor == null)
        {
            return Integer.MAX_VALUE;
        }

        String actorName = getActorName(actor);
        if (actorName == null || actorName.isEmpty())
        {
            return Integer.MAX_VALUE;
        }

        LocalPoint actorPoint = actor.getLocalLocation();
        if (actorPoint == null)
        {
            return Integer.MAX_VALUE;
        }

        return actorPoint.distanceTo(sourceLocal);
    }

    private Candidate findClosestNpcToWorldPoint(WorldPoint sourcePoint, Actor excludedActor)
    {
        Candidate closest = null;
        for (NPC npc : client.getNpcs())
        {
            if (npc == excludedActor)
            {
                continue;
            }

            int distance = getDistanceToWorldPoint(sourcePoint, npc);
            if (distance == Integer.MAX_VALUE)
            {
                continue;
            }

            if (closest == null || distance < closest.distance)
            {
                closest = new Candidate(npc, distance);
                if (distance == 0)
                {
                    return closest;
                }
            }
        }
        return closest;
    }

    private Candidate findClosestPlayerToWorldPoint(WorldPoint sourcePoint, Actor excludedActor)
    {
        Candidate closest = null;
        for (Player player : client.getPlayers())
        {
            if (player == excludedActor)
            {
                continue;
            }

            int distance = getDistanceToWorldPoint(sourcePoint, player);
            if (distance == Integer.MAX_VALUE)
            {
                continue;
            }

            if (closest == null || distance < closest.distance)
            {
                closest = new Candidate(player, distance);
                if (distance == 0)
                {
                    return closest;
                }
            }
        }
        return closest;
    }

    private Candidate findClosestNpcToLocalPoint(LocalPoint sourcePoint, Actor excludedActor)
    {
        Candidate closest = null;
        for (NPC npc : client.getNpcs())
        {
            if (npc == excludedActor)
            {
                continue;
            }

            int distance = getDistanceToLocalPoint(sourcePoint, npc);
            if (distance == Integer.MAX_VALUE)
            {
                continue;
            }

            if (closest == null || distance < closest.distance)
            {
                closest = new Candidate(npc, distance);
                if (distance == 0)
                {
                    return closest;
                }
            }
        }
        return closest;
    }

    private Candidate findClosestPlayerToLocalPoint(LocalPoint sourcePoint, Actor excludedActor)
    {
        Candidate closest = null;
        for (Player player : client.getPlayers())
        {
            if (player == excludedActor)
            {
                continue;
            }

            int distance = getDistanceToLocalPoint(sourcePoint, player);
            if (distance == Integer.MAX_VALUE)
            {
                continue;
            }

            if (closest == null || distance < closest.distance)
            {
                closest = new Candidate(player, distance);
                if (distance == 0)
                {
                    return closest;
                }
            }
        }
        return closest;
    }

    private Actor chooseBestFallbackActor(Candidate npcCandidate, Candidate playerCandidate, Actor targetActor, int biasDistance)
    {
        Actor localPlayer = client.getLocalPlayer();

        // If the projectile is targeting us, never attribute source to us unless
        // there is no other valid candidate.
        if (targetActor != null
            && targetActor == localPlayer
            && playerCandidate != null
            && playerCandidate.actor == localPlayer)
        {
            if (npcCandidate != null)
            {
                return npcCandidate.actor;
            }
            return null;
        }

        int npcScore = scoreCandidate(npcCandidate, true, targetActor, localPlayer, biasDistance);
        int playerScore = scoreCandidate(playerCandidate, false, targetActor, localPlayer, biasDistance);

        if (npcScore == Integer.MAX_VALUE && playerScore == Integer.MAX_VALUE)
        {
            return null;
        }

        if (npcScore <= playerScore)
        {
            return npcCandidate != null ? npcCandidate.actor : null;
        }

        return playerCandidate != null ? playerCandidate.actor : null;
    }

    private int scoreCandidate(Candidate candidate, boolean npcCandidate, Actor targetActor, Actor localPlayer, int biasDistance)
    {
        if (candidate == null)
        {
            return Integer.MAX_VALUE;
        }

        int score = candidate.distance;

        // Slightly favor NPCs when distance is close.
        if (npcCandidate)
        {
            score = Math.max(0, score - biasDistance);
        }

        // Strongly de-prioritize local player as fallback source.
        if (!npcCandidate && localPlayer != null && candidate.actor == localPlayer)
        {
            score += biasDistance * 6;
            if (targetActor != null && targetActor == localPlayer)
            {
                score += biasDistance * 8;
            }
        }

        return score;
    }

    private static final class Candidate
    {
        private final Actor actor;
        private final int distance;

        private Candidate(Actor actor, int distance)
        {
            this.actor = actor;
            this.distance = distance;
        }
    }

    /**
     * Info class for tracked projectiles.
     */
    @Getter
    public static class TrackedProjectileInfo
    {
        private final Color color;
        private final ProjectileHighlighterConfig.OverlayStyle overlayStyle;

        public TrackedProjectileInfo(Color color, ProjectileHighlighterConfig.OverlayStyle overlayStyle)
        {
            this.color = color;
            this.overlayStyle = overlayStyle;
        }
    }
}
