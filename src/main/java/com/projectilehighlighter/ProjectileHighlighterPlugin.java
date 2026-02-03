package com.projectilehighlighter;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.projectilehighlighter.model.ProjectileEntry;
import com.projectilehighlighter.model.ProjectileGroup;
import com.projectilehighlighter.ui.ProjectileHighlighterPanel;
import com.projectilehighlighter.util.GroupStorage;
import com.projectilehighlighter.util.ProjectileNames;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.overlay.OverlayManager;

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
    @Inject
    private Client client;

    @Inject
    private ProjectileHighlighterConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ChatMessageManager chatMessageManager;

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

        // Create navigation button with generated icon (no external file needed)
        navButton = NavigationButton.builder()
            .tooltip("Projectile Highlighter")
            .icon(createDefaultIcon())
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
     * Create a simple default icon if the resource is not found.
     */
    private BufferedImage createDefaultIcon()
    {
        BufferedImage img = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(new Color(200, 100, 50));
        g.fillOval(2, 2, 14, 14);
        g.setColor(Color.WHITE);
        g.drawOval(2, 2, 14, 14);
        g.dispose();
        return img;
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

            // Debug mode: log projectile info to chat
            if (config.debugMode())
            {
                sendDebugMessage(projectile);
            }

            // Feed to panel for recent list
            if (panel != null)
            {
                panel.addRecentProjectile(projectileId);
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
            return new TrackedProjectileInfo(config.defaultColor(), config.overlayStyle());
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
        if (actor instanceof NPC)
        {
            return ((NPC) actor).getName();
        }
        if (actor instanceof Player)
        {
            return ((Player) actor).getName();
        }
        return null;
    }

    /**
     * Send a debug message to chat with projectile information.
     */
    private void sendDebugMessage(Projectile projectile)
    {
        String name = ProjectileNames.getName(projectile.getId());
        String nameDisplay = name != null ? " (" + name + ")" : "";

        String message = new ChatMessageBuilder()
            .append(ChatColorType.HIGHLIGHT)
            .append("[Projectile] ")
            .append(ChatColorType.NORMAL)
            .append("ID: ")
            .append(ChatColorType.HIGHLIGHT)
            .append(String.valueOf(projectile.getId()))
            .append(nameDisplay)
            .append(ChatColorType.NORMAL)
            .append(" | Target: ")
            .append(ChatColorType.HIGHLIGHT)
            .append(projectile.getInteracting() != null ? projectile.getInteracting().getName() : "None")
            .build();

        chatMessageManager.queue(QueuedMessage.builder()
            .type(net.runelite.api.ChatMessageType.GAMEMESSAGE)
            .runeLiteFormattedMessage(message)
            .build());
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
