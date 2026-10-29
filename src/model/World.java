package model;

/** Реальный мир. Агенту следует пользоваться датчиками, а не читать содержимое клеток. */
public final class World {
    private final int size;
    private final WorldCell[][] cells;
    private final Position startPosition;
    private final Hunter hunter;
    private boolean lastBump;
    private boolean lastScream;

    World(int size, WorldCell[][] cells) {
        this.size = size;
        this.cells = cells;
        this.startPosition = new Position(size - 1, 0);
        this.hunter = new Hunter(startPosition, Direction.RIGHT);
    }
    public int getSize() { return size; }
    public Position getStartPosition() { return startPosition; }
    public Hunter getHunter() { return hunter; }
    public boolean isInside(Position p) {
        return p != null && p.getRow() >= 0 && p.getRow() < size && p.getCol() >= 0 && p.getCol() < size;
    }

    /** Ветерок и запах появляются только в клетках с ортогональным расстоянием 1. */
    public Percepts getCurrentPercepts() {
        if (!hunter.isAlive() || hunter.hasExited()) return Percepts.none();
        Position p = hunter.getPosition();
        WorldCell cell = cells[p.getRow()][p.getCol()];
        boolean breeze = adjacentPit(p);
        // В соответствии с учебником запах сохраняется даже после гибели Вампуса.
        boolean stench = adjacentWumpus(p);
        Percepts percepts = new Percepts(breeze, stench, cell.hasGold() && !hunter.hasGold(), lastBump, lastScream);
        lastBump = false;
        lastScream = false;
        return percepts;
    }

    public ActionResult turnLeft() {
        if (!canAct()) return ActionResult.rejected("Охотник не может действовать после завершения игры.");
        hunter.turnLeft();
        return ok("Охотник повернул налево.");
    }
    public ActionResult turnRight() {
        if (!canAct()) return ActionResult.rejected("Охотник не может действовать после завершения игры.");
        hunter.turnRight();
        return ok("Охотник повернул направо.");
    }
    public ActionResult moveForward() {
        if (!canAct()) return ActionResult.rejected("Охотник не может двигаться после завершения игры.");
        Position next = hunter.getPosition().step(hunter.getDirection());
        if (!isInside(next)) {
            lastBump = true;
            return new ActionResult(false, "Удар: впереди стена.", false, "", true, false, false, false);
        }
        hunter.setPosition(next);
        WorldCell cell = cells[next.getRow()][next.getCol()];
        if (cell.hasPit()) {
            hunter.setAlive(false);
            return new ActionResult(true, "Охотник упал в яму и погиб в клетке " + next + ".", true, "яма", false, false, false, false);
        }
        if (cell.hasWumpus() && !hunter.hasKilledWumpus()) {
            hunter.setAlive(false);
            return new ActionResult(true, "Охотник встретил живого Вампуса и погиб в клетке " + next + ".", true, "Вампус", false, false, false, false);
        }
        return ok("Переход в клетку " + next + ".");
    }
    public ActionResult shoot() {
        if (!canAct()) return ActionResult.rejected("Охотник не может стрелять после завершения игры.");
        if (!hunter.hasArrow()) return ActionResult.rejected("Стрела уже израсходована.");
        hunter.spendArrow();
        Position p = hunter.getPosition().step(hunter.getDirection());
        while (isInside(p)) {
            WorldCell cell = cells[p.getRow()][p.getCol()];
            if (cell.hasWumpus() && !hunter.hasKilledWumpus()) {
                hunter.setWumpusKilled(true);
                lastScream = true;
                return new ActionResult(true, "Стрела попала в Вампуса! По пещере разнёсся крик.", false, "", false, true, false, false);
            }
            p = p.step(hunter.getDirection());
        }
        return ok("Стрела улетела мимо. Она израсходована.");
    }
    public ActionResult grabGold() {
        if (!canAct()) return ActionResult.rejected("Охотник не может взять золото после завершения игры.");
        WorldCell cell = cells[hunter.getPosition().getRow()][hunter.getPosition().getCol()];
        if (!cell.hasGold() || hunter.hasGold()) return ActionResult.rejected("В этой клетке нет доступного золота.");
        if (!hunter.hasKilledWumpus()) return ActionResult.rejected("Сначала необходимо убить Вампуса.");
        hunter.setHasGold(true);
        return new ActionResult(true, "Золото забрано. Теперь нужно вернуться в стартовую комнату.", false, "", false, false, true, false);
    }
    /** Climb/Выход разрешён только из стартовой комнаты, как в описании среды. */
    public ActionResult climb() {
        if (!canAct()) return ActionResult.rejected("Игра уже завершена.");
        if (!hunter.getPosition().equals(startPosition)) return ActionResult.rejected("Выйти можно только из стартовой комнаты " + startPosition + ".");
        hunter.setExited(true);
        String message = hunter.hasGold() ? "Охотник вышел из пещеры с золотом! Победа." : "Охотник вышел из пещеры без золота.";
        return new ActionResult(true, message, false, "", false, false, false, true);
    }
    /** Только режим наблюдателя GUI может вызывать этот метод. */
    public WorldCell getCellForObserver(int row, int col) {
        if (row < 0 || row >= size || col < 0 || col >= size) throw new IndexOutOfBoundsException("Клетка вне карты");
        return cells[row][col];
    }
    private boolean canAct() { return hunter.isAlive() && !hunter.hasExited(); }
    private ActionResult ok(String message) { return new ActionResult(true, message, false, "", false, false, false, false); }
    private boolean adjacentPit(Position p) {
        int r = p.getRow(), c = p.getCol();
        return pitAt(r - 1, c) || pitAt(r + 1, c) || pitAt(r, c - 1) || pitAt(r, c + 1);
    }
    private boolean pitAt(int r, int c) { return r >= 0 && r < size && c >= 0 && c < size && cells[r][c].hasPit(); }
    private boolean adjacentWumpus(Position p) {
        int r = p.getRow(), c = p.getCol();
        return wumpusAt(r - 1, c) || wumpusAt(r + 1, c) || wumpusAt(r, c - 1) || wumpusAt(r, c + 1);
    }
    private boolean wumpusAt(int r, int c) { return r >= 0 && r < size && c >= 0 && c < size && cells[r][c].hasWumpus(); }
}
