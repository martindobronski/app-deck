package streamdeck;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamDeckApp extends JFrame {

    static {
        System.setProperty("apple.awt.application.name", "App Deck");
    }

    private static final int COLS = 6;
    private static final int ROWS = 4;
    private static final int BUTTON_SIZE = 120;
    private static final int BOTTOM_ROW_START = (ROWS - 1) * COLS;
    private static final int ICON_SIZE = 48;
    private static final int DRAG_THRESHOLD = 5;

    private final List<List<ButtonConfig>> pages;
    private final String configPath;
    private final JButton[] btnComponents = new JButton[ROWS * COLS];
    private final Map<String, Icon> iconCache = new ConcurrentHashMap<>();
    private final JPanel gridPanel;
    private final JWindow dragGhost;
    private final javax.swing.Timer refreshTimer;
    private Set<String> runningApps = new HashSet<>();
    private Set<String> runningCmdLines = new HashSet<>();
    private int dragSourceIndex = -1;
    private Point dragStart;
    private int currentPage = 0;
    private boolean longPressHandled;
    private boolean dragPerformed;
    private int longPressGridIdx = -1;
    private javax.swing.Timer longPressTimer;
    private final Set<String> killedApps = new HashSet<>();
    private ButtonConfig currentFolder;
    private int savedRootPage;
    private boolean searchDialogOpen;
    private java.awt.KeyEventDispatcher searchKeyDispatcher;

    public StreamDeckApp(List<List<ButtonConfig>> pages, String configPath) {
        this.pages = pages;
        this.configPath = configPath;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("App Deck");
        setIconImage(loadAppIcon());
        if (Taskbar.isTaskbarSupported()) {
            Taskbar.getTaskbar().setIconImage(loadAppIcon());
        }

        JMenuBar menuBar = new JMenuBar();
        JMenu appMenu = new JMenu("App Desk");
        JMenuItem aboutItem = new JMenuItem("Über App Desk");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "App Desk V1.0\nEin Stream-Deck-artiger Schaltflächenstarter für macOS.\n\nErstellt am 16.05.26",
            "Über App Desk", JOptionPane.INFORMATION_MESSAGE));
        appMenu.add(aboutItem);
        appMenu.addSeparator();
        JMenuItem searchItem = new JMenuItem("Suche mit Strg + F");
        searchItem.addActionListener(e -> showSearchDialog(""));
        appMenu.add(searchItem);
        appMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("App Desk beenden");
        exitItem.addActionListener(e -> System.exit(0));
        appMenu.add(exitItem);
        menuBar.add(Box.createHorizontalStrut(6));
        menuBar.add(appMenu);
        menuBar.add(Box.createHorizontalGlue());
        JMenu helpMenu = new JMenu("Hilfe");
        JMenuItem docsItem = new JMenuItem("Dokumentation anzeigen");
        docsItem.addActionListener(e -> {
            try {
                File pdf = new File("Dokumentation.pdf");
                if (pdf.exists()) {
                    Desktop.getDesktop().open(pdf);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Dokumentation.pdf nicht gefunden.",
                        "Hilfe", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Fehler beim Offnen: " + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });
        helpMenu.add(docsItem);
        menuBar.add(helpMenu);
        menuBar.add(Box.createHorizontalStrut(6));
        setJMenuBar(menuBar);

        gridPanel = new JPanel(new GridLayout(ROWS, COLS, 10, 10));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        gridPanel.setOpaque(true);
        gridPanel.setBackground(new Color(50, 50, 55));
        getContentPane().setBackground(new Color(50, 50, 55));
        add(gridPanel, BorderLayout.CENTER);

        JLabel versionLabel = new JLabel("V1.0 vom 16.05.26", SwingConstants.CENTER);
        versionLabel.setFont(versionLabel.getFont().deriveFont(9f));
        versionLabel.setForeground(new Color(150, 150, 150));
        versionLabel.setBackground(new Color(50, 50, 55));
        versionLabel.setOpaque(true);
        versionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        add(versionLabel, BorderLayout.SOUTH);

        dragGhost = new JWindow();
        dragGhost.setAlwaysOnTop(true);

        for (int i = 0; i < ROWS * COLS; i++) {
            int gridIdx = i;
            int row = gridIdx / COLS;
            int col = gridIdx % COLS;
            JButton btn = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean empty = Boolean.TRUE.equals(getClientProperty("empty"));
                    if (empty) {
                        g2.setPaint(new Color(55, 55, 60));
                        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                        g2.dispose();
                        super.paintComponent(g);
                        if (isFocusOwner()) drawFocusRing(g, getWidth(), getHeight());
                        return;
                    }
                    boolean pressed = getModel().isPressed();
                    boolean hovered = Boolean.TRUE.equals(getClientProperty("hovered"));
                    Paint bg;
                    if (pressed)
                        bg = new GradientPaint(0, 0, new Color(210, 210, 215), 0, getHeight(), new Color(185, 185, 190));
                    else if (hovered)
                        bg = new GradientPaint(0, 0, new Color(230, 245, 255), 0, getHeight(), new Color(200, 225, 245));
                    else
                        bg = new GradientPaint(0, 0, new Color(248, 248, 250), 0, getHeight(), new Color(225, 225, 230));
                    g2.setPaint(bg);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                    g2.dispose();
                    super.paintComponent(g);
                    if (isFocusOwner()) drawFocusRing(g, getWidth(), getHeight());
                }
            };
            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    ((JComponent)e.getComponent()).putClientProperty("hovered", true);
                    e.getComponent().repaint();
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    ((JComponent)e.getComponent()).putClientProperty("hovered", false);
                    e.getComponent().repaint();
                }
            });
            btn.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
            btn.setFont(btn.getFont().deriveFont(Font.BOLD, 10f));
            btn.setForeground(Color.BLACK);
            btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            btn.setHorizontalTextPosition(SwingConstants.CENTER);
            btn.setIconTextGap(4);
            btn.setOpaque(false);
            btn.setBorder(ROUNDED_BORDER);

            if (row == ROWS - 1 && col == COLS - 1) {
                btn.setText("\u25B6");
                btn.addActionListener(e -> nextPage());
            } else {
                btn.addActionListener(e -> {
                    if (longPressHandled) { longPressHandled = false; return; }
                    if (dragPerformed) { dragPerformed = false; return; }
                    int pageIdx = gridToPageIndex(gridIdx);
                    if (pageIdx < 0) {
                        if (currentFolder != null && currentPage == 0) { leaveFolder(); return; }
                        prevPage(); return;
                    }
                    List<ButtonConfig> btns = currentPageBtns();
                    if (pageIdx < btns.size() && btns.get(pageIdx) != null)
                        execute(btns.get(pageIdx));
                });
                btn.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        if (e.isPopupTrigger()) { showPopup(e, gridIdx); return; }
                        int pageIdx = gridToPageIndex(gridIdx);
                        if (pageIdx < 0) return;
                        if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                            dragSourceIndex = gridIdx;
                            dragStart = e.getPoint();
                            longPressHandled = false;
                            dragPerformed = false;
                            longPressGridIdx = gridIdx;
                            List<ButtonConfig> btns = currentPageBtns();
                            if (pageIdx < btns.size() && btns.get(pageIdx) != null) {
                                ButtonConfig cfg = btns.get(pageIdx);
                                if ("PROGRAM".equals(cfg.getType())
                                    && isAppRunning(extractAppName(cfg.getTarget()).toLowerCase())) {
                                    longPressTimer = new javax.swing.Timer(800, ev -> {
                                        if (longPressGridIdx == gridIdx) {
                                            killApp(cfg.getTarget());
                                            String appKey = extractAppName(cfg.getTarget()).toLowerCase();
                                            btnComponents[gridIdx].setBorder(ROUNDED_BORDER);
                                            killedApps.add(appKey);
                                            javax.swing.Timer cooldown = new javax.swing.Timer(12000, ev2 -> killedApps.remove(appKey));
                                            cooldown.setRepeats(false);
                                            cooldown.start();
                                            longPressHandled = true;
                                        }
                                    });
                                    longPressTimer.setRepeats(false);
                                    longPressTimer.start();
                                }
                            }
                        }
                    }
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent e) {
                        if (longPressTimer != null) { longPressTimer.stop(); longPressTimer = null; }
                        dragGhost.setVisible(false);
                        if (longPressHandled) { longPressHandled = false; return; }
                        if (e.isPopupTrigger()) { showPopup(e, gridIdx); }
                        else if (dragSourceIndex >= 0 && dragStart != null) {
                            if (e.getPoint().distance(dragStart) > DRAG_THRESHOLD) {
                                dragPerformed = true;
                                handleDrop();
                            }
                        }
                        dragSourceIndex = -1;
                        dragStart = null;
                        StreamDeckApp.this.setCursor(Cursor.getDefaultCursor());
                    }
                });
                btn.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(java.awt.event.MouseEvent e) {
                        if (dragSourceIndex < 0 || dragStart == null) return;
                        StreamDeckApp.this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        if (e.getPoint().distance(dragStart) <= DRAG_THRESHOLD) return;
                        if (longPressTimer != null) { longPressTimer.stop(); longPressTimer = null; }
                        if (!dragGhost.isVisible()) {
                            BufferedImage img = new BufferedImage(btn.getWidth(), btn.getHeight(), BufferedImage.TYPE_INT_ARGB);
                            Graphics2D g = img.createGraphics();
                            btn.paint(g);
                            g.dispose();
                            JLabel ghostLabel = new JLabel(new ImageIcon(img));
                            ghostLabel.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 60), 2));
                            dragGhost.getContentPane().removeAll();
                            dragGhost.getContentPane().add(ghostLabel);
                            dragGhost.pack();
                            Point sp = new Point();
                            SwingUtilities.convertPointToScreen(sp, btn);
                            dragGhost.setLocation(sp);
                            dragGhost.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            dragGhost.setVisible(true);
                        }
                        Point sp = e.getLocationOnScreen();
                        dragGhost.setLocation(sp.x - dragGhost.getWidth() / 2, sp.y - dragGhost.getHeight() / 2);
                    }
                });
            }

            btnComponents[i] = btn;
            gridPanel.add(btn);
        }

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        updateAllButtons();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (refreshTimer != null) refreshTimer.stop();
                if (searchKeyDispatcher != null)
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(searchKeyDispatcher);
            }
        });

        searchKeyDispatcher = e -> {
            if (e.getID() == KeyEvent.KEY_TYPED) {
                char c = e.getKeyChar();
                if (!Character.isISOControl(c) && !e.isMetaDown() && !e.isControlDown() && !e.isAltDown() && !searchDialogOpen) {
                    showSearchDialog(String.valueOf(c));
                    return true;
                }
                return false;
            }
            if (e.getID() != KeyEvent.KEY_PRESSED) return false;
            if ((e.isMetaDown() || e.isControlDown()) && e.getKeyCode() == KeyEvent.VK_F) {
                showSearchDialog("");
                return true;
            }
            if (!e.isMetaDown() && !e.isControlDown() && !e.isAltDown()) {
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                int cur = -1;
                for (int i = 0; i < btnComponents.length; i++) {
                    if (btnComponents[i] == focusOwner) { cur = i; break; }
                }
                if (cur >= 0) {
                    int row = cur / COLS;
                    int col = cur % COLS;
                    int target = -1;
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT:  if (col > 0) target = cur - 1; break;
                        case KeyEvent.VK_RIGHT: if (col < COLS - 1) target = cur + 1; break;
                        case KeyEvent.VK_UP:    if (row > 0) target = cur - COLS; break;
                        case KeyEvent.VK_DOWN:  if (row < ROWS - 1) target = cur + COLS; break;
                        case KeyEvent.VK_ENTER: btnComponents[cur].doClick(); return true;
                    }
                    if (target >= 0) {
                        btnComponents[target].requestFocusInWindow();
                        return true;
                    }
                }
            }
            return false;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(searchKeyDispatcher);

        refreshTimer = new javax.swing.Timer(5000, e -> {
            pollRunningApps();
            updateRunningIndicators();
        });
        refreshTimer.start();
        pollRunningApps();
    }

    private int gridToPageIndex(int gridIdx) {
        if (gridIdx == COLS * ROWS - 1) return -1;
        if (currentFolder != null && gridIdx == BOTTOM_ROW_START) return -1;
        int row = gridIdx / COLS;
        int col = gridIdx % COLS;
        if (row < ROWS - 1) return gridIdx;
        if (col == 0) return currentPage == 0 && currentFolder == null ? gridIdx : -1;
        return BOTTOM_ROW_START + col - (currentPage == 0 && currentFolder == null ? 0 : 1);
    }

    private int pageToGridIndex(int pageIdx) {
        if (pageIdx < BOTTOM_ROW_START) return pageIdx;
        if (currentPage == 0 && currentFolder == null) return pageIdx;
        return pageIdx + 1;
    }

    private List<List<ButtonConfig>> currentPageList() {
        return currentFolder != null ? currentFolder.getPages() : pages;
    }

    private List<ButtonConfig> currentPageBtns() {
        List<List<ButtonConfig>> list = currentPageList();
        while (currentPage >= list.size()) list.add(new ArrayList<>());
        return list.get(currentPage);
    }

    private void prevPage() {
        if (currentPage > 0) { currentPage--; updateAllButtons(); }
    }

    private void nextPage() {
        List<List<ButtonConfig>> list = currentPageList();
        if (currentPage + 1 >= list.size()) list.add(new ArrayList<>());
        currentPage++;
        updateAllButtons();
    }

    private void leaveFolder() {
        currentFolder = null;
        currentPage = savedRootPage;
        updateAllButtons();
    }

    private void updateButton(int gridIdx) {
        int row = gridIdx / COLS;
        int col = gridIdx % COLS;
        JButton btn = btnComponents[gridIdx];
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 10f));

        if (row == ROWS - 1 && col == COLS - 1) {
            btn.setFont(btn.getFont().deriveFont(28f));
            btn.setEnabled(true);
            btn.setIcon(null);
            btn.putClientProperty("empty", false);
            return;
        }

        List<ButtonConfig> btns = currentPageBtns();
        int pageIdx = gridToPageIndex(gridIdx);
        if (pageIdx < 0) {
            btn.putClientProperty("empty", false);
            if (currentPage > 0 || currentFolder != null) {
                btn.setFont(btn.getFont().deriveFont(28f));
                btn.setText("\u25C0");
                btn.setToolTipText("Vorherige Seite");
                btn.setEnabled(true);
            } else {
                btn.setText("");
                btn.setToolTipText(null);
                btn.setEnabled(false);
            }
            btn.setIcon(null);
            return;
        }

        ButtonConfig cfg = pageIdx < btns.size() ? btns.get(pageIdx) : null;
        btn.putClientProperty("empty", cfg == null);
        if (cfg != null) {
            btn.setIcon(null);
            btn.setText("<html><center>" + cfg.getLabel().replace("\n", "<br>") + "</center></html>");
            btn.setToolTipText(cfg.getType() + ": " + cfg.getTarget());
            btn.setEnabled(true);
            if ("FOLDER".equals(cfg.getType()))
                btn.setIcon(getFolderIcon());
            else if ("COPY".equals(cfg.getType()))
                btn.setIcon(getCopyIcon());
            else
                loadIconAsync(gridIdx, cfg.getType(), cfg.getTarget());
            if ("PROGRAM".equals(cfg.getType())) {
                String appKey = extractAppName(cfg.getTarget()).toLowerCase();
                btn.setBorder(isAppRunning(appKey) && !killedApps.contains(appKey)
                    ? ROUNDED_BORDER_RUNNING : ROUNDED_BORDER);
            } else {
                btn.setBorder(ROUNDED_BORDER);
            }
        } else {
            btn.setText("");
            btn.setToolTipText(null);
            btn.setIcon(null);
            btn.setEnabled(true);
            btn.setBorder(null);
            btn.putClientProperty("empty", true);
            btn.putClientProperty("hovered", false);
        }
    }

    private void updateAllButtons() {
        if (currentFolder != null)
            setTitle("App Deck - " + currentFolder.getLabel() + " (Seite " + (currentPage + 1) + "/" + Math.max(1, currentFolder.getPages().size()) + ")");
        else
            setTitle("App Deck (Seite " + (currentPage + 1) + "/" + Math.max(1, pages.size()) + ")");
        for (int i = 0; i < ROWS * COLS; i++) updateButton(i);
    }

    private void pollRunningApps() {
        Set<String> apps = new HashSet<>();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                "osascript", "-e",
                "tell application \"System Events\" to get name of every process"
            });
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                for (String name : line.split(",")) {
                    String trimmed = name.trim();
                    if (!trimmed.isEmpty()) apps.add(trimmed.toLowerCase());
                }
            }
            p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        Set<String> cmds = new HashSet<>();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"ps", "-ef"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                cmds.add(line.toLowerCase());
            }
            p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        runningApps = apps;
        runningCmdLines = cmds;
        killedApps.removeIf(k -> !isAppRunning(k));
    }

    private boolean isAppRunning(String appKey) {
        if (runningApps.contains(appKey)) return true;
        return runningCmdLines.stream().anyMatch(cl -> cl.contains(appKey));
    }

    private void updateRunningIndicators() {
        List<ButtonConfig> btns = currentPageBtns();
        for (int i = 0; i < ROWS * COLS; i++) {
            if (gridToPageIndex(i) < 0) continue;
            int pageIdx = gridToPageIndex(i);
            if (pageIdx >= btns.size() || btns.get(pageIdx) == null) continue;
            ButtonConfig cfg = btns.get(pageIdx);
            if (!"PROGRAM".equals(cfg.getType())) continue;

            JButton btn = btnComponents[i];
            String appKey = extractAppName(cfg.getTarget()).toLowerCase();
            btn.setBorder(isAppRunning(appKey) && !killedApps.contains(appKey)
                ? ROUNDED_BORDER_RUNNING : ROUNDED_BORDER);
        }
    }

    private String extractAppName(String target) {
        String t = target.replace("\\ ", " ");
        if (t.startsWith("open -a ")) {
            String name = t.substring(8).trim();
            return name.replaceAll("\\.app$", "");
        }
        if (t.startsWith("open \"")) {
            int end = t.indexOf("\"", 6);
            if (end > 0) return new File(t.substring(6, end)).getName().replaceAll("\\.app$", "");
        }
        if (t.endsWith(".app")) return new File(t).getName().replaceAll("\\.app$", "");
        return new File(t).getName();
    }

    private void killApp(String target) {
        String name = extractAppName(target).replace("\"", "\\\"");
        try {
            new ProcessBuilder("osascript", "-e",
                "tell application \"" + name + "\" to quit").start();
        } catch (IOException ignored) {}
    }

    private void showPopup(java.awt.event.MouseEvent e, int gridIdx) {
        int pageIdx = gridToPageIndex(gridIdx);
        if (pageIdx < 0) return;

        JPopupMenu popup = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Bearbeiten...");
        editItem.addActionListener(ev -> editButton(pageIdx));
        popup.add(editItem);

        JMenuItem folderItem = new JMenuItem("Ordner anlegen");
        folderItem.addActionListener(ev -> {
            List<ButtonConfig> btns = currentPageBtns();
            ButtonConfig folderCfg = new ButtonConfig("Ordner", "FOLDER", "");
            List<List<ButtonConfig>> folderPages = new ArrayList<>();
            folderPages.add(new ArrayList<>());
            folderCfg.setPages(folderPages);
            while (pageIdx >= btns.size()) btns.add(null);
            btns.set(pageIdx, folderCfg);
            saveAndRefresh();
        });
        popup.add(folderItem);

        List<ButtonConfig> btns = currentPageBtns();
        if (pageIdx < btns.size() && btns.get(pageIdx) != null) {
            JMenuItem clearItem = new JMenuItem("Entfernen");
            clearItem.addActionListener(ev -> {
                btns.set(pageIdx, null);
                saveAndRefresh();
            });
            popup.add(clearItem);
        }

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private void editButton(int pageIdx) {
        List<ButtonConfig> btns = currentPageBtns();
        ButtonConfig cfg = pageIdx < btns.size() && btns.get(pageIdx) != null
            ? btns.get(pageIdx) : new ButtonConfig("", "URL", "");

        JTextField labelField = new JTextField(cfg.getLabel(), 20);
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"URL", "PROGRAM", "FOLDER", "COPY"});
        typeBox.setSelectedItem(cfg.getType());

        JTextArea targetArea = new JTextArea(cfg.getTarget(), 2, 20);
        targetArea.setLineWrap(true);
        targetArea.setWrapStyleWord(true);
        targetArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void check() {
                String text = targetArea.getText().trim();
                if (text.isEmpty()) return;
                if ("FOLDER".equals(typeBox.getSelectedItem()) || "COPY".equals(typeBox.getSelectedItem())) return;
                if (text.startsWith("http://") || text.startsWith("https://")) {
                    typeBox.setSelectedItem("URL");
                    String suggested = suggestLabel(text);
                    if (suggested != null) labelField.setText(suggested);
                } else if (text.startsWith("file://")) {
                    typeBox.setSelectedItem("URL");
                    String suggested = suggestLabel(text);
                    if (suggested != null) labelField.setText(suggested);
                } else if (text.startsWith("open -a ")) {
                    typeBox.setSelectedItem("PROGRAM");
                    String name = text.substring(8).trim().replace("\\ ", " ");
                    if (!name.isEmpty()) labelField.setText(name.replaceAll("\\.app$", ""));
                } else if (text.startsWith("open \"")) {
                    typeBox.setSelectedItem("PROGRAM");
                    int end = text.indexOf("\"", 6);
                    if (end > 0) labelField.setText(new File(text.substring(6, end)).getName().replaceAll("\\.app$", ""));
                } else if (text.endsWith(".app")) {
                    typeBox.setSelectedItem("PROGRAM");
                    labelField.setText(new File(text.replace("\\ ", " ")).getName().replaceAll("\\.app$", ""));
                }
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { check(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { check(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { check(); }
        });

        JScrollPane targetScroll = new JScrollPane(targetArea);
        targetScroll.setPreferredSize(new Dimension(200, 40));

        JButton browseBtn = new JButton("...");
        browseBtn.setPreferredSize(new Dimension(32, 26));
        browseBtn.setMargin(new Insets(0, 0, 0, 0));
        browseBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser("/Applications");
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setDialogTitle("Programm auswählen");
            StringBuilder prefix = new StringBuilder();
            javax.swing.Timer resetTimer = new javax.swing.Timer(1200, e -> prefix.setLength(0));
            resetTimer.setRepeats(false);
            java.awt.KeyEventDispatcher ked = e -> {
                if (e.getID() != KeyEvent.KEY_TYPED) return false;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner == null || !SwingUtilities.isDescendingFrom(focusOwner, fc)) return false;
                char c = e.getKeyChar();
                if (!Character.isLetterOrDigit(c) && c != '-' && c != ' ' && c != '.') return false;
                prefix.append(c);
                resetTimer.restart();
                File[] files = fc.getCurrentDirectory().listFiles();
                if (files != null) {
                    String search = prefix.toString().toLowerCase();
                    for (File f : files) {
                        if (f.getName().toLowerCase().startsWith(search)) {
                            fc.setSelectedFile(f);
                            fc.ensureFileIsVisible(f);
                            break;
                        }
                    }
                }
                return true;
            };
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ked);
            int result = fc.showOpenDialog(this);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(ked);
            if (result == JFileChooser.APPROVE_OPTION) {
                String path = fc.getSelectedFile().getAbsolutePath();
                if (path.endsWith(".app")) {
                    typeBox.setSelectedItem("PROGRAM");
                    targetArea.setText("open \"" + path + "\"");
                } else {
                    typeBox.setSelectedItem("URL");
                    targetArea.setText("file://" + path);
                }
            }
        });

        typeBox.addActionListener(ev -> {
            boolean isFolder = "FOLDER".equals(typeBox.getSelectedItem());
            boolean isCopy = "COPY".equals(typeBox.getSelectedItem());
            targetArea.setEnabled(!isFolder);
            targetArea.setRows(isCopy ? 5 : 2);
            browseBtn.setVisible(!isFolder && !isCopy);
            targetScroll.setPreferredSize(new Dimension(200, isCopy ? 90 : 40));
        });

        JPanel targetPanel = new JPanel(new BorderLayout(4, 0));
        targetPanel.add(targetScroll, BorderLayout.CENTER);
        targetPanel.add(browseBtn, BorderLayout.EAST);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Label:"), gbc);
        gbc.gridx = 1;
        panel.add(labelField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Typ:"), gbc);
        gbc.gridx = 1;
        panel.add(typeBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Ziel:"), gbc);
        gbc.gridx = 1;
        panel.add(targetPanel, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Button konfigurieren", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            ButtonConfig updated = new ButtonConfig(
                labelField.getText(), (String) typeBox.getSelectedItem(), targetArea.getText());
            if ("FOLDER".equals(updated.getType())) {
                if (cfg.getPages() != null)
                    updated.setPages(cfg.getPages());
                else {
                    List<List<ButtonConfig>> folderPages = new ArrayList<>();
                    folderPages.add(new ArrayList<>());
                    updated.setPages(folderPages);
                }
            }
            while (pageIdx >= btns.size()) btns.add(null);
            btns.set(pageIdx, updated);
            saveAndRefresh();
        }
    }

    private void handleDrop() {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mousePos, gridPanel);
        Component target = gridPanel.getComponentAt(mousePos);
        int targetGrid = -1;
        for (int i = 0; i < btnComponents.length; i++) {
            if (btnComponents[i] == target) { targetGrid = i; break; }
        }

        int srcIdx = gridToPageIndex(dragSourceIndex);
        int tgtIdx = gridToPageIndex(targetGrid);
        if (srcIdx < 0 && dragSourceIndex >= 0) return;

        List<ButtonConfig> btns = currentPageBtns();
        while (srcIdx >= btns.size()) btns.add(null);
        ButtonConfig sourceCfg = btns.get(srcIdx);
        if (sourceCfg == null) return;

        if (tgtIdx < 0) {
            if (targetGrid == BOTTOM_ROW_START) {
                int targetPage = currentPage - 1;
                List<List<ButtonConfig>> targetList = currentPageList();
                if (targetPage < 0) {
                    if (currentFolder == null) return;
                    targetPage = savedRootPage;
                    targetList = pages;
                }
                btns.set(srcIdx, null);
                while (targetPage >= targetList.size()) targetList.add(new ArrayList<>());
                List<ButtonConfig> prevBtns = targetList.get(targetPage);
                int emptyIdx = -1;
                for (int i = 0; i < prevBtns.size(); i++) {
                    if (prevBtns.get(i) == null) { emptyIdx = i; break; }
                }
                if (emptyIdx >= 0) prevBtns.set(emptyIdx, sourceCfg);
                else prevBtns.add(sourceCfg);
                try { ConfigLoader.save(configPath, pages); } catch (IOException ex) {}
                if (currentPage == 0 && currentFolder != null) { leaveFolder(); return; }
                iconCache.clear();
                updateAllButtons();
            }
            return;
        }

        if (srcIdx == tgtIdx) return;

        List<ButtonConfig> tgtBtns = currentPageBtns();
        while (tgtIdx >= tgtBtns.size()) tgtBtns.add(null);
        ButtonConfig targetCfg = tgtBtns.get(tgtIdx);

        if ("FOLDER".equals(targetCfg != null ? targetCfg.getType() : null)) {
            List<List<ButtonConfig>> folderPages = targetCfg.getPages();
            if (folderPages == null) {
                folderPages = new ArrayList<>();
                targetCfg.setPages(folderPages);
            }
            if (folderPages.isEmpty()) folderPages.add(new ArrayList<>());
            List<ButtonConfig> firstPage = folderPages.get(0);
            int emptyIdx = -1;
            for (int i = 0; i < firstPage.size(); i++) {
                if (firstPage.get(i) == null) { emptyIdx = i; break; }
            }
            if (emptyIdx >= 0) firstPage.set(emptyIdx, sourceCfg);
            else firstPage.add(sourceCfg);
            btns.set(srcIdx, null);
        } else {
            ButtonConfig temp = btns.get(srcIdx);
            btns.set(srcIdx, btns.get(tgtIdx));
            btns.set(tgtIdx, temp);
        }

        updateButton(dragSourceIndex);
        updateButton(targetGrid);

        try {
            ConfigLoader.save(configPath, pages);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Fehler beim Speichern: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAndRefresh() {
        try {
            ConfigLoader.save(configPath, pages);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Fehler beim Speichern: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        iconCache.clear();
        updateAllButtons();
    }

    private void execute(ButtonConfig cfg) {
        if ("FOLDER".equals(cfg.getType())) {
            if (currentFolder != null) return;
            savedRootPage = currentPage;
            currentPage = 0;
            currentFolder = cfg;
            updateAllButtons();
            return;
        }
        if ("COPY".equals(cfg.getType())) {
            StringSelection sel = new StringSelection(cfg.getTarget());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            int gridIdx = -1;
            List<ButtonConfig> btns = currentPageBtns();
            for (int i = 0; i < btnComponents.length; i++) {
                int pi = gridToPageIndex(i);
                if (pi >= 0 && pi < btns.size() && btns.get(pi) == cfg) { gridIdx = i; break; }
            }
            if (gridIdx >= 0) {
                JButton fb = btnComponents[gridIdx];
                String orig = fb.getText();
                fb.setText("Kopiert!");
                javax.swing.Timer reset = new javax.swing.Timer(1200, ev -> { fb.setText(orig); });
                reset.setRepeats(false);
                reset.start();
            }
            return;
        }
        try {
            switch (cfg.getType().toUpperCase()) {
                case "URL" -> Desktop.getDesktop().browse(URI.create(cfg.getTarget()));
                case "PROGRAM" -> Runtime.getRuntime().exec(parseCommand(cfg.getTarget()));
                default -> JOptionPane.showMessageDialog(this, "Unknown type: " + cfg.getType());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSearchDialog(String initialText) {
        List<Object[]> allEntries = new ArrayList<>();
        for (int p = 0; p < pages.size(); p++) {
            List<ButtonConfig> page = pages.get(p);
            for (int i = 0; i < page.size(); i++) {
                ButtonConfig cfg = page.get(i);
                if (cfg != null) {
                    allEntries.add(new Object[]{cfg, p, null, i});
                    if ("FOLDER".equals(cfg.getType()) && cfg.getPages() != null) {
                        for (int fp = 0; fp < cfg.getPages().size(); fp++) {
                            List<ButtonConfig> fpage = cfg.getPages().get(fp);
                            for (int fi = 0; fi < fpage.size(); fi++) {
                                ButtonConfig fcfg = fpage.get(fi);
                                if (fcfg != null)
                                    allEntries.add(new Object[]{fcfg, p, cfg, fp, fi});
                            }
                        }
                    }
                }
            }
        }

        if (allEntries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keine konfigurierten Buttons vorhanden.", "Suche", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JTextField searchField = new JTextField(initialText, 20);
        if (!initialText.isEmpty()) {
            searchField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    searchField.setCaretPosition(searchField.getText().length());
                    searchField.removeFocusListener(this);
                }
            });
        }
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> resultList = new JList<>(listModel);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setFont(new Font("Dialog", Font.PLAIN, 12));

        List<Object[]> filtered = new ArrayList<>();

        Runnable filterAction = () -> {
            String q = searchField.getText().toLowerCase();
            listModel.clear();
            filtered.clear();
            for (Object[] entry : allEntries) {
                ButtonConfig cfg = (ButtonConfig) entry[0];
                if (cfg.getLabel().toLowerCase().contains(q) || cfg.getTarget().toLowerCase().contains(q)) {
                    filtered.add(entry);
                    int rootPage = (int) entry[1];
                    String loc = entry[2] != null
                        ? "S." + (rootPage + 1) + " > " + ((ButtonConfig) entry[2]).getLabel() + " > S." + ((int) entry[4] + 1)
                        : "S." + (rootPage + 1);
                    listModel.addElement(cfg.getLabel() + "  (" + loc + ")");
                }
            }
        };

        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) { filterAction.run(); }
        });

        JPanel dialogPanel = new JPanel(new BorderLayout(6, 6));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        dialogPanel.add(searchField, BorderLayout.NORTH);
        dialogPanel.add(new JScrollPane(resultList), BorderLayout.CENTER);

        JDialog dialog = new JDialog(this, "Suche (Esc schließen)", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.add(dialogPanel);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        dialog.getRootPane().getActionMap().put("close", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { dialog.dispose(); }
        });
        dialog.pack();
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);

        resultList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && !filtered.isEmpty()) {
                    int idx = resultList.getSelectedIndex();
                    if (idx >= 0 && idx < filtered.size()) navigateToResult(filtered.get(idx));
                    dialog.dispose();
                }
            }
        });

        java.awt.event.KeyAdapter listNav = new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP) {
                    if (listModel.size() == 0) return;
                    e.consume();
                    int idx = resultList.getSelectedIndex();
                    if (idx < 0) idx = 0;
                    else if (e.getKeyCode() == KeyEvent.VK_UP && idx > 0) idx--;
                    else if (e.getKeyCode() == KeyEvent.VK_DOWN && idx < listModel.size() - 1) idx++;
                    resultList.setSelectedIndex(idx);
                    resultList.ensureIndexIsVisible(idx);
                    resultList.requestFocusInWindow();
                }
            }
        };
        searchField.addKeyListener(listNav);

        resultList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP && resultList.getSelectedIndex() <= 0) {
                    searchField.requestFocusInWindow();
                    searchField.selectAll();
                }
            }
        });

        java.awt.event.KeyAdapter enterKey = new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !filtered.isEmpty()) {
                    int idx = resultList.getSelectedIndex();
                    if (idx < 0) idx = 0;
                    if (idx < filtered.size()) navigateToResult(filtered.get(idx));
                    dialog.dispose();
                }
            }
        };
        resultList.addKeyListener(enterKey);
        searchField.addKeyListener(enterKey);

        if (!initialText.isEmpty()) {
            filterAction.run();
        }

        searchDialogOpen = true;
        dialog.setVisible(true);
        searchDialogOpen = false;
    }

    private void navigateToResult(Object[] entry) {
        ButtonConfig cfg = (ButtonConfig) entry[0];
        int rootPage = (int) entry[1];
        ButtonConfig folderCfg = (ButtonConfig) entry[2];

        if (folderCfg != null) {
            savedRootPage = rootPage;
            currentPage = 0;
            currentFolder = folderCfg;
        } else {
            currentFolder = null;
            currentPage = rootPage;
        }
        updateAllButtons();

        List<ButtonConfig> btns = currentPageBtns();
        int pageIdx = entry.length == 5 ? (int) entry[4] : (int) entry[3];
        int gridIdx = pageToGridIndex(pageIdx);
        if (gridIdx >= 0) {
            JButton btn = btnComponents[gridIdx];
            Border origBorder = btn.getBorder();
            Border yellowBorder = BorderFactory.createLineBorder(new Color(255, 200, 0), 4);
            final boolean[] visible = {true};
            javax.swing.Timer blink = new javax.swing.Timer(400, ev -> {
                visible[0] = !visible[0];
                btn.setBorder(visible[0] ? yellowBorder : origBorder);
            });
            blink.start();
            javax.swing.Timer reset = new javax.swing.Timer(5000, ev -> {
                blink.stop();
                if (currentFolder != null || currentPage == rootPage) {
                    String appKey = "PROGRAM".equals(cfg.getType()) ? extractAppName(cfg.getTarget()).toLowerCase() : null;
                    btn.setBorder(appKey != null && isAppRunning(appKey) && !killedApps.contains(appKey)
                        ? ROUNDED_BORDER_RUNNING : ROUNDED_BORDER);
                }
            });
            reset.setRepeats(false);
            reset.start();
        }
    }

    private String[] parseCommand(String cmd) {
        List<String> args = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < cmd.length(); i++) {
            char c = cmd.charAt(i);
            if (c == '"') inQuote = !inQuote;
            else if (c == ' ' && !inQuote) {
                if (!current.isEmpty()) { args.add(current.toString()); current.setLength(0); }
            } else current.append(c);
        }
        if (!current.isEmpty()) args.add(current.toString());
        return args.toArray(new String[0]);
    }

    private void loadIconAsync(int gridIdx, String type, String target) {
        String cacheKey = type + "::" + target;
        Icon cached = iconCache.get(cacheKey);
        if (cached != null) {
            btnComponents[gridIdx].setIcon(cached);
            return;
        }
        new Thread(() -> {
            Icon icon = resolveIcon(type, target);
            if (icon != null) {
                SwingUtilities.invokeLater(() -> {
                    List<ButtonConfig> btns = currentPageBtns();
                    int pageIdx = gridToPageIndex(gridIdx);
                    ButtonConfig cfg = pageIdx >= 0 && pageIdx < btns.size() ? btns.get(pageIdx) : null;
                    String currentKey = cfg != null ? cfg.getType() + "::" + cfg.getTarget() : "";
                    if (currentKey.equals(cacheKey))
                        btnComponents[gridIdx].setIcon(icon);
                });
            }
        }).start();
    }

    private Icon resolveIcon(String type, String target) {
        String cacheKey = type + "::" + target;
        Icon cached = iconCache.get(cacheKey);
        if (cached != null) return cached;
        Icon icon = switch (type) {
            case "PROGRAM" -> resolveProgramIcon(target);
            case "URL" -> resolveFavicon(target);
            default -> null;
        };
        if (icon != null) iconCache.put(cacheKey, icon);
        else {
            icon = getGlobeIcon();
            iconCache.put(cacheKey, icon);
        }
        return icon;
    }

    private Icon resolveProgramIcon(String target) {
        File file = resolveFile(target);
        if (file == null || !file.exists()) return null;
        Image iconImage = loadSvgFromDir(file.getParentFile());
        if (iconImage == null && file.getName().endsWith(".app")) iconImage = extractMacAppIcon(file);
        if (iconImage == null) iconImage = getNativeIcon(file);
        if (iconImage == null) {
            Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);
            if (icon != null && icon.getIconWidth() > 0) {
                BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = bi.createGraphics();
                icon.paintIcon(null, g, 0, 0);
                g.dispose();
                iconImage = bi;
            }
        }
        if (iconImage == null) return null;
        return new ImageIcon(iconImage.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));
    }

    private Image extractMacAppIcon(File appBundle) {
        try {
            File resources = new File(appBundle, "Contents/Resources");
            if (!resources.isDirectory()) return null;
            File[] icnsFiles = resources.listFiles((dir, name) -> name.endsWith(".icns"));
            if (icnsFiles == null || icnsFiles.length == 0) return null;
            File icnsFile = icnsFiles[0];
            String tag = Integer.toHexString(icnsFile.getAbsolutePath().hashCode());
            File pngFile = new File(System.getProperty("java.io.tmpdir"),
                "sd_icon_" + tag + "_" + icnsFile.getName() + ".png");
            ProcessBuilder pb = new ProcessBuilder("sips", "-s", "format", "png",
                icnsFile.getAbsolutePath(), "--out", pngFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (pngFile.isFile() && pngFile.length() > 0) {
                Image img = ImageIO.read(pngFile);
                pngFile.delete();
                return img;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Image loadSvgFromDir(File dir) {
        if (dir == null || !dir.isDirectory()) return null;
        File svg = new File(dir, "favicon.svg");
        if (!svg.isFile()) return null;
        try {
            String tag = Integer.toHexString(svg.getAbsolutePath().hashCode());
            File png = new File(System.getProperty("java.io.tmpdir"),
                "sd_svg_" + tag + ".png");
            if (!png.isFile() || png.length() == 0) {
                ProcessBuilder pb = new ProcessBuilder("sips", "-s", "format", "png",
                    "--resampleWidth", Integer.toString(ICON_SIZE * 2),
                    svg.getAbsolutePath(), "--out", png.getAbsolutePath());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                if (!png.isFile() || png.length() == 0) return null;
            }
            return ImageIO.read(png);
        } catch (Exception ignored) { return null; }
    }

    private Icon resolveFavicon(String url) {
        try {
            if (url.startsWith("file://")) {
                File f = new File(url.substring(7));
                Image svgImg = loadSvgFromDir(f.getParentFile());
                if (svgImg != null) return new ImageIcon(svgImg.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));
            }
            String domain = new URI(url).getHost();
            if (domain == null || domain.isEmpty()) return null;
            Image img = ImageIO.read(new URL("https://www.google.com/s2/favicons?domain=" + domain + "&sz=64"));
            if (img == null) return null;
            return new ImageIcon(img.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));
        } catch (Exception ignored) { return null; }
    }

    private String suggestLabel(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host != null) {
                String[] parts = host.split("\\.");
                String mainDomain = parts.length >= 2 ? parts[parts.length - 2] : parts[0];
                String subdomain = parts.length > 2 ? parts[0] : null;
                String name = Character.toUpperCase(mainDomain.charAt(0)) + mainDomain.substring(1);
                if (subdomain != null && !subdomain.equals("www"))
                    name += " " + Character.toUpperCase(subdomain.charAt(0)) + subdomain.substring(1);
                if (path != null && !path.isEmpty() && !path.equals("/")) {
                    String segment = path.replaceAll("^/+|/+$", "");
                    if (!segment.isEmpty()) {
                        String last = segment.contains("/") ? segment.substring(segment.lastIndexOf('/') + 1) : segment;
                        if (!last.isEmpty()) name += " " + Character.toUpperCase(last.charAt(0)) + last.substring(1);
                    }
                }
                return name;
            } else if (path != null && !path.isEmpty()) {
                String segment = path.replaceAll("^/+|/+$", "");
                String last = segment.contains("/") ? segment.substring(segment.lastIndexOf('/') + 1) : segment;
                String parent = segment.contains("/") ? segment.substring(0, segment.lastIndexOf('/')) : "";
                if (!last.isEmpty()) {
                    if (!parent.isEmpty() && last.matches("(?i)(index|default)\\.[a-z0-9]+")) {
                        String dir = parent.contains("/") ? parent.substring(parent.lastIndexOf('/') + 1) : parent;
                        if (!dir.isEmpty()) return Character.toUpperCase(dir.charAt(0)) + dir.substring(1);
                    }
                    last = last.replaceAll("\\.[^.]+$", "");
                    return Character.toUpperCase(last.charAt(0)) + last.substring(1);
                }
            }
            return null;
        } catch (Exception ignored) { return null; }
    }

    @SuppressWarnings("unchecked")
    private Image getNativeIcon(File file) {
        try {
            Class<?> sfClass = Class.forName("sun.awt.shell.ShellFolder");
            java.lang.reflect.Method getSF = sfClass.getMethod("getShellFolder", File.class);
            Object sf = getSF.invoke(null, file);
            java.lang.reflect.Method getIcon = sfClass.getMethod("getIcon", boolean.class);
            return (Image) getIcon.invoke(sf, Boolean.TRUE);
        } catch (Exception ignored) { return null; }
    }

    private File resolveFile(String target) {
        String path = target.replace("\\ ", " ");
        File f = new File(path);
        if (f.exists()) return f;
        if (path.startsWith("open ")) {
            String rest = path.substring(5).trim();
            if (rest.startsWith("\"")) {
                int end = rest.indexOf("\"", 1);
                if (end > 0) { f = new File(rest.substring(1, end)); if (f.exists()) return f; }
            } else if (rest.startsWith("-a ")) {
                String appName = rest.substring(3).trim();
                f = new File("/Applications/" + appName + ".app");
                if (f.exists()) return f;
                f = new File("/System/Applications/" + appName + ".app");
                if (f.exists()) return f;
            } else {
                f = new File(rest);
                if (f.exists()) return f;
            }
        }
        return null;
    }

    private static final Border ROUNDED_BORDER = new RoundedShadowBorder(new Color(170, 170, 175), 1f);
    private static final Border ROUNDED_BORDER_RUNNING = new RoundedShadowBorder(new Color(0, 180, 0), 4f);
    private static Icon globeIcon;
    private static Icon folderIcon;
    private static Icon copyIcon;

    private static Icon getGlobeIcon() {
        if (globeIcon == null) globeIcon = new ImageIcon(createGlobeImage());
        return globeIcon;
    }

    private static Icon getFolderIcon() {
        if (folderIcon == null) folderIcon = new ImageIcon(createFolderImage());
        return folderIcon;
    }

    private static Icon getCopyIcon() {
        if (copyIcon == null) copyIcon = new ImageIcon(createCopyImage());
        return copyIcon;
    }

    private static BufferedImage createGlobeImage() {
        int s = ICON_SIZE;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(70, 140, 220));
        g.fillOval(2, 2, s - 4, s - 4);

        g.setColor(new Color(255, 255, 255, 100));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(2, s / 4, s - 4, (s - 4) / 2);
        g.drawOval(s / 4, 2, (s - 4) / 2, s - 4);

        g.setStroke(new BasicStroke(1.8f));
        g.setColor(new Color(50, 110, 190));
        g.drawOval(2, 2, s - 4, s - 4);

        g.dispose();
        return img;
    }

    private static BufferedImage createFolderImage() {
        int s = ICON_SIZE;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int pad = 4, tabW = 14, tabH = 6;
        GradientPaint gp = new GradientPaint(0, pad + tabH, new Color(245, 215, 85),
            0, s - pad, new Color(210, 175, 50));
        g.setPaint(gp);
        g.fillRoundRect(pad + 2, pad + tabH - 3, s - pad * 2 - 2, s - pad - tabH, 6, 6);

        g.setColor(new Color(215, 185, 55));
        g.fillRoundRect(pad + 5, pad, tabW, tabH, 4, 4);

        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(170, 130, 20));
        g.drawRoundRect(pad + 5, pad, tabW, tabH, 4, 4);
        g.drawRoundRect(pad + 2, pad + tabH - 3, s - pad * 2 - 2, s - pad - tabH, 6, 6);

        g.dispose();
        return img;
    }

    private static BufferedImage createCopyImage() {
        int s = ICON_SIZE;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int p = 5, clipW = 6, clipH = 5;
        g.setColor(new Color(200, 200, 210));
        g.fillRoundRect(p + (s - p * 2 - clipW) / 2, p - 2, clipW, clipH, 2, 2);

        g.setColor(new Color(100, 120, 200));
        g.fillRoundRect(p, p + clipH - 2, s - p * 2, s - p - clipH + 2, 5, 5);

        g.setColor(new Color(230, 240, 255));
        g.setStroke(new BasicStroke(2f));
        int lx = p + 6, ly = p + clipH + 4;
        for (int i = 0; i < 3; i++) {
            int y = ly + i * 8;
            g.drawLine(lx, y, s - lx, y);
        }
        g.drawLine(lx, ly + 24, s - lx - 6, ly + 24);

        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(80, 100, 170));
        g.drawRoundRect(p, p + clipH - 2, s - p * 2, s - p - clipH + 2, 5, 5);

        g.dispose();
        return img;
    }

    private void drawFocusRing(Graphics g, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(80, 140, 230, 180));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(3, 3, w - 7, h - 7, 12, 12);
        g2.dispose();
    }

    private static class RoundedShadowBorder extends AbstractBorder {
        private static final int ARC = 14;
        private static final int SHADOW = 3;
        private final Color lineColor;
        private final float strokeWidth;

        RoundedShadowBorder(Color lineColor, float strokeWidth) {
            this.lineColor = lineColor;
            this.strokeWidth = strokeWidth;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(0, 0, 0, 35));
            g2.fillRoundRect(x + SHADOW, y + SHADOW, w - SHADOW - 1, h - SHADOW - 1, ARC, ARC);

            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(strokeWidth));
            int inset = (int) Math.ceil(strokeWidth / 2);
            g2.drawRoundRect(x + inset, y + inset, w - 1 - inset * 2, h - 1 - inset * 2, ARC, ARC);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 4, 4 + SHADOW, 4 + SHADOW);
        }

        @Override
        public boolean isBorderOpaque() { return false; }
    }

    private Image loadAppIcon() {
        try {
            InputStream is = getClass().getResourceAsStream("/streamdeck/app-icon.png");
            if (is != null) return ImageIO.read(is);
        } catch (Exception ignored) {}
        return createAppIcon();
    }

    private BufferedImage createAppIcon() {
        int s = 256;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(22, 22, 34));
        g.fillRoundRect(8, 8, s - 16, s - 16, 40, 40);

        int cols = 5, rows = 3, gap = 12;
        int cw = (s - 16 - gap * (cols + 1)) / cols;
        int ch = (s - 16 - gap * (rows + 1)) / rows;
        int ox = 8 + gap, oy = 8 + gap;

        int[][] colors = {
            {0x4A90D9, 0x50C878, 0xE8A040, 0xD95959, 0x9B59B6},
            {0x1ABC9C, 0xE67E22, 0x3498DB, 0xE74C3C, 0x2ECC71},
            {0xF39C12, 0x2980B9, 0x8E44AD, 0x27AE60, 0xD35400}
        };

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = ox + c * (cw + gap);
                int y = oy + r * (ch + gap);
                Color base = new Color(colors[r][c]);
                GradientPaint gp = new GradientPaint(x, y, base.brighter(), x, y + ch, base.darker());
                g.setPaint(gp);
                g.fillRoundRect(x, y, cw, ch, 8, 8);
            }
        }

        g.dispose();
        return img;
    }

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : "config.json";

        if (!new File(configPath).exists()) {
            String homeCfg = System.getProperty("user.home")
                + "/Library/Application Support/App Deck/config.json";
            if (new File(homeCfg).exists()) {
                configPath = homeCfg;
            } else {
                try {
                    new File(homeCfg).getParentFile().mkdirs();
                    ConfigLoader.save(homeCfg, List.of(new java.util.ArrayList<>()));
                    configPath = homeCfg;
                } catch (IOException e2) {
                    JOptionPane.showMessageDialog(null,
                        "Config not found: " + configPath
                            + "\nCould not create default config.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        List<List<ButtonConfig>> pages;
        try {
            pages = ConfigLoader.load(configPath);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                "Error loading config: " + configPath + "\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String finalConfigPath = configPath;
        SwingUtilities.invokeLater(() -> new StreamDeckApp(pages, finalConfigPath).setVisible(true));
    }
}
