package com.projectilehighlighter.ui;

import com.projectilehighlighter.model.RecentProjectile;
import com.projectilehighlighter.util.ProjectileNames;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

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
        setLayout(new BorderLayout(5, 0));
        Color bgColor = (rowIndex % 2 == 0) ? ROW_COLOR_1 : ROW_COLOR_2;
        setBackground(bgColor);
        setBorder(new EmptyBorder(6, 8, 6, 8));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        String displayName = ProjectileNames.getDisplayName(projectile.getProjectileId());
        JLabel nameLabel = new JLabel(projectile.getProjectileId() + ": " + displayName);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        nameLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        add(nameLabel, BorderLayout.CENTER);

        // Right side: Add button
        JButton addBtn = new JButton(PLUS_ICON);
        addBtn.setToolTipText("Add to group");
        addBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        addBtn.setFocusPainted(false);
        addBtn.setContentAreaFilled(false);
        addBtn.setOpaque(false);
        addBtn.addActionListener(e -> onAddToGroup.accept(projectile));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setBackground(bgColor);
        btnPanel.add(addBtn);
        add(btnPanel, BorderLayout.EAST);
    }

    private static final Icon PLUS_ICON = createPlusIcon();

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
}
