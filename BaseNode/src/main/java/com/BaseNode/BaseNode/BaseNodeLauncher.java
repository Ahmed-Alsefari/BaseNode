package com.BaseNode.BaseNode;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

public class BaseNodeLauncher extends JFrame {

    private static final String LOG_PATH     = "logs/basenode.log";
    private static final String UPLOADS_PATH = "../Uploads";
    private static final int    APP_PORT     = 8080;
    private static final String BASE_URL     = "http://localhost:" + APP_PORT;
    private static final boolean IS_DOCKER   = System.getenv("DOCKER") != null;

    private Process          nportProcess;
    private volatile String  tunnelUrl     = "";
    private volatile String  dbUrl         = "";
    private volatile boolean springStarted = false;

    private final Runnable springStarter;

    private JTextField  nameField;
    private RoundButton startBtn, stopBtn, openBtn, downloadBtn;
    private LinkLabel   urlLabel, dbLabel;
    private JLabel      statusDot;

    static final Color BG         = new Color(0xF4F6F8);
    static final Color BTN_START  = new Color(0x2E7D32);
    static final Color BTN_STOP   = new Color(0xC62828);
    static final Color BTN_DL     = new Color(0x37474F);
    static final Color BTN_OPEN   = new Color(0x1565C0);
    static final Color FIELD_BG   = new Color(0xECEFF1);
    static final Color LABEL_FG   = new Color(0x546E7A);
    static final Color LINK_FG    = new Color(0x1565C0);
    static final Color BORDER_CLR = new Color(0xB0BEC5);
    static final Color STOPPED    = new Color(0x9E9E9E);
    static final Color STARTING   = new Color(0xF57F17);

