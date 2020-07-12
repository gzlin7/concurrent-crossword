/*
 * Copyright (c) 2019 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course
 * staff.
 */
package crossword;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import crossword.Entry.Direction;
import edu.mit.eecs.parserlib.UnableToParseException;

/**
 * Immutable type
 * Puzzle represents the solutions to a consistent crossword puzzle (as defined
 * in project handout) that can be played by two players in a match.
 * Positions are 0-indexed.
 * 
 */
public class Puzzle {
    // fields will include name, puzzle id (filename), description, numbered word
    // entries, clues, and possibly dimensions (for easier rendering)

    /**
     * Make a new puzzle by parsing a file.
     * 
     * @param filename path to a puzzle file
     * @return a new crossword puzzle with the name, description, word entries,
     *         and clues from the given file
     * @throws IOException              if an error occurs reading the file.
     * @throws IllegalArgumentException if the file is syntactically invalid
     *                                  or the file is inconsistent as defined in
     *                                  the handout.
     */
    public static Puzzle parseFromFile(String filename) throws IOException, IllegalArgumentException {
        try {
            // read entire file content into String
            String content = new String(Files.readAllBytes(Paths.get(filename)));

            return PuzzleParser.parse(filename, content);
        } catch (UnableToParseException e) {
            // UnableToParse (puzzle is syntactically invalid)
            // throw IllegalArgumentException
            throw new IllegalArgumentException(e);
        }
    }

    private final String id;
    private final String name;
    private final String description;
    private final ArrayList<Entry> wordEntries;

    // Abstraction function(id, name, description, wordEntries):
    //   represents the puzzle with the given id, name, and description, with
    //   the given list of entries.
    //
    // Representation invariant(id, name, description, wordEntries):
    //   wordEntries should be consistent: intersection at same letter,
    //   words cannot overlap in the same direction, words should be unique
    //   puzzle id and name are not empty
    //
    // Safety from rep exposure:
    //   all fields are private, final
    //   all public methods return immutable types
    //   constructor: make a copy of the mutable List<Entry> input of immutable Entry
    //   getWordEntries(): makes a defensive copy of the return list
    //
    // Thread safety argument:
    //   immutable type, no beneficient mutation

    /**
     * Create a puzzle.
     * 
     * @param id      puzzle id, must not be empty
     * @param name    puzzle name, must not be empty
     * @param desc    description
     * @param entries word entries should be consistent (precondition)
     * @throws IllegalArgumentException if the entries inconsistent as defined in
     *                                  the handout.
     */
    public Puzzle(String id, String name, String desc, List<Entry> entries) {
        this.id = id;
        this.name = name;
        this.description = desc;

        // check if list of input entries is consistent
        if (!isConsistent(entries))
            throw new IllegalArgumentException("Word entries not consistent.");

        // make a copy of the mutable List input of immutable Entry
        wordEntries = new ArrayList<>(entries);

        checkRep();
    }

    // Check that the rep invariant is true
    private void checkRep() {
        assert this.name.length() > 0 : "Puzzle name is empty";
        assert this.id.length() > 0 : "Puzzle id is empty";
        assert isConsistent(wordEntries) : "Puzzle is not consistent";
    }
    
