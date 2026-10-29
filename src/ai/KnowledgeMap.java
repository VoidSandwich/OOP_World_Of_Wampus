package ai;

import model.Direction;
import model.Percepts;
import model.Position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * База знаний агента. Сохраняет все сенсорные наблюдения, исключения и доказанные факты.
 * Учитывает известное правило этой версии мира: две ямы не соседствуют по стороне.
 */
public final class KnowledgeMap {
    private static final double PIT_PRIOR = 0.15;
    private final int size;
    private final boolean[][] visited;
    private final boolean[][] knownPit;
    private final Observation[][] observations;
    private final Set<String> announcedFacts = new HashSet<String>();
    private final List<String> pendingConclusions = new ArrayList<String>();

    private boolean[][] pitPossible;
    private boolean[][] wumpusPossible;
    private boolean[][] pitSuspected;
    private boolean[][] wumpusSuspected;
    private boolean wumpusKilled;
    private boolean wumpusPositionConfirmedByDeath;
    private Position knownWumpusPosition;
    private Position goldPosition;

    private static final class Observation {
        final boolean stench;
        final boolean breeze;
        final boolean glitter;
        Observation(Percepts p, Observation previous) {
            // База знаний накапливает положительные факты, а не забывает их после Grab/Scream.
            stench = p.hasStench() || (previous != null && previous.stench);
            breeze = p.hasBreeze() || (previous != null && previous.breeze);
            glitter = p.hasGlitter() || (previous != null && previous.glitter);
        }
    }

    public KnowledgeMap(int size) {
        if (size < 2) throw new IllegalArgumentException("Размер карты должен быть не меньше 2.");
        this.size = size;
        visited = new boolean[size][size];
        knownPit = new boolean[size][size];
        observations = new Observation[size][size];
        recompute();
    }

    public int getSize() { return size; }
    public boolean isInside(Position p) {
        return p != null && p.getRow() >= 0 && p.getRow() < size && p.getCol() >= 0 && p.getCol() < size;
    }

    public void observe(Position p, Percepts percepts) {
        if (!isInside(p) || percepts == null) return;
        int r = p.getRow(), c = p.getCol();
        boolean wasVisited = visited[r][c];
        boolean oldPitSuspicion = pitSuspected[r][c] || knownPit[r][c];
        boolean oldWumpusSuspicion = wumpusSuspected[r][c] || isKnownWumpus(p);
        visited[r][c] = true;
        // Прямое выживание в клетке сильнее старой гипотезы о её опасности.
        knownPit[r][c] = false;
        observations[r][c] = new Observation(percepts, observations[r][c]);
        if (percepts.hasGlitter()) goldPosition = p;
        recompute();
        if (!wasVisited) {
            addFact("Клетка " + p + " посещена: охотник выжил, следовательно, в ней нет ямы и живого Вампуса.");
            if (oldPitSuspicion) addFact("Прямая проверка " + p + " сняла прежнее подозрение на яму.");
            if (oldWumpusSuspicion) addFact("Прямая проверка " + p + " сняла прежнее подозрение на живого Вампуса.");
        }
        addFact(percepts.hasBreeze()
                ? "В " + p + " есть ветерок: хотя бы одна ортогонально соседняя клетка содержит яму."
                : "В " + p + " нет ветерка: ортогональные соседи исключены из кандидатов на яму.");
        if (percepts.hasStench()) {
            addFact(wumpusKilled
                    ? "В " + p + " сохраняется запах после гибели Вампуса; он больше не рассматривается как действующая угроза."
                    : "В " + p + " есть запах: Вампус находится в одной из ортогонально соседних клеток.");
        } else {
            addFact("В " + p + " нет запаха: ортогональные соседи исключены из кандидатов на Вампуса.");
        }
        if (percepts.hasGlitter()) addFact("Обнаружен блеск: золото находится в " + p + ".");
        addNewlyProvedFacts();
    }

