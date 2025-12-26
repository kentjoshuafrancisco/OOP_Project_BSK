import javax.swing.*;
import java.awt.*;

public class NewCardGradientPanel extends JPanel {

    private final Color baseColor;

    public NewCardGradientPanel(Color baseColor) {
        this.baseColor = baseColor;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint gp = new GradientPaint(
                0, 0, baseColor.brighter(),
                0, getHeight(), baseColor.darker()
        );

        g2.setPaint(gp);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
    }
}