    public BaseNodeLauncher(Runnable springStarter) {
        super("BaseNode");

        ImageIcon icon = new ImageIcon(
                getClass().getResource("/static/images/BaseNode2.png")
        );

        Image scaled = icon.getImage().getScaledInstance(
                32,
                32,
                Image.SCALE_SMOOTH
        );

        setIconImage(scaled);
        this.springStarter = springStarter;

        if (IS_DOCKER) {
            // Docker mode: start everything headlessly without building any UI
            startDockerMode();
            return;
        }

        ensureDependencies();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int c = JOptionPane.showConfirmDialog(
                        BaseNodeLauncher.this,
                        "Stop the server and exit?",
                        "BaseNode", JOptionPane.YES_NO_OPTION);
                if (c == JOptionPane.YES_OPTION) {
                    shutdownNPort();
                    System.exit(0);
                }
            }
        });

        setResizable(false);
        buildUI();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void ensureDependencies() {
        try {
            // Check Node.js
            Process nodeCheck = Runtime.getRuntime().exec("node --version");
            nodeCheck.waitFor();
            if (nodeCheck.exitValue() != 0) {
                System.out.println("[BaseNode] Installing Node.js...");
                new ProcessBuilder("winget", "install", "OpenJS.NodeJS", "--silent", "--accept-source-agreements", "--accept-package-agreements")
                        .inheritIO().start().waitFor();
            }

            // Check NPort
            Process nportCheck = Runtime.getRuntime().exec("nport --version");
            nportCheck.waitFor();
            if (nportCheck.exitValue() != 0) {
                System.out.println("[BaseNode] Installing NPort...");
                new ProcessBuilder("cmd.exe", "/c", "npm", "install", "-g", "nport")
                        .inheritIO().start().waitFor();
            }

        } catch (Exception e) {
            System.err.println("[BaseNode] Dependency check failed: " + e.getMessage());
        }
    }


    // ─── Docker / headless mode ───────────────────────────────────────────────

    private void startDockerMode() {
        System.out.println("[BaseNode] Running in Docker mode");

        new Thread(() -> {
            try {
                // 1. Start Spring Boot
                if (!springStarted) {
                    springStarted = true;
                    new Thread(springStarter, "spring-boot").start();
                }

                System.out.println("[BaseNode] Waiting for Spring Boot...");
                waitForSpring();
                System.out.println("[BaseNode] Spring Boot is up");

                // 2. Start NPort with basenode-N, auto-retry on taken
                startNPortDocker();

            } catch (Exception ex) {
                System.err.println("[BaseNode] Startup failed: " + ex.getMessage());
            }
        }, "docker-starter").start();
    }

    private void startNPortDocker() throws IOException {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        String name = "basenode";

        while (true) {
            System.out.println("[BaseNode] Trying subdomain: " + name);

            ProcessBuilder pb = new ProcessBuilder("nport",
                    String.valueOf(APP_PORT), "-s", name);
            pb.redirectErrorStream(true);
            nportProcess = pb.start();

            boolean taken = false;

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(nportProcess.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("[NPort] " + line);

                    if (line.contains("already in use") || line.contains("Failed to connect")) {
                        taken = true;
                        nportProcess.destroyForcibly();
                        break;
                    }

                    for (String part : line.split("\\s+")) {
                        if (part.startsWith("https://") && part.contains(".nport.link")) {
                            tunnelUrl = part.trim();
                            dbUrl     = tunnelUrl + "/h2-console";
                            System.out.println("──────────────────────────────");
                            System.out.println("URL web:  " + tunnelUrl);
                            System.out.println("DB web:   " + dbUrl);
                            System.out.println("──────────────────────────────");
                        }
                    }
                }
            }

            if (!taken) break;

            System.out.println("[NPort] Subdomain \"" + name + "\" is taken. Enter a new name:");
            name = scanner.nextLine().trim().toLowerCase();
            while (!name.matches("[a-z0-9-]+")) {
                System.out.println("[NPort] Invalid name. Use only letters, numbers, and hyphens:");
                name = scanner.nextLine().trim().toLowerCase();
            }
        }
    }
    // ─── GUI mode ─────────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel();
        root.setBackground(BG);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel("~ BaseNode ~");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(0x263238));
        title.setAlignmentX(CENTER_ALIGNMENT);
        root.add(title);
        root.add(vgap(12));

        nameField = new JTextField("my-server");
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nameField.setBackground(FIELD_BG);
        nameField.setForeground(new Color(0x263238));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(10, BORDER_CLR),
                new EmptyBorder(8, 12, 8, 12)));
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        nameField.setToolTipText("NPort subdomain — letters, numbers, hyphens only");
        root.add(nameField);
        root.add(vgap(10));

        JPanel row1 = row();
        startBtn = new RoundButton("START", BTN_START);
        stopBtn  = new RoundButton("STOP",  BTN_STOP);
        stopBtn.setEnabled(false);
        startBtn.addActionListener(e -> onStart());
        stopBtn .addActionListener(e -> onStop());
        row1.add(startBtn);
        row1.add(stopBtn);
        root.add(row1);
        root.add(vgap(8));

        statusDot = new JLabel("   Stopped");
        statusDot.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusDot.setForeground(STOPPED);
        statusDot.setAlignmentX(LEFT_ALIGNMENT);
        root.add(statusDot);
        root.add(vgap(10));

        urlLabel = new LinkLabel("URL web:  (not started)", () -> openBrowser(tunnelUrl));
        root.add(urlLabel);
        root.add(vgap(7));

        dbLabel = new LinkLabel("DB web:   (not started)", () -> openBrowser(dbUrl));
        root.add(dbLabel);
        root.add(vgap(14));

        JPanel row2 = row();
        openBtn     = new RoundButton("OPEN",     BTN_OPEN);
        downloadBtn = new RoundButton("Download", BTN_DL);
        openBtn    .addActionListener(e -> openUploadsFolder());
        downloadBtn.addActionListener(e -> downloadLog());
        row2.add(openBtn);
        row2.add(downloadBtn);
        root.add(row2);
        root.add(vgap(4));

        JPanel row3 = row();
        JLabel lOpen = subLabel("Uploads folder");
        JLabel lDown = subLabel("Logs.txt");
        lOpen.setHorizontalAlignment(SwingConstants.CENTER);
        lDown.setHorizontalAlignment(SwingConstants.CENTER);
        row3.add(lOpen);
        row3.add(lDown);
        root.add(row3);

        setContentPane(root);
        setPreferredSize(new Dimension(300, 390));
    }

    private void onStart() {
        String name = nameField.getText().trim().toLowerCase();
        if (name.isEmpty() || !name.matches("[a-z0-9-]+")) {
            JOptionPane.showMessageDialog(this,
                    "Server name must contain only letters, numbers, and hyphens.",
                    "Invalid Name", JOptionPane.WARNING_MESSAGE);
            return;
        }

        startBtn .setEnabled(false);
        nameField.setEnabled(false);
        setStatus("Starting Spring Boot...", STARTING);

        new Thread(() -> {
            try {
                if (!springStarted) {
                    springStarted = true;
                    new Thread(springStarter, "spring-boot").start();
                }

                waitForSpring();

                SwingUtilities.invokeLater(() -> setStatus("Connecting tunnel...", STARTING));

                startNPort(name);

                SwingUtilities.invokeLater(() -> {
                    stopBtn.setEnabled(true);
                    setStatus("Running", BTN_START);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    startBtn .setEnabled(true);
                    nameField.setEnabled(true);
                    setStatus("Error: " + ex.getMessage(), BTN_STOP);
                });
            }
        }, "launcher-start").start();
    }

    private void onStop() {
        stopBtn.setEnabled(false);
        shutdownNPort();
        tunnelUrl = "";
        dbUrl     = "";
        SwingUtilities.invokeLater(() -> {
            startBtn .setEnabled(true);
            nameField.setEnabled(true);
            urlLabel.reset("URL web:  (not started)");
            dbLabel .reset("DB web:   (not started)");
            setStatus("Stopped", STOPPED);
        });
    }

    private void waitForSpring() throws Exception {
        for (int i = 0; i < 90; i++) {
            try {
                HttpURLConnection c = (HttpURLConnection)
                        new URL(BASE_URL + "/login").openConnection();
                c.setConnectTimeout(1000);
                c.setReadTimeout(1000);
                c.connect();
                if (c.getResponseCode() > 0) return;
            } catch (Exception ignored) {}
            Thread.sleep(1000);
        }
        throw new RuntimeException("Spring Boot did not respond within 90 s.");
    }

    private void startNPort(String name) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "nport.cmd",
                String.valueOf(APP_PORT), "-s", name ,"-l", "en");
        pb.redirectErrorStream(true);
        nportProcess = pb.start();

        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(nportProcess.getInputStream()))) {
                String line;
                boolean failed = false;

                while ((line = br.readLine()) != null) {
                    System.out.println("[NPort] " + line);

                    if (line.contains("already in use") || line.contains("Failed to connect")) {
                        failed = true;
                    }

                    for (String part : line.split("\\s+")) {
                        if (part.startsWith("https://") && part.contains(".nport.link")) {
                            tunnelUrl = part.trim();
                            dbUrl     = tunnelUrl + "/h2-console";
                            String uploadsPath = new File(UPLOADS_PATH).getCanonicalPath();

                            System.out.println("──────────────────────────────");
                            System.out.println("URL web:        " + tunnelUrl);
                            System.out.println("DB web:         " + dbUrl);
                            System.out.println("Uploads folder: " + uploadsPath);
                            System.out.println("──────────────────────────────");

                            final String u = tunnelUrl, d = dbUrl;
                            SwingUtilities.invokeLater(() -> {
                                urlLabel.setLink("URL web:  " + u);
                                dbLabel .setLink("DB web:   " + d);
                            });
                            break;
                        }
                    }
                }

                if (failed) {
                    System.out.println("[NPort] Subdomain \"" + name + "\" is taken. Enter a new name:");

                    java.util.concurrent.atomic.AtomicReference<String> answer = new java.util.concurrent.atomic.AtomicReference<>(null);
                    JOptionPane pane = new JOptionPane(
                            "Subdomain \"" + name + "\" is already in use.\nEnter a different name:",
                            JOptionPane.WARNING_MESSAGE,
                            JOptionPane.OK_CANCEL_OPTION
                    );
                    JTextField inputField = new JTextField();
                    pane.setMessage(new Object[]{
                            "Subdomain \"" + name + "\" is already in use.\nEnter a different name:", inputField
                    });
                    JDialog dialog = pane.createDialog(BaseNodeLauncher.this, "Subdomain Taken");

                    Thread consoleThread = new Thread(() -> {
                        java.util.Scanner scanner = new java.util.Scanner(System.in);
                        String input = scanner.nextLine().trim().toLowerCase();
                        if (input.matches("[a-z0-9-]+") && answer.compareAndSet(null, input)) {
                            System.out.println("[NPort] Using: " + input);
                            SwingUtilities.invokeLater(() -> {
                                dialog.dispose();
                                nameField.setText(input);
                                nameField.setEnabled(true);
                                startBtn.setEnabled(true);
                                stopBtn.setEnabled(false);
                                setStatus("Stopped", STOPPED);
                                startBtn.doClick();
                            });
                        }
                    }, "console-input");
                    consoleThread.setDaemon(true);
                    consoleThread.start();

                    SwingUtilities.invokeLater(() -> {
                        dialog.setVisible(true);
                        String newNameUI = inputField.getText().trim().toLowerCase();
                        if (newNameUI.matches("[a-z0-9-]+") && answer.compareAndSet(null, newNameUI)) {
                            consoleThread.interrupt();
                            nameField.setText(newNameUI);
                            nameField.setEnabled(true);
                            startBtn.setEnabled(true);
                            stopBtn.setEnabled(false);
                            setStatus("Stopped", STOPPED);
                            startBtn.doClick();
                        }
                    });
                }

            } catch (Exception ignored) {}
        }, "nport-reader").start();
    }

    private void shutdownNPort() {
        if (nportProcess != null && nportProcess.isAlive())
            nportProcess.destroyForcibly();
        nportProcess = null;
    }

    private void openUploadsFolder() {
        try {
            File dir = new File(UPLOADS_PATH).getCanonicalFile();
            if (!dir.exists()) dir.mkdirs();
            Desktop.getDesktop().open(dir);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not open Uploads folder:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void downloadLog() {
        File logFile = new File(LOG_PATH);
        if (!logFile.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Log file not found:\n" + logFile.getAbsolutePath(),
                    "Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Log File");
        chooser.setSelectedFile(new File("basenode.log.txt"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        try {
            File dest = chooser.getSelectedFile();
            Files.copy(logFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(this,
                    "Log saved to:\n" + dest.getAbsolutePath(),
                    "Downloaded ✓", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save log:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openBrowser(String url) {
        if (url == null || url.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "URL not available yet - wait for the tunnel to connect.",
                    "Not Ready", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try { Desktop.getDesktop().browse(new URI(url)); }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not open browser:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setStatus(String text, Color color) {
        statusDot.setText("   " + text);
        statusDot.setForeground(color);
    }

    private static JPanel row() {
        JPanel p = new JPanel(new GridLayout(1, 2, 10, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return p;
    }

    static Component vgap(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }

    static JLabel subLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(LABEL_FG);
        return l;
    }

    static class RoundButton extends JButton {
        private final Color bg;
        RoundButton(String text, Color bg) {
            super(text);
            this.bg = bg;
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = !isEnabled()            ? new Color(0xBDBDBD)
                    : getModel().isPressed()  ? bg.darker()
                    : getModel().isRollover() ? bg.brighter()
                    : bg;
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class LinkLabel extends JPanel {
        private final JLabel   label;
        private final Runnable onClick;
        private boolean        active = false;

        LinkLabel(String text, Runnable onClick) {
            this.onClick = onClick;
            setBackground(FIELD_BG);
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setBorder(BorderFactory.createCompoundBorder(
                    new RoundBorder(10, BORDER_CLR),
                    new EmptyBorder(8, 12, 8, 12)));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            label = new JLabel(text);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            label.setForeground(LABEL_FG);
            add(label);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { if (active) onClick.run(); }
                @Override public void mouseEntered(MouseEvent e) {
                    if (active) setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                @Override public void mouseExited(MouseEvent e) { setCursor(Cursor.getDefaultCursor()); }
            });
        }
        void setLink(String text) {
            active = true;
            label.setText("<html><u>" + text + "</u></html>");
            label.setForeground(LINK_FG);
            revalidate(); repaint();
        }
        void reset(String text) {
            active = false;
            label.setText(text);
            label.setForeground(LABEL_FG);
            setCursor(Cursor.getDefaultCursor());
            revalidate(); repaint();
        }
    }

    static class RoundBorder extends AbstractBorder {
        private final int radius; private final Color color;
        RoundBorder(int r, Color c) { radius = r; color = c; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x, y, w-1, h-1, radius, radius));
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) {
            return new Insets(radius/2, radius/2, radius/2, radius/2);
        }
    }
}