    /**
     * Returns a string containing all of the questions in the puzzle,
     * where the format is each question on its own line, with the question
     * in the form <code>"[id] "[hint]""</code>.
     * 
     * @return string containing all the question hints for the puzzle
     */
    public String questionsString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < wordEntries.size(); i++) {
            Entry e = wordEntries.get(i);
            result.append("\n").append(i + 1).append(" \"").append(e.getClue()).append("\"");
        }
        return result.toString().substring(1);  // cuts off initial new line
    }

    /**
     * Check if a given object is equals to this Puzzle.
     * Two puzzle are equal if they have the same ID
     * 
     * @param that any object
     * @return true if objects are equal
     */
    @Override public boolean equals(Object that) {
        return that instanceof Puzzle && this.sameValue((Puzzle) that);
    }
    
    /**
     * Private helper, asserts that the Puzzles are structurally equal.
     * 
     * @param that puzzle to check equality with
     * @return true if equal
     */
    private boolean sameValue(Puzzle that) {
        return this.id.equals(that.id);
    }

    /**
     * Return hash code.
     * 
     * @return hash code value consistent with the equals()
     */
    @Override public int hashCode() {
        return id.hashCode();
    }
    
    /**
     * Return String representation of the puzzle's entries. Entry answer (word) is not included.
     * Only the length, position, and orientation of the words, as well as their associated hint
     * e.g. 
     * 3 0 1 DOWN "feline companion"
     * 3 1 0 ACROSS "lounging place for feline companion"
     * @see {@link Entry#toString() Entry.toString} for formatting of each Entry
     */
    @Override public String toString() {
        StringBuilder builder = new StringBuilder();
        
        for (Entry entry: wordEntries) {
            builder.append(entry).append("\n");
        }
        return builder.toString();
    }

    /**
     * Get puzzle id
     * 
     * @return puzzle id
     */
    public String getId() {
        return id;
    }

    /**
     * Get puzzle name
     * 
     * @return puzzle name
     */
    public String getName() {
        return name;
    }

    /**
     * Get puzzle description
     * 
     * @return description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the size of the minimum size board that can contain this puzzle
     * 
     * @return list where first entry is number of rows needed, second entry is
     * number of columns needed.
     */
    public List<Integer> getBoardSize() {
        int maxRowEnd = -1;
        int maxColEnd = -1;
        for (Entry e : wordEntries) {
            if (e.getDirection() == Direction.ACROSS && e.getEnd() > maxColEnd) {
                maxColEnd = e.getEnd();
            }
            if (e.getDirection() == Direction.DOWN && e.getEnd() > maxRowEnd) {
                maxRowEnd = e.getEnd();
            }
        }
        return List.of(maxRowEnd + 1, maxColEnd + 1);  // add 1 to offset 0 index
    }
    
    /**
     * Checks if the given position is present in a word in the board; if this is
     * false, then the position given is an empty square (would be filled in black
     * in a typical newspaper crossword)
     * 
     * @param pos the position to check
     * @return true if the position is present in some entry
     */
    public boolean containsPosition(BoardPosition pos) {
        for (Entry e : wordEntries) {
            switch(e.getDirection()) {
            case ACROSS:
                if (pos.getRow() == e.getRow() &&  // in the same row
                        pos.getCol() >= e.getCol() &&  // after start of word
                        pos.getCol() <= e.getEnd()) {  // before end of word
                    return true;
                }
                break;
            case DOWN:
                if (pos.getCol() == e.getCol() &&  // in the same column
                        pos.getRow() >= e.getRow() &&  // after start of word
                        pos.getRow() <= e.getEnd()) {  // before end of word
                    return true;
                }
                break;
            default:
                throw new RuntimeException("Entry should always have direction");
            }  // end switch block
        }
        return false;
    }
    
    /**
     * Returns a list of the words starting at the given position. The words should follow the 
     * format <code>"[number] [direction]"</code>, so for example if word 1 going down
     * starts at this square then wordStartsHere should be <code>"1 DOWN"</code>.
     * @param pos the position to check for words starting at
     * @return the list of words starting at this position
     */
    public List<String> wordsStartingHere(BoardPosition pos) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < wordEntries.size(); i++) {
            Entry e = wordEntries.get(i);
            if (e.getRow() == pos.getRow() && e.getCol() == pos.getCol()) {
                result.add(Integer.toString(i + 1) + " " + e.getDirection());
            }
        }
        return result;
    }

    /**
     * Get word entries. List index + 1 will be the word id, so that word id start
     * from 1.
     * 
     * @return word entries
     */
    public List<Entry> getWordEntries() {
        // makes a defensive copy of the return list
        return new ArrayList<>(wordEntries);
    }

    // Check if the given word entries are consistent.
    private static boolean isConsistent(List<Entry> entries) {
        int total = entries.size();
        // compare each pair of word entries
        for (int i = 0; i < total - 1; ++i) {
            for (int j = i + 1; j < total; ++j) {
                Entry entry1 = entries.get(i);
                Entry entry2 = entries.get(j);
                if (entry1.getWord().equals(entry2.getWord())) {
                    // all words should be unique
                    return false;
                }

                if (entry1.getDirection() == entry2.getDirection()) {
                    if (entry1.getDirection() == Entry.Direction.ACROSS) {
                        if (entry1.getRow() == entry2.getRow() && entry2.getCol() <= entry1.getEnd()
                                && entry1.getCol() <= entry2.getEnd()) {
                            // 2 ACROSS words on the same row should not overlap
                            return false;
                        }
                    } else {
                        // both are DOWN
                        if (entry1.getCol() == entry2.getCol()) {
                            if (entry2.getRow() <= entry1.getEnd() && entry1.getRow() <= entry2.getEnd()) {
                                // 2 DOWN words on the same column should not overlap
                                return false;
                            }
                        }
                    }
                } else {
                    // different directions
                    Entry down, across;
                    if (entry1.getDirection() == Entry.Direction.ACROSS) {
                        across = entry1;
                        down = entry2;
                    } else {
                        across = entry2;
                        down = entry1;
                    }
                    // find index of ACROSS word
                    int index1 = down.getCol() - across.getCol();
                    if (index1 >= 0 && index1 <= (across.getEnd() - across.getCol())) {
                        // find index of DOWN word
                        int index2 = across.getRow() - down.getRow();
                        if (index2 >= 0 && index2 <= (down.getEnd() - down.getRow())
                                && across.getWord().charAt(index1) != down.getWord().charAt(index2)) {
                            // two words intersect, they must intersect at the same letter.
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}
