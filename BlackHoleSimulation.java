import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 2D Relativistic Gravitational Lensing & Accretion Simulation
 * Inspired by General Relativity & "Simulating Black Holes" (Kavan)
 */
public class BlackHoleSimulation extends JPanel {

    private static final int WIDTH = 900, HEIGHT = 700;
    private static final double BH_X = WIDTH / 2.0, BH_Y = HEIGHT / 2.0;
    private static final double RS = 45.0; // Schwarzschild radius in pixels

    // Mode: 0 = Light Ray Lensing, 1 = Accretion Particles, 2 = Both
    private int displayMode = 0;
    private boolean isPaused = false;

    // Light rays for 2D Gravitational Lensing
    static class LightRay {
        double x, y;
        double vx, vy;
        List<Point.Double> trail = new ArrayList<>();
        boolean captured = false;
        boolean escaped = false;
        Color color;

        public LightRay(double x, double y, double vx, double vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            trail.add(new Point.Double(x, y));
        }

        public void step(double dt, double bhX, double bhY, double rs) {
            if (captured || escaped) return;

            // Relativistic light deflection:
            // a = -1.5 * rs * L^2 / r^5 * r_vec
            double rx = x - bhX;
            double ry = y - bhY;
            double r2 = rx * rx + ry * ry;
            double r = Math.sqrt(r2);

            if (r <= rs) {
                captured = true;
                return;
            }
            if (r > 1200.0) {
                escaped = true;
                return;
            }

            // Angular momentum L = rx * vy - ry * vx
            double L = rx * vy - ry * vx;
            double L2 = L * L;
            double r5 = r2 * r2 * r;

            double aMag = -1.5 * rs * L2 / Math.max(r5, 1e-4);
            double ax = aMag * rx;
            double ay = aMag * ry;

            // Update direction & position
            vx += ax * dt;
            vy += ay * dt;

            // Normalize speed to c = 6.0
            double speed = Math.sqrt(vx * vx + vy * vy);
            vx = (vx / speed) * 6.0;
            vy = (vy / speed) * 6.0;

            x += vx * dt;
            y += vy * dt;

            trail.add(new Point.Double(x, y));
            if (trail.size() > 400) {
                trail.remove(0);
            }
        }
    }

    // Accretion disk particles
    static class Particle {
        double x, y, vx, vy;
        Color color;

        public Particle(double x, double y, double vx, double vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
        }

        public void step(double dt, double bhX, double bhY, double rs) {
            double rx = bhX - x;
            double ry = bhY - y;
            double dist2 = rx * rx + ry * ry;
            double dist = Math.sqrt(dist2);

            if (dist < rs * 1.05) {
                // Respawn at outer edge
                double angle = Math.random() * 2 * Math.PI;
                double rSpawn = rs * 3.5 + Math.random() * rs * 2.5;
                x = bhX + rSpawn * Math.cos(angle);
                y = bhY + rSpawn * Math.sin(angle);
                double vOrb = Math.sqrt(2800.0 / rSpawn);
                vx = -vOrb * Math.sin(angle);
                vy = vOrb * Math.cos(angle);
                return;
            }

            // Paczynski-Wiita pseudo-Newtonian potential for Schwarzschild accretion:
            // F = GM / (r - rs)^2
            double effDist = Math.max(dist - rs * 0.7, 2.0);
            double force = 2800.0 / (effDist * effDist);
            vx += (force * rx / dist) * dt;
            vy += (force * ry / dist) * dt;

            // Slight viscous drag
            vx *= 0.9997;
            vy *= 0.9997;

            x += vx * dt;
            y += vy * dt;
        }
    }

    private final List<LightRay> rays = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    public BlackHoleSimulation() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        spawnRays();
        spawnParticles();

