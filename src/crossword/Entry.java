/*
 * Copyright (c) 2019 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course
 * staff.
 */
package crossword;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable type
 * Word Entry in a crossword puzzle solution.
 * Note: word id is maintained by Puzzle.
 * 
 */

public class Entry {
    // Abstraction function(word, clue, direction, row, col, end):
    //   Represents the entry in a crossword puzzle with word as answer and
    //   the given clue, going in the given direction, starting at the given
    //   row and column and ending at row end (if it's DOWN) or column end
    //   (if it's ACROSS).
    // Representation invariant:
    //   word and clue are both non-empty, and have no newlines; word has no whitespace;
    //   end - start = word.length() - 1 (where start = row if DOWN, col if ACROSS)
    // Safety from rep exposure:
    //   all fields are private, final
    //   all public methods return immutable types
    // Thread safety argument:
    //   immutable type, no beneficient mutation

    public static enum Direction {
        ACROSS, DOWN
    }

    private final String word;
    private final String clue;
    private final Direction direction;
    private final int row;
    private final int col;
    // row (for DOWN) or column (for ACROSS) number of last char of word
    private final int end;

    /**
     * Create a word entry.
     * 
     * @param word      word, must be nonempty and have no whitespace; capitalization
     *                  does not matter
     * @param clue      clue, must be nonempty
     * @param direction direction either ACROSS or DOWN
     * @param row       row coordinate (0-indexed) of first cell of the word
     * @param col       column coordinate (0-indexed) of first cell of the word
     */
    public Entry(String word, String clue, String direction, int row, int col) {
        this.word = word.toUpperCase();
        this.clue = clue;
        this.direction = Direction.valueOf(direction);
        this.row = row;
        this.col = col;

        int len = word.length();
        end = this.direction == Direction.ACROSS ? col + len - 1 : row + len - 1;
        checkRep();
    }
    
    /**
     * Asserts the rep invariant is true.
     */
    private void checkRep() {
        assert word.length() > 0 : "word in Entry is empty";
        assert clue.length() > 0 : "clue in Entry is empty";
        assert word.indexOf('\r') == -1 : "word contains new line";
        assert word.indexOf('\n') == -1 : "word contains new line";
        assert word.indexOf(' ') == -1 : "word contains whitespace";
        assert word.indexOf('\t') == -1 : "word contains whitespace (tab)";
        assert clue.indexOf('\r') == -1 : "clue contains new line";
        assert clue.indexOf('\n') == -1 : "clue contains new line";

        // start is the column if it's across, the row if it's down
        int start = this.direction == Direction.ACROSS ? this.col : this.row;
        assert this.end - start == word.length() - 1 : 
            "start " + start  + " and end " + end + 
            " are not consistent with word length " + word.length();
    }

    /**
     * Check if a given object is equals to this entry.
     * Two entries are equal if they have the same word, clue, direction, row, and
     * column.
     * 
     * @param that any object
     * @return true if objects are equal
     */
    @Override public boolean equals(Object that) {
        if (that instanceof Entry) {
            Entry other = (Entry) that;
            return (word.equals(other.word) && clue.equals(other.clue) && direction == other.direction
                    && row == other.row && col == other.col);
        } else {
            return false;
        }
    }

    /**
     * Return hash code.
     * 
     * @return hash code value consistent with the equals()
     */
    @Override public int hashCode() {
        return Objects.hash(word, clue, direction, row, col, end);
    }
    
    /**
     * Return String representation of the word entry without the answer (for security).
     * Only the length, position, and orientation of the words, as well as their associated hint
     * e.g. 3 0 1 DOWN "feline companion".
     * Matches the grammar: <p>
     * <code>
     * RESULT ::= LENGTH " " ROW " " COL " " DIRECTION " " CLUE<br>
     * LENGTH ::= Int<br>
     * ROW ::= Int<br>
     * COL ::= Int<br>
     * DIRECTION ::= "ACROSS" | "DOWN"<br>
     * CLUE ::= '"' [^\r\n] '"'<br>
     * Int ::= [0-9]+
     * </code>
     */
    @Override public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(word.length()).append(" ");
        builder.append(row).append(" ");
        builder.append(col).append(" ");
        builder.append(direction).append(" ");
        builder.append("\"").append(clue).append("\"");
        return builder.toString();
    }

    /**
     * Get word
     * 
     * @return word
     */
    public String getWord() {
        return word;
    }

    /**
     * Get clue
     * 
     * @return clue
     */
    public String getClue() {
        return clue;
    }

    /**
     * Get direction
     * 
     * @return direction either ACROSS or DOWN
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Get Row coordinate (0-indexed) of first cell of the word
     * 
     * @return row
     */
    public int getRow() {
        return row;
    }

    /**
     * Get Column coordinate (0-indexed) of first cell of the word
     * 
     * @return column
     */
    public int getCol() {
        return col;
    }

    /**
     * Get row (for DOWN) or column (for ACROSS) number of last char word
     * 
     * @return row (for DOWN) or column (for ACROSS) number of last char word
     */
    public int getEnd() {
        return end;
    }
    
    /**
     * Creates a list of the BoardPositions contained in this Entry. Starting
     * at the beginning of the word, and going in order to the end.
     * 
     * @return list of board positions contained
     */
    public List<BoardPosition> getPositions() {
        List<BoardPosition> result = new ArrayList<>();
        if (direction == Direction.ACROSS) {
            for (int c = col; c <= end; c++) {
                result.add(new BoardPosition(row, c));
            }
        }
        else { // direction is down
            for (int r = row; r <= end; r++) {
                result.add(new BoardPosition(r, col));
            }
        }
        return result;
    }

}
