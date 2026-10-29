package model;

/** Направление, в котором смотрит охотник. */
public enum Direction {
    UP(-1, 0, "↑"),
    RIGHT(0, 1, "→"),
    DOWN(1, 0, "↓"),
    LEFT(0, -1, "←");

    private final int rowDelta;
    private final int colDelta;
    private final String symbol;

    Direction(int rowDelta, int colDelta, String symbol) {
        this.rowDelta = rowDelta;
        this.colDelta = colDelta;
        this.symbol = symbol;
    }

    public int getRowDelta() {
        return rowDelta;
    }

    public int getColDelta() {
        return colDelta;
    }

    public String getSymbol() {
        return symbol;
    }

    public Direction turnLeft() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    public Direction turnRight() {
        return values()[(ordinal() + 1) % values().length];
    }
}