        Timer timer = new Timer(16, e -> {
            if (!isPaused) {
                updateSim();
            }
            repaint();
        });
        timer.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    spawnRays();
                } else if (e.getKeyCode() == KeyEvent.VK_M) {
                    displayMode = (displayMode + 1) % 3;
                } else if (e.getKeyCode() == KeyEvent.VK_P) {
                    isPaused = !isPaused;
                }
            }
        });
        setFocusable(true);
    }

    private void spawnRays() {
        rays.clear();
        int count = 65;
        double startX = 30.0;
        double spacing = (HEIGHT - 100.0) / count;

        for (int i = 0; i <= count; i++) {
            double startY = 50.0 + i * spacing;
            // Spectral colors across impact parameters
            float hue = (float) i / count;
            Color col = Color.getHSBColor(hue * 0.75f + 0.1f, 0.9f, 1.0f);
            rays.add(new LightRay(startX, startY, 6.0, 0.0, col));
        }
    }

    private void spawnParticles() {
        particles.clear();
        for (int i = 0; i < 350; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double radius = RS * 2.2 + Math.random() * RS * 3.8;
            double speed = Math.sqrt(2800.0 / radius);
            double px = BH_X + radius * Math.cos(angle);
            double py = BH_Y + radius * Math.sin(angle);
            double vx = -speed * Math.sin(angle);
            double vy = speed * Math.cos(angle);

            float t = (float) ((radius - RS * 2.2) / (RS * 3.8));
            Color c = t < 0.4f ? new Color(255, 240, 200) : (t < 0.7f ? new Color(255, 140, 20) : new Color(220, 40, 10));
            particles.add(new Particle(px, py, vx, vy, c));
        }
    }

    private void updateSim() {
        for (int step = 0; step < 2; step++) {
            for (LightRay ray : rays) {
                ray.step(0.7, BH_X, BH_Y, RS);
            }
            for (Particle p : particles) {
                p.step(0.4, BH_X, BH_Y, RS);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background starfield
        g2.setColor(new Color(25, 25, 45));
        for (int i = 0; i < 60; i++) {
            int sx = (int) ((i * 137.5) % WIDTH);
            int sy = (int) ((i * 229.3) % HEIGHT);
            g2.fillRect(sx, sy, 1, 1);
        }

        // Draw Accretion Disk particles
        if (displayMode == 1 || displayMode == 2) {
            for (Particle p : particles) {
                g2.setColor(p.color);
                g2.fillOval((int) (p.x - 1.5), (int) (p.y - 1.5), 3, 3);
            }
        }

        // Draw Light Ray Geodesics
        if (displayMode == 0 || displayMode == 2) {
            for (LightRay ray : rays) {
                if (ray.trail.size() < 2) continue;
                Path2D path = new Path2D.Double();
                Point.Double p0 = ray.trail.get(0);
                path.moveTo(p0.x, p0.y);
                for (int i = 1; i < ray.trail.size(); i++) {
                    Point.Double pt = ray.trail.get(i);
                    path.lineTo(pt.x, pt.y);
                }
                g2.setColor(ray.captured ? new Color(ray.color.getRed(), ray.color.getGreen(), ray.color.getBlue(), 90) : ray.color);
                g2.setStroke(new BasicStroke(ray.captured ? 1.0f : 1.6f));
                g2.draw(path);
            }
        }

        // Photon sphere ring (r = 1.5 rs)
        int rPh = (int) (RS * 1.5);
        g2.setColor(new Color(255, 200, 80, 80));
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0));
        g2.drawOval((int) (BH_X - rPh), (int) (BH_Y - rPh), rPh * 2, rPh * 2);

        // Critical Shadow boundary (impact parameter b_crit = 2.6 rs)
        int rCrit = (int) (RS * 2.6);
        g2.setColor(new Color(100, 180, 255, 60));
        g2.drawOval((int) (BH_X - rCrit), (int) (BH_Y - rCrit), rCrit * 2, rCrit * 2);

        // Event Horizon (r = rs)
        int rH = (int) RS;
        g2.setColor(Color.BLACK);
        g2.fillOval((int) (BH_X - rH), (int) (BH_Y - rH), rH * 2, rH * 2);
        g2.setColor(new Color(255, 80, 20));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval((int) (BH_X - rH), (int) (BH_Y - rH), rH * 2, rH * 2);

        // UI Text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString("Relativistic Black Hole Lensing Simulation (2D Geodesics)", 20, 28);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(200, 200, 200));
        g2.drawString("Mode: " + (displayMode == 0 ? "Light Ray Deflection" : (displayMode == 1 ? "Accretion Disk" : "Combined")) + "  |  [M] Toggle Mode  |  [SPACE] Shoot Rays  |  [P] Pause", 20, 50);
        g2.drawString("Event Horizon: r = rs (red circle) | Photon Sphere: r = 1.5 rs (dashed amber) | Shadow: b = 2.6 rs (blue circle)", 20, HEIGHT - 20);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2D Relativistic Gravitational Lensing - General Relativity");
            BlackHoleSimulation sim = new BlackHoleSimulation();
            frame.add(sim);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
