package ui;

import ai.KnowledgeMap;
import control.GameController;
import control.HunterCommand;
import model.Direction;
import model.Hunter;
import model.Percepts;
import model.Position;
import model.WorldCell;
import model.WorldGenerator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.List;

/** Главное окно тёмной темы для World of Wumpus. */
@SuppressWarnings("serial")
public final class MainWindow extends JFrame {
    private static final Color COLOR_APP = new Color(31, 34, 37);
    private static final Color COLOR_PANEL = new Color(41, 45, 49);
    private static final Color COLOR_PANEL_ALT = new Color(48, 52, 56);
    private static final Color COLOR_INPUT = new Color(56, 60, 64);
    private static final Color COLOR_BORDER = new Color(67, 72, 77);
    private static final Color COLOR_TEXT = new Color(224, 227, 230);
    private static final Color COLOR_MUTED = new Color(166, 173, 179);

    private static final Color COLOR_UNKNOWN = new Color(51, 55, 59);
    private static final Color COLOR_PIT_SUSPECT = new Color(79, 72, 43);
    private static final Color COLOR_WUMPUS_SUSPECT = new Color(79, 51, 54);
    private static final Color COLOR_BOTH_SUSPECT = new Color(88, 63, 39);
    private static final Color COLOR_OK = new Color(45, 64, 81);
    private static final Color COLOR_PROVEN_SAFE = new Color(52, 67, 54);
    private static final Color COLOR_KNOWN_PIT = new Color(88, 51, 45);
    private static final Color COLOR_KNOWN_WUMPUS = new Color(91, 43, 48);
    private static final Color COLOR_HUNTER = new Color(47, 70, 55);
    private static final Color COLOR_ACTIVE_SENSOR = new Color(49, 71, 55);
    private static final Color COLOR_INACTIVE_SENSOR = new Color(53, 57, 61);

    private static final Color FG_UNKNOWN = new Color(198, 203, 208);
    private static final Color FG_PIT_SUSPECT = new Color(222, 201, 132);
    private static final Color FG_WUMPUS_SUSPECT = new Color(224, 155, 154);
    private static final Color FG_BOTH_SUSPECT = new Color(230, 179, 119);
    private static final Color FG_OK = new Color(143, 183, 225);
    private static final Color FG_PROVEN_SAFE = new Color(166, 201, 157);
    private static final Color FG_KNOWN_PIT = new Color(230, 151, 130);
    private static final Color FG_KNOWN_WUMPUS = new Color(232, 143, 148);
    private static final Color FG_HUNTER = new Color(177, 211, 177);
    private static final Color FG_GOLD = new Color(226, 202, 119);

    private final GameController controller;
    private final JPanel boardPanel = new JPanel();
    private JLabel[][] mainLabels;
    private JLabel[][] detailLabels;
    private final JLabel coordinateValue = new JLabel("—");
    private final JLabel directionValue = new JLabel("—");
    private final JLabel arrowValue = new JLabel("—");
    private final JLabel aliveValue = new JLabel("—");
    private final JLabel goldValue = new JLabel("—");
    private final JLabel wumpusValue = new JLabel("—");
    private final JLabel scoreValue = new JLabel("0");
    private final JLabel statusValue = new JLabel("—");
    private final JLabel breezeSensor = new JLabel("Ветерок: НЕТ");
    private final JLabel stenchSensor = new JLabel("Запах: НЕТ");
    private final JLabel glitterSensor = new JLabel("Блеск: НЕТ");
    private final JLabel bumpSensor = new JLabel("Удар: НЕТ");
    private final JLabel screamSensor = new JLabel("Крик: НЕТ");
    private final JTextArea logArea = new JTextArea();

    private final JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(8, 2, 40, 1));
    private final JCheckBox percentPitsCheck = new JCheckBox("15% ям по умолчанию", true);
    private final SpinnerNumberModel pitCountModel = new SpinnerNumberModel(10, 0, 31, 1);
    private final JSpinner pitCountSpinner = new JSpinner(pitCountModel);
    private final JCheckBox observerCheck = new JCheckBox("Показать скрытую карту (наблюдатель)");
    private final JComboBox<Integer> speedCombo = new JComboBox<Integer>(new Integer[]{0, 250, 500, 750, 1000});
    private final JButton autoButton = new JButton("Автоизучение");
    private final JButton pauseButton = new JButton("Пауза");
    private final Timer autoTimer;

    public MainWindow() {
        super("World of Wumpus — лабораторная работа Java");
        configureDarkUiDefaults();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setMinimumSize(new Dimension(950, 650));
        setPreferredSize(new Dimension(Math.max(950, Math.min(1320, screen.width - 60)),
                Math.max(650, Math.min(860, screen.height - 80))));
        setLayout(new BorderLayout(8, 8));

        controller = new GameController(8, null);
        autoTimer = new Timer(500, event -> {
            boolean continueAuto = controller.autoStep();
            refreshView();
            if (!continueAuto) stopAutoMode();
        });
        autoTimer.setCoalesce(true);

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildMainArea(), BorderLayout.CENTER);
        add(buildBottomStatus(), BorderLayout.SOUTH);
        wireEvents();
        applyDarkComponentTree(getContentPane());
        rebuildBoard();
        refreshView();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildTopPanel() {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBorder(new EmptyBorder(7, 8, 4, 8));

        JPanel mapSettings = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        mapSettings.add(new JLabel("Размер поля N×N:"));
        sizeSpinner.setPreferredSize(new Dimension(70, 28));
        mapSettings.add(sizeSpinner);
        mapSettings.add(percentPitsCheck);
        mapSettings.add(new JLabel("Количество ям:"));
        pitCountSpinner.setPreferredSize(new Dimension(78, 28));
        pitCountSpinner.setEnabled(false);
        mapSettings.add(pitCountSpinner);
        JButton newMapButton = new JButton("Создать новое поле");
        newMapButton.addActionListener(e -> createNewMap());
        mapSettings.add(newMapButton);
        mapSettings.add(observerCheck);

        JPanel autoSettings = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        autoSettings.add(new JLabel("Скорость автоизучения:"));
        speedCombo.setSelectedItem(500);
        speedCombo.setPreferredSize(new Dimension(112, 29));
        speedCombo.setMinimumSize(new Dimension(112, 29));
        autoSettings.add(speedCombo);
        autoSettings.add(new JLabel("мс между действиями"));
        autoSettings.add(autoButton);
        autoSettings.add(pauseButton);

        outer.add(mapSettings);
        outer.add(autoSettings);
        return outer;
    }

    private JSplitPane buildMainArea() {
        boardPanel.setBorder(BorderFactory.createTitledBorder("Карта пещеры"));
        JScrollPane boardScroll = new JScrollPane(boardPanel);
        boardScroll.getVerticalScrollBar().setUnitIncrement(18);
        boardScroll.getHorizontalScrollBar().setUnitIncrement(18);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(0, 4, 4, 6));
        right.add(buildHunterPanel());
        right.add(Box.createVerticalStrut(8));
        right.add(buildManualPanel());
        right.add(Box.createVerticalStrut(8));
        right.add(buildLegendPanel());
        right.add(Box.createVerticalStrut(8));
        right.add(buildLogPanel());
        JScrollPane rightScroll = new JScrollPane(right);
        rightScroll.setPreferredSize(new Dimension(405, 650));
        rightScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardScroll, rightScroll);
        split.setResizeWeight(0.72);
        split.setOneTouchExpandable(true);
        split.setBorder(null);
        return split;
    }

    private JPanel buildHunterPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Состояние охотника и датчики"));
        JPanel values = new JPanel(new GridLayout(0, 2, 5, 3));
        addStateRow(values, "Координаты:", coordinateValue);
        addStateRow(values, "Направление:", directionValue);
        addStateRow(values, "Стрела:", arrowValue);
        addStateRow(values, "Жив:", aliveValue);
        addStateRow(values, "Золото:", goldValue);
        addStateRow(values, "Вампус:", wumpusValue);
        addStateRow(values, "Счёт:", scoreValue);
        panel.add(values, BorderLayout.NORTH);
        JPanel sensors = new JPanel(new GridLayout(0, 1, 3, 3));
        sensors.add(sensor(breezeSensor));
        sensors.add(sensor(stenchSensor));
        sensors.add(sensor(glitterSensor));
        sensors.add(sensor(bumpSensor));
        sensors.add(sensor(screamSensor));
        panel.add(sensors, BorderLayout.CENTER);
        return panel;
    }

    private void addStateRow(JPanel panel, String caption, JLabel value) {
        panel.add(new JLabel(caption));
        value.setFont(value.getFont().deriveFont(Font.BOLD));
        panel.add(value);
    }
    private JLabel sensor(JLabel label) {
        label.setOpaque(true);
        label.setBorder(new EmptyBorder(4, 7, 4, 7));
        label.setBackground(COLOR_INACTIVE_SENSOR);
        label.setForeground(COLOR_MUTED);
        return label;
    }

    private JPanel buildManualPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Ручное управление"));
        JPanel buttons = new JPanel(new GridLayout(0, 2, 5, 5));
        addCommandButton(buttons, "Повернуть налево", HunterCommand.TURN_LEFT);
        addCommandButton(buttons, "Повернуть направо", HunterCommand.TURN_RIGHT);
        addCommandButton(buttons, "Шаг вперёд", HunterCommand.FORWARD);
        addCommandButton(buttons, "Выстрелить", HunterCommand.SHOOT);
        addCommandButton(buttons, "Забрать золото", HunterCommand.GRAB_GOLD);
        addCommandButton(buttons, "Выйти (Climb)", HunterCommand.CLIMB);
        panel.add(buttons, BorderLayout.CENTER);
        return panel;
    }
    private void addCommandButton(JPanel panel, String title, HunterCommand command) {
        JButton button = new JButton(title);
        button.setMargin(new Insets(7, 5, 7, 5));
        button.addActionListener(e -> {
            stopAutoMode();
            controller.execute(command);
            refreshView();
        });
        panel.add(button);
    }

    private JPanel buildLegendPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Обозначения карты"));
        addLegend(panel, "? — неизвестно", COLOR_UNKNOWN, FG_UNKNOWN);
        addLegend(panel, "Я? — яма возможна", COLOR_PIT_SUSPECT, FG_PIT_SUSPECT);
        addLegend(panel, "В? — Вампус возможен", COLOR_WUMPUS_SUSPECT, FG_WUMPUS_SUSPECT);
        addLegend(panel, "Я?/В? — обе угрозы", COLOR_BOTH_SUSPECT, FG_BOTH_SUSPECT);
        addLegend(panel, "ОК — посещено", COLOR_OK, FG_OK);
        addLegend(panel, "БЕЗОП. — безопасно", COLOR_PROVEN_SAFE, FG_PROVEN_SAFE);
        return panel;
    }
    private void addLegend(JPanel panel, String text, Color bg, Color fg) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(bg);
        label.setForeground(fg);
        label.setBorder(new EmptyBorder(4, 5, 4, 5));
        panel.add(label);
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Журнал действий и логических выводов"));
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setPreferredSize(new Dimension(365, 270));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomStatus() {
        JPanel panel = new JPanel(new BorderLayout(6, 4));
        panel.setBorder(new EmptyBorder(4, 10, 7, 10));
        panel.add(new JLabel("Состояние игры:"), BorderLayout.WEST);
        statusValue.setFont(statusValue.getFont().deriveFont(Font.BOLD));
        panel.add(statusValue, BorderLayout.CENTER);
        return panel;
    }

    private void wireEvents() {
        percentPitsCheck.addActionListener(e -> {
            pitCountSpinner.setEnabled(!percentPitsCheck.isSelected());
            updatePitSpinnerRange();
        });
        sizeSpinner.addChangeListener(e -> updatePitSpinnerRange());
        observerCheck.addActionListener(e -> refreshView());
        speedCombo.addActionListener(e -> updateTimerDelay());
        autoButton.addActionListener(e -> {
            if (autoTimer.isRunning()) stopAutoMode();
            else if (!controller.isFinished()) {
                updateTimerDelay();
                autoTimer.start();
                autoButton.setText("Авто: работает");
            }
        });
        pauseButton.addActionListener(e -> stopAutoMode());
    }

    private void updateTimerDelay() {
        Object value = speedCombo.getSelectedItem();
        if (value instanceof Integer) {
            // 0 отображается в поле как 0 мс, но Swing получает 1 мс для отзывчивости окна.
            int delay = Math.max(1, ((Integer) value).intValue());
            autoTimer.setDelay(delay);
            autoTimer.setInitialDelay(delay);
        }
    }

    private void updatePitSpinnerRange() {
        int size = ((Integer) sizeSpinner.getValue()).intValue();
        int max = WorldGenerator.getMaxPitCount(size);
        pitCountModel.setMaximum(Integer.valueOf(max));
        int current = ((Number) pitCountSpinner.getValue()).intValue();
        if (current > max) pitCountSpinner.setValue(Integer.valueOf(max));
        if (current < 0) pitCountSpinner.setValue(Integer.valueOf(0));
    }

    private void createNewMap() {
        stopAutoMode();
        int size = ((Integer) sizeSpinner.getValue()).intValue();
        Integer custom = percentPitsCheck.isSelected() ? null : Integer.valueOf(((Number) pitCountSpinner.getValue()).intValue());
        try {
            controller.newGame(size, custom);
            rebuildBoard();
            refreshView();
        } catch (IllegalArgumentException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Некорректные параметры", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopAutoMode() {
        autoTimer.stop();
        autoButton.setText("Автоизучение");
    }

    private void rebuildBoard() {
        int n = controller.getWorld().getSize();
        boardPanel.removeAll();
        boardPanel.setLayout(new GridLayout(n, n, 2, 2));
        mainLabels = new JLabel[n][n];
        detailLabels = new JLabel[n][n];
        int cellSize = Math.max(42, Math.min(82, 650 / n));
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) {
            JPanel tile = new JPanel(new BorderLayout(1, 1));
            tile.setBackground(COLOR_UNKNOWN);
            tile.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
            JLabel coordinate = new JLabel((r + 1) + "," + (c + 1), SwingConstants.LEFT);
            coordinate.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(8, Math.min(11, cellSize / 5))));
            coordinate.setForeground(COLOR_MUTED);
            coordinate.setBorder(new EmptyBorder(1, 3, 0, 2));
            JLabel main = new JLabel("?", SwingConstants.CENTER);
            main.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, Math.min(19, cellSize / 3))));
            main.setForeground(FG_UNKNOWN);
            JLabel detail = new JLabel(" ", SwingConstants.CENTER);
            detail.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(8, Math.min(10, cellSize / 5))));
            detail.setForeground(COLOR_MUTED);
            tile.add(coordinate, BorderLayout.NORTH);
            tile.add(main, BorderLayout.CENTER);
            tile.add(detail, BorderLayout.SOUTH);
            tile.setPreferredSize(new Dimension(cellSize, cellSize));
            mainLabels[r][c] = main;
            detailLabels[r][c] = detail;
            boardPanel.add(tile);
        }
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private void refreshView() {
        if (controller.getWorld() == null || mainLabels == null || mainLabels.length != controller.getWorld().getSize()) return;
        Hunter hunter = controller.getWorld().getHunter();
        Position hunterPos = hunter.getPosition();
        KnowledgeMap knowledge = controller.getKnowledgeMap();
        boolean observer = observerCheck.isSelected();
        int n = controller.getWorld().getSize();
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) {
            Position p = new Position(r, c);
            JPanel tile = (JPanel) boardPanel.getComponent(r * n + c);
            JLabel main = mainLabels[r][c];
            JLabel detail = detailLabels[r][c];
            main.setForeground(FG_UNKNOWN);
            detail.setForeground(COLOR_MUTED);
            detail.setText(" ");
            if (p.equals(hunterPos)) {
                main.setText("О " + hunter.getDirection().getSymbol());
                main.setForeground(hunter.isAlive() ? FG_HUNTER : FG_KNOWN_WUMPUS);
                tile.setBackground(hunter.isAlive() ? COLOR_HUNTER : COLOR_KNOWN_WUMPUS);
                if (observer) {
                    String observedContent = hiddenContent(r, c);
                    detail.setText(!hunter.isAlive() ? observedContent + " / ПОГИБ"
                            : hunter.hasGold() ? observedContent + " / ЗОЛОТО У НЕГО" : observedContent);
                } else if (!hunter.isAlive()) detail.setText("ПОГИБ");
                else if (hunter.hasGold()) detail.setText("ЗОЛОТО");
            } else if (observer) {
                showHiddenCell(tile, main, detail, r, c);
            } else if (knowledge.isKnownPit(p)) {
                main.setText("ЯМА!"); main.setForeground(FG_KNOWN_PIT); tile.setBackground(COLOR_KNOWN_PIT);
            } else if (knowledge.isKnownWumpus(p) && !hunter.hasKilledWumpus()) {
                main.setText("В!"); main.setForeground(FG_KNOWN_WUMPUS); tile.setBackground(COLOR_KNOWN_WUMPUS);
            } else if (knowledge.isKnownWumpus(p) && hunter.hasKilledWumpus() && !knowledge.isVisited(p)) {
                main.setText("В УБИТ"); main.setForeground(FG_KNOWN_WUMPUS); tile.setBackground(COLOR_KNOWN_WUMPUS);
            } else if (knowledge.isVisited(p)) {
                main.setText("ОК"); main.setForeground(FG_OK); tile.setBackground(COLOR_OK);
            } else if (knowledge.isPitSuspected(p) && knowledge.isWumpusSuspected(p)) {
                main.setText("Я?/В?"); main.setForeground(FG_BOTH_SUSPECT); tile.setBackground(COLOR_BOTH_SUSPECT);
            } else if (knowledge.isPitSuspected(p)) {
                main.setText("Я?"); main.setForeground(FG_PIT_SUSPECT); tile.setBackground(COLOR_PIT_SUSPECT);
            } else if (knowledge.isWumpusSuspected(p)) {
                main.setText("В?"); main.setForeground(FG_WUMPUS_SUSPECT); tile.setBackground(COLOR_WUMPUS_SUSPECT);
            } else if (knowledge.isKnownSafe(p)) {
                main.setText("БЕЗОП."); main.setForeground(FG_PROVEN_SAFE); tile.setBackground(COLOR_PROVEN_SAFE);
            } else {
                main.setText("?"); main.setForeground(FG_UNKNOWN); tile.setBackground(COLOR_UNKNOWN);
            }
        }

        coordinateValue.setText(hunterPos.toString());
        directionValue.setText(hunter.getDirection().getSymbol() + " " + directionRussian(hunter.getDirection()));
        arrowValue.setText(hunter.hasArrow() ? "ЕСТЬ" : "НЕТ");
        aliveValue.setText(hunter.isAlive() ? "ДА" : "НЕТ");
        goldValue.setText(hunter.hasGold() ? "ЕСТЬ" : "НЕТ");
        wumpusValue.setText(hunter.hasKilledWumpus() ? "УБИТ" : "ЖИВ / неизвестно");
        scoreValue.setText(Integer.toString(controller.getScore()));
        statusValue.setText(controller.getStatus());
        Percepts percepts = controller.getCurrentPercepts();
        updateSensor(breezeSensor, "Ветерок", percepts.hasBreeze());
        updateSensor(stenchSensor, "Запах", percepts.hasStench());
        updateSensor(glitterSensor, "Блеск", percepts.hasGlitter());
        updateSensor(bumpSensor, "Удар", percepts.hasBump());
        updateSensor(screamSensor, "Крик", percepts.hasScream());
        refreshLog();
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private String directionRussian(Direction d) {
        switch (d) {
            case UP: return "вверх";
            case RIGHT: return "вправо";
            case DOWN: return "вниз";
            case LEFT: return "влево";
            default: return "";
        }
    }
    private void updateSensor(JLabel label, String name, boolean active) {
        label.setText(name + ": " + (active ? "ДА" : "НЕТ"));
        label.setBackground(active ? COLOR_ACTIVE_SENSOR : COLOR_INACTIVE_SENSOR);
        label.setForeground(active ? FG_PROVEN_SAFE : COLOR_MUTED);
        label.setFont(label.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN));
    }
    private void refreshLog() {
        StringBuilder text = new StringBuilder();
        List<String> lines = controller.getLogLines();
        for (String line : lines) text.append(line).append('\n');
        logArea.setText(text.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    private String hiddenContent(int r, int c) {
        WorldCell cell = controller.getWorld().getCellForObserver(r, c);
        if (cell.hasPit()) return "ЯМА";
        if (cell.hasWumpus()) return "ВАМПУС";
        if (cell.hasGold()) return "ЗОЛОТО";
        return "ПУСТО";
    }
    private void showHiddenCell(JPanel tile, JLabel main, JLabel detail, int r, int c) {
        WorldCell cell = controller.getWorld().getCellForObserver(r, c);
        if (cell.hasPit()) {
            main.setText("ЯМА"); main.setForeground(FG_KNOWN_PIT); tile.setBackground(COLOR_KNOWN_PIT);
            if (cell.hasGold()) detail.setText("ЗОЛОТО В ЯМЕ");
        }
        else if (cell.hasWumpus()) { main.setText("ВАМПУС"); main.setForeground(FG_KNOWN_WUMPUS); tile.setBackground(COLOR_KNOWN_WUMPUS); }
        else if (cell.hasGold()) { main.setText("ЗОЛОТО"); main.setForeground(FG_GOLD); tile.setBackground(new Color(78, 68, 39)); }
        else { main.setText("ПУСТО"); main.setForeground(FG_UNKNOWN); tile.setBackground(COLOR_UNKNOWN); }
        detail.setText("наблюдатель");
    }

    private static void configureDarkUiDefaults() {
        UIManager.put("Panel.background", COLOR_PANEL);
        UIManager.put("Panel.foreground", COLOR_TEXT);
        UIManager.put("Label.foreground", COLOR_TEXT);
        UIManager.put("Button.background", COLOR_PANEL_ALT);
        UIManager.put("Button.foreground", COLOR_TEXT);
        UIManager.put("Button.select", new Color(64, 70, 75));
        UIManager.put("Button.focus", COLOR_BORDER);
        UIManager.put("CheckBox.background", COLOR_PANEL);
        UIManager.put("CheckBox.foreground", COLOR_TEXT);
        UIManager.put("ComboBox.background", COLOR_INPUT);
        UIManager.put("ComboBox.foreground", COLOR_TEXT);
        UIManager.put("ComboBox.selectionBackground", new Color(67, 82, 94));
        UIManager.put("ComboBox.selectionForeground", COLOR_TEXT);
        UIManager.put("List.background", COLOR_INPUT);
        UIManager.put("List.foreground", COLOR_TEXT);
        UIManager.put("List.selectionBackground", new Color(67, 82, 94));
        UIManager.put("List.selectionForeground", COLOR_TEXT);
        UIManager.put("Spinner.background", COLOR_INPUT);
        UIManager.put("Spinner.foreground", COLOR_TEXT);
        UIManager.put("TextField.background", COLOR_INPUT);
        UIManager.put("TextField.foreground", COLOR_TEXT);
        UIManager.put("TextField.caretForeground", COLOR_TEXT);
        UIManager.put("TextArea.background", new Color(30, 33, 36));
        UIManager.put("TextArea.foreground", new Color(206, 211, 215));
        UIManager.put("TextArea.caretForeground", COLOR_TEXT);
        UIManager.put("TextArea.selectionBackground", new Color(65, 81, 94));
        UIManager.put("ScrollPane.background", COLOR_PANEL);
        UIManager.put("Viewport.background", COLOR_PANEL);
        UIManager.put("ScrollBar.background", COLOR_APP);
        UIManager.put("ScrollBar.track", COLOR_APP);
        UIManager.put("ScrollBar.thumb", new Color(79, 85, 91));
        UIManager.put("SplitPane.background", COLOR_APP);
        UIManager.put("SplitPane.dividerSize", 7);
        UIManager.put("TitledBorder.titleColor", new Color(190, 197, 202));
        UIManager.put("OptionPane.background", COLOR_PANEL);
        UIManager.put("OptionPane.messageForeground", COLOR_TEXT);
        UIManager.put("ToolTip.background", COLOR_PANEL_ALT);
        UIManager.put("ToolTip.foreground", COLOR_TEXT);
    }

    private void applyDarkComponentTree(Component component) {
        if (component instanceof JButton) {
            JButton b = (JButton) component;
            b.setUI(new BasicButtonUI());
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setBorderPainted(true);
            b.setFocusPainted(false);
            b.setRolloverEnabled(false);
            b.setBackground(COLOR_PANEL_ALT);
            b.setForeground(COLOR_TEXT);
            b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(COLOR_BORDER), new EmptyBorder(5, 8, 5, 8)));
        } else if (component instanceof JCheckBox) {
            component.setForeground(COLOR_TEXT);
            component.setBackground(COLOR_PANEL);
            ((JCheckBox) component).setOpaque(false);
        } else if (component instanceof JComboBox<?>) {
            @SuppressWarnings("unchecked") JComboBox<Object> combo = (JComboBox<Object>) component;
            combo.setUI(new BasicComboBoxUI());
            combo.setOpaque(true);
            combo.setBackground(COLOR_INPUT);
            combo.setForeground(COLOR_TEXT);
            combo.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
            combo.setRenderer((list, value, index, selected, focus) -> {
                JLabel label = new JLabel(value == null ? "" : value.toString());
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(3, 6, 3, 6));
                label.setBackground(selected ? new Color(67, 82, 94) : COLOR_INPUT);
                label.setForeground(COLOR_TEXT);
                return label;
            });
        } else if (component instanceof JSpinner) {
            component.setForeground(COLOR_TEXT);
            component.setBackground(COLOR_INPUT);
        } else if (component instanceof JTextArea) {
            component.setForeground(new Color(206, 211, 215));
            component.setBackground(new Color(30, 33, 36));
            ((JTextArea) component).setCaretColor(COLOR_TEXT);
            ((JTextArea) component).setSelectionColor(new Color(65, 81, 94));
        } else if (component instanceof javax.swing.text.JTextComponent) {
            component.setForeground(COLOR_TEXT);
            component.setBackground(COLOR_INPUT);
        } else if (component instanceof javax.swing.JScrollBar) {
            javax.swing.JScrollBar bar = (javax.swing.JScrollBar) component;
            bar.setUI(new BasicScrollBarUI() {
                @Override protected void configureScrollBarColors() {
                    thumbColor = new Color(76, 82, 88);
                    thumbDarkShadowColor = COLOR_APP;
                    thumbHighlightColor = new Color(91, 98, 104);
                    thumbLightShadowColor = new Color(82, 88, 94);
                    trackColor = COLOR_APP;
                }
                @Override protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
                @Override protected JButton createIncreaseButton(int orientation) { return zeroButton(); }
                private JButton zeroButton() {
                    JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); b.setMinimumSize(new Dimension(0, 0)); b.setMaximumSize(new Dimension(0, 0));
                    return b;
                }
            });
            bar.setBackground(COLOR_APP);
            bar.setForeground(COLOR_MUTED);
            bar.setOpaque(true);
        } else if (component instanceof JScrollPane) {
            JScrollPane pane = (JScrollPane) component;
            pane.setBackground(COLOR_PANEL);
            pane.getViewport().setBackground(COLOR_PANEL);
            pane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        } else if (component instanceof JLabel) {
            component.setForeground(COLOR_TEXT);
        } else if (component instanceof JPanel) {
            component.setBackground(COLOR_PANEL);
            component.setForeground(COLOR_TEXT);
            ((JPanel) component).setOpaque(true);
        } else if (component instanceof Container) {
            component.setBackground(COLOR_PANEL);
            component.setForeground(COLOR_TEXT);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) applyDarkComponentTree(child);
        }
    }

    public static void launch() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { }
        configureDarkUiDefaults();
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
