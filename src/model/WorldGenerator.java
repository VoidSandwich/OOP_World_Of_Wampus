package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Генератор поля. Ямы не соприкасаются ортогонально; диагональное соседство разрешено. */
public final class WorldGenerator {
    public static final double DEFAULT_PIT_PERCENT = 0.15;
    public static final int MIN_SIZE = 2;

    private WorldGenerator() { }

    public static int getDefaultPitCount(int size) {
        validateSize(size);
        return (int) Math.round(size * (double) size * DEFAULT_PIT_PERCENT);
    }

    /** Гарантированный верхний предел: столько ям всегда можно расположить по одной шахматной раскраске. */
    public static int getMaxPitCount(int size) {
        validateSize(size);
        return Math.max(0, (size * size) / 2 - 1);
    }

    public static World generateWithDefaultPitPercent(int size) {
        return generate(size, getDefaultPitCount(size));
    }

    public static World generate(int size, int pitCount) {
        return generate(size, pitCount, new Random());
    }

    public static World generate(int size, int pitCount, Random random) {
        validateSize(size);
        if (random == null) throw new IllegalArgumentException("Random не должен быть null");
        int maxPits = getMaxPitCount(size);
        if (pitCount < 0 || pitCount > maxPits) {
            throw new IllegalArgumentException("Для поля " + size + "x" + size
                    + " количество ям должно быть от 0 до " + maxPits
                    + ". Соседние по стороне ямы запрещены.");
        }

        WorldCell[][] cells = new WorldCell[size][size];
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) cells[r][c] = new WorldCell();
        Position start = new Position(size - 1, 0);

        List<Position> wumpusCandidates = new ArrayList<Position>();
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) {
            Position p = new Position(r, c);
            if (!p.equals(start) && !orthogonalNeighbours(p, start, size).isEmpty()) wumpusCandidates.add(p);
        }
        Collections.shuffle(wumpusCandidates, random);
        Position wumpus = null;
        Position gold = null;
        for (Position candidate : wumpusCandidates) {
            List<Position> goldOptions = orthogonalNeighbours(candidate, start, size);
            if (!goldOptions.isEmpty()) {
                wumpus = candidate;
                gold = goldOptions.get(random.nextInt(goldOptions.size()));
                break;
            }
        }
        if (wumpus == null || gold == null) throw new IllegalStateException("Не удалось разместить Вампуса и золото.");
        cells[wumpus.getRow()][wumpus.getCol()].setWumpus(true);
        cells[gold.getRow()][gold.getCol()].setGold(true);

        List<Position> selectedPits = chooseNonAdjacentPits(size, pitCount, start, wumpus, gold, random);
        if (selectedPits.size() != pitCount) {
            throw new IllegalStateException("Не удалось разместить требуемое число неприкасающихся ям.");
        }
        for (Position p : selectedPits) cells[p.getRow()][p.getCol()].setPit(true);
        return new World(size, cells);
    }

    private static List<Position> chooseNonAdjacentPits(int size, int pitCount, Position start,
                                                        Position wumpus, Position gold, Random random) {
        if (pitCount == 0) return Collections.emptyList();
        List<Position> candidates = new ArrayList<Position>();
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) {
            Position p = new Position(r, c);
            // Классическая модель допускает яму в комнате с золотом; Вампус и стартовая комната исключены.
            if (!p.equals(start) && !p.equals(wumpus)) candidates.add(p);
        }

        // Для обычных значений (в том числе 15%) выбираем случайные позиции разными порядками.
        for (int attempt = 0; attempt < 80; attempt++) {
            Collections.shuffle(candidates, random);
            List<Position> result = new ArrayList<Position>();
            boolean[][] placed = new boolean[size][size];
            for (Position p : candidates) {
                if (!hasAdjacentPit(placed, p, size)) {
                    result.add(p);
                    placed[p.getRow()][p.getCol()] = true;
                    if (result.size() == pitCount) return result;
                }
            }
        }

        // Гарантированный fallback: одна шахматная группа не содержит соседей по стороне.
        int startParity = (start.getRow() + start.getCol()) & 1;
        List<Position> fallback = new ArrayList<Position>();
        for (Position p : candidates) {
            if (((p.getRow() + p.getCol()) & 1) != startParity) fallback.add(p);
        }
        Collections.shuffle(fallback, random);
        if (fallback.size() < pitCount) throw new IllegalArgumentException("Слишком много ям для правила отсутствия соседства.");
        return new ArrayList<Position>(fallback.subList(0, pitCount));
    }

    private static boolean hasAdjacentPit(boolean[][] placed, Position p, int size) {
        int r = p.getRow(), c = p.getCol();
        return (r > 0 && placed[r - 1][c]) || (r + 1 < size && placed[r + 1][c])
                || (c > 0 && placed[r][c - 1]) || (c + 1 < size && placed[r][c + 1]);
    }

    private static List<Position> orthogonalNeighbours(Position p, Position excluded, int size) {
        List<Position> result = new ArrayList<Position>();
        for (Direction d : Direction.values()) {
            Position n = p.step(d);
            if (inside(n, size) && !n.equals(excluded)) result.add(n);
        }
        return result;
    }

    private static boolean inside(Position p, int size) {
        return p.getRow() >= 0 && p.getRow() < size && p.getCol() >= 0 && p.getCol() < size;
    }

    private static void validateSize(int size) {
        if (size < MIN_SIZE || size > 100) throw new IllegalArgumentException("Размер поля должен быть от 2 до 100.");
    }
}
