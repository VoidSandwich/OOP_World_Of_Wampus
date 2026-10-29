package model;

/** Физическое состояние охотника. База логических знаний находится в отдельном классе. */
public final class Hunter {
    private Position position;
    private Direction direction;
    private boolean alive = true;
    private boolean hasArrow = true;
    private boolean hasGold;
    private boolean wumpusKilled;
    private boolean exited;

    Hunter(Position position, Direction direction) { this.position = position; this.direction = direction; }
    public Position getPosition() { return position; }
    public Direction getDirection() { return direction; }
    public boolean isAlive() { return alive; }
    public boolean hasArrow() { return hasArrow; }
    public boolean hasGold() { return hasGold; }
    public boolean hasKilledWumpus() { return wumpusKilled; }
    public boolean hasExited() { return exited; }
    void setPosition(Position position) { this.position = position; }
    void turnLeft() { direction = direction.turnLeft(); }
    void turnRight() { direction = direction.turnRight(); }
    void spendArrow() { hasArrow = false; }
    void setAlive(boolean alive) { this.alive = alive; }
    void setHasGold(boolean hasGold) { this.hasGold = hasGold; }
    void setWumpusKilled(boolean value) { wumpusKilled = value; }
    void setExited(boolean value) { exited = value; }
}
