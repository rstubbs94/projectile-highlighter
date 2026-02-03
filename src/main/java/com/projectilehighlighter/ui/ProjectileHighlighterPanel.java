package com.projectilehighlighter.ui;

import com.projectilehighlighter.ProjectileHighlighterConfig;
import com.projectilehighlighter.model.ProjectileEntry;
import com.projectilehighlighter.model.ProjectileGroup;
import com.projectilehighlighter.model.RecentProjectile;
import com.projectilehighlighter.util.GroupStorage;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Main sidebar panel for Projectile Highlighter plugin.
 * Shows groups of projectiles and recent projectile list.
 */
public class ProjectileHighlighterPanel extends PluginPanel
{
    private static final int MAX_RECENT_PROJECTILES = 10;
    private static final Color SECTION_HEADER_COLOR = new Color(100, 180, 255);
    private static final Color RECENT_HEADER_COLOR = new Color(255, 180, 100);

    private final GroupStorage groupStorage;
    private final ProjectileHighlighterConfig config;
    private final ColorPickerManager colorPickerManager;

    // Recent projectiles tracking (ordered by insertion, newest last)
    private final LinkedHashMap<Integer, RecentProjectile> recentProjectiles = new LinkedHashMap<>();
	private final Map<String, Boolean> groupExpansionState = new HashMap<>();

    // UI Components
    private final JPanel groupsContainer;
    private final JPanel recentContainer;
    private final JLabel noGroupsLabel;
    private final JLabel noRecentLabel;

    private int tempProjectileId = -1;

