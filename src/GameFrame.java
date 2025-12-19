package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class GameFrame extends JFrame {

    private GameBoardPanel boardPanel;
    private JButton rollButton, resetButton;
    private JLabel statusLabel, diceDisplayLabel;
    private JTextPane logArea;
    private JTextArea scoreBoardArea;
    private JSlider volumeSlider;
    private StyledDocument doc;

    private List<Player> players = new ArrayList<>();
    // List untuk menyimpan urutan juara
    private List<Player> rankingList = new ArrayList<>();

    private int currentPlayerIndex = 0;
    private boolean isAnimating = false;

    private SoundManager soundManager;
    private Font mainFont = new Font("Segoe UI", Font.BOLD, 14);
    private final Color PANEL_BG = new Color(28, 28, 35);
    private final Color CARD_BG = new Color(40, 40, 50);

    public GameFrame() {
        soundManager = new SoundManager();
        soundManager.playBackgroundMusic("bgm.wav");

        setTitle("Neon Spiral: Point Collector Edition");
        setSize(1350, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(33, 33, 33));

        // --- UI Setup ---
        boardPanel = new GameBoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(350, 0));
        rightPanel.setBackground(PANEL_BG);
        rightPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Info Card
        JPanel infoCard = createCardPanel();
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        JLabel titleLbl = new JLabel("PILOT TURN");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(Color.GRAY);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel = new JLabel("Waiting...");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setForeground(Color.CYAN);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        diceDisplayLabel = new JLabel("-");
        diceDisplayLabel.setFont(new Font("Segoe UI", Font.BOLD, 70));
        diceDisplayLabel.setForeground(new Color(255, 215, 0));
        diceDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoCard.add(Box.createVerticalStrut(10));
        infoCard.add(titleLbl); infoCard.add(statusLabel);
        infoCard.add(Box.createVerticalStrut(5)); infoCard.add(diceDisplayLabel);
        infoCard.add(Box.createVerticalStrut(10));

        // Score Card
        JPanel scoreCard = createCardPanel();
        scoreCard.setLayout(new BorderLayout());
        JLabel scoreTitle = new JLabel(" LIVE SCORES");
        scoreTitle.setForeground(new Color(255, 215, 0));
        scoreBoardArea = new JTextArea();
        scoreBoardArea.setEditable(false);
        scoreBoardArea.setBackground(CARD_BG);
        scoreBoardArea.setForeground(Color.WHITE);
        scoreBoardArea.setFont(new Font("Consolas", Font.BOLD, 14));
        scoreCard.add(scoreTitle, BorderLayout.NORTH);
        scoreCard.add(scoreBoardArea, BorderLayout.CENTER);

        // Log Card
        JPanel logCard = createCardPanel();
        logCard.setLayout(new BorderLayout());
        JLabel logTitle = new JLabel(" LOGS");
        logTitle.setForeground(Color.GRAY);
        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 36));
        doc = logArea.getStyledDocument();
        logCard.add(logTitle, BorderLayout.NORTH);
        logCard.add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Controls
        JPanel controlContainer = new JPanel(new GridLayout(3, 1, 0, 8));
        controlContainer.setOpaque(false);
        rollButton = new JButton("ROLL DICE");
        resetButton = new JButton("NEW GAME");
        styleModernButton(rollButton, new Color(0, 180, 216));
        styleModernButton(resetButton, new Color(230, 57, 70));

        JPanel volumePanel = createCardPanel();
        volumePanel.setLayout(new BorderLayout());
        volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.setOpaque(false);
        volumeSlider.addChangeListener(e -> soundManager.setVolume(volumeSlider.getValue() / 100f));
        volumePanel.add(new JLabel("  BGM "), BorderLayout.WEST);
        volumePanel.add(volumeSlider, BorderLayout.CENTER);

        controlContainer.add(rollButton);
        controlContainer.add(resetButton);
        controlContainer.add(volumePanel);

        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        top.add(infoCard, BorderLayout.NORTH); top.add(scoreCard, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout()); bottom.setOpaque(false);
        bottom.setPreferredSize(new Dimension(0, 350));
        bottom.add(logCard, BorderLayout.CENTER); bottom.add(controlContainer, BorderLayout.SOUTH);

        rightPanel.add(top, BorderLayout.NORTH);
        rightPanel.add(bottom, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        rollButton.addActionListener(e -> playTurn());
        resetButton.addActionListener(e -> startNewGame());

        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(this::setupGame);
    }

    // --- LOGIKA SETUP GAME (INPUT NAMA & WARNA) ---
    private void setupGame() {
        String[] options = {"1 Pilot", "2 Pilots", "3 Pilots", "4 Pilots"};
        int choice = JOptionPane.showOptionDialog(this, "Initialize System: How many players?", "System Setup",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == -1) System.exit(0);
        int playerCount = choice + 1;

        players.clear();
        // Daftar Warna Tersedia
        List<ColorOption> availableColors = new ArrayList<>();
        availableColors.add(new ColorOption("Cyan Neon", new Color(0, 255, 255)));
        availableColors.add(new ColorOption("Magenta Hot", new Color(255, 50, 150)));
        availableColors.add(new ColorOption("Lime Green", new Color(50, 255, 100)));
        availableColors.add(new ColorOption("Sunset Orange", new Color(255, 165, 0)));
        availableColors.add(new ColorOption("Electric Blue", new Color(30, 144, 255)));
        availableColors.add(new ColorOption("Plasma Purple", new Color(180, 80, 255)));
        availableColors.add(new ColorOption("Golden Ray", new Color(255, 215, 0)));

        for (int i = 0; i < playerCount; i++) {
            JPanel panel = new JPanel(new GridLayout(0, 1));
            JTextField nameField = new JTextField("Pilot " + (i + 1));
            JComboBox<ColorOption> colorBox = new JComboBox<>(availableColors.toArray(new ColorOption[0]));
            panel.add(new JLabel("Name Player " + (i + 1) + ":")); panel.add(nameField);
            panel.add(new JLabel("Select Color:")); panel.add(colorBox);

            int result = JOptionPane.showConfirmDialog(this, panel, "Player " + (i+1), JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) System.exit(0);

            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Pilot " + (i + 1);
            ColorOption selected = (ColorOption) colorBox.getSelectedItem();
            players.add(new Player(i, name, selected.color));

            // Hapus warna agar tidak duplikat
            availableColors.remove(selected);
        }

        boardPanel.setPlayers(players);
        rankingList.clear();
        for(Player p : players) p.reset();

        currentPlayerIndex = 0;
        logArea.setText("");
        logToConsole("System Initialized.", Color.WHITE);


        isAnimating = false;        // Reset status animasi
        rollButton.setEnabled(true); // NYALAKAN KEMBALI TOMBOL ROLL


        updateStatus();
        updateScoreBoard();
    }

    private void startNewGame() {
        boardPanel.generateRandomBoard();
        setupGame();
    }

    private void updateStatus() {
        if(players.isEmpty()) return;
        Player p = players.get(currentPlayerIndex);
        statusLabel.setText(p.getName());
        statusLabel.setForeground(p.getColor());
    }

    private void updateScoreBoard() {
        StringBuilder sb = new StringBuilder();
        for (Player p : players) {
            String status = p.isFinished() ? " [DONE]" : "";
            sb.append(String.format(" %-10s : %3d pts%s\n", p.getName(), p.getScore(), status));
        }
        scoreBoardArea.setText(sb.toString());
    }

    private void playTurn() {
        if (isAnimating) return;
        Player player = players.get(currentPlayerIndex);
        soundManager.play("dice.wav");
        int dice = (int) (Math.random() * 6) + 1;
        diceDisplayLabel.setText(String.valueOf(dice));
        boolean isGreen = Math.random() < 0.8;

        logToConsole(player.getName() + " rolled " + dice + (isGreen ? " [FWD]" : " [BCK]"), isGreen ? Color.GREEN : Color.RED);

        List<Integer> movePath = calculatePath(player, dice, isGreen);
        rollButton.setEnabled(false);
        animateMove(player, movePath);
    }

    private List<Integer> calculatePath(Player p, int dice, boolean isGreen) {
        List<Integer> path = new ArrayList<>();
        Stack<Integer> history = p.getHistory();
        int current = p.getPosition();
        boolean canUseLadder = isPrime(current);

        for (int i = 0; i < dice; i++) {
            if (isGreen) {
                int nextStep = current + 1;
                if (nextStep > 100) nextStep = 100;
                Integer ladderDest = boardPanel.getLadderDestination(nextStep);
                if (canUseLadder && ladderDest != null) {
                    history.push(nextStep); current = ladderDest; history.push(current);
                    logToConsole(" >> Warp Gate Active!", Color.ORANGE);
                } else {
                    current = nextStep;
                    if (history.isEmpty() || history.peek() != current) history.push(current);
                }
            } else {
                if (history.size() > 1) { history.pop(); current = history.peek(); }
                else current = 1;
            }
            path.add(current);
            if (current == 100) break;
        }
        return path;
    }

    private void animateMove(Player p, List<Integer> path) {
        isAnimating = true;
        Timer timer = new Timer(300, null);
        timer.addActionListener(new ActionListener() {
            int stepIndex = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (stepIndex < path.size()) {
                    int nextPos = path.get(stepIndex);
                    int points = boardPanel.collectPoint(nextPos);
                    if (points > 0) {
                        p.addScore(points);
                        updateScoreBoard();
                        logToConsole(" ★ Collected " + points + " pts!", Color.YELLOW);
                        soundManager.play("coin.wav");
                    } else soundManager.play("step.wav");

                    p.setPosition(nextPos);
                    boardPanel.repaint();
                    stepIndex++;
                } else {
                    timer.stop();
                    finishTurn();
                }
            }
        });
        timer.start();
    }

    // --- LOGIKA MENYELESAIKAN GILIRAN & MENENTUKAN JUARA ---
    private void finishTurn() {
        Player p = players.get(currentPlayerIndex);

        // 1. Cek Apakah Pemain Finish (Posisi 100)
        if (p.getPosition() == 100) {
            if (!p.isFinished()) {
                p.setFinished(true);
                rankingList.add(p); // Tambah ke daftar juara
                soundManager.play("win.wav");
                logToConsole("🏆 " + p.getName() + " finished Rank #" + rankingList.size(), Color.CYAN);
                JOptionPane.showMessageDialog(this, p.getName() + " reached Finish! Rank #" + rankingList.size());
            }
        }

        // 2. Cek Apakah Game Over (Hanya sisa 1 orang yang belum finish)
        long activeCount = players.stream().filter(pl -> !pl.isFinished()).count();
        if (activeCount == 0 || (players.size() > 1 && activeCount == 1)) {
            handleGameOver(activeCount);
            return;
        }

        // 3. Putar Giliran (Lewati pemain yang sudah Finish)
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (players.get(currentPlayerIndex).isFinished());

        updateStatus();
        updateScoreBoard();
        isAnimating = false;
        rollButton.setEnabled(true);
    }

    private void handleGameOver(long activeCount) {
        // Jika sisa 1 orang, dia dianggap Kalah (Loser)
        if (activeCount == 1) {
            for (Player pl : players) {
                if (!pl.isFinished()) {
                    pl.setFinished(true);
                    rankingList.add(pl); // Masukkan di urutan terakhir
                    break;
                }
            }
        }

        // Tampilkan Popup Juara
        StringBuilder sb = new StringBuilder("===== FINAL RANKING =====\n\n");
        for (int i = 0; i < rankingList.size(); i++) {
            Player winner = rankingList.get(i);
            String rankStr;
            if (i == 0) rankStr = "🥇 1st (CHAMPION)";
            else if (i == 1) rankStr = "🥈 2nd Place";
            else if (i == 2) rankStr = "🥉 3rd Place";
            else if (i == rankingList.size() - 1 && players.size() > 1) rankStr = "💀 LOSER";
            else rankStr = (i+1)+"th Place";

            sb.append(rankStr).append(": ").append(winner.getName())
                    .append(" (Score: ").append(winner.getScore()).append(")\n");
        }

        scoreBoardArea.setText(sb.toString());
        JOptionPane.showMessageDialog(this, new JTextArea(sb.toString()), "Game Over", JOptionPane.INFORMATION_MESSAGE);

        statusLabel.setText("GAME OVER");
        rollButton.setEnabled(false);
    }

    private void logToConsole(String text, Color c) {
        try {
            SimpleAttributeSet keyWord = new SimpleAttributeSet();
            StyleConstants.setForeground(keyWord, c);
            StyleConstants.setFontFamily(keyWord, "Consolas");
            doc.insertString(doc.getLength(), text + "\n", keyWord);
            logArea.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {}
    }

    private boolean isPrime(int n) {
        if (n <= 1) return false;
        for (int i = 2; i <= Math.sqrt(n); i++) if (n % i == 0) return false;
        return true;
    }

    private JPanel createCardPanel() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(60,60,70),1,true), new EmptyBorder(10,10,10,10)));
        return p;
    }
    private void styleModernButton(JButton btn, Color bg) {
        btn.setFont(mainFont); btn.setBackground(bg); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false); btn.setBorderPainted(false);
    }

    // Helper untuk ComboBox Warna
    private static class ColorOption {
        String name; Color color;
        ColorOption(String n, Color c) { name=n; color=c; }
        public String toString() { return name; }
    }
}