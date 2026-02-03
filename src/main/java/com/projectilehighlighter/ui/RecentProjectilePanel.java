package com.projectilehighlighter.ui;

import com.projectilehighlighter.model.RecentProjectile;
import com.projectilehighlighter.util.ProjectileNames;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import static java.awt.Cursor.HAND_CURSOR;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Panel displaying a single recent projectile row in a table format.
 */
public class RecentProjectilePanel extends JPanel
{
	private static final Color ROW_COLOR_1 = new Color(35, 35, 35);
	private static final Color ROW_COLOR_2 = new Color(42, 42, 42);

	// Column widths - shared with header
	public static final int ADD_COLUMN_WIDTH = 26;
	public static final int ID_COLUMN_WIDTH = 50;
	public static final int ROW_HEIGHT = 22;

	private static final Font ID_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
	private static final Font TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	private static final Color SEPARATOR_COLOR = new Color(60, 60, 60);

	public RecentProjectilePanel(RecentProjectile projectile, int rowIndex,
								 Consumer<RecentProjectile> onAddToGroup)
	{
		setLayout(new BorderLayout());
		Color bgColor = (rowIndex % 2 == 0) ? ROW_COLOR_1 : ROW_COLOR_2;
		setBackground(bgColor);
		setBorder(new EmptyBorder(0, 8, 0, 6));
		setMinimumSize(new Dimension(0, ROW_HEIGHT));
		setPreferredSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));

		// Use a container with BoxLayout for the columns
		JPanel rowContent = new JPanel();
		rowContent.setLayout(new BoxLayout(rowContent, BoxLayout.X_AXIS));
		rowContent.setOpaque(false);

		// Column 1: Add button (fixed width)
		JButton addBtn = new JButton(PLUS_ICON);
		addBtn.setToolTipText("Add to group");
		addBtn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		addBtn.setMargin(new Insets(0, 0, 0, 0));
		addBtn.setFocusPainted(false);
		addBtn.setContentAreaFilled(false);
		addBtn.setOpaque(false);
		addBtn.setCursor(Cursor.getPredefinedCursor(HAND_CURSOR));
		addBtn.addActionListener(e -> onAddToGroup.accept(projectile));
		rowContent.add(createFixedWidthPanel(addBtn, ADD_COLUMN_WIDTH, bgColor));

		// Separator
		rowContent.add(createSeparator(bgColor));

		// Column 2: ID (fixed width)
		String projectileLabel = String.valueOf(projectile.getProjectileId());
		JLabel idLabel = new JLabel(projectileLabel);
		idLabel.setForeground(Color.WHITE);
		idLabel.setFont(ID_FONT);
		String projectileName = ProjectileNames.getDisplayName(projectile.getProjectileId());
		String defaultName = "Projectile #" + projectile.getProjectileId();
		if (projectileName == null || projectileName.equalsIgnoreCase(defaultName))
		{
			projectileName = null;
		}
		idLabel.setToolTipText(projectileName != null ? projectileName : "Projectile " + projectile.getProjectileId());
		rowContent.add(createFixedWidthPanel(idLabel, ID_COLUMN_WIDTH, bgColor));

		// Separator
		rowContent.add(createSeparator(bgColor));
		rowContent.add(Box.createHorizontalStrut(6));

		// Column 3: Source (fills remaining space, truncates with ellipsis)
		String sourceLabel = projectile.getSourceDisplay();
		if (projectileName != null && !projectileName.isEmpty())
		{
			sourceLabel = sourceLabel + " • " + projectileName;
		}
		final String fullSourceText = sourceLabel;

		JLabel sourceText = new JLabel(sourceLabel)
		{
			@Override
			public Dimension getPreferredSize()
			{
				// Allow shrinking
				return new Dimension(0, ROW_HEIGHT);
			}
		};
		sourceText.setForeground(new Color(200, 200, 200));
		sourceText.setFont(TEXT_FONT);
		sourceText.setToolTipText(fullSourceText);
		rowContent.add(sourceText);

		add(rowContent, BorderLayout.CENTER);
	}

	private static JPanel createFixedWidthPanel(JComponent content, int width, Color bgColor)
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);
		panel.setPreferredSize(new Dimension(width, ROW_HEIGHT));
		panel.setMinimumSize(new Dimension(width, ROW_HEIGHT));
		panel.setMaximumSize(new Dimension(width, ROW_HEIGHT));
		panel.add(content);
		return panel;
	}

	private static JPanel createSeparator(Color bgColor)
	{
		JPanel sep = new JPanel();
		sep.setBackground(SEPARATOR_COLOR);
		sep.setPreferredSize(new Dimension(1, ROW_HEIGHT - 4));
		sep.setMinimumSize(new Dimension(1, ROW_HEIGHT - 4));
		sep.setMaximumSize(new Dimension(1, ROW_HEIGHT - 4));
		return sep;
	}

	public static final Icon PLUS_ICON = createPlusIcon();

	private static Icon createPlusIcon()
	{
		int size = 16;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(new Color(90, 200, 90));
		int mid = size / 2;
		int inset = 4;
		g.drawLine(mid, inset, mid, size - inset);
		g.drawLine(inset, mid, size - inset, mid);
		g.dispose();
		return new ImageIcon(image);
	}
}
