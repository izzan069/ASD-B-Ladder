import java.awt.*;
import java.util.Stack;

public class Player {
    private String name;
    private int position;
    private Color avatarColor;
    private int id;
    private int score;
    private boolean isFinished; // Penanda status finish

    private Stack<Integer> stepHistory;

    public Player(int id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.avatarColor = color;
        this.position = 1;
        this.score = 0;
        this.isFinished = false; // Awal game belum finish

        this.stepHistory = new Stack<>();
        this.stepHistory.push(1);
    }

    public void setPosition(int pos) {
        this.position = pos;
    }

    public int getPosition() {
        return position;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public int getScore() {
        return score;
    }

    public void resetScore() {
        this.score = 0;
    }

    // --- Getter & Setter untuk Status Finish ---
    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }
    // -------------------------------------------

    public Stack<Integer> getHistory() {
        return stepHistory;
    }

    public Color getColor() {
        return avatarColor;
    }

    public String getName() {
        return name;
    }

    // Reset data untuk New Game
    public void reset() {
        this.position = 1;
        this.score = 0;
        this.isFinished = false;
        this.stepHistory.clear();
        this.stepHistory.push(1);
    }
}