package ai;

import control.HunterCommand;
import model.Direction;
import model.Percepts;
import model.Position;

import java.util.List;

/**
 * Knowledge-based agent: сначала строгие логические выводы и безопасные клетки;
 * после исчерпания доступных доказанно безопасных целей приоритетно устраняет однозначно
 * установленного Вампуса; лишь затем выбирает рискованный фронтир.
 */
public final class LogicalAgent {
    private final Pathfinder pathfinder = new Pathfinder();

    public HunterCommand chooseAction(KnowledgeMap map, Position current, Position start,
                                      Direction direction, boolean hasArrow,
                                      boolean wumpusKilled, boolean hasGold,
                                      boolean alive, boolean exited, Percepts percepts) {
        if (!alive || exited) return HunterCommand.WAIT;

        if (hasGold) {
            if (current.equals(start)) return HunterCommand.CLIMB;
            List<Position> returnPath = pathfinder.shortestPath(current, start, map);
            if (returnPath.isEmpty()) returnPath = pathfinder.safestPath(current, start, map);
            HunterCommand returnAction = followPath(returnPath, current, direction);
            // После золота единственная цель — выход. Не переключаемся на случайное исследование.
            return returnAction == null ? HunterCommand.WAIT : returnAction;
        }

        if (!wumpusKilled && hasArrow) {
            // Если цель стала однозначной, исследуем сначала все достижимые Safe-клетки.
            Position safeTarget = pathfinder.nearestSafeUnvisited(current, map);
            if (safeTarget != null) {
                HunterCommand move = followPath(pathfinder.shortestPath(current, safeTarget, map), current, direction);
                if (move != null) return move;
            }
            List<Position> wumpusCandidates = map.getPossibleWumpusPositions();
            if (wumpusCandidates.size() == 1) {
                Position wumpus = wumpusCandidates.get(0);
                Position firingPosition = pathfinder.nearestSafeFiringPosition(current, wumpus, map);
                if (firingPosition != null) {
                    if (!current.equals(firingPosition)) {
                        HunterCommand move = followPath(pathfinder.shortestPath(current, firingPosition, map), current, direction);
                        if (move != null) return move;
                    } else {
                        Direction aim = directionToLine(current, wumpus);
                        if (aim != null) {
                            if (direction != aim) return turnToward(direction, aim);
                            return HunterCommand.SHOOT;
                        }
                    }
                }
            }
            // Если однозначная цель Вампуса уже найдена, сначала пытаемся выстрелить
            // с доказанно безопасной позиции. Не исследуем рискованные комнаты только
            // ради продолжения обхода всей карты, когда можно устранить главную угрозу.
            List<Position> riskAwareSafePath = pathfinder.safestPathToSafeUnvisited(current, map);
            if (!riskAwareSafePath.isEmpty()) {
                HunterCommand move = followPath(riskAwareSafePath, current, direction);
                if (move != null) return move;
            }
        }

        // После убийства Вампуса: если золото уже обнаружено, идти к нему по Safe-клеткам.
        if (wumpusKilled && map.getGoldPosition() != null && !current.equals(map.getGoldPosition())) {
            List<Position> goldPath = pathfinder.shortestPath(current, map.getGoldPosition(), map);
            HunterCommand move = followPath(goldPath, current, direction);
            if (move != null) return move;
        }
        if (wumpusKilled && percepts != null && percepts.hasGlitter() && !hasGold) return HunterCommand.GRAB_GOLD;

        // В учебном мире золото находится в одной из комнат, соседних с Вампусом.
        if (wumpusKilled && map.getGoldPosition() == null) {
            List<Position> goldCandidates = map.getPossibleGoldPositions(start);
            Position reachableGoldCandidate = nearestFromList(current, direction, goldCandidates, map);
            if (reachableGoldCandidate != null) {
                List<Position> path = pathfinder.shortestPath(current, reachableGoldCandidate, map);
                HunterCommand move = followPath(path, current, direction);
                if (move != null) return move;
                if (current.equals(reachableGoldCandidate)) {
                    // Наличие Glitter будет отражено после следующего наблюдения клетки.
                    if (percepts != null && percepts.hasGlitter()) return HunterCommand.GRAB_GOLD;
                }
            }
        }

        // Базовая осторожная стратегия: исследуем всё доступное, что уже доказано безопасным.
        Position safeTarget = pathfinder.nearestSafeUnvisited(current, map);
        if (safeTarget != null) {
            HunterCommand move = followPath(pathfinder.shortestPath(current, safeTarget, map), current, direction);
            if (move != null) return move;
        }
        List<Position> riskAwareSafePath = pathfinder.safestPathToSafeUnvisited(current, map);
        if (!riskAwareSafePath.isEmpty()) {
            HunterCommand move = followPath(riskAwareSafePath, current, direction);
            if (move != null) return move;
        }

        // После убийства Вампуса сначала исследуем наиболее перспективные комнаты-соседи,
        // поскольку по правилам мира золото лежит в одной из них.
        if (wumpusKilled && map.getGoldPosition() == null) {
            List<Position> goldCandidates = map.getPossibleGoldPositions(start);
            Pathfinder.FrontierPlan goldFrontier = pathfinder.safestFrontierAmong(current, map, goldCandidates);
            HunterCommand goldRiskAction = followFrontier(goldFrontier, current, direction);
            if (goldRiskAction != null) return goldRiskAction;
        }

        // Нет гарантированно безопасных неисследованных клеток: оцениваем риски по KB.
        Pathfinder.FrontierPlan frontier = pathfinder.safestFrontier(current, map);
        HunterCommand frontierAction = followFrontier(frontier, current, direction);
        return frontierAction == null ? HunterCommand.WAIT : frontierAction;
    }

