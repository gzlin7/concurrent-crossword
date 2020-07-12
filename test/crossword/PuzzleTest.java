/*
 * Copyright (c) 2018 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course
 * staff.
 */
package crossword;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.mit.eecs.parserlib.Parser;

public class PuzzleTest {

    // Testing strategy for parseFromFile():
    // file: file not exist, valid puzzle file, invalid puzzle file

    // Testing strategy for Constructor:
    // entries: consistent entries, intersect inconsistent, overlap inconsistent,
    // words not unique

    @Test public void testAssertionsEnabled() {
        assertThrows(AssertionError.class, () -> {
            assert false;
        }, "make sure assertions are enabled with VM argument '-ea'");
    }

    @Test public void testParserLibVersion() {
        assertThat("parserlib.jar needs to be version 3.1.x", Parser.VERSION, startsWith("3.1"));
    }

    // cover parseFromFile():
    // File not exist
    @Test public void testMissingFileException() throws IOException {
        // file not exists
        assertThrows(IOException.class, () -> {
            Puzzle.parseFromFile("puzzles/no-such-file.puzzle");
        });
    }

    // cover parseFromFile():
    // valid puzzle file
    @Test public void testParsePuzzle() throws IOException {
        Puzzle puzzle = Puzzle.parseFromFile("puzzles" + File.separator + "simple.puzzle");
        assertEquals("simple", puzzle.getId(), "expected id is filename only without path");
        assertEquals(8, puzzle.getWordEntries().size(), "expected 8 entries");
        assertEquals("Easy", puzzle.getName(), "incorrect name parsed from file");
        assertEquals("An easy puzzle to get started", puzzle.getDescription(), "incorrect description parsed from file");
    }

    // cover parseFromFile():
    // invalid puzzle file
    @Test public void testParseInconsistentPuzzle() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> {
            Puzzle.parseFromFile("puzzles/inconsistent.puzzle");
        });
    }

    // cover Constructor with inconsistent intersect entries
    @Test public void testEntriesInconsistentIntersect() {
        Entry entry1 = new Entry("cat", "feline companion", "DOWN", 0, 0);
        Entry entry2 = new Entry("mat", "lounging place for feline companion", "ACROSS", 1, 0);
        List<Entry> entries = List.of(entry1, entry2);
        assertThrows(IllegalArgumentException.class, () -> {
            new Puzzle("id", "puzzle inconsistent intersect", "desc", entries);
        });
    }

    // cover Constructor with inconsistent overlap entries
    @Test public void testEntriesInconsistentOverlap() {
        Entry entry1 = new Entry("there", "This word is fine", "ACROSS", 0, 0);
        Entry entry2 = new Entry("real", "Illegal overlap with there", "ACROSS", 0, 3);
        List<Entry> entries = List.of(entry1, entry2);
        assertThrows(IllegalArgumentException.class, () -> {
            new Puzzle("id", "bad overlap", "desc", entries);
        });

        Entry entry3 = new Entry("starting", "This word is fine", "DOWN", 2, 2);
        Entry entry4 = new Entry("art", "Illegal overlap with there", "DOWN", 4, 2);
        List<Entry> entries2 = List.of(entry3, entry4);
        assertThrows(IllegalArgumentException.class, () -> {
            new Puzzle("id", "bad overlap", "desc", entries2);
        });
    }

    // cover Constructor with inconsistent non-unique entries
    @Test public void testEntriesInconsistentUnique() {
        Entry entry1 = new Entry("cat", "feline companion", "DOWN", 0, 1);
        Entry entry2 = new Entry("mat", "lounging place for feline companion", "ACROSS", 1, 0);
        Entry entry3 = new Entry("cat", "meow", "ACROSS", 4, 0);
        List<Entry> entries = List.of(entry1, entry2, entry3);
        assertThrows(IllegalArgumentException.class, () -> {
            new Puzzle("id", "puzzle word not unique", "desc", entries);
        });
    }
}
