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
 * Panel displaying a single recent projectile with add-to-group button.
 */
public class RecentProjectilePanel extends JPanel
{
    private static final Color ROW_COLOR_1 = ColorScheme.DARK_GRAY_COLOR;
    private static final Color ROW_COLOR_2 = new Color(45, 45, 45);

    public RecentProjectilePanel(RecentProjectile projectile, int rowIndex,
                                  Consumer<RecentProjectile> onAddToGroup)
    {
        setLayout(new BorderLayout(6, 0));
        Color bgColor = (rowIndex % 2 == 0) ? ROW_COLOR_1 : ROW_COLOR_2;
        setBackground(bgColor);
        setBorder(new EmptyBorder(5, 8, 5, 6));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        String displayName = ProjectileNames.getDisplayName(projectile.getProjectileId());
        JLabel nameLabel = new JLabel(projectile.getProjectileId() + ": " + displayName);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(nameLabel, BorderLayout.CENTER);

        // Right side: Add button
        JButton addBtn = new JButton(PLUS_ICON);
        addBtn.setToolTipText("Add to group");
        addBtn.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        addBtn.setFocusPainted(false);
        addBtn.setContentAreaFilled(false);
        addBtn.setOpaque(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(HAND_CURSOR));
        addBtn.addActionListener(e -> onAddToGroup.accept(projectile));
        add(addBtn, BorderLayout.EAST);
    }

    private static final Icon PLUS_ICON = createPlusIcon();

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
}
