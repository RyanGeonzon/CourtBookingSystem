import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class CourtPanel extends JPanel {

    private BufferedImage image;

    public CourtPanel() {
        try {
            image = ImageIO.read(getClass().getResource("frankie-lopez-i792zAvm_9M-unsplash.jpg"));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Could not load court.jpg: " + e.getMessage());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (image != null) {
            // Scale the image to fill the panel without stretching it
            double scale = Math.max((double) w / image.getWidth(),
                    (double) h / image.getHeight());
            int drawW = (int) (image.getWidth() * scale);
            int drawH = (int) (image.getHeight() * scale);
            int x = (w - drawW) / 2;
            int y = (h - drawH) / 2;
            g2.drawImage(image, x, y, drawW, drawH, null);
        } else {
            g2.setColor(new Color(0xD49A58));   // fallback if the image is missing
            g2.fillRect(0, 0, w, h);
        }

        // Dark strip at the bottom so the white text is readable
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, h - 150, w, 150);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 40));
        g2.drawString("Your court", 32, h - 70);
        g2.drawString("is waiting.", 32, h - 24);

        g2.dispose();
    }
}