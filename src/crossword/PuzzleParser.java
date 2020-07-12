/*
 * Copyright (c) 2017-2018 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course
 * staff.
 */
package crossword;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.mit.eecs.parserlib.ParseTree;
import edu.mit.eecs.parserlib.Parser;
import edu.mit.eecs.parserlib.UnableToParseException;
// import edu.mit.eecs.parserlib.Visualizer;

public class PuzzleParser {

    private static final int ROW_INDEX = 3;
    private static final int COL_INDEX = 4;

    /**
     * Main method. Parses test input.
     * 
     * @param args command line arguments, not used
     * @throws UnableToParseException if input can't be parsed
     */
    public static void main(final String[] args) throws UnableToParseException {
        // test puzzle input with Java-style single-line comments
        final String input = ">> \"Simple Puzzle\" \"A trivial puzzle designed to show how puzzles work\" // works \n"
                + "//again\n" + " (cat, \"feline companion\", DOWN, 0, 1) // comment haha \n // another comment\n"
                + " (mat, \"lounging place for feline companion\", ACROSS, 1, 0)//end";
        System.out.println(input);
        final Puzzle puzzle = PuzzleParser.parse("id", input);
        System.out.println(puzzle.getName() + ": number of word entries= " + puzzle.getWordEntries().size());
    }

    // the nonterminals of the grammar
    private static enum PuzzleGrammar {
        FILE, NAME, DESCRIPTION, ENTRY, WORDNAME, CLUE, DIRECTION, ROW, COL, STRING, STRINGINDENT, INT, WHITESPACE, WHITESPACE2, COMMENT,
    }

    private static Parser<PuzzleGrammar> parser = makeParser();

    /**
     * Compile the grammar into a parser.
     * 
     * @return parser for the grammar
     * @throws RuntimeException if grammar file can't be read or has syntax errors
     */
    private static Parser<PuzzleGrammar> makeParser() {
        try {
            // read the grammar as a file, relative to the project root.
            final File grammarFile = new File("src/crossword/Puzzle.g");
            return Parser.compile(grammarFile, PuzzleGrammar.FILE);

            // Parser.compile() throws two checked exceptions.
            // Translate these checked exceptions into unchecked RuntimeExceptions,
            // because these failures indicate internal bugs rather than client errors
        } catch (IOException e) {
            throw new RuntimeException("can't read the grammar file", e);
        } catch (UnableToParseException e) {
            throw new RuntimeException("the grammar has a syntax error", e);
        }
    }

    /**
     * Parse a string into a Puzzle.
     * 
     * @param puzzleId puzzle id
     * @param content  string to parse
     * @return Expression parsed from the string
     * @throws UnableToParseException if the string doesn't match the Puzzle grammar
     */
    public static Puzzle parse(String puzzleId, final String content) throws UnableToParseException {
        // parse the example into a parse tree
        final ParseTree<PuzzleGrammar> parseTree = parser.parse(content);

        // display the parse tree in various ways, for debugging only
        // System.out.println("parse tree " + parseTree);
        // Visualizer.showInBrowser(parseTree);

        // only need filename without path for puzzle id, and without '.puzzle' at the end
        int lastIndex = puzzleId.lastIndexOf(File.separator);
        if (lastIndex >= 0) {
            puzzleId = puzzleId.substring(lastIndex + 1, puzzleId.lastIndexOf('.'));
        }

        // make an AST from the parse tree
        final Puzzle puzzle = makeAbstractSyntaxTree(puzzleId, parseTree);
        // System.out.println("AST " + expression);

        return puzzle;
    }

    /**
     * Convert a parse tree into an abstract syntax tree.
     * 
     * @param parseTree constructed according to the grammar in Puzzle.g
     * @return abstract syntax tree corresponding to parseTree
     */
    private static Puzzle makeAbstractSyntaxTree(final String puzzleId, final ParseTree<PuzzleGrammar> parseTree) {
        if (parseTree.name().equals(PuzzleGrammar.FILE)) {
            // Grammar: file ::= ">>" name description "\n" entry*;
            final List<ParseTree<PuzzleGrammar>> children = parseTree.children();
            String name = getGrammarString(children.get(0));
            String desc = getGrammarString(children.get(1));
            
            // parse entries
            ArrayList<Entry> entries = new ArrayList<>();
            for (int i = 2; i < children.size(); ++i) {
                // Grammar: entry ::= "(" wordName "," clue "," direction "," row "," col ")";
                final List<ParseTree<PuzzleGrammar>> entryChildren = children.get(i).children();
                String word = entryChildren.get(0).text();
                String clue = getGrammarString(entryChildren.get(1));
                String direction = entryChildren.get(2).text();
                int row = Integer.parseInt(entryChildren.get(ROW_INDEX).text());
                int col = Integer.parseInt(entryChildren.get(COL_INDEX).text());

                Entry entry = new Entry(word, clue, direction, row, col);
                entries.add(entry);
            }

            // create puzzle
            return new Puzzle(puzzleId, name, desc, entries);
        } else {
            throw new AssertionError("should not get here");
        }
    }

    // Get the content of Grammar String or StringIndent without the beginning and
    // ending double quotes
    private static String getGrammarString(final ParseTree<PuzzleGrammar> parseTree) {
        String text = parseTree.text();
        // take out beginning and ending double quotes
        text = text.substring(1, text.length() - 1);
        return text;
    }

}
