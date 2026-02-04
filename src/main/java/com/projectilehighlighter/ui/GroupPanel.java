package com.projectilehighlighter.ui;

import com.projectilehighlighter.ProjectileHighlighterConfig.OverlayStyle;
import com.projectilehighlighter.model.ProjectileEntry;
import com.projectilehighlighter.model.ProjectileGroup;
import com.projectilehighlighter.util.ProjectileNames;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.Consumer;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Expandable panel displaying a projectile group with its entries.
 * Uses vertical layout to fit within RuneLite's limited sidebar width.
 */
public class GroupPanel extends JPanel
{
	private static final Color ENABLED_COLOR = new Color(80, 200, 80);
	private static final Color DISABLED_COLOR = new Color(210, 80, 80);
	private static final Color ACTION_ICON_COLOR = new Color(200, 200, 200);
	private static final Icon EYE_OPEN_ICON;
	private static final Icon EYE_OPEN_ICON_HOVER;
	private static final Icon EYE_CLOSED_ICON;
	private static final Icon EYE_CLOSED_ICON_HOVER;
	private static final Icon EDIT_ICON;
	private static final Icon SAVE_ICON;
	private static final Icon PLUS_ICON = createPlusIcon();
	private static final Icon MINUS_ICON = createMinusIcon();
	private static final Icon EXPORT_ICON;
	// Overlay style icons
	private static final BufferedImage OUTLINE_IMAGE;
	private static final BufferedImage SHADED_IMAGE;
	private static final BufferedImage SOLID_IMAGE;
	private static final BufferedImage TILE_IMAGE;
	private static final Icon OUTLINE_ICON;
	private static final Icon SHADED_ICON;
	private static final Icon SOLID_ICON;
	private static final Icon TILE_ICON;
	private static final Color STYLE_ICON_COLOR = new Color(150, 150, 150);
private static final Color PANEL_BG = new Color(28, 28, 28);
private static final Color PANEL_BG_ALT = new Color(34, 34, 34);
private static final Color HEADER_BG = new Color(40, 40, 40);
private static final Color HEADER_BG_ALT = new Color(48, 48, 48);
	private static final Color ENTRY_BG = new Color(50, 50, 50);
	private static final Color ENTRY_BG_ALT = new Color(60, 60, 60);

    private final ProjectileGroup group;
    private final JPanel entriesPanel;
    private JLabel expandLabel;
    private JLabel countLabel;
    private boolean expanded;
	private final Color headerBackground;
	private final Color panelBackground;
	private final Consumer<Boolean> onExpansionChanged;

    private final Consumer<ProjectileGroup> onToggleEnabled;
    private final Consumer<ProjectileGroup> onRename;
    private final Consumer<ProjectileGroup> onDelete;
    private final Consumer<ProjectileGroup> onAddEntry;
    private final Consumer<ProjectileGroup> onExport;
    private final Runnable onGroupChanged;
    private final ColorPickerManager colorPickerManager;

