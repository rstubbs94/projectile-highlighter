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
	private static final Icon EYE_OPEN_ICON;
	private static final Icon EYE_OPEN_ICON_HOVER;
	private static final Icon EYE_CLOSED_ICON;
	private static final Icon EYE_CLOSED_ICON_HOVER;
	private static final Icon EDIT_ICON;
	private static final Icon SAVE_ICON;
	private static final Icon PLUS_ICON = createPlusIcon();
	private static final Icon MINUS_ICON = createMinusIcon();
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
    private final Runnable onGroupChanged;
    private final ColorPickerManager colorPickerManager;

    public GroupPanel(ProjectileGroup group,
                      Consumer<ProjectileGroup> onToggleEnabled,
                      Consumer<ProjectileGroup> onRename,
                      Consumer<ProjectileGroup> onDelete,
                      Consumer<ProjectileGroup> onAddEntry,
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
        this.onGroupChanged = onGroupChanged;
        this.colorPickerManager = colorPickerManager;
		this.panelBackground = alternateRowColor ? PANEL_BG_ALT : PANEL_BG;
		this.headerBackground = alternateRowColor ? HEADER_BG_ALT : HEADER_BG;
		this.expanded = initiallyExpanded;
		this.onExpansionChanged = onExpansionChanged;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(panelBackground);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR));

        // Header (two rows stacked vertically)
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel);

        // Entries (expandable)
        entriesPanel = new JPanel();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        entriesPanel.setBackground(panelBackground);
		entriesPanel.setBorder(new EmptyBorder(0, 12, 0, 0));
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
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(headerBackground);
        header.setBorder(new EmptyBorder(6, 8, 4, 8));

        // Row 1: Expand arrow + status + name + count
        JPanel row1 = new JPanel();
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS));
        row1.setBackground(headerBackground);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);

		expandLabel = new JLabel(expanded ? "▼" : "▶");
		expandLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		expandLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        expandLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row1.add(expandLabel);
		row1.add(Box.createHorizontalStrut(4));

		JLabel statusDot = new JLabel("●");
		statusDot.setForeground(group.isEnabled() ? ENABLED_COLOR : DISABLED_COLOR);
		statusDot.setToolTipText(group.isEnabled() ? "Enabled" : "Disabled");
		statusDot.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        row1.add(statusDot);
        row1.add(Box.createHorizontalStrut(4));

		JLabel nameLabel = new JLabel(group.getName());
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        row1.add(nameLabel);
        row1.add(Box.createHorizontalStrut(4));

		countLabel = new JLabel("(" + group.getEntryCount() + ")");
		countLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		countLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        row1.add(countLabel);

		row1.add(Box.createHorizontalGlue());
		addHeaderToggleListener(row1);
		addHeaderToggleListener(expandLabel);
		addHeaderToggleListener(statusDot);
		addHeaderToggleListener(nameLabel);
		addHeaderToggleListener(countLabel);
        header.add(row1);
        header.add(Box.createVerticalStrut(4));

        // Row 2: Buttons (always visible)
        JPanel row2 = new JPanel();
        row2.setLayout(new BoxLayout(row2, BoxLayout.X_AXIS));
        row2.setBackground(headerBackground);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);

		int spacing = 6;
		row2.add(Box.createHorizontalGlue());

		Dimension btnSize = new Dimension(40, 28);

		JButton toggleBtn = new JButton();
		updateToggleButtonIcon(toggleBtn, false);
		toggleBtn.setMargin(new Insets(2, 2, 2, 2));
		toggleBtn.setPreferredSize(btnSize);
		toggleBtn.setMaximumSize(btnSize);
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
		row2.add(toggleBtn);
		row2.add(Box.createHorizontalStrut(spacing));

		JButton renameBtn = createIconButton(EDIT_ICON, "Rename group");
		renameBtn.addActionListener(e -> onRename.accept(group));
		row2.add(renameBtn);
		row2.add(Box.createHorizontalStrut(spacing));

		JButton deleteBtn = createIconButton(MINUS_ICON, "Delete group");
		deleteBtn.addActionListener(e -> onDelete.accept(group));
		row2.add(deleteBtn);

		header.add(row2);

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

        if (group.getEntries() == null || group.getEntries().isEmpty())
        {
            JLabel emptyLabel = new JLabel("No projectiles in this group");
            emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            emptyLabel.setBorder(new EmptyBorder(6, 28, 6, 6));
            emptyLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 13));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            entriesPanel.add(emptyLabel);
        }
        else
        {
			int rowIndex = 0;
            for (ProjectileEntry entry : group.getEntries())
            {
                JPanel entryPanel = createEntryPanel(entry, rowIndex++);
                entriesPanel.add(entryPanel);
            }
        }

		JPanel addButtonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
		addButtonRow.setBackground(ENTRY_BG);
		addButtonRow.setBorder(new EmptyBorder(4, 28, 8, 6));
		addButtonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton addEntryBtn = createIconButton(PLUS_ICON, "Add projectile to this group");
		addEntryBtn.addActionListener(e -> onAddEntry.accept(group));
		addButtonRow.add(addEntryBtn);

		JLabel addLabel = new JLabel("Add Projectile");
		addLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		addLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		addButtonRow.add(addLabel);

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
		BufferedImage visible = ImageUtil.loadImageResource(GroupPanel.class, "visible_icon.png");
		BufferedImage invisible = ImageUtil.loadImageResource(GroupPanel.class, "invisible_icon.png");
		BufferedImage edit = ImageUtil.loadImageResource(GroupPanel.class, "edit_icon.png");
		BufferedImage save = ImageUtil.loadImageResource(GroupPanel.class, "save_icon.png");

		BufferedImage editRecolored = recolorImage(edit, new Color(200, 200, 200));
		BufferedImage saveRecolored = recolorImage(save, new Color(140, 210, 160));

		EYE_OPEN_ICON = new ImageIcon(visible);
		EYE_OPEN_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(visible, 0.5f));
		EYE_CLOSED_ICON = new ImageIcon(invisible);
		EYE_CLOSED_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(invisible, 0.5f));
		EDIT_ICON = new ImageIcon(editRecolored);
		SAVE_ICON = new ImageIcon(saveRecolored);
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

	private JPanel createEntryPanel(ProjectileEntry entry, int rowIndex)
	{
		Color rowBackground = (rowIndex % 2 == 0) ? ENTRY_BG : ENTRY_BG_ALT;
		final boolean[] editing = {entry.getProjectileId() < 0};

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(rowBackground);
		panel.setBorder(new EmptyBorder(6, 32, 6, 8));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 1: Color swatch + editable ID/name
        JPanel row1 = new JPanel();
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS));
        row1.setBackground(rowBackground);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel colorSwatch = new JPanel();
		colorSwatch.setPreferredSize(new Dimension(16, 16));
        colorSwatch.setMaximumSize(new Dimension(16, 16));
        colorSwatch.setMinimumSize(new Dimension(16, 16));
        colorSwatch.setBackground(entry.getColor());
        colorSwatch.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
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
				openColorPicker(entry, colorSwatch);
			}
		});
        row1.add(colorSwatch);
        row1.add(Box.createHorizontalStrut(6));

		String initialIdText = entry.getProjectileId() < 0 ? "" : String.valueOf(entry.getProjectileId());
		JTextField idField = new JTextField(initialIdText, 5);
		idField.setMaximumSize(new Dimension(64, 20));
		idField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		idField.setForeground(Color.WHITE);
		idField.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
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
		nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		nameField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
		nameField.setForeground(Color.WHITE);
		nameField.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(70, 70, 70)),
			BorderFactory.createEmptyBorder(2, 6, 2, 6)
		));
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
        row1.add(Box.createHorizontalGlue());

        panel.add(row1);
        panel.add(Box.createVerticalStrut(5));

        // Row 2: Style dropdown + Name button + Remove button
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

        JComboBox<OverlayStyle> styleCombo = new JComboBox<>(OverlayStyle.values());
        styleCombo.setSelectedItem(initialStyle);
		styleCombo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		Dimension comboSize = new Dimension(88, 24);
        styleCombo.setPreferredSize(comboSize);
        styleCombo.setMaximumSize(comboSize);
        styleCombo.setToolTipText("Overlay style");
        styleCombo.addActionListener(e -> {
            OverlayStyle selected = (OverlayStyle) styleCombo.getSelectedItem();
            if (selected != null && selected != entry.getOverlayStyle())
            {
                entry.setOverlayStyle(selected);
                onGroupChanged.run();
            }
        });
        row2.add(styleCombo);
        row2.add(Box.createHorizontalGlue());

		JButton editButton = createIconButton(EDIT_ICON, "Edit projectile");
		editButton.addActionListener(e -> {
			if (!editing[0])
			{
				editing[0] = true;
				setInlineEditingState(true, idField, nameField, colorSwatch, editButton);
				idField.requestFocusInWindow();
				idField.selectAll();
			}
			else
			{
				applyIdChange(entry, idField);
				applyNameChange(entry, nameField);
				editing[0] = false;
				setInlineEditingState(false, idField, nameField, colorSwatch, editButton);

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

		setInlineEditingState(editing[0], idField, nameField, colorSwatch, editButton);
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

	private void setInlineEditingState(boolean editing, JTextField idField, JTextField nameField, JPanel colorSwatch, JButton toggleButton)
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

	private void addHeaderToggleListener(Component component)
	{
		component.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				toggleExpanded();
			}
		});
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
		int size = 22;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(new Color(90, 200, 90));
		int mid = size / 2;
		int inset = 5;
		g.drawLine(mid, inset, mid, size - inset);
		g.drawLine(inset, mid, size - inset, mid);
		g.dispose();
		return new ImageIcon(image);
	}

	private static Icon createMinusIcon()
	{
		int size = 22;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(new Color(220, 80, 80));
		int mid = size / 2;
		int inset = 5;
		g.drawLine(inset, mid, size - inset, mid);
		g.dispose();
		return new ImageIcon(image);
	}

	private static JButton createIconButton(Icon icon, String tooltip)
	{
		JButton button = new JButton(icon);
		button.setToolTipText(tooltip);
		button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		button.setFocusPainted(false);
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return button;
	}

	private String formatEntryLabel(ProjectileEntry entry)
    {
        String custom = entry.getCustomName();
        String displayName = (custom != null && !custom.isEmpty()) ?
            custom :
            ProjectileNames.getDisplayName(entry.getProjectileId());
        return entry.getProjectileId() + ": " + displayName;
    }

    private void openColorPicker(ProjectileEntry entry, JPanel colorSwatch)
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
