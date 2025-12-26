import javax.swing.*;
import java.awt.*;

public class NewScreenGradientPanel extends JPanel {

    public NewScreenGradientPanel() {
        setLayout(new GridBagLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);

        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(20, 150, 50),
                0, getHeight(), new Color(0, 80, 0)
        );

        g2.setPaint(gradient);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }
}
