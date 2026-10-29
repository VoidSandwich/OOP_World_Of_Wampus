package ai;

import model.Direction;
import model.Position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.PriorityQueue;

/** BFS-поиск кратчайшего маршрута, который проходит только через доказанно безопасные клетки. */
public final class Pathfinder {
    public List<Position> shortestPath(Position start, Position goal, KnowledgeMap map) {
        if (start == null || goal == null || map == null || !map.isInside(start) || !map.isInside(goal)) return Collections.emptyList();
        if (start.equals(goal)) return Collections.emptyList();
        SearchTree tree = search(start, map);
        if (tree.distance[goal.getRow()][goal.getCol()] < 0) return Collections.emptyList();
        return reconstruct(start, goal, tree.previous);
    }

    /**
     * Если неизвестные клетки отделяют агента от уже доказанно безопасных,
     * ищет маршрут к безопасной неисследованной цели с минимальным суммарным риском.
     * Такой маршрут предпочитает несколько неизвестных клеток одной клетке,
     * на которую указывает сильное подозрение, если это действительно безопаснее.
     */
    public List<Position> safestPathToSafeUnvisited(Position start, KnowledgeMap map) {
        if (start == null || map == null || !map.isInside(start)) return Collections.emptyList();
        RiskSearchTree tree = riskSearch(start, map);
        Position bestTarget = null;
        double bestCost = Double.POSITIVE_INFINITY;
        int bestSteps = Integer.MAX_VALUE;

        for (int r = 0; r < map.getSize(); r++) {
            for (int c = 0; c < map.getSize(); c++) {
                Position candidate = new Position(r, c);
                if (map.isVisited(candidate) || !map.isKnownSafe(candidate)) continue;
                double cost = tree.cost[r][c];
                int steps = tree.steps[r][c];
                if (!Double.isFinite(cost) || steps <= 0) continue;
                if (cost < bestCost - 1e-9 || (Math.abs(cost - bestCost) <= 1e-9 && steps < bestSteps)) {
                    bestTarget = candidate;
                    bestCost = cost;
                    bestSteps = steps;
                }
            }
        }
        return bestTarget == null ? Collections.<Position>emptyList()
                : reconstruct(start, bestTarget, tree.previous);
    }

    /**
     * Ищет наименее рискованный маршрут к заданной цели. Используется, например,
     * когда золото уже взято и охотнику нужно вернуться к выходу.
     */
    public List<Position> safestPath(Position start, Position goal, KnowledgeMap map) {
        if (start == null || goal == null || map == null
                || !map.isInside(start) || !map.isInside(goal)) return Collections.emptyList();
        if (start.equals(goal)) return Collections.emptyList();
        RiskSearchTree tree = riskSearch(start, map);
        if (!Double.isFinite(tree.cost[goal.getRow()][goal.getCol()])) return Collections.emptyList();
        return reconstruct(start, goal, tree.previous);
    }

    private RiskSearchTree riskSearch(Position start, KnowledgeMap map) {
        int n = map.getSize();
        double[][] cost = new double[n][n];
        int[][] steps = new int[n][n];
        Position[][] previous = new Position[n][n];
        for (int r = 0; r < n; r++) {
            java.util.Arrays.fill(cost[r], Double.POSITIVE_INFINITY);
            java.util.Arrays.fill(steps[r], Integer.MAX_VALUE);
        }

        PriorityQueue<RiskNode> queue = new PriorityQueue<RiskNode>();
        cost[start.getRow()][start.getCol()] = 0.0;
        steps[start.getRow()][start.getCol()] = 0;
        queue.add(new RiskNode(start, 0.0, 0));

        while (!queue.isEmpty()) {
            RiskNode node = queue.poll();
            Position current = node.position;
            int cr = current.getRow(), cc = current.getCol();
            if (node.cost > cost[cr][cc] + 1e-9 || node.steps != steps[cr][cc]) continue;

            for (Direction direction : Direction.values()) {
                Position next = current.step(direction);
                if (!map.isInside(next) || map.isKnownPit(next) || map.isKnownWumpus(next)) continue;
                double risk = map.getCombinedRisk(next);
                // Клетка, доказанная опасной, не может быть частью рискованного маршрута.
                if (risk >= 0.999) continue;

                // -ln(1-p) складывается по пути и соответствует вероятности выживания.
                // Небольшая цена шага разрешает равенства в пользу более короткого пути.
                double edgeCost = -Math.log(Math.max(1e-9, 1.0 - risk)) + 0.02;
                double newCost = node.cost + edgeCost;
                int newSteps = node.steps + 1;
                int nr = next.getRow(), nc = next.getCol();
                if (newCost < cost[nr][nc] - 1e-9
                        || (Math.abs(newCost - cost[nr][nc]) <= 1e-9 && newSteps < steps[nr][nc])) {
                    cost[nr][nc] = newCost;
                    steps[nr][nc] = newSteps;
                    previous[nr][nc] = current;
                    queue.add(new RiskNode(next, newCost, newSteps));
                }
            }
        }
        return new RiskSearchTree(cost, steps, previous);
    }

    private static final class RiskNode implements Comparable<RiskNode> {
        final Position position;
        final double cost;
        final int steps;

        RiskNode(Position position, double cost, int steps) {
            this.position = position;
            this.cost = cost;
            this.steps = steps;
        }

        @Override
        public int compareTo(RiskNode other) {
            int byCost = Double.compare(cost, other.cost);
            return byCost != 0 ? byCost : Integer.compare(steps, other.steps);
        }
    }

    private static final class RiskSearchTree {
        final double[][] cost;
        final int[][] steps;
        final Position[][] previous;
        RiskSearchTree(double[][] cost, int[][] steps, Position[][] previous) {
            this.cost = cost;
            this.steps = steps;
            this.previous = previous;
        }
    }