    public void markDeath(Position p, String reason) {
        if (!isInside(p)) return;
        int r = p.getRow(), c = p.getCol();
        visited[r][c] = false;
        if (reason != null && reason.toLowerCase().contains("ям")) {
            knownPit[r][c] = true;
            addFact("Подтверждено непосредственным событием: в " + p + " находится яма.");
        }
        if (reason != null && reason.toLowerCase().contains("вампус")) {
            knownWumpusPosition = p;
            wumpusPositionConfirmedByDeath = true;
            addFact("Подтверждено непосредственным событием: в " + p + " находился Вампус.");
        }
        recompute();
    }

    public void markWumpusKilled() {
        if (wumpusKilled) return;
        List<Position> candidates = getPossibleWumpusPositions();
        if (knownWumpusPosition == null && candidates.size() == 1) knownWumpusPosition = candidates.get(0);
        wumpusKilled = true;
        addFact("Получен Scream: Вампус убит. Его клетка больше не смертельно опасна.");
        recompute();
    }

    public boolean isWumpusKilled() { return wumpusKilled; }
    public Position getGoldPosition() { return goldPosition; }
    public Position getKnownWumpusPosition() {
        if (knownWumpusPosition != null) return knownWumpusPosition;
        List<Position> candidates = getPossibleWumpusPositions();
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    public List<Position> getPossibleGoldPositions(Position start) {
        Position w = getKnownWumpusPosition();
        if (w == null) return Collections.emptyList();
        List<Position> result = new ArrayList<Position>();
        for (Direction d : Direction.values()) {
            Position p = w.step(d);
            if (!isInside(p) || p.equals(start) || visited[p.getRow()][p.getCol()]) continue;
            if (knownPit[p.getRow()][p.getCol()]) continue;
            result.add(p);
        }
        return result;
    }

    public boolean isVisited(Position p) { return isInside(p) && visited[p.getRow()][p.getCol()]; }
    public boolean isKnownPit(Position p) { return isInside(p) && knownPit[p.getRow()][p.getCol()]; }
    public boolean isKnownWumpus(Position p) {
        return isInside(p) && !wumpusKilled && wumpusPossible[p.getRow()][p.getCol()]
                && getPossibleWumpusPositions().size() == 1;
    }
    public boolean isKnownSafe(Position p) {
        if (!isInside(p)) return false;
        int r = p.getRow(), c = p.getCol();
        if (knownPit[r][c]) return false;
        if (visited[r][c]) return true;
        if (pitPossible[r][c]) return false;
        return wumpusKilled || !wumpusPossible[r][c];
    }
    public boolean isPitSuspected(Position p) { return isInside(p) && !visited[p.getRow()][p.getCol()] && pitSuspected[p.getRow()][p.getCol()]; }
    public boolean isWumpusSuspected(Position p) { return isInside(p) && !wumpusKilled && !visited[p.getRow()][p.getCol()] && wumpusSuspected[p.getRow()][p.getCol()]; }
    public boolean hasBreezeObservation(Position p) { return isInside(p) && observations[p.getRow()][p.getCol()] != null && observations[p.getRow()][p.getCol()].breeze; }
    public boolean hasStenchObservation(Position p) { return isInside(p) && observations[p.getRow()][p.getCol()] != null && observations[p.getRow()][p.getCol()].stench; }
    public boolean hasGlitterObservation(Position p) { return isInside(p) && observations[p.getRow()][p.getCol()] != null && observations[p.getRow()][p.getCol()].glitter; }
    public boolean hasAnyStenchObservation() {
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++)
            if (observations[r][c] != null && observations[r][c].stench) return true;
        return false;
    }
    public List<Position> getPossibleWumpusPositions() {
        if (wumpusKilled) return Collections.emptyList();
        List<Position> result = new ArrayList<Position>();
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++)
            if (wumpusPossible[r][c] && !visited[r][c] && !knownPit[r][c]) result.add(new Position(r, c));
        return Collections.unmodifiableList(result);
    }

    /** Вероятность ямы: базовый prior 15%, обновлённый ограничениями Breeze и известным правилом без соседних ям. */
    public double getPitRisk(Position p) {
        if (!isInside(p)) return 1.0;
        int r = p.getRow(), c = p.getCol();
        if (knownPit[r][c]) return 1.0;
        if (visited[r][c] || !pitPossible[r][c]) return 0.0;
        double odds = PIT_PRIOR / (1.0 - PIT_PRIOR);
        for (int sr = 0; sr < size; sr++) for (int sc = 0; sc < size; sc++) {
            Observation o = observations[sr][sc];
            Position source = new Position(sr, sc);
            if (o == null || !o.breeze || source.manhattanDistance(p) != 1) continue;
            // Не повышаем риск неизвестного соседа по Breeze, который уже объяснён
            // подтверждённой ямой возле источника этого показания.
            if (hasAdjacentKnownPit(source)) continue;
            int candidates = 0;
            for (Direction d : Direction.values()) {
                Position n = source.step(d);
                if (isInside(n) && pitPossible[n.getRow()][n.getCol()]
                        && !visited[n.getRow()][n.getCol()] && !knownPit[n.getRow()][n.getCol()]) candidates++;
            }
            if (candidates <= 1) return 1.0;
            double probabilityNoBreezeIfThisCandidateWereSafe = 1.0 - Math.pow(1.0 - PIT_PRIOR, candidates - 1);
            if (probabilityNoBreezeIfThisCandidateWereSafe > 0.000001) odds /= probabilityNoBreezeIfThisCandidateWereSafe;
        }
        return Math.min(0.99, odds / (1.0 + odds));
    }

    /** При ровно одном возможном местоположении Вампуса вероятность равна 1, иначе равномерный prior по кандидатам. */
    public double getWumpusRisk(Position p) {
        if (!isInside(p) || wumpusKilled || visited[p.getRow()][p.getCol()] || !wumpusPossible[p.getRow()][p.getCol()]) return 0.0;
        int count = getPossibleWumpusPositions().size();
        return count <= 1 ? 1.0 : 1.0 / count;
    }

    public double getCombinedRisk(Position p) {
        double pit = getPitRisk(p), wumpus = getWumpusRisk(p);
        return 1.0 - (1.0 - pit) * (1.0 - wumpus);
    }

    public int getInformationGain(Position p) {
        int count = 0;
        for (Direction d : Direction.values()) {
            Position n = p.step(d);
            if (isInside(n) && !isVisited(n) && !isKnownPit(n)) count++;
        }
        return count;
    }

    public List<String> drainConclusions() {
        List<String> result = new ArrayList<String>(pendingConclusions);
        pendingConclusions.clear();
        return result;
    }

    private void recompute() {
        // Логически выведенная позиция Вампуса должна пересчитываться при каждом новом
        // наблюдении. Не сохраняем старый вывод, если новые ограничения изменили множество кандидатов.
        if (!wumpusPositionConfirmedByDeath && !wumpusKilled) knownWumpusPosition = null;
        pitPossible = new boolean[size][size];
        wumpusPossible = new boolean[size][size];
        pitSuspected = new boolean[size][size];
        wumpusSuspected = new boolean[size][size];
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) {
            pitPossible[r][c] = !visited[r][c] && !knownPit[r][c];
            wumpusPossible[r][c] = !wumpusKilled && !visited[r][c] && !knownPit[r][c];
        }

        // Негативные показания датчиков являются строгими логическими ограничениями.
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) {
            Observation o = observations[r][c];
            if (o == null) continue;
            Position source = new Position(r, c);
            for (Direction d : Direction.values()) {
                Position n = source.step(d);
                if (!isInside(n)) continue;
                int nr = n.getRow(), nc = n.getCol();
                if (!o.breeze && !knownPit[nr][nc]) pitPossible[nr][nc] = false;
                if (!o.stench && !wumpusKilled) wumpusPossible[nr][nc] = false;
            }
        }

        // Известная особенность созданного по заданию мира: ортогонально соседних ям не бывает.
        boolean changed;
        do {
            changed = false;
            for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) {
                if (!knownPit[r][c]) continue;

                // Подтверждённая яма больше не должна оставаться среди кандидатов.
                if (pitPossible[r][c]) {
                    pitPossible[r][c] = false;
                    changed = true;
                }
                if (wumpusPossible[r][c]) wumpusPossible[r][c] = false;

                for (Direction d : Direction.values()) {
                    Position n = new Position(r, c).step(d);
                    if (isInside(n) && pitPossible[n.getRow()][n.getCol()]) {
                        pitPossible[n.getRow()][n.getCol()] = false;
                        changed = true;
                    }
                }
            }

            for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) {
                Observation o = observations[r][c];
                if (o == null || !o.breeze) continue;
                Position source = new Position(r, c);

                // Breeze означает «хотя бы одна соседняя яма», а не «ровно одна яма».
                // Если соседняя яма уже известна, это ограничение уже выполнено.
                // Поэтому нельзя объявлять ещё одного соседа ямой по тому же Breeze.
                if (hasAdjacentKnownPit(source)) continue;

                List<Position> candidates = new ArrayList<Position>();
                for (Direction d : Direction.values()) {
                    Position n = source.step(d);
                    if (isInside(n) && pitPossible[n.getRow()][n.getCol()]
                            && !visited[n.getRow()][n.getCol()]
                            && !knownPit[n.getRow()][n.getCol()]) {
                        candidates.add(n);
                    }
                }
                if (candidates.size() == 1) {
                    Position pit = candidates.get(0);
                    int pr = pit.getRow(), pc = pit.getCol();
                    if (!knownPit[pr][pc]) {
                        knownPit[pr][pc] = true;
                        // Синхронизируем производные множества сразу, не дожидаясь
                        // следующей итерации пересчёта.
                        pitPossible[pr][pc] = false;
                        wumpusPossible[pr][pc] = false;
                        changed = true;
                    }
                }
            }
        } while (changed);

        // Для Вампуса пересекаем соседей всех источников запаха и исключаем соседей без запаха.
        if (!wumpusKilled) {
            List<Position> stenchSources = new ArrayList<Position>();
            for (int r = 0; r < size; r++) for (int c = 0; c < size; c++)
                if (observations[r][c] != null && observations[r][c].stench) stenchSources.add(new Position(r, c));
            if (!stenchSources.isEmpty()) {
                for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) {
                    if (!wumpusPossible[r][c] || visited[r][c] || knownPit[r][c]) continue;
                    Position candidate = new Position(r, c);
                    for (Position source : stenchSources) {
                        if (candidate.manhattanDistance(source) != 1) {
                            wumpusPossible[r][c] = false;
                            break;
                        }
                    }
                }
                List<Position> candidates = new ArrayList<Position>();
                for (int r = 0; r < size; r++) for (int c = 0; c < size; c++)
                    if (wumpusPossible[r][c] && !visited[r][c] && !knownPit[r][c]) candidates.add(new Position(r, c));
                if (candidates.size() == 1) {
                    knownWumpusPosition = candidates.get(0);
                    addFact("Логический вывод: единственная клетка, совместимая со всеми показаниями запаха, — " + knownWumpusPosition + ".");
                }
            }
        } else {
            for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) wumpusPossible[r][c] = false;
        }

        // Только положительные датчики создают пометки подозрения; диагонали не используются.
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) {
            Position target = new Position(r, c);
            if (visited[r][c] || knownPit[r][c]) continue;
            for (Direction d : Direction.values()) {
                Position source = target.step(d);
                if (!isInside(source)) continue;
                Observation o = observations[source.getRow()][source.getCol()];
                if (o == null) continue;
                if (o.breeze && pitPossible[r][c]) pitSuspected[r][c] = true;
                if (!wumpusKilled && o.stench && wumpusPossible[r][c]) wumpusSuspected[r][c] = true;
            }
        }
    }

    private boolean hasAdjacentKnownPit(Position source) {
        for (Direction direction : Direction.values()) {
            Position neighbour = source.step(direction);
            if (isInside(neighbour) && knownPit[neighbour.getRow()][neighbour.getCol()]) {
                return true;
            }
        }
        return false;
    }

    private void addNewlyProvedFacts() {
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) {
            Position p = new Position(r, c);
            if (knownPit[r][c]) addFact("Логический вывод: в клетке " + p + " находится яма.");
            if (!visited[r][c] && !knownPit[r][c] && !pitPossible[r][c]
                    && (wumpusKilled || !wumpusPossible[r][c])) {
                addFact("Логический вывод: клетка " + p + " безопасна по накопленной базе знаний.");
            }
        }
    }
    private void addFact(String fact) {
        if (announcedFacts.add(fact)) pendingConclusions.add(fact);
    }
}
