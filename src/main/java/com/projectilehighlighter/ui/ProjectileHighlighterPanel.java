package com.projectilehighlighter.ui;

import com.projectilehighlighter.ProjectileHighlighterConfig;
import com.projectilehighlighter.model.ProjectileEntry;
import com.projectilehighlighter.model.ProjectileGroup;
import com.projectilehighlighter.model.RecentProjectile;
import com.projectilehighlighter.util.GroupStorage;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
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

    private static final Icon IMPORT_ICON;
    private static final Icon EXPORT_ICON;

    static
    {
        BufferedImage importImg = ImageUtil.loadImageResource(ProjectileHighlighterPanel.class, "import_icon.png");
        BufferedImage exportImg = ImageUtil.loadImageResource(ProjectileHighlighterPanel.class, "export_icon.png");

        BufferedImage importRecolored = recolorImage(importImg, new Color(150, 180, 220));
        BufferedImage exportRecolored = recolorImage(exportImg, new Color(150, 180, 220));

        IMPORT_ICON = new ImageIcon(importRecolored);
        EXPORT_ICON = new ImageIcon(exportRecolored);
    }

    /**
     * Recolors all non-transparent pixels in an image to the specified color,
     * preserving the alpha channel.
     */
    private static BufferedImage recolorImage(BufferedImage source, Color targetColor)
    {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int targetRgb = targetColor.getRGB() & 0x00FFFFFF;

        for (int y = 0; y < source.getHeight(); y++)
        {
            for (int x = 0; x < source.getWidth(); x++)
            {
                int pixel = source.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                if (alpha > 0)
                {
                    result.setRGB(x, y, (alpha << 24) | targetRgb);
                }
            }
        }
        return result;
    }

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
        headerPanel.setBorder(new EmptyBorder(6, 8, 6, 8));

        JLabel titleLabel = new JLabel("Projectile Highlighter");
        titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // ===== MAIN CONTENT (scrollable) =====
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // ----- Groups Section -----
        JPanel groupsSection = new JPanel(new BorderLayout());
        groupsSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
		groupsSection.setAlignmentX(Component.LEFT_ALIGNMENT);
		groupsSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel groupsHeader = createSectionHeader("Groups", SECTION_HEADER_COLOR);

		// Header buttons panel (right side)
		JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		headerButtons.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerButtons.setOpaque(false);

		JButton importBtn = createIconButton(IMPORT_ICON, "Import groups from clipboard");
		importBtn.addActionListener(e -> importGroups());
		headerButtons.add(importBtn);

		JButton exportBtn = createIconButton(EXPORT_ICON, "Export all groups to clipboard");
		exportBtn.addActionListener(e -> exportGroups());
		headerButtons.add(exportBtn);

		JButton addGroupBtn = createPlusButton("Create a new projectile group");
		addGroupBtn.addActionListener(e -> createNewGroup());
		headerButtons.add(addGroupBtn);

		groupsHeader.add(headerButtons, BorderLayout.EAST);
        groupsSection.add(groupsHeader, BorderLayout.NORTH);

        groupsContainer = new JPanel();
        groupsContainer.setLayout(new BoxLayout(groupsContainer, BoxLayout.Y_AXIS));
        groupsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		groupsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        noGroupsLabel = new JLabel("No groups yet. Click + to add.");
        noGroupsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        noGroupsLabel.setBorder(new EmptyBorder(8, 10, 8, 10));
		noGroupsLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));

        groupsSection.add(groupsContainer, BorderLayout.CENTER);
        mainContent.add(groupsSection);

        // Spacer
        mainContent.add(Box.createVerticalStrut(8));

        // ----- Recent Projectiles Section -----
        JPanel recentSection = new JPanel(new BorderLayout());
        recentSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
		recentSection.setAlignmentX(Component.LEFT_ALIGNMENT);
		recentSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel recentHeader = createSectionHeader("Recent Projectiles", RECENT_HEADER_COLOR);
        JButton clearRecentBtn = new JButton("Clear");
        clearRecentBtn.setToolTipText("Clear recent projectiles");
        clearRecentBtn.setMargin(new Insets(1, 4, 1, 4));
		clearRecentBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        clearRecentBtn.addActionListener(e -> {
            recentProjectiles.clear();
            refreshRecentList();
        });
        recentHeader.add(clearRecentBtn, BorderLayout.EAST);
        recentSection.add(recentHeader, BorderLayout.NORTH);

        recentContainer = new JPanel();
        recentContainer.setLayout(new BoxLayout(recentContainer, BoxLayout.Y_AXIS));
        recentContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		recentContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        noRecentLabel = new JLabel("No projectiles seen yet");
        noRecentLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        noRecentLabel.setBorder(new EmptyBorder(8, 10, 8, 10));
		noRecentLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));

        recentSection.add(recentContainer, BorderLayout.CENTER);
        mainContent.add(recentSection);

        // Add padding at bottom
        mainContent.add(Box.createVerticalStrut(10));

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
        header.setBorder(new EmptyBorder(5, 8, 5, 8));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel label = new JLabel(text);
        label.setForeground(color);
		label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
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
					this::exportGroup,
					() -> groupStorage.updateGroup(group),
					colorPickerManager,
					rowIndex % 2 == 1,
					expanded,
					isExpanded -> groupExpansionState.put(group.getId(), isExpanded)
				);
				groupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
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

	private void exportGroup(ProjectileGroup group)
	{
		String json = groupStorage.exportGroupToJson(group);
		StringSelection selection = new StringSelection(json);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, null);

		JOptionPane.showMessageDialog(
			this,
			"Exported group '" + group.getName() + "' to clipboard.",
			"Export Successful",
			JOptionPane.INFORMATION_MESSAGE
		);
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
		int size = 18;
		java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = image.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new java.awt.BasicStroke(2.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
		g.setColor(new Color(90, 200, 90));
		int mid = size / 2;
		int inset = 4;
		g.drawLine(mid, inset, mid, size - inset);
		g.drawLine(inset, mid, size - inset, mid);
		g.dispose();
		return new ImageIcon(image);
	}

	private JButton createPlusButton(String tooltip)
	{
		JButton button = new JButton(PLUS_ICON);
		button.setToolTipText(tooltip);
		button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		button.setFocusPainted(false);
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return button;
	}

	private JButton createIconButton(Icon icon, String tooltip)
	{
		JButton button = new JButton(icon);
		button.setToolTipText(tooltip);
		button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		button.setFocusPainted(false);
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return button;
	}

	private JButton createSmallTextButton(String text, String tooltip)
	{
		JButton button = new JButton(text);
		button.setToolTipText(tooltip);
		button.setMargin(new Insets(1, 4, 1, 4));
		button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		button.setFocusPainted(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return button;
	}

	private void exportGroups()
	{
		List<ProjectileGroup> groups = groupStorage.getGroups();
		if (groups.isEmpty())
		{
			JOptionPane.showMessageDialog(
				this,
				"No groups to export.",
				"Export",
				JOptionPane.INFORMATION_MESSAGE
			);
			return;
		}

		String json = groupStorage.exportToJson();
		StringSelection selection = new StringSelection(json);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, null);

		JOptionPane.showMessageDialog(
			this,
			"Exported " + groups.size() + " group(s) to clipboard.",
			"Export Successful",
			JOptionPane.INFORMATION_MESSAGE
		);
	}

	private void importGroups()
	{
		// Read from clipboard
		String clipboardText;
		try
		{
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboardText = (String) clipboard.getData(DataFlavor.stringFlavor);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(
				this,
				"Could not read from clipboard.",
				"Import Error",
				JOptionPane.ERROR_MESSAGE
			);
			return;
		}

		if (clipboardText == null || clipboardText.trim().isEmpty())
		{
			JOptionPane.showMessageDialog(
				this,
				"Clipboard is empty.",
				"Import Error",
				JOptionPane.ERROR_MESSAGE
			);
			return;
		}

		// Ask user whether to merge or replace
		Object[] options = {"Merge (keep existing)", "Replace all", "Cancel"};
		int choice = JOptionPane.showOptionDialog(
			this,
			"How would you like to import the groups?",
			"Import Groups",
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			options,
			options[0]
		);

		if (choice == 2 || choice == JOptionPane.CLOSED_OPTION)
		{
			return; // Cancelled
		}

		boolean replaceExisting = (choice == 1);

		try
		{
			String result = groupStorage.importFromJson(clipboardText, replaceExisting);
			refreshGroupsList();
			JOptionPane.showMessageDialog(
				this,
				result,
				"Import Successful",
				JOptionPane.INFORMATION_MESSAGE
			);
		}
		catch (IllegalArgumentException e)
		{
			JOptionPane.showMessageDialog(
				this,
				e.getMessage(),
				"Import Error",
				JOptionPane.ERROR_MESSAGE
			);
		}
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
