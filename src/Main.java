package src; /**
 * ES234317-Algorithm and Data Structures
 * Semester Ganjil, 2025/2026
 * Ladder Game
 * Muhammad Izzan Aquilla - 5026241069
 * Muhammad Faqih Maulana - 5026241174
 */

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            GameFrame game = new GameFrame();
            game.setVisible(true);
        });
    }
}