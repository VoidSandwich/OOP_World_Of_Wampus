package model;

import java.util.Objects;

/** Координаты клетки. Внутри программы row/col начинаются с нуля. */
public final class Position {
    private final int row;
    private final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Position step(Direction direction) {
        Objects.requireNonNull(direction, "direction");
        return new Position(row + direction.getRowDelta(), col + direction.getColDelta());
    }

    public int manhattanDistance(Position other) {
        return Math.abs(row - other.row) + Math.abs(col - other.col);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Position)) return false;
        Position position = (Position) object;
        return row == position.row && col == position.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    /** Для журнала и интерфейса координаты показываются с единицы: (строка, столбец). */
    @Override
    public String toString() {
        return "(" + (row + 1) + ", " + (col + 1) + ")";
    }
}
