package crossword;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class MatchTest {
    /*
     * Testing strategy:  
     *  Note that the only observers of the Match class are matchView, getId, getPlayers, and toString (isMatchOver is a mutator).
     *  All tests for Match should test based on the results from the observers
     *  (Mainly test based of the results from matchView because that is the observer that the game uses)
     *  
     *  Note that tryGuess and challenge both return feedback messages indicating if the
     *  guess/challenge was successful or unsuccessful, and then why it was unsuccessful if so.
     *  Also test based of these feedback messages from these mutators.
     *  
     *  Partition Operations on Match m as follows:
     *  
     *  Guesses:
     *      Guess is consistent with the state of the puzzle:
     *          guess conflicts with previous unconfirmed word that this player entered
     *          guess does not conflict at all
     *      
     *      
     *      Guess is not consistent with the state of the puzzle:
     *          length of guess is not correct
     *          guess is not cosistent with confirmed words in puzzle
     *          guess is not consistent with words entered by other players
     *  
     *  
     *  Challenges:
     *      Challenge satisfies the validity constraints:
     *          the challenged word was originally correct
     *          the challenged word was originally incorrect, the proposed word is correct:
     *              *Include the special case where multiple words that are inconsistent with this correct challenge get cleared 
     *          the challenged word was originally incorrect, the proposed word is also incorrect.
     *      
     *      Challenge does not satisfy the validity constraints:
     *          the challenged word was entered by this player (can’t challenge your own words)
     *          the challenged word was already confirmed
     *          the proposed word is the same as the word already there
     *          the proposed word is not of the correct length
     *          
     *  
     *  Miscellaneous:
     *      test player is added to the game
     *      test game ends when all cells have their correct letters
     *  
     *  
     */    
    
    private static final String SIMPLE_PUZZLE_FILE = "puzzles" + File.separator + "simple.puzzle";
    private static final String MIN_PUZZLE_FILE = "puzzles" + File.separator + "minimal.puzzle";
    private static final String VALID_GUESS_RESPONSE = "Valid guess";
    private static final String INVALID_GUESS_LENGTH = "Invalid guess, wrong word length";
    private static final String INVALID_GUESS_ALL_SAME = "Invalid guess, same as existing guess";
    private static final String INVALID_GUESS_INCONSISTENT = "Invalid guess, inconsistent with current board";
    private static final String CHALLENGE_SUCCESS_RESPONSE = "Successful challenge!";
    private static final String INCORRECT_CHALLENGE_ALREADY_CORRECT = "Failed challenge, target word was already correct";
    private static final String INCORRECT_CHALLENGE_BOTH_INCORRECT = "Failed challenge, target word and your guess both incorrect";
    private static final String INVALID_CHALLENGE = "Invalid challenge";
    private static final String INVALID_CHALLENGE_LENGTH = "Invalid challenge, wrong length";
    private static final String INVALID_CHALLENGE_CONFIRMED = "Invalid challenge, all spaces already confirmed";
    private static final String INVALID_CHALLENGE_NO_DIFFERENCE = "Invalid challenge, same as existing word";
    private static final String INVALID_CHALLENGE_YOURS = "Invalid challenge, you control this word";
    private static final String INVALID_CHALLENGE_NO_GUESS = "Invalid challenge, not all squares have guesses";

    
    @Test public void testAssertionsEnabled() {
        assertThrows(AssertionError.class, () -> {
            assert false;
        }, "make sure assertions are enabled with VM argument '-ea'");
    }
    
    // tests toString
    @Test
    public void testToString() throws IOException {
        Puzzle puzzle = Puzzle.parseFromFile(SIMPLE_PUZZLE_FILE);
        Match m = new Match("match1", "the best possible match", puzzle, "adchurch");
        String expected = "match1 \"the best possible match\"";
        assertEquals(expected, m.toString(), "toString is incorrect");
    }
    
    // tests string rep for board before any gameplay occurs
    @Test
    public void testSimpleStringRep() throws IOException {
        Puzzle puzzle = Puzzle.parseFromFile(MIN_PUZZLE_FILE);
        Match m = new Match("match1", "the next best possible match", puzzle, "gzlin");
        String expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "_ 1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "_ 2 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "_ 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
//                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("gzlin"), "incorrect match view string");
        m.addPlayer("lconboy");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "_ 1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "_ 2 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "_ 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("gzlin"), "incorrect match view string");
    }
    
    // test guessing a word, clearing with another guess, second user invalid guess,
    // second user valid guess
    @Test
    public void testBasicGuesses() throws IOException {
        Puzzle puzzle = Puzzle.parseFromFile(MIN_PUZZLE_FILE);
        Match m = new Match("match1", "the next best possible match", puzzle, "gzlin");
        m.addPlayer("lconboy");
        
        // invalid guess, too long
        String response = m.tryGuess("gzlin", 1, "catoctopus");
        assertEquals(INVALID_GUESS_LENGTH, response, "incorrect error message");
        String expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "_ 1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "_ 2 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "_ 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("gzlin"), "incorrect match view string");
        
        // valid guess (but stupid as hell)
        response = m.tryGuess("gzlin", 1, "crt");
        assertEquals(VALID_GUESS_RESPONSE, response, "incorrect response message");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C >1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "_ 2 ACROSS\n" + 
                "R\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "T 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("gzlin"), "incorrect match view string");
        
        // valid guess, keeps first guess
        response = m.tryGuess("gzlin", 2, "mrt");
        assertEquals(VALID_GUESS_RESPONSE, response, "incorrect response message");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C >1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "M >2 ACROSS\n" + 
                "R\n" + 
                "T\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "T 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("gzlin"), "incorrect match view string");
        
        // valid guess, overwrites first
        response = m.tryGuess("gzlin", 4, "FAX");
        assertEquals(VALID_GUESS_RESPONSE, response, "incorrect response message");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "_ 1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "M >2 ACROSS\n" + 
                "R\n" + 
                "T\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "F >4 ACROSS\n" + 
                "A\n" + 
                "X\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("gzlin"), "incorrect match view string");
        // check other perspective
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "_ 1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "M 2 ACROSS\n" + 
                "R\n" + 
                "T\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "F 4 ACROSS\n" + 
                "A\n" + 
                "X\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("lconboy"), "incorrect match view string");
        
        // try conflicting guess from other user
        response = m.tryGuess("lconboy", 1, "CAT");
        assertEquals(INVALID_GUESS_INCONSISTENT, response, "incorrect response message");
        
        // try valid guess from other user
        response = m.tryGuess("lconboy", 3, "car");
        assertEquals(VALID_GUESS_RESPONSE, response, "incorrect response message");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C 1 DOWN 3 ACROSS\n" + 
                "A\n" + 
                "R\n" + 
                "M >2 ACROSS\n" + 
                "R\n" + 
                "T\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "F >4 ACROSS\n" + 
                "A\n" + 
                "X\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("gzlin"), "incorrect match view string");
        // check other perspective
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C 1 DOWN >3 ACROSS\n" + 
                "A\n" + 
                "R\n" + 
                "M 2 ACROSS\n" + 
                "R\n" + 
                "T\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "F 4 ACROSS\n" + 
                "A\n" + 
                "X\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("lconboy"), "incorrect match view string");
    }
    
    // test basic challenges: invalid challenge, failed challenge because both incorrect, 
    // failed challenge on correct word, successful challenge clearing inconsistent input
    @Test
    public void testBasicChallenges() throws IOException {
        Puzzle puzzle = Puzzle.parseFromFile(MIN_PUZZLE_FILE);
        Match m = new Match("match1", "the next next best possible match", puzzle, "gzlin");
        m.addPlayer("lconboy");
        
        // setup board with guess
        String response = m.tryGuess("gzlin", 3, "cat");
        assertEquals(VALID_GUESS_RESPONSE, response, "incorrect response message");
        String expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C 1 DOWN >3 ACROSS\n" + 
                "A\n" + 
                "T\n" + 
                "_ 2 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "_ 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("gzlin"), "incorrect match view string");
        
        // rejected challenge, too short
        response = m.challenge("lconboy", 3, "tr");
        assertEquals(INVALID_CHALLENGE_LENGTH, response, "incorrect error message");

        // failed challenge, both incorrect
        response = m.challenge("lconboy", 3, "rum");
        assertEquals(INCORRECT_CHALLENGE_BOTH_INCORRECT, response, "incorrect error message");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "_ 1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "_ 2 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "_ 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy -1\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("lconboy"), "incorrect match view string");
        
        // add correct guess
        response = m.tryGuess("gzlin", 3, "car");
        assertEquals(VALID_GUESS_RESPONSE, response, "incorrect response message");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C 1 DOWN 3 ACROSS\n" + 
                "A\n" + 
                "R\n" + 
                "_ 2 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "_ 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy -1\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("lconboy"), "incorrect match view string");
        
        // failed challenge, original is correct
        response = m.challenge("lconboy", 3, "BUT");
        assertEquals(INCORRECT_CHALLENGE_ALREADY_CORRECT, response, "incorrect response message");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "+C 1 DOWN 3 ACROSS\n" + 
                "+A\n" + 
                "+R\n" + 
                "_ 2 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "_ 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy -2\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("lconboy"), "incorrect match view string");
        
        // add incorrect guesses
        response = m.tryGuess("lconboy", 4, "PAT");
        assertEquals(VALID_GUESS_RESPONSE, response, "incorrect response message");
        response = m.tryGuess("gzlin", 1, "CAP");
        assertEquals(VALID_GUESS_RESPONSE, response, "incorrect response message");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "+C 1 DOWN 3 ACROSS\n" + 
                "+A\n" + 
                "+R\n" + 
                "_ 2 ACROSS\n" + 
                "A\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "P >4 ACROSS\n" + 
                "A\n" + 
                "T\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy -2\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("lconboy"), "incorrect match view string");

        // make correct challenge, clear out inconsistent answers
        response = m.challenge("gzlin", 4, "TAX");
        assertEquals(CHALLENGE_SUCCESS_RESPONSE, response, "incorrect response message");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "+C 1 DOWN >3 ACROSS\n" + 
                "+A\n" + 
                "+R\n" + 
                "_ 2 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "+T >4 ACROSS\n" + 
                "+A\n" + 
                "+X\n" + 
                "Scores:\n" + 
                "gzlin 2\n" + 
                "lconboy -2\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView("gzlin"), "incorrect match view string");
    }
    
    // player 1 guesses 1 word, then conflicting overlapping word, then conflicting same word,
    // then consistent overlapping word; player 2 guesses incorrect length word, then same word as
    // player 1 (different guess though), then same word as player 1 (same guess), then conflicting
    // overlapping word, then invalid word ID, then consistent overlapping word. Player 1 tries 
    // challenging word with some empty squares
    @Test
    public void testLotsOfGuesses() throws IOException {
        Puzzle puzzle = Puzzle.parseFromFile(MIN_PUZZLE_FILE);
        final String player1 = "gzlin";
        final String player2 = "lconboy";
        Match m = new Match("match1", "the next next best possible match", puzzle, player1);
        m.addPlayer(player2);
        
        // player 1 makes their guesses
        m.tryGuess(player1, 1, "CAT");
        String expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C >1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "_ 2 ACROSS\n" + 
                "A\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "T 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player1), "incorrect match view string");
        m.tryGuess(player1, 2, "MOP");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "_ 1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "M >2 ACROSS\n" + 
                "O\n" + 
                "P\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "_ 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player1), "incorrect match view string");
        String response = m.tryGuess(player1, 2, "MAP");
        assertEquals(VALID_GUESS_RESPONSE, response, "incorrect feedback response");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "_ 1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "M >2 ACROSS\n" + 
                "A\n" + 
                "P\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "_ 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player1), "incorrect match view string");
        m.tryGuess(player1, 1, "CAP");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C >1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "M >2 ACROSS\n" + 
                "A\n" + 
                "P\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "P 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player1), "incorrect match view string");
        assertEquals(expected.replaceAll(">", ""), m.matchView(player2), "incorrect match view string");
        
        // player 2 tries guesses
        response = m.tryGuess(player2, 1, "CATT");
        assertEquals(INVALID_GUESS_LENGTH, response, "incorrect feedback response");
        response = m.tryGuess(player2, 1, "CAT");
        assertEquals(INVALID_GUESS_INCONSISTENT, response, "incorrect feedback response");
        response = m.tryGuess(player2, 1, "CAP");
        assertEquals(INVALID_GUESS_ALL_SAME, response, "incorrect feedback response");
        response = m.tryGuess(player2, 4, "TAX");
        assertThrows(IllegalArgumentException.class, () -> {  // invalid word ID, out of bounds
            m.tryGuess(player2, 5, "PAX");
        });
        assertEquals(INVALID_GUESS_INCONSISTENT, response, "incorrect feedback response");
        response = m.tryGuess(player2, 4, "PAX");  // finally consistent
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C 1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "M 2 ACROSS\n" + 
                "A\n" + 
                "P\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "P >4 ACROSS\n" + 
                "A\n" + 
                "X\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player2), "incorrect match view string");
        
        // invalid challenge
        response = m.challenge(player1, 3, "CAT");
        assertEquals(INVALID_CHALLENGE_NO_GUESS, response, "incorrect feedback string");
    }
    
    // player 1 guesses three overlapping words (all incorrect); player 2 incorrectly
    // challenges incorrect word. Player 1 then guesses 3 words (still all incorrect); player 2
    // puts in incorrect length challenge, then correctly challenges incorrect word. Player 2 then
    // makes guess inconsistent with confirmed word, then same guess but consistent; player 1 makes
    // guess inconsistent with confirmed word, then challenges with same word, then player 2 challenges 
    // with different word (still not allowed). Player 1 then tries challenging already confirmed word
    @Test
    public void testLotsOfChallenges() throws IOException {
        Puzzle puzzle = Puzzle.parseFromFile(MIN_PUZZLE_FILE);
        final String player1 = "gzlin";
        final String player2 = "lconboy";
        Match m = new Match("match1", "the next next best possible match", puzzle, player1);
        m.addPlayer(player2);
        
        // player 1 makes bad guesses
        m.tryGuess(player1, 1, "ABC");
        m.tryGuess(player1, 3, "ABC");
        m.tryGuess(player1, 4, "CBA");
        String expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "A >1 DOWN >3 ACROSS\n" + 
                "B\n" + 
                "C\n" + 
                "_ 2 ACROSS\n" + 
                "B\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "C >4 ACROSS\n" + 
                "B\n" + 
                "A\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player1), "incorrect match view string");
        
        // incorrect challenge
        String response = m.challenge(player2, 1, "CAP");
        assertEquals(INCORRECT_CHALLENGE_BOTH_INCORRECT, response, "incorrect feedback string");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "A 1 DOWN >3 ACROSS\n" + 
                "B\n" + 
                "C\n" + 
                "_ 2 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "C >4 ACROSS\n" + 
                "B\n" + 
                "A\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy -1\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player1), "incorrect match view string");
        m.tryGuess(player1, 1, "AZC");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "A >1 DOWN >3 ACROSS\n" + 
                "B\n" + 
                "C\n" + 
                "_ 2 ACROSS\n" + 
                "Z\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "C >4 ACROSS\n" + 
                "B\n" + 
                "A\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy -1\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player1), "incorrect match view string");
        
        // player 2 incorrect length, then correct challenge
        response = m.challenge(player2, 1, "CATS");
        assertEquals(INVALID_CHALLENGE_LENGTH, response, "incorrect feedback string");
        response = m.challenge(player2, 1, "CAT");
        assertEquals(CHALLENGE_SUCCESS_RESPONSE, response, "incorrect feedback string");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "+C >1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "_ 2 ACROSS\n" + 
                "+A\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "+T 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 1\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player2), "incorrect match view string");
        
        // player 2 inconsistent then consistent with confirmed word
        response = m.tryGuess(player2, 4, "Pax");
        assertEquals(INVALID_GUESS_INCONSISTENT, response, "incorrect feedback string");
        response = m.tryGuess(player2, 4, "tap");
        assertEquals(VALID_GUESS_RESPONSE, response, "incorrect feedback string");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "+C >1 DOWN 3 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "_ 2 ACROSS\n" + 
                "+A\n" + 
                "_\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "+T >4 ACROSS\n" + 
                "A\n" + 
                "P\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 1\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player2), "incorrect match view string");
        
        // player 1 inconsistent, then challenges with same word
        response = m.tryGuess(player1, 3, "Bar");
        assertEquals(INVALID_GUESS_INCONSISTENT, response, "incorrect feedback string");
        response = m.challenge(player1, 4, "Tap");
        assertEquals(INVALID_CHALLENGE_NO_DIFFERENCE, response, "incorrect feedback string");
        
        // player 2 challenges own word
        response = m.challenge(player2, 4, "TAX");
        assertEquals(INVALID_CHALLENGE_YOURS, response, "incorrect feedback string");
        
        // player 1 challenges confirmed word
        response = m.challenge(player1, 1, "CAP");
        assertEquals(INVALID_CHALLENGE_CONFIRMED, response, "incorrect feedback string");
    }
    
    // player 1 guesses all squares, one incorrectly; player 2 challenges to end the game
    @Test
    public void testChallengeToWin() throws IOException {
        Puzzle puzzle = Puzzle.parseFromFile(MIN_PUZZLE_FILE);
        final String player1 = "gzlin";
        final String player2 = "lconboy";
        Match m = new Match("match1", "the next next best possible match", puzzle, player1);
        m.addPlayer(player2);
        
        // player 1 guesses (almost) entire board
        m.tryGuess(player1, 1, "CAT");
        m.tryGuess(player1, 2, "MAT");
        m.tryGuess(player1, 3, "CAR");
        m.tryGuess(player1, 4, "TAD");
        
        String expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C >1 DOWN >3 ACROSS\n" + 
                "A\n" + 
                "R\n" + 
                "M >2 ACROSS\n" + 
                "A\n" + 
                "T\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "T >4 ACROSS\n" + 
                "A\n" + 
                "D\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player1), "incorrect match view string");
        
        // player 2 challenges to end game
        String response = m.challenge(player2, 1, "CAT");
        assertEquals(INVALID_CHALLENGE_NO_DIFFERENCE, response, "incorrect feedback string");
        m.challenge(player2, 4, "TAX");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C 1 DOWN 3 ACROSS\n" + 
                "A\n" + 
                "R\n" + 
                "M 2 ACROSS\n" + 
                "A\n" + 
                "T\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "+T >4 ACROSS\n" + 
                "+A\n" + 
                "+X\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 2\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player2), "incorrect match view string");
        
        // finalize game; all confirmed, scores updated
        m.isMatchOver();
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "+C 1 DOWN 3 ACROSS\n" + 
                "+A\n" + 
                "+R\n" + 
                "+M 2 ACROSS\n" + 
                "+A\n" + 
                "+T\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "+T >4 ACROSS\n" + 
                "+A\n" + 
                "+X\n" + 
                "Scores:\n" + 
                "gzlin 3\n" + 
                "lconboy 3\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player2), "incorrect match view string");
        
    }
    
    // player 2 guesses all but one word, player 1 guesses the last to end the game. Note:
    // CAT never actually guessed by anyone
    @Test
    public void testGuessToWin() throws IOException {
        Puzzle puzzle = Puzzle.parseFromFile(MIN_PUZZLE_FILE);
        final String player1 = "gzlin";
        final String player2 = "lconboy";
        Match m = new Match("match1", "the next next best possible match", puzzle, player1);
        m.addPlayer(player2);
        
        // player 1 guesses (almost) entire board
        m.tryGuess(player1, 2, "MAT");
        m.tryGuess(player1, 3, "CAR");
        
        String expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C 1 DOWN >3 ACROSS\n" + 
                "A\n" + 
                "R\n" + 
                "M >2 ACROSS\n" + 
                "A\n" + 
                "T\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "_ 4 ACROSS\n" + 
                "_\n" + 
                "_\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player1), "incorrect match view string");
        
        // player 2 guesses 1 word to end game
        m.tryGuess(player2, 4, "TAX");
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "C 1 DOWN 3 ACROSS\n" + 
                "A\n" + 
                "R\n" + 
                "M 2 ACROSS\n" + 
                "A\n" + 
                "T\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "T >4 ACROSS\n" + 
                "A\n" + 
                "X\n" + 
                "Scores:\n" + 
                "gzlin 0\n" + 
                "lconboy 0\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player2), "incorrect match view string");
        
        // finalize game; all confirmed, scores updated
        m.isMatchOver();
        expected = 
                "3x4\n" + 
                "Squares:\n" + 
                "EMPTY\n" + 
                "+C 1 DOWN 3 ACROSS\n" + 
                "+A\n" + 
                "+R\n" + 
                "+M 2 ACROSS\n" + 
                "+A\n" + 
                "+T\n" + 
                "EMPTY\n" + 
                "EMPTY\n" + 
                "+T >4 ACROSS\n" + 
                "+A\n" + 
                "+X\n" + 
                "Scores:\n" + 
                "gzlin 2\n" + 
                "lconboy 1\n" + 
                "Questions:\n" + 
                "1 \"feline companion\"\n" + 
                "2 \"lounging place for feline companion\"\n" + 
                "3 \"gas powered vehicle\"\n" + 
                "4 \"nobody likes April 15\"";
        assertEquals(expected, m.matchView(player2), "incorrect match view string");
    }

}
