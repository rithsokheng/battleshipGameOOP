package com.battleship.model;

import java.util.Objects;

/** Immutable algebraic coordinate (e.g. "B5") on a dynamically sized board. */
public final class Coordinate {

    private final int row;
    private final int col;

    public Coordinate(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /** Parses algebraic notation like "A1" or "J10" into a Coordinate (0-indexed internally). */
    public static Coordinate fromAlgebraic(String notation) {
        char letter = Character.toUpperCase(notation.charAt(0));
        int col = letter - 'A';
        int row = Integer.parseInt(notation.substring(1)) - 1;
        return new Coordinate(row, col);
    }

    public boolean isWithinBounds(int boardSize) {
        return row >= 0 && row < boardSize && col >= 0 && col < boardSize;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    @Override
    public String toString() {
        return String.valueOf((char) ('A' + col)) + (row + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinate)) return false;
        Coordinate that = (Coordinate) o;
        return row == that.row && col == that.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}
