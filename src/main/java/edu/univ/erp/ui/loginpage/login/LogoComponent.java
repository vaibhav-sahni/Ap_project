// In src/LoginPage/src/login/LogoComponent.java
package login;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

public class LogoComponent extends JComponent {

    private Image logo;

    public LogoComponent() {
        try {
            // Load your logo image from the 'login' package
            logo = ImageIO.read(getClass().getResource("/resources/iiitd_logo.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (logo != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // This draws the image, scaling it to fit the component's size
            g2.drawImage(logo, 0, 0, getWidth(), getHeight(), this);
            g2.dispose();
        }
    }
}