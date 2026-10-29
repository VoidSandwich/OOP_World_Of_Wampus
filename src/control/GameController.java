package control;

import ai.KnowledgeMap;
import ai.LogicalAgent;
import model.ActionResult;
import model.Hunter;
import model.Percepts;
import model.Position;
import model.World;
import model.WorldGenerator;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Связывает реальный мир, базу знаний агента, стратегию и журнал. */
public final class GameController {
    private final LogicalAgent agent = new LogicalAgent();
    private final List<String> logLines = new ArrayList<String>();
    private final List<Runnable> listeners = new ArrayList<Runnable>();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private World world;
    private KnowledgeMap knowledgeMap;
    private Percepts currentPercepts = Percepts.none();
    private Position lastRecordedPosition;
    private Percepts lastRecordedPercepts;
    private String status = "Нет игры";
    private int score;

    public GameController(int size, Integer customPitCount) { newGame(size, customPitCount); }
    public void addListener(Runnable listener) { if (listener != null) listeners.add(listener); }
    public World getWorld() { return world; }
    public KnowledgeMap getKnowledgeMap() { return knowledgeMap; }
    public Percepts getCurrentPercepts() { return currentPercepts; }
    public List<String> getLogLines() { return Collections.unmodifiableList(new ArrayList<String>(logLines)); }
    public String getStatus() { return status; }
    public int getScore() { return score; }
    public boolean isFinished() { return world == null || !world.getHunter().isAlive() || world.getHunter().hasExited(); }

    public void newGame(int size, Integer customPitCount) {
        int pitCount = customPitCount == null ? WorldGenerator.getDefaultPitCount(size) : customPitCount.intValue();
        world = WorldGenerator.generate(size, pitCount);
        knowledgeMap = new KnowledgeMap(size);
        currentPercepts = Percepts.none();
        lastRecordedPosition = null;
        lastRecordedPercepts = null;
        score = 0;
        logLines.clear();
        status = "Исследование пещеры";
        log("Система", "Создано поле " + size + "x" + size + ", ям: " + pitCount + " (соседство ям по стороне запрещено).");
        log("Система", "Охотник начинает в " + world.getHunter().getPosition() + " и смотрит вправо.");
        log("Система", "Цель: найти золото, убить Вампуса, вернуться в стартовую комнату и выйти.");
        observeCurrentCell();
        notifyListeners();
    }

    public ActionResult execute(HunterCommand command) {
        if (command == null || command == HunterCommand.WAIT) return ActionResult.rejected("Нет действия.");
        Hunter hunter = world.getHunter();
        if (!hunter.isAlive() || hunter.hasExited()) return ActionResult.rejected("Игра уже завершена.");

        ActionResult result;
        switch (command) {
            case TURN_LEFT: result = world.turnLeft(); break;
            case TURN_RIGHT: result = world.turnRight(); break;
            case FORWARD: result = world.moveForward(); break;
            case SHOOT: result = world.shoot(); break;
            case GRAB_GOLD: result = world.grabGold(); break;
            case CLIMB: result = world.climb(); break;
            default: return ActionResult.rejected("Неизвестная команда.");
        }
        if (result.isSuccessful()) {
            score -= 1; // PEAS: каждое выполненное действие стоит 1 очко.
            if (command == HunterCommand.SHOOT) score -= 10;
            if (result.hasCollectedGold()) score += 1000;
            if (result.hasHunterDied()) score -= 1000;
        }
        log("Действие", result.getMessage());

        if (result.hasHunterDied()) {
            knowledgeMap.markDeath(hunter.getPosition(), result.getDeathReason());
            status = "Поражение — охотник погиб: " + result.getDeathReason();
        } else {
            if (result.hasScreamed()) knowledgeMap.markWumpusKilled();
            if (!hunter.hasExited()) observeCurrentCell();
            appendConclusions();
            if (hunter.hasExited()) {
                status = hunter.hasGold() ? "Победа: золото доставлено в стартовую комнату и охотник вышел." : "Охотник вышел без золота.";
                log("Система", status);
            } else if (hunter.hasGold()) {
                status = "Золото найдено — возвращайтесь в стартовую комнату " + world.getStartPosition() + ".";
            } else if (hunter.hasKilledWumpus()) {
                status = "Вампус убит; ищем золото и путь к выходу.";
            } else {
                status = "Исследование пещеры";
            }
        }
        notifyListeners();
        return result;
    }

    public boolean autoStep() {
        if (isFinished()) return false;
        Hunter h = world.getHunter();
        HunterCommand command = agent.chooseAction(knowledgeMap, h.getPosition(), world.getStartPosition(),
                h.getDirection(), h.hasArrow(), h.hasKilledWumpus(), h.hasGold(), h.isAlive(), h.hasExited(), currentPercepts);
        if (command == null || command == HunterCommand.WAIT) {
            status = "Автоизучение остановлено: в базе знаний нет безопасного продолжения или обоснованного фронтира. Можно продолжить вручную.";
            log("Агент", status);
            notifyListeners();
            return false;
        }
        execute(command);
        return !isFinished();
    }

    private void observeCurrentCell() {
        Hunter h = world.getHunter();
        if (!h.isAlive() || h.hasExited()) return;
        Percepts fresh = world.getCurrentPercepts();
        Position p = h.getPosition();
        boolean changed = lastRecordedPosition == null || !lastRecordedPosition.equals(p)
                || !samePercepts(lastRecordedPercepts, fresh) || fresh.hasBump() || fresh.hasScream();
        currentPercepts = fresh;
        if (changed) {
            knowledgeMap.observe(p, fresh);
            log("Датчики", "Клетка " + p + ": " + fresh.toRussianText() + " " + fresh + ".");
            lastRecordedPosition = p;
            lastRecordedPercepts = fresh;
            appendConclusions();
        }
    }

    private boolean samePercepts(Percepts a, Percepts b) {
        if (a == null || b == null) return false;
        return a.hasStench() == b.hasStench() && a.hasBreeze() == b.hasBreeze()
                && a.hasGlitter() == b.hasGlitter() && a.hasBump() == b.hasBump() && a.hasScream() == b.hasScream();
    }
    private void appendConclusions() { for (String fact : knowledgeMap.drainConclusions()) log("Логика", fact); }
    private void log(String category, String message) {
        logLines.add("[" + LocalTime.now().format(timeFormat) + "] [" + category + "] " + message);
        while (logLines.size() > 900) logLines.remove(0);
    }
    private void notifyListeners() { for (Runnable r : new ArrayList<Runnable>(listeners)) r.run(); }
}