    public Position nearestSafeUnvisited(Position start, KnowledgeMap map) {
        SearchTree tree = search(start, map);
        Position best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int r = 0; r < map.getSize(); r++) for (int c = 0; c < map.getSize(); c++) {
            Position p = new Position(r, c);
            int d = tree.distance[r][c];
            if (d < 0 || map.isVisited(p) || !map.isKnownSafe(p)) continue;
            if (d < bestDistance) { best = p; bestDistance = d; }
        }
        return best;
    }

    public Position nearestSafeFiringPosition(Position start, Position wumpus, KnowledgeMap map) {
        if (start == null || wumpus == null) return null;
        SearchTree tree = search(start, map);
        Position best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int r = 0; r < map.getSize(); r++) for (int c = 0; c < map.getSize(); c++) {
            Position p = new Position(r, c);
            if (!map.isKnownSafe(p) || tree.distance[r][c] < 0) continue;
            if (p.getRow() != wumpus.getRow() && p.getCol() != wumpus.getCol()) continue;
            if (tree.distance[r][c] < bestDistance) { best = p; bestDistance = tree.distance[r][c]; }
        }
        return best;
    }

    /** Выбирает фронтир с минимальной оценённой вероятностью гибели; при равном риске предпочитает больше информации и короткий маршрут. */
    public FrontierPlan safestFrontier(Position start, KnowledgeMap map) {
        return safestFrontier(start, map, null);
    }

    /** Тот же вероятностный выбор, но ограниченный кандидатами на золото после убийства Вампуса. */
    public FrontierPlan safestFrontierAmong(Position start, KnowledgeMap map, List<Position> preferredTargets) {
        return safestFrontier(start, map, preferredTargets);
    }

    private FrontierPlan safestFrontier(Position start, KnowledgeMap map, List<Position> preferredTargets) {
        SearchTree tree = search(start, map);
        FrontierPlan best = null;
        for (int r = 0; r < map.getSize(); r++) for (int c = 0; c < map.getSize(); c++) {
            Position anchor = new Position(r, c);
            int distance = tree.distance[r][c];
            if (distance < 0 || !map.isKnownSafe(anchor)) continue;
            for (Direction d : Direction.values()) {
                Position target = anchor.step(d);
                if (!map.isInside(target) || map.isVisited(target) || map.isKnownPit(target)
                        || map.isKnownWumpus(target) || map.isKnownSafe(target)) continue;
                if (preferredTargets != null && !preferredTargets.contains(target)) continue;
                double risk = map.getCombinedRisk(target);
                if (risk >= 0.999) continue; // доказанную опасность никогда не выбираем даже в режиме риска.
                int information = map.getInformationGain(target);
                List<Position> path = anchor.equals(start) ? Collections.<Position>emptyList()
                        : reconstruct(start, anchor, tree.previous);
                FrontierPlan candidate = new FrontierPlan(anchor, target, path, risk, information, distance);
                if (better(candidate, best)) best = candidate;
            }
        }
        return best;
    }

    private boolean better(FrontierPlan candidate, FrontierPlan current) {
        if (current == null) return true;
        if (candidate.risk < current.risk - 0.0001) return true;
        if (candidate.risk > current.risk + 0.0001) return false;
        if (candidate.informationGain != current.informationGain) return candidate.informationGain > current.informationGain;
        return candidate.pathDistance < current.pathDistance;
    }

    private SearchTree search(Position start, KnowledgeMap map) {
        int n = map.getSize();
        int[][] distance = new int[n][n];
        Position[][] previous = new Position[n][n];
        for (int r = 0; r < n; r++) java.util.Arrays.fill(distance[r], -1);
        Queue<Position> queue = new ArrayDeque<Position>();
        queue.add(start);
        distance[start.getRow()][start.getCol()] = 0;
        while (!queue.isEmpty()) {
            Position current = queue.remove();
            for (Direction d : Direction.values()) {
                Position next = current.step(d);
                if (!map.isInside(next) || distance[next.getRow()][next.getCol()] >= 0 || !map.isKnownSafe(next)) continue;
                distance[next.getRow()][next.getCol()] = distance[current.getRow()][current.getCol()] + 1;
                previous[next.getRow()][next.getCol()] = current;
                queue.add(next);
            }
        }
        return new SearchTree(distance, previous);
    }

    private List<Position> reconstruct(Position start, Position goal, Position[][] previous) {
        List<Position> result = new ArrayList<Position>();
        Position cursor = goal;
        while (cursor != null && !cursor.equals(start)) {
            result.add(cursor);
            cursor = previous[cursor.getRow()][cursor.getCol()];
        }
        if (cursor == null) return Collections.emptyList();
        Collections.reverse(result);
        return result;
    }

    public static final class FrontierPlan {
        private final Position anchor;
        private final Position target;
        private final List<Position> pathToAnchor;
        private final double risk;
        private final int informationGain;
        private final int pathDistance;
        FrontierPlan(Position anchor, Position target, List<Position> pathToAnchor, double risk, int informationGain, int pathDistance) {
            this.anchor = anchor; this.target = target; this.pathToAnchor = new ArrayList<Position>(pathToAnchor);
            this.risk = risk; this.informationGain = informationGain; this.pathDistance = pathDistance;
        }
        public Position getAnchor() { return anchor; }
        public Position getTarget() { return target; }
        public List<Position> getPathToAnchor() { return Collections.unmodifiableList(pathToAnchor); }
        public double getRisk() { return risk; }
        public int getInformationGain() { return informationGain; }
    }

    private static final class SearchTree {
        final int[][] distance;
        final Position[][] previous;
        SearchTree(int[][] distance, Position[][] previous) { this.distance = distance; this.previous = previous; }
    }
}
