package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class GameBoardPanel extends JPanel {

    private final int TOTAL_NODES = 100;

    private Map<Integer, Point> nodeCoords = new HashMap<>();
    private Map<Integer, Integer> ladders = new HashMap<>();
    private Map<Integer, Integer> nodePoints = new HashMap<>();
    private List<Player> players = new ArrayList<>();

    // --- VARIABEL NAVIGASI KAMERA (ZOOM & PAN) ---
    private double zoomFactor = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private Point dragStartPoint;

    // Colors
    private final Color BG_CENTER = new Color(20, 10, 40);
    private final Color BG_OUTER = new Color(10, 5, 20);
    private final Color PATH_COLOR = new Color(255, 255, 255, 60);
    private final Color LADDER_GLOW = new Color(0, 255, 200, 100);

    public GameBoardPanel() {
        // Setup Mouse Listener untuk Zoom dan Geser
        setupNavigationListeners();
        generateRandomBoard();

        // Listener saat layar diubah ukurannya
        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                calculateSpiralNodes();
                repaint();
            }
        });

        setToolTipText("Scroll: Zoom | Drag: Pan");
    }

    // --- SETUP MOUSE (ZOOM & PAN) ---
    private void setupNavigationListeners() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint != null) {
                    // Hitung pergeseran
                    double deltaX = e.getX() - dragStartPoint.getX();
                    double deltaY = e.getY() - dragStartPoint.getY();
                    translateX += deltaX;
                    translateY += deltaY;
                    dragStartPoint = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double zoomMultiplier = 1.1;
                double oldZoom = zoomFactor;

                // Zoom In/Out logic
                if (e.getWheelRotation() < 0) zoomFactor *= zoomMultiplier;
                else zoomFactor /= zoomMultiplier;

                // Batas Zoom
                if (zoomFactor < 0.1) zoomFactor = 0.1;
                if (zoomFactor > 5.0) zoomFactor = 5.0;

                // Arahkan Zoom ke posisi mouse
                double mouseX = e.getX();
                double mouseY = e.getY();
                translateX = mouseX - (mouseX - translateX) * (zoomFactor / oldZoom);
                translateY = mouseY - (mouseY - translateY) * (zoomFactor / oldZoom);

                repaint();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    // --- LOGIKA SPIRAL BESAR (TANPA SKALA PAKSA) ---
    // Ini menggunakan logika "Constant Arc Length" yang Anda minta
    private void calculateSpiralNodes() {
        nodeCoords.clear();

        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;

        // Jarak fisik pixel antar node
        double distanceBetweenNodes = 45.0;
        // Jarak antar lapisan spiral
        double gapBetweenLoops = 55.0;

        double currentAngle = 0.0;
        double currentRadius = 0.0;

        // Node 1 di tengah
        nodeCoords.put(1, new Point(cx, cy));

        for (int i = 2; i <= TOTAL_NODES; i++) {
            if (i == 2) {
                currentRadius = distanceBetweenNodes;
                currentAngle = 0;
            } else {
                // Rumus agar jarak node konstan meski radius membesar
                double deltaAngle = distanceBetweenNodes / currentRadius;
                currentAngle += deltaAngle;

                double b = gapBetweenLoops / (2 * Math.PI);
                currentRadius = b * currentAngle;

                double minRadius = (i - 1) * (distanceBetweenNodes * 0.15);
                if (currentRadius < minRadius) currentRadius = minRadius + distanceBetweenNodes;
            }

            int x = cx + (int) (currentRadius * Math.cos(currentAngle));
            int y = cy + (int) (currentRadius * Math.sin(currentAngle));

            nodeCoords.put(i, new Point(x, y));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (nodeCoords.isEmpty()) calculateSpiralNodes();

        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Gambar Background (Statis, tidak ikut zoom)
        drawBackground(g2);

        // --- MULAI TRANSFORMASI KAMERA ---
        AffineTransform oldTransform = g2.getTransform();
        AffineTransform at = new AffineTransform();
        at.translate(translateX, translateY); // Geser
        at.scale(zoomFactor, zoomFactor);     // Zoom
        g2.transform(at);

        // 2. Gambar Objek Game (Ikut Zoom)
        drawGrid(g2); // Dekorasi Grid
        drawSpiralPath(g2);

        for (Map.Entry<Integer, Integer> entry : ladders.entrySet()) {
            drawNeonLadder(g2, entry.getKey(), entry.getValue());
        }
        for (int i = 1; i <= TOTAL_NODES; i++) {
            drawNode3D(g2, i);
        }
        for (Map.Entry<Integer, Integer> entry : nodePoints.entrySet()) {
            drawPointItem(g2, entry.getKey(), entry.getValue());
        }
        drawPlayers(g2);
        drawLabel(g2, 1, "START", Color.GREEN);
        drawLabel(g2, 100, "FINISH", Color.CYAN);

        // --- KEMBALIKAN TRANSFORMASI ---
        g2.setTransform(oldTransform);

        // 3. Gambar UI Overlay (Info Zoom di pojok kiri atas)
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2.drawString(String.format("Zoom: %.1fx | Pos: %.0f, %.0f", zoomFactor, translateX, translateY), 10, 20);
    }

    // --- HELPER METHODS ---

    public void setPlayers(List<Player> players) { this.players = players; repaint(); }
    public Integer getLadderDestination(int node) { return ladders.get(node); }

    public void generateRandomBoard() {
        ladders.clear(); nodePoints.clear();
        Random rand = new Random();
        for (int i = 0; i < 7; i++) {
            int start = rand.nextInt(80) + 2;
            int end = start + rand.nextInt(25) + 5;
            if (end < 100 && !ladders.containsKey(start) && !ladders.containsValue(start)) ladders.put(start, end);
        }
        for (int i = 0; i < 20; i++) {
            int node = rand.nextInt(98) + 2;
            if (!nodePoints.containsKey(node)) nodePoints.put(node, rand.nextInt(10) + 1);
        }
    }

    public int collectPoint(int node) {
        if (nodePoints.containsKey(node)) {
            int val = nodePoints.get(node); nodePoints.remove(node); return val;
        } return 0;
    }

    private void drawBackground(Graphics2D g2) {
        int w = getWidth(); int h = getHeight();
        RadialGradientPaint rgp = new RadialGradientPaint(new Point(w/2, h/2), Math.max(w, h), new float[]{0.0f, 1.0f}, new Color[]{BG_CENTER, BG_OUTER});
        g2.setPaint(rgp); g2.fillRect(0, 0, w, h);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 10)); g2.setStroke(new BasicStroke(1));
        int cx = getWidth()/2, cy = getHeight()/2;
        for(int r=100; r<5000; r+=200) g2.drawOval(cx-r, cy-r, r*2, r*2);
    }

    private void drawSpiralPath(Graphics2D g2) {
        if (nodeCoords.isEmpty()) return;
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g2.setColor(PATH_COLOR);
        int[] xPoints = new int[TOTAL_NODES]; int[] yPoints = new int[TOTAL_NODES];
        for (int i = 0; i < TOTAL_NODES; i++) { Point p = nodeCoords.get(i + 1); if (p != null) { xPoints[i] = p.x; yPoints[i] = p.y; } }
        g2.drawPolyline(xPoints, yPoints, TOTAL_NODES);
    }

    private void drawNeonLadder(Graphics2D g2, int start, int end) {
        Point p1 = nodeCoords.get(start); Point p2 = nodeCoords.get(end); if (p1 == null || p2 == null) return;
        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g2.setStroke(dashed); g2.setColor(LADDER_GLOW); g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        g2.fillOval(p1.x-4, p1.y-4, 8, 8); g2.fillOval(p2.x-4, p2.y-4, 8, 8);
    }

    private void drawNode3D(Graphics2D g2, int i) {
        Point p = nodeCoords.get(i); if (p == null) return;
        boolean isPrm = isPrime(i); int size = isPrm ? 32 : 24;
        Color c1 = (i==100)?Color.CYAN:(i==1)?Color.GREEN:(isPrm)?new Color(255,200,50):new Color(80,80,100);
        Color c2 = (i==100)?Color.BLUE:(i==1)?new Color(0,100,0):(isPrm)?new Color(200,100,0):new Color(30,30,40);
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(p.x-size/4, p.y-size/4), size, new float[]{0f, 1f}, new Color[]{c1, c2}));
        g2.fillOval(p.x-size/2, p.y-size/2, size, size);
        if (ladders.containsKey(i)) { g2.setColor(new Color(0,255,200)); g2.setStroke(new BasicStroke(2)); g2.drawOval(p.x-size/2-3, p.y-size/2-3, size+6, size+6); }
        g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif", Font.PLAIN, isPrm?11:9));
        String txt = String.valueOf(i); FontMetrics fm = g2.getFontMetrics();
        g2.drawString(txt, p.x-fm.stringWidth(txt)/2, p.y+fm.getAscent()/2-2);
    }

    private void drawPointItem(Graphics2D g2, int node, int val) {
        Point p = nodeCoords.get(node); if(p==null) return;
        g2.setColor(new Color(255, 215, 0, 200)); g2.fillOval(p.x+6, p.y-12, 14, 14);
        g2.setColor(Color.BLACK); g2.setFont(new Font("Arial",Font.BOLD,9));
        g2.drawString(String.valueOf(val), p.x+9, p.y-2);
    }

    private void drawPlayers(Graphics2D g2) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Player pl : players) {
            int pos = pl.getPosition(); Point p = nodeCoords.get(pos); if(p==null) continue;
            int c = counts.getOrDefault(pos, 0); counts.put(pos, c+1);
            int xo = (c==1)?10:(c==2)?10:(c==3)?-10:(c>0)?-10:0;
            int yo = (c==1)?-10:(c==2)?10:(c==3)?10:(c>0)?-10:0;
            g2.setColor(pl.getColor()); g2.fillOval(p.x-10+xo, p.y-10+yo, 20, 20);
            g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(2)); g2.drawOval(p.x-10+xo, p.y-10+yo, 20, 20);
        }
    }

    private void drawLabel(Graphics2D g2, int idx, String txt, Color c) {
        Point p = nodeCoords.get(idx); if(p==null)return;
        g2.setColor(c); g2.setFont(new Font("Arial",Font.BOLD,10)); g2.drawString(txt, p.x-15, p.y+25);
    }

    private boolean isPrime(int n) {
        if(n<=1)return false; for(int i=2;i<=Math.sqrt(n);i++)if(n%i==0)return false; return true;
    }
}