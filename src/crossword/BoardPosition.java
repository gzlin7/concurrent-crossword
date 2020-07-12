package crossword;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable class representing a position on a crossword board, containing some helpers
 * for common operations with board positions.
 * 
 * @author Andrew Churchill
 */
public class BoardPosition {    
    private final int row;
    private final int col;
    
    
    // Abstraction function(row, col):
    //   Represents the position on a board in row row and column col.
    // Representation invariant(row, col):
    //   row >= 0, col >= 0.
    // Safety from rep exposure:
    //   rep is only primitive types
    // Thread safety argument:
    //   immutable type (int and col are set once and never change); no beneficient 
    //   mutation either
    
    /**
     * Creates new BoardPosition instance, at specified row and column.
     * 
     * @param row row of position, must be nonnegative
     * @param col column of position, must be nonnegative
     */
    public BoardPosition(int row, int col) {
        this.row = row;
        this.col = col;
        checkRep();
    }

    /**
     * Asserts the rep invariant is true.
     */
    private void checkRep() {
        assert row >= 0 : "row is negative";
        assert col >= 0 : "col is negative";
    }
    
    /**
     * Gets the row of this board position
     * 
     * @return row number
     */
    public int getRow() {
        return this.row;
    }
    
    /**
     * Gets the column of this board position
     * 
     * @return column number
     */
    public int getCol() {
        return this.col;
    }
    
    // Static helper methods
    
    /**
     * Static helper method, gives a list of all board positions in a board
     * with the given dimensions, in row-major order.
     * <p>For example, allBoardPositions(3, 3) would return a list of the format:
     * <br>
     * <code>[(0, 0), (0, 1), (0, 2), (1, 0), (1, 1), (1, 2), (2, 0), (2, 1), (2, 2)]</code>.
     * 
     * @param rows rows in board, must be positive
     * @param cols columns in board, must be positive
     * @return list with BoardPositions for all locations on board in row major order
     */
    public static List<BoardPosition> allBoardPositions(int rows, int cols) {
        List<BoardPosition> allPositions = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                allPositions.add(new BoardPosition(row, col));
            }
        }
        return allPositions;
    }
    
    // Object contract methods
    
    @Override 
    public String toString() {
        return "BoardPosition(" + row + ", " + col + ")";
    }
    
    @Override
    public boolean equals(Object that) {
        return that instanceof BoardPosition && this.sameValue((BoardPosition) that);
    }
    
    /**
     * Checks if BoardPositions are structurally equal.
     * 
     * @param that other BoardPosition to check equality with
     * @return true if they are structurally equal
     */
    private boolean sameValue(BoardPosition that) {
        return this.row == that.row && this.col == that.col;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}
