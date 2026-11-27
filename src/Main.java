import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            // Makes colors look the same on Windows, Mac and Linux
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) { }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
