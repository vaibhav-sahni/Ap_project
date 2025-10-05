package edu.univ.erp.ui.loginpage.login;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

public class CrossButton extends JComponent {

    private boolean mouseOver = false;

    public CrossButton() {
        // Set cursor to hand when hovering
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add mouse listeners to detect hover and clicks
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mouseOver = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseOver = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // When clicked, close the application
                System.exit(0);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Set the background color and transparency
        // Make it slightly darker when the mouse is over it
        if (mouseOver) {
            g2.setColor(new Color(255, 255, 255, 50)); // More opaque on hover
        } else {
            g2.setColor(new Color(255, 255, 255, 30)); // Default transparency
        }

        // Fill the circle
        g2.fillOval(0, 0, getWidth(), getHeight());

        // --- Draw the 'X' ---
        g2.setColor(Color.WHITE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // Set font size relative to the button size
        g2.setFont(getFont().deriveFont(java.awt.Font.BOLD, (float)getWidth() / 2.5f));

        String text = "X";
        // Center the 'X' text inside the circle
        int stringWidth = g2.getFontMetrics().stringWidth(text);
        int stringHeight = g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent();
        int x = (getWidth() - stringWidth) / 2;
        int y = (getHeight() + stringHeight) / 2;
        g2.drawString(text, x, y);

        g2.dispose();
        super.paintComponent(g);
    }
}