    public GroupPanel(ProjectileGroup group,
                      Consumer<ProjectileGroup> onToggleEnabled,
                      Consumer<ProjectileGroup> onRename,
                      Consumer<ProjectileGroup> onDelete,
                      Consumer<ProjectileGroup> onAddEntry,
                      Consumer<ProjectileGroup> onExport,
                      Runnable onGroupChanged,
                      ColorPickerManager colorPickerManager,
					  boolean alternateRowColor,
					  boolean initiallyExpanded,
					  Consumer<Boolean> onExpansionChanged)
    {
        this.group = group;
        this.onToggleEnabled = onToggleEnabled;
        this.onRename = onRename;
        this.onDelete = onDelete;
        this.onAddEntry = onAddEntry;
        this.onExport = onExport;
        this.onGroupChanged = onGroupChanged;
        this.colorPickerManager = colorPickerManager;
		this.panelBackground = alternateRowColor ? PANEL_BG_ALT : PANEL_BG;
		this.headerBackground = alternateRowColor ? HEADER_BG_ALT : HEADER_BG;
		this.expanded = initiallyExpanded;
		this.onExpansionChanged = onExpansionChanged;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(panelBackground);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR));
		setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header (two rows stacked vertically)
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel);

        // Entries (expandable)
        entriesPanel = new JPanel();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        entriesPanel.setBackground(panelBackground);
		entriesPanel.setBorder(new EmptyBorder(0, 8, 0, 0));
		entriesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        entriesPanel.setVisible(expanded);
        add(entriesPanel);

		if (expanded)
		{
			buildEntriesPanel();
		}
    }

    @Override
    public Dimension getMaximumSize()
    {
        // Allow full width but constrain height to preferred
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    private JPanel createHeaderPanel()
    {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBackground(headerBackground);
        header.setBorder(new EmptyBorder(4, 6, 4, 6));
		header.setAlignmentX(Component.LEFT_ALIGNMENT);

		// Clickable left side panel (for expand/collapse)
		JPanel leftSide = new JPanel();
		leftSide.setLayout(new BoxLayout(leftSide, BoxLayout.X_AXIS));
		leftSide.setBackground(headerBackground);
		leftSide.setOpaque(false);
		leftSide.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Single row: Expand arrow + status + name + count + buttons
		expandLabel = new JLabel(expanded ? "▼" : "▶");
		expandLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		expandLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		leftSide.add(expandLabel);
		leftSide.add(Box.createHorizontalStrut(3));

		JLabel statusDot = new JLabel("●");
		statusDot.setForeground(group.isEnabled() ? ENABLED_COLOR : DISABLED_COLOR);
		statusDot.setToolTipText(group.isEnabled() ? "Enabled" : "Disabled");
		statusDot.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        leftSide.add(statusDot);
        leftSide.add(Box.createHorizontalStrut(3));

		JLabel nameLabel = new JLabel(group.getName());
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        leftSide.add(nameLabel);
        leftSide.add(Box.createHorizontalStrut(3));

		countLabel = new JLabel("(" + group.getEntryCount() + ")");
		countLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		countLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        leftSide.add(countLabel);

		// Add click listener to the entire left side panel
		leftSide.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				toggleExpanded();
			}
		});

		header.add(leftSide);
		header.add(Box.createHorizontalGlue());

		// Buttons on the right
		JButton toggleBtn = new JButton();
		updateToggleButtonIcon(toggleBtn, false);
		toggleBtn.setMargin(new Insets(1, 1, 1, 1));
		toggleBtn.setFocusPainted(false);
		toggleBtn.setContentAreaFilled(false);
		toggleBtn.setOpaque(false);
		toggleBtn.setBorder(BorderFactory.createEmptyBorder());
		toggleBtn.addActionListener(e -> {
			onToggleEnabled.accept(group);
			updateToggleButtonIcon(toggleBtn, false);
		});
		toggleBtn.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				updateToggleButtonIcon(toggleBtn, true);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				updateToggleButtonIcon(toggleBtn, false);
			}
		});
		header.add(toggleBtn);
		header.add(Box.createHorizontalStrut(2));

		JButton renameBtn = createIconButton(EDIT_ICON, "Rename group");
		renameBtn.addActionListener(e -> onRename.accept(group));
		header.add(renameBtn);
		header.add(Box.createHorizontalStrut(2));

		JButton exportBtn = createIconButton(EXPORT_ICON, "Export group to clipboard");
		exportBtn.addActionListener(e -> onExport.accept(group));
		header.add(exportBtn);
		header.add(Box.createHorizontalStrut(2));

		JButton deleteBtn = createIconButton(MINUS_ICON, "Delete group");
		deleteBtn.addActionListener(e -> onDelete.accept(group));
		header.add(deleteBtn);

        return header;
    }

	private void toggleExpanded()
	{
        expanded = !expanded;
        expandLabel.setText(expanded ? "▼" : "▶");
        if (expanded)
        {
            buildEntriesPanel();
        }
        entriesPanel.setVisible(expanded);
        revalidate();
        repaint();
		if (onExpansionChanged != null)
		{
			onExpansionChanged.accept(expanded);
		}
    }

	private void buildEntriesPanel()
	{
        entriesPanel.removeAll();
		int rowIndex = 0;

        if (group.getEntries() == null || group.getEntries().isEmpty())
        {
            JLabel emptyLabel = new JLabel("No projectiles in this group");
            emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            emptyLabel.setBorder(new EmptyBorder(6, 10, 6, 6));
            emptyLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            entriesPanel.add(emptyLabel);
        }
        else
        {
            for (ProjectileEntry entry : group.getEntries())
            {
                JPanel entryPanel = createEntryPanel(entry, rowIndex++);
                entriesPanel.add(entryPanel);
            }
        }

		Color addRowBackground = (rowIndex % 2 == 0) ? ENTRY_BG : ENTRY_BG_ALT;
		JPanel addButtonRow = new JPanel();
		addButtonRow.setLayout(new BoxLayout(addButtonRow, BoxLayout.X_AXIS));
		addButtonRow.setBackground(addRowBackground);
		addButtonRow.setBorder(new EmptyBorder(4, 10, 6, 6));
		addButtonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton addEntryBtn = createIconButton(PLUS_ICON, "Add projectile to this group");
		addEntryBtn.addActionListener(e -> onAddEntry.accept(group));
		addButtonRow.add(addEntryBtn);
		addButtonRow.add(Box.createHorizontalGlue());

		entriesPanel.add(addButtonRow);
		updateCountLabel();
        entriesPanel.revalidate();
        entriesPanel.repaint();
    }

	private void updateCountLabel()
	{
		if (countLabel != null)
		{
			countLabel.setText("(" + group.getEntryCount() + ")");
		}
	}

	static
	{
		// Icon attribution (Flaticon):
		// edit_icon.png by Pixel perfect – https://www.flaticon.com/free-icon/edit_1827933
		// save_icon.png by Freepik – https://www.flaticon.com/free-icon/diskette_2874091
		// export_icon.png by Dewi Sari – https://www.flaticon.com/free-icon/export_8828334
		// tile_icon.png by Freepik – https://www.flaticon.com/free-icon/square-hand-drawn-shape-outline_35472
		BufferedImage visible = ImageUtil.loadImageResource(GroupPanel.class, "visible_icon.png");
		BufferedImage invisible = ImageUtil.loadImageResource(GroupPanel.class, "invisible_icon.png");
		BufferedImage edit = ImageUtil.loadImageResource(GroupPanel.class, "edit_icon.png");
		BufferedImage save = ImageUtil.loadImageResource(GroupPanel.class, "save_icon.png");
		BufferedImage export = ImageUtil.loadImageResource(GroupPanel.class, "export_icon.png");

		BufferedImage editRecolored = recolorImage(edit, ACTION_ICON_COLOR);
		BufferedImage saveRecolored = recolorImage(save, ACTION_ICON_COLOR);
		BufferedImage exportRecolored = recolorImage(export, ACTION_ICON_COLOR);
		BufferedImage visibleRecolored = recolorImage(visible, ACTION_ICON_COLOR);
		BufferedImage invisibleRecolored = recolorImage(invisible, ACTION_ICON_COLOR);

		EYE_OPEN_ICON = new ImageIcon(visibleRecolored);
		EYE_OPEN_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(visibleRecolored, 0.5f));
		EYE_CLOSED_ICON = new ImageIcon(invisibleRecolored);
		EYE_CLOSED_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(invisibleRecolored, 0.5f));
		EDIT_ICON = new ImageIcon(editRecolored);
		SAVE_ICON = new ImageIcon(saveRecolored);
		EXPORT_ICON = new ImageIcon(exportRecolored);

		// Load overlay style icons
		OUTLINE_IMAGE = ImageUtil.loadImageResource(GroupPanel.class, "outline_icon.png");
		SHADED_IMAGE = ImageUtil.loadImageResource(GroupPanel.class, "shaded_icon.png");
		SOLID_IMAGE = ImageUtil.loadImageResource(GroupPanel.class, "solid_icon.png");
		TILE_IMAGE = ImageUtil.loadImageResource(GroupPanel.class, "tile_icon.png");

		OUTLINE_ICON = new ImageIcon(tintImageWithColor(OUTLINE_IMAGE, STYLE_ICON_COLOR));
		SHADED_ICON = new ImageIcon(tintImageWithColor(SHADED_IMAGE, STYLE_ICON_COLOR));
		SOLID_ICON = new ImageIcon(tintImageWithColor(SOLID_IMAGE, STYLE_ICON_COLOR));
		TILE_ICON = new ImageIcon(tintImageWithColor(TILE_IMAGE, STYLE_ICON_COLOR));
	}

	/**
	 * Recolors all non-transparent pixels in an image to the specified color,
	 * preserving the alpha channel.
	 */
	private static BufferedImage recolorImage(BufferedImage source, Color targetColor)
	{
		BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		int rgb = targetColor.getRGB() & 0x00FFFFFF; // Get RGB without alpha
		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				int pixel = source.getRGB(x, y);
				int alpha = (pixel >> 24) & 0xFF;
				if (alpha > 0)
				{
					result.setRGB(x, y, (alpha << 24) | rgb);
				}
			}
		}
		return result;
	}

	private static BufferedImage tintImageWithColor(BufferedImage source, Color tintColor)
	{
		return tintImageWithColor(source, tintColor, 0f);
	}

	private static BufferedImage tintImageWithColor(BufferedImage source, Color tintColor, float contrastBoost)
	{
		BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		float baseR = tintColor.getRed();
		float baseG = tintColor.getGreen();
		float baseB = tintColor.getBlue();

		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				int pixel = source.getRGB(x, y);
				int alpha = (pixel >> 24) & 0xFF;
				if (alpha == 0)
				{
					continue;
				}

				int r = (pixel >> 16) & 0xFF;
				int g = (pixel >> 8) & 0xFF;
				int b = pixel & 0xFF;
				float intensity = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f; // perceptual luminance 0..1

				float factor = 1f + (intensity - 0.5f) * (0.8f + contrastBoost);
				factor = Math.max(0.3f, Math.min(1.5f, factor));

				int newR = clampColor(Math.round(baseR * factor));
				int newG = clampColor(Math.round(baseG * factor));
				int newB = clampColor(Math.round(baseB * factor));

				result.setRGB(x, y, (alpha << 24) | (newR << 16) | (newG << 8) | newB);
			}
		}

		return result;
	}

	private static int clampColor(int value)
	{
		return Math.min(255, Math.max(0, value));
	}

	private JPanel createEntryPanel(ProjectileEntry entry, int rowIndex)
	{
		Color rowBackground = (rowIndex % 2 == 0) ? ENTRY_BG : ENTRY_BG_ALT;
		final boolean[] editing = {entry.getProjectileId() < 0};
		final JButton[] styleButtons = new JButton[4];

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(rowBackground);
		panel.setBorder(new EmptyBorder(4, 8, 4, 6));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 1: Color swatch + ID + Name
        JPanel row1 = new JPanel();
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS));
        row1.setBackground(rowBackground);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel colorSwatch = new JPanel();
		colorSwatch.setPreferredSize(new Dimension(14, 14));
        colorSwatch.setMaximumSize(new Dimension(14, 14));
        colorSwatch.setMinimumSize(new Dimension(14, 14));
        colorSwatch.setBackground(entry.getColor());
        colorSwatch.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 1));
        colorSwatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        colorSwatch.setToolTipText("Click to change color");
		colorSwatch.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (!editing[0])
				{
					return;
				}
				openColorPicker(entry, colorSwatch, styleButtons, editing);
			}
		});
        row1.add(colorSwatch);
        row1.add(Box.createHorizontalStrut(6));

		String initialIdText = entry.getProjectileId() < 0 ? "" : String.valueOf(entry.getProjectileId());
		JTextField idField = new JTextField(initialIdText, 5);
		idField.setPreferredSize(new Dimension(50, 20));
		idField.setMaximumSize(new Dimension(50, 20));
		idField.setMinimumSize(new Dimension(50, 20));
		idField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		idField.setForeground(Color.WHITE);
		idField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		idField.setEditable(false);
		idField.setFocusable(false);
		idField.addActionListener(e -> {
			if (idField.isEditable())
			{
				applyIdChange(entry, idField);
			}
		});
		idField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				if (idField.isEditable())
				{
					applyIdChange(entry, idField);
				}
			}
		});
		row1.add(idField);
		row1.add(Box.createHorizontalStrut(6));

		JTextField nameField = new JTextField(entry.getCustomName() != null ? entry.getCustomName() : "");
		nameField.setPreferredSize(new Dimension(80, 20));
		nameField.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
		nameField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		nameField.setForeground(Color.WHITE);
		nameField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		nameField.setEditable(false);
		nameField.setFocusable(false);
		nameField.addActionListener(e -> {
			if (nameField.isEditable())
			{
				applyNameChange(entry, nameField);
			}
		});
		nameField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				if (nameField.isEditable())
				{
					applyNameChange(entry, nameField);
				}
			}
		});
		row1.add(nameField);

        panel.add(row1);
        panel.add(Box.createVerticalStrut(4));

        // Row 2: Style icon buttons + action buttons (right-aligned)
        JPanel row2 = new JPanel();
        row2.setLayout(new BoxLayout(row2, BoxLayout.X_AXIS));
        row2.setBackground(rowBackground);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);

        OverlayStyle initialStyle = entry.getOverlayStyle();
        if (initialStyle == null)
        {
            initialStyle = OverlayStyle.HULL;
            entry.setOverlayStyle(initialStyle);
        }

        // Style icon button group
        JPanel styleButtonGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        styleButtonGroup.setBackground(rowBackground);
        styleButtonGroup.setOpaque(false);

        // Order: Outline | Filled Outline | Filled | Tile
        styleButtons[0] = createStyleButton(OverlayStyle.OUTLINE, OUTLINE_IMAGE, OUTLINE_ICON, "Outline", entry, editing, styleButtons);
        styleButtons[1] = createStyleButton(OverlayStyle.HULL, SHADED_IMAGE, SHADED_ICON, "Filled Outline", entry, editing, styleButtons);
        styleButtons[2] = createStyleButton(OverlayStyle.FILLED, SOLID_IMAGE, SOLID_ICON, "Filled", entry, editing, styleButtons);
        styleButtons[3] = createStyleButton(OverlayStyle.TILE, TILE_IMAGE, TILE_ICON, "Tile", entry, editing, styleButtons);

        for (JButton btn : styleButtons)
        {
            styleButtonGroup.add(btn);
        }

        row2.add(styleButtonGroup);
        row2.add(Box.createHorizontalGlue());

		JButton editButton = createIconButton(EDIT_ICON, "Edit projectile");
		editButton.addActionListener(e -> {
			if (!editing[0])
			{
				editing[0] = true;
				setInlineEditingState(true, idField, nameField, colorSwatch, editButton, styleButtons, entry);
				idField.requestFocusInWindow();
				idField.selectAll();
			}
			else
			{
				applyIdChange(entry, idField);
				applyNameChange(entry, nameField);
				editing[0] = false;
				setInlineEditingState(false, idField, nameField, colorSwatch, editButton, styleButtons, entry);

				if (entry.getProjectileId() < 0)
				{
					group.removeEntry(entry);
					buildEntriesPanel();
					return;
				}
			}
		});
        row2.add(editButton);
        row2.add(Box.createHorizontalStrut(4));

		setInlineEditingState(editing[0], idField, nameField, colorSwatch, editButton, styleButtons, entry);
		if (editing[0])
		{
			idField.requestFocusInWindow();
			idField.selectAll();
		}

		JButton removeBtn = createIconButton(MINUS_ICON, "Remove from group");
		removeBtn.addActionListener(e -> {
			group.removeEntry(entry);
			onGroupChanged.run();
			buildEntriesPanel();
		});
        row2.add(removeBtn);

        panel.add(row2);

        return panel;
	}

	private void setInlineEditingState(boolean editing, JTextField idField, JTextField nameField, JPanel colorSwatch, JButton toggleButton, JButton[] styleButtons, ProjectileEntry entry)
	{
		Color enabledBg = new Color(55, 55, 55);
		Color disabledBg = new Color(25, 25, 25);
		Color disabledFg = new Color(180, 180, 180);
		Color enabledFg = Color.WHITE;

		idField.setEditable(editing);
		idField.setFocusable(editing);
		idField.setBackground(editing ? enabledBg : disabledBg);
		idField.setForeground(editing ? enabledFg : disabledFg);

		nameField.setEditable(editing);
		nameField.setFocusable(editing);
		nameField.setBackground(editing ? enabledBg : disabledBg);
		nameField.setForeground(editing ? enabledFg : disabledFg);

		if (colorSwatch != null)
		{
			colorSwatch.setBorder(BorderFactory.createLineBorder(editing ? Color.WHITE : new Color(90, 90, 90), 1));
			colorSwatch.setCursor(Cursor.getPredefinedCursor(editing ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
			colorSwatch.setToolTipText(editing ? "Click to change color" : "Enable edit to change color");
			colorSwatch.setEnabled(editing);
		}

		if (toggleButton != null)
		{
			toggleButton.setIcon(editing ? SAVE_ICON : EDIT_ICON);
			toggleButton.setToolTipText(editing ? "Save projectile changes" : "Edit projectile");
		}

		refreshStyleButtons(styleButtons, entry, editing);
	}

	private void applyIdChange(ProjectileEntry entry, JTextField field)
	{
		String text = field.getText().trim();
		if (text.isEmpty())
		{
			field.setText(entry.getProjectileId() < 0 ? "" : String.valueOf(entry.getProjectileId()));
			return;
		}

		try
		{
			int newId = Integer.parseInt(text);
			if (newId < 0)
			{
				field.setText(entry.getProjectileId() < 0 ? "" : String.valueOf(entry.getProjectileId()));
				return;
			}

			if (newId != entry.getProjectileId())
			{
				entry.setProjectileId(newId);
				field.setText(String.valueOf(entry.getProjectileId()));
				onGroupChanged.run();
			}
			else
			{
				field.setText(String.valueOf(entry.getProjectileId()));
			}
		}
		catch (NumberFormatException ignored)
		{
			field.setText(entry.getProjectileId() < 0 ? "" : String.valueOf(entry.getProjectileId()));
		}
	}

	private void applyNameChange(ProjectileEntry entry, JTextField field)
	{
		String text = field.getText().trim();
		String newName = text.isEmpty() ? null : text;
		String current = entry.getCustomName();

		if (Objects.equals(current, newName))
		{
			field.setText(text);
			return;
		}

		entry.setCustomName(newName);
		field.setText(text);
		if (entry.getProjectileId() >= 0)
		{
			onGroupChanged.run();
		}
	}

	private void updateToggleButtonIcon(JButton toggleBtn, boolean hover)
	{
		if (group.isEnabled())
		{
			toggleBtn.setIcon(hover ? EYE_OPEN_ICON_HOVER : EYE_OPEN_ICON);
			toggleBtn.setToolTipText("Shown");
		}
		else
		{
			toggleBtn.setIcon(hover ? EYE_CLOSED_ICON_HOVER : EYE_CLOSED_ICON);
			toggleBtn.setToolTipText("Hidden");
		}
	}

	private static Icon createEyeIcon(boolean hidden)
	{
		int width = 28;
		int height = 16;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

		Color strokeColor = hidden ? new Color(150, 150, 150) : Color.WHITE;
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(strokeColor);

		int paddingX = 2;
		int paddingY = 3;
		int ovalWidth = width - paddingX * 2;
		int ovalHeight = height - paddingY * 2;
		g.drawOval(paddingX, paddingY, ovalWidth, ovalHeight);

		int pupilRadius = 3;
		int pupilX = width / 2 - pupilRadius;
		int pupilY = height / 2 - pupilRadius;

		if (!hidden)
		{
			g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawOval(width / 2 - 5, height / 2 - 5, 10, 10);
			g.fillOval(pupilX, pupilY, pupilRadius * 2, pupilRadius * 2);
		}
		else
		{
			Color slashColor = new Color(170, 170, 170);
			g.setColor(slashColor);
			g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(paddingX + 1, height - paddingY - 1, width - paddingX - 1, paddingY + 1);
		}

		g.dispose();
		return new ImageIcon(image);
	}

	private static Icon createPlusIcon()
	{
		int size = 18;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(new Color(90, 200, 90));
		int mid = size / 2;
		int inset = 4;
		g.drawLine(mid, inset, mid, size - inset);
		g.drawLine(inset, mid, size - inset, mid);
		g.dispose();
		return new ImageIcon(image);
	}

	private static Icon createMinusIcon()
	{
		int size = 18;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(new Color(220, 80, 80));
		int mid = size / 2;
		int inset = 4;
		g.drawLine(inset, mid, size - inset, mid);
		g.dispose();
		return new ImageIcon(image);
	}

	private static JButton createIconButton(Icon icon, String tooltip)
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

	private JButton createStyleButton(OverlayStyle style, BufferedImage baseImage, Icon neutralIcon,
									  String tooltip, ProjectileEntry entry, boolean[] editing, JButton[] allButtons)
	{
		JButton button = new JButton(neutralIcon);
		button.setToolTipText(tooltip);
		button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		button.setFocusPainted(false);
		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setDisabledIcon(neutralIcon);

		button.putClientProperty("style", style);
		button.putClientProperty("tooltip", tooltip);
		button.putClientProperty("baseImage", baseImage);
		button.putClientProperty("neutralIcon", neutralIcon);

		button.addActionListener(e -> {
			if (!editing[0])
			{
				return;
			}

			if (style != entry.getOverlayStyle())
			{
				entry.setOverlayStyle(style);
				if (entry.getProjectileId() >= 0)
				{
					onGroupChanged.run();
				}
			}

			refreshStyleButtons(allButtons, entry, editing[0]);
		});

		return button;
	}

	private void refreshStyleButtons(JButton[] buttons, ProjectileEntry entry, boolean editing)
	{
		if (buttons == null)
		{
			return;
		}

		Color baseColor = entry.getColor() != null ? entry.getColor() : new Color(150, 150, 150);

		for (JButton button : buttons)
		{
			if (button == null)
			{
				continue;
			}

			OverlayStyle btnStyle = (OverlayStyle) button.getClientProperty("style");
			boolean selected = btnStyle == entry.getOverlayStyle();

			button.setEnabled(editing);
			button.setCursor(Cursor.getPredefinedCursor(editing ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
			String tooltip = (String) button.getClientProperty("tooltip");
			button.setToolTipText(editing ? tooltip : "Press edit to change style");
			button.setOpaque(false);
			button.setContentAreaFilled(false);
			button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

			if (selected)
			{
				OverlayStyle style = (OverlayStyle) button.getClientProperty("style");
				BufferedImage baseImage = (BufferedImage) button.getClientProperty("baseImage");
				Icon coloredIcon = new ImageIcon(tintImageWithColor(baseImage, baseColor, determineContrastBoost(style)));
				button.setIcon(coloredIcon);
				button.setDisabledIcon(coloredIcon);
			}
			else
			{
				Icon neutralIcon = (Icon) button.getClientProperty("neutralIcon");
				button.setIcon(neutralIcon);
				button.setDisabledIcon(neutralIcon);
			}
		}
	}

	private float determineContrastBoost(OverlayStyle style)
	{
		switch (style)
		{
			case HULL:
				return 0.9f; // strongest contrast for filled outline
			case OUTLINE:
				return 0.6f;
			case FILLED:
				return 0.4f;
			case TILE:
			default:
				return 0.5f;
		}
	}

    private void openColorPicker(ProjectileEntry entry, JPanel colorSwatch, JButton[] styleButtons, boolean[] editing)
    {
        RuneliteColorPicker colorPicker = colorPickerManager.create(
            SwingUtilities.windowForComponent(this),
            entry.getColor(),
            "Projectile " + entry.getProjectileId() + " Color",
            false  // Show alpha slider
        );
        colorPicker.setOnClose(newColor -> {
            entry.setColor(newColor);
            colorSwatch.setBackground(newColor);
			refreshStyleButtons(styleButtons, entry, editing[0]);
			if (entry.getProjectileId() >= 0)
			{
				onGroupChanged.run();
			}
        });
        colorPicker.setVisible(true);
    }

    public void refresh()
    {
        removeAll();

        JPanel headerPanel = createHeaderPanel();
        add(headerPanel);

        entriesPanel.removeAll();
        entriesPanel.setVisible(expanded);
        if (expanded)
        {
            buildEntriesPanel();
        }
        add(entriesPanel);

        revalidate();
        repaint();
    }
}