    public ProjectileHighlighterPanel(GroupStorage groupStorage,
                                       ProjectileHighlighterConfig config,
                                       ColorPickerManager colorPickerManager)
    {
        super(false);
        this.groupStorage = groupStorage;
        this.config = config;
        this.colorPickerManager = colorPickerManager;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // ===== HEADER =====
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Projectile Highlighter");
        titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // ===== MAIN CONTENT (scrollable) =====
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // ----- Groups Section -----
        JPanel groupsSection = new JPanel(new BorderLayout());
        groupsSection.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel groupsHeader = createSectionHeader("Groups", SECTION_HEADER_COLOR);
		JButton addGroupBtn = createPlusButton("Create a new projectile group");
		addGroupBtn.addActionListener(e -> createNewGroup());
		groupsHeader.add(addGroupBtn, BorderLayout.EAST);
        groupsSection.add(groupsHeader, BorderLayout.NORTH);

        groupsContainer = new JPanel();
        groupsContainer.setLayout(new BoxLayout(groupsContainer, BoxLayout.Y_AXIS));
        groupsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        noGroupsLabel = new JLabel("No groups created yet. Click + to add one.");
        noGroupsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        noGroupsLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
		noGroupsLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 13));

        groupsSection.add(groupsContainer, BorderLayout.CENTER);
        mainContent.add(groupsSection);

        // Spacer
        mainContent.add(Box.createVerticalStrut(15));

        // ----- Recent Projectiles Section -----
        JPanel recentSection = new JPanel(new BorderLayout());
        recentSection.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel recentHeader = createSectionHeader("Recent Projectiles", RECENT_HEADER_COLOR);
        JButton clearRecentBtn = new JButton("Clear");
        clearRecentBtn.setToolTipText("Clear recent projectiles");
        clearRecentBtn.setMargin(new Insets(1, 4, 1, 4));
		clearRecentBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        clearRecentBtn.addActionListener(e -> {
            recentProjectiles.clear();
            refreshRecentList();
        });
        recentHeader.add(clearRecentBtn, BorderLayout.EAST);
        recentSection.add(recentHeader, BorderLayout.NORTH);

        recentContainer = new JPanel();
        recentContainer.setLayout(new BoxLayout(recentContainer, BoxLayout.Y_AXIS));
        recentContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        noRecentLabel = new JLabel("No projectiles seen yet");
        noRecentLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        noRecentLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
		noRecentLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 13));

        recentSection.add(recentContainer, BorderLayout.CENTER);
        mainContent.add(recentSection);

        // Add padding at bottom
        mainContent.add(Box.createVerticalStrut(20));

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        // Initial refresh
        refreshGroupsList();
        refreshRecentList();
    }

    private JPanel createSectionHeader(String text, Color color)
    {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setBorder(new EmptyBorder(6, 10, 6, 10));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel label = new JLabel(text);
        label.setForeground(color);
		label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        header.add(label, BorderLayout.WEST);

        return header;
    }

    private void createNewGroup()
    {
        String name = JOptionPane.showInputDialog(
            this,
            "Enter group name:",
            "Create Group",
            JOptionPane.PLAIN_MESSAGE
        );

        if (name != null && !name.trim().isEmpty())
        {
            ProjectileGroup group = ProjectileGroup.builder()
                .name(name.trim())
                .enabled(true)
                .build();
            groupStorage.addGroup(group);
            refreshGroupsList();
        }
    }

    public void refreshGroupsList()
    {
        groupsContainer.removeAll();

        List<ProjectileGroup> groups = groupStorage.getGroups();

        if (groups.isEmpty())
        {
            groupsContainer.add(noGroupsLabel);
        }
        else
        {
			int rowIndex = 0;
			for (ProjectileGroup group : groups)
			{
				boolean expanded = groupExpansionState.getOrDefault(group.getId(), Boolean.FALSE);
				GroupPanel groupPanel = new GroupPanel(
					group,
					this::toggleGroupEnabled,
					this::renameGroup,
					this::deleteGroup,
					this::addProjectileToGroup,
					() -> groupStorage.updateGroup(group),
					colorPickerManager,
					rowIndex % 2 == 1,
					expanded,
					isExpanded -> groupExpansionState.put(group.getId(), isExpanded)
				);
				groupsContainer.add(groupPanel);
				rowIndex++;
			}
		}

        groupsContainer.revalidate();
        groupsContainer.repaint();
    }

    private void toggleGroupEnabled(ProjectileGroup group)
    {
        groupStorage.toggleGroupEnabled(group);
        refreshGroupsList();
    }

    private void renameGroup(ProjectileGroup group)
    {
        String newName = JOptionPane.showInputDialog(
            this,
            "Enter new name:",
            group.getName()
        );

        if (newName != null && !newName.trim().isEmpty())
        {
            groupStorage.renameGroup(group, newName.trim());
            refreshGroupsList();
        }
    }

	private void deleteGroup(ProjectileGroup group)
	{
        int result = JOptionPane.showConfirmDialog(
            this,
            "Delete group '" + group.getName() + "'?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION)
        {
            groupStorage.deleteGroup(group);
			groupExpansionState.remove(group.getId());
            refreshGroupsList();
		}
	}

	private void addProjectileToGroup(ProjectileGroup group)
	{
		ProjectileEntry entry = ProjectileEntry.createDefault(
			generateTempProjectileId(),
			config.defaultColor(),
			config.overlayStyle()
		);
		group.addEntry(entry);
		refreshGroupsList();
	}

    /**
     * Add a projectile to the recent list (called from plugin on ProjectileMoved).
     * All modifications to recentProjectiles must happen on the EDT to avoid
     * ConcurrentModificationException when the UI is also accessing the map.
     */
    public void addRecentProjectile(int projectileId)
    {
        // Move all map modifications to the EDT to prevent concurrent access issues
        SwingUtilities.invokeLater(() -> {
            // Update existing or add new
            RecentProjectile recent = RecentProjectile.builder()
                .projectileId(projectileId)
                .timestamp(System.currentTimeMillis())
                .build();

            // Remove if exists (to move to end/refresh timestamp)
            recentProjectiles.remove(projectileId);
            recentProjectiles.put(projectileId, recent);

            // Prune to MAX_RECENT
            while (recentProjectiles.size() > MAX_RECENT_PROJECTILES)
            {
                Integer oldestKey = recentProjectiles.keySet().iterator().next();
                recentProjectiles.remove(oldestKey);
            }

            refreshRecentList();
        });
    }

    private void refreshRecentList()
    {
        recentContainer.removeAll();

        if (recentProjectiles.isEmpty())
        {
            recentContainer.add(noRecentLabel);
        }
        else
        {
            // Show newest first
            List<RecentProjectile> reversed = new ArrayList<>(recentProjectiles.values());
            Collections.reverse(reversed);

            int rowIndex = 0;
            for (RecentProjectile recent : reversed)
            {
                RecentProjectilePanel panel = new RecentProjectilePanel(
                    recent,
                    rowIndex,
                    this::showAddToGroupDialog
                );
                recentContainer.add(panel);
                rowIndex++;
            }
        }

        recentContainer.revalidate();
        recentContainer.repaint();
    }

    private void showAddToGroupDialog(RecentProjectile projectile)
    {
        List<ProjectileGroup> groups = groupStorage.getGroups();

        if (groups.isEmpty())
        {
            int result = JOptionPane.showConfirmDialog(
                this,
                "No groups exist. Create a new group for this projectile?",
                "No Groups",
                JOptionPane.YES_NO_OPTION
            );

            if (result == JOptionPane.YES_OPTION)
            {
                String name = JOptionPane.showInputDialog(
                    this,
                    "Enter group name:",
                    "Create Group",
                    JOptionPane.PLAIN_MESSAGE
                );

                if (name != null && !name.trim().isEmpty())
                {
                    ProjectileEntry entry = createEntryFromRecent(projectile);
                    ProjectileGroup group = ProjectileGroup.builder()
                        .name(name.trim())
                        .enabled(true)
                        .entries(new ArrayList<>(Collections.singletonList(entry)))
                        .build();
                    groupStorage.addGroup(group);
                    refreshGroupsList();
                }
            }
            return;
        }

        // Show group selection dialog
        String[] groupNames = groups.stream()
            .map(ProjectileGroup::getName)
            .toArray(String[]::new);

        String selected = (String) JOptionPane.showInputDialog(
            this,
            "Add projectile ID " + projectile.getProjectileId() + " to:",
            "Add to Group",
            JOptionPane.QUESTION_MESSAGE,
            null,
            groupNames,
            groupNames[0]
        );

        if (selected != null)
		{
			for (ProjectileGroup group : groups)
			{
				if (group.getName().equals(selected))
				{
                    // Check if already in group
                    if (group.findEntryById(projectile.getProjectileId()) != null)
                    {
                        JOptionPane.showMessageDialog(
                            this,
                            "Projectile ID " + projectile.getProjectileId() +
                            " is already in group '" + group.getName() + "'",
                            "Already Added",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        return;
                    }

                    ProjectileEntry entry = createEntryFromRecent(projectile);
                    group.addEntry(entry);
                    groupStorage.updateGroup(group);
                    refreshGroupsList();
                    break;
                }
			}
		}
	}

	private static final Icon PLUS_ICON = createPlusIcon();

	private static Icon createPlusIcon()
	{
		int size = 22;
		java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = image.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new java.awt.BasicStroke(3.5f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
		g.setColor(new Color(90, 200, 90));
		int mid = size / 2;
		int inset = 5;
		g.drawLine(mid, inset, mid, size - inset);
		g.drawLine(inset, mid, size - inset, mid);
		g.dispose();
		return new ImageIcon(image);
	}

	private JButton createPlusButton(String tooltip)
	{
		JButton button = new JButton(PLUS_ICON);
		button.setToolTipText(tooltip);
		button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		button.setFocusPainted(false);
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return button;
	}

	private ProjectileEntry createEntryFromRecent(RecentProjectile projectile)
	{
		return ProjectileEntry.createDefault(
			projectile.getProjectileId(),
			config.defaultColor(),
			config.overlayStyle()
		);
	}

	private int generateTempProjectileId()
	{
		return tempProjectileId--;
	}
}