    private HunterCommand followFrontier(Pathfinder.FrontierPlan frontier, Position current, Direction direction) {
        if (frontier == null) return null;
        if (!current.equals(frontier.getAnchor())) {
            HunterCommand move = followPath(frontier.getPathToAnchor(), current, direction);
            if (move != null) return move;
        }
        Direction targetDirection = directionBetween(current, frontier.getTarget());
        if (targetDirection == null) return null;
        if (direction != targetDirection) return turnToward(direction, targetDirection);
        return HunterCommand.FORWARD;
    }

    private Position nearestFromList(Position current, Direction direction, List<Position> candidates, KnowledgeMap map) {
        Position best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Position p : candidates) {
            if (!map.isKnownSafe(p)) continue;
            List<Position> path = pathfinder.shortestPath(current, p, map);
            int distance = current.equals(p) ? 0 : path.isEmpty() ? Integer.MAX_VALUE : path.size();
            if (distance < bestDistance) { best = p; bestDistance = distance; }
        }
        return best;
    }

    private HunterCommand followPath(List<Position> path, Position current, Direction facing) {
        if (path == null || path.isEmpty()) return null;
        Direction desired = directionBetween(current, path.get(0));
        if (desired == null) return null;
        return facing == desired ? HunterCommand.FORWARD : turnToward(facing, desired);
    }
    private Direction directionBetween(Position from, Position to) {
        if (from.getRow() - 1 == to.getRow() && from.getCol() == to.getCol()) return Direction.UP;
        if (from.getRow() + 1 == to.getRow() && from.getCol() == to.getCol()) return Direction.DOWN;
        if (from.getCol() - 1 == to.getCol() && from.getRow() == to.getRow()) return Direction.LEFT;
        if (from.getCol() + 1 == to.getCol() && from.getRow() == to.getRow()) return Direction.RIGHT;
        return null;
    }
    private Direction directionToLine(Position from, Position to) {
        if (from.getRow() == to.getRow()) return to.getCol() < from.getCol() ? Direction.LEFT : to.getCol() > from.getCol() ? Direction.RIGHT : null;
        if (from.getCol() == to.getCol()) return to.getRow() < from.getRow() ? Direction.UP : to.getRow() > from.getRow() ? Direction.DOWN : null;
        return null;
    }
    private HunterCommand turnToward(Direction current, Direction target) {
        if (current.turnLeft() == target) return HunterCommand.TURN_LEFT;
        if (current.turnRight() == target) return HunterCommand.TURN_RIGHT;
        return HunterCommand.TURN_RIGHT;
    }
}
