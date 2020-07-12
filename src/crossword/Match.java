package crossword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import crossword.Entry.Direction;

/**
 * Mutable and threadsafe ADT
 * Represents a Crossword Extravaganza match with 1-2 players
 * on a consistent crossword puzzle.
 * 
 * <p>
 */
public class Match {
    /*
     * AF(id, description, puzzle, players, playerToScore, boardState, changeListeners, matchOver):
     *   represents the Match with the given id, description, and correct puzzle,
     *   with the given set of players (and their given scores in playerToScore),
     *   where the current state of the match is represented by the map from board
     *   positions to board squares, and the given listeners are watching. If matchOver,
     *   then the match has finished.
     * RI(id, description, puzzle, players, playerToScore, boardState, changeListeners, matchOver):
     *   always:
     *      id is nonempty and has no whitespace; description is nonempty; players
     *      has length 1 or 2; playerToScore has same player set as players; there
     *      is one entry in boardState for every position in a grid of minimum size
     *      for puzzle; a position in boardState is empty if and only if it does
     *      not appear in any of the words in the puzzle; BoardSquares in an entry have
     *      a consistent control state (so for an across entry, all would contain the
     *      same player with control in the across direction)
     *   while there is only 1 player:
     *      there are no guesses, score is 0 for the one player
     *   while there are two players:
     *      no additional checks
     * SRE:
     *   all fields are private and final; no rep exposure in constructor or observers (defensive
     *   copies used when needed)
     * TS:
     *   uses monitor pattern; fields are all threadsafe; no multithreading used internally
     */

    private static final String VALID_GUESS_RESPONSE = "Valid guess";
    private static final String INVALID_GUESS_LENGTH = "Invalid guess, wrong word length";
    private static final String INVALID_GUESS_ALL_SAME = "Invalid guess, same as existing guess";
    private static final String INVALID_GUESS_INCONSISTENT = "Invalid guess, inconsistent with current board";
    private static final String CHALLENGE_SUCCESS_RESPONSE = "Successful challenge!";
    private static final String INCORRECT_CHALLENGE_ALREADY_CORRECT = "Failed challenge, target word was already correct";
    private static final String INCORRECT_CHALLENGE_BOTH_INCORRECT = "Failed challenge, target word and your guess both incorrect";
    private static final String INVALID_CHALLENGE_LENGTH = "Invalid challenge, wrong length";
    private static final String INVALID_CHALLENGE_CONFIRMED = "Invalid challenge, all spaces already confirmed";
    private static final String INVALID_CHALLENGE_NO_DIFFERENCE = "Invalid challenge, same as existing word";
    private static final String INVALID_CHALLENGE_YOURS = "Invalid challenge, you control this word";
    private static final String INVALID_CHALLENGE_NO_GUESS = "Invalid challenge, not all squares have guesses";
    private static final int CHALLENGE_SUCCESS_SCORE = 2;

    private final String id;
    private final String description;
    private final Puzzle puzzle;
    private final List<String> players = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> playerToScore = Collections.synchronizedMap(new HashMap<>());
    private final Map<BoardPosition, BoardSquare> boardState = Collections.synchronizedMap(new HashMap<>());
    private final List<GameChangeListener> changeListeners = Collections.synchronizedList(new ArrayList<>());
    private boolean matchOver;
    

    /**
     * Creates a new match with id matchID played on puzzle with player1
     * 
     * @param matchID     id of new match, nonempty and cannot contain whitespace
     * @param description the description of the new match, nonempty
     * @param puzzle      puzzle that match is played on, must be consistent
     * @param player1     first player in the match
     */
    public Match(String matchID, String description, Puzzle puzzle, String player1) {
        // set up basic fields as passed in
        this.id = matchID;
        this.description = description;
        this.puzzle = puzzle; // fine because Puzzle is an immutable ADT
        this.players.add(player1);
        this.playerToScore.put(player1, 0);
        this.matchOver = false;
        
        // set up board state
        List<Integer> boardSize = puzzle.getBoardSize();
        int rows = boardSize.get(0);
        int cols = boardSize.get(1);
        for (BoardPosition pos : BoardPosition.allBoardPositions(rows, cols)) {
            if (!puzzle.containsPosition(pos)) {
                this.boardState.put(pos, BoardSquare.empty());
            } else {
                this.boardState.put(pos, BoardSquare.nonEmpty(puzzle.wordsStartingHere(pos)));
            }
        }
        checkRep();
    }

    /**
     * Asserts the rep invariant is true
     */
    private synchronized void checkRep() {
        // make sure string fields follow rules
        assert id.length() > 0 : "id cannot be empty";
        assert id.indexOf(" ") == -1 : "id cannot contain whitespace";
        assert id.indexOf("\t") == -1 : "id cannot contain whitespace (tab)";
        assert description.length() > 0 : "description cannot be empty";
        for (GameChangeListener gcl : changeListeners) {
            assert gcl != null : "null game change listener";
        }

        // make sure boardState contains all the right positions
        List<Integer> minDimensions = this.puzzle.getBoardSize();
        int rows = minDimensions.get(0);
        int cols = minDimensions.get(1);
        for (BoardPosition pos : BoardPosition.allBoardPositions(rows, cols)) {
            assert boardState.containsKey(pos) : "board state does not contain necessary position " + pos;
            assert puzzle.containsPosition(pos) != boardState.get(pos)
                    .isEmpty() : "puzzle and boardState don't agree about if pos " + pos + " is empty";
        }

        // make sure no empty board squares within words
        for (Entry e : puzzle.getWordEntries()) {
            for (BoardPosition bp : e.getPositions()) {
                assert !boardState.get(bp).isEmpty() : "board square in word is empty";
            }
        }

        // make sure players and playersToScore follow rules
        assert playerToScore.keySet()
                .equals(Set.copyOf(players)) : "set of players in playerToScore not equal to set in players";
        assert players.size() == 1 || players.size() == 2 : "incorrect number of players: " + players.size();

        // make sure all entries are consistent
        for (Entry e : puzzle.getWordEntries()) {
            BoardSquare first = boardState.get(new BoardPosition(e.getRow(), e.getCol()));
            for (BoardPosition bp : e.getPositions()) {
                assert boardState.get(bp).getPlayer(e.getDirection()).equals(
                        first.getPlayer(e.getDirection())) : "state not consistent across all squares for entry " + e
                                + "; inconsistent at " + bp;
            }
        }

        // check special conditions if only one player in match
        if (players.size() == 1) {
            assert playerToScore.get(players.get(0)) == 0 : "score != 0 before game started";
            for (BoardPosition pos : BoardPosition.allBoardPositions(rows, cols)) {
                assert !boardState.get(pos).hasGuess() : "square at " + pos + " has guess before game started";
            }
        }
    }

    /**
     * Adds a player to an existing match
     * 
     * @param player2 new player to be added to this
     */
    public synchronized void addPlayer(String player2) {
        if (players.size() != 1) {
            throw new IllegalArgumentException("Match already has two players");
        }
        if (matchOver) {
            throw new IllegalArgumentException("match already over");
        }
        if (players.contains(player2)) {
            throw new IllegalArgumentException("Player " + player2 + " already in match");
        }
        players.add(player2);
        playerToScore.put(player2, 0);
        
        // player2 joined, board changed, notify listeners
        notifyBoardChange();
        
        checkRep();
    }
    
    /**
     * Private helper; notifies listeners in {@link #changeListeners} about 
     * changes to the board; protected from modifications to list while iterating.
     */
    private void notifyBoardChange() {
        // synchronize on listeners to prevent concurrent modification
        synchronized (changeListeners) {
//            System.out.println("In notifyBoardChange, #=" + changeListeners.size());
            for (GameChangeListener listener : changeListeners) {
                listener.boardChanged();
            }
        }
    }
    
    /**
     * Gets the match ID
     * 
     * @return ID of this match
     */
    public synchronized String getId() {
        return this.id;
    }

    /**
     * Returns a representation of the match for the given player for Text server.
     * The representation follows the grammar:
     * <p>
     * <code>
        BOARD ::= DIMENSIONS SQUARE_STATES SCORES QUESTIONS <br>
        DIMENSIONS ::= ROWS "x" COLS "\n" // dimensions of whole board<br>
        SQUARE_STATES ::= "Squares:\n" (SQUARE "\n")+ // row-major order<br>
        SCORES ::= "Scores:\n" (" " USER " " SCORE "\n")+<br>
        QUESTIONS ::= "Questions:\n" (WORD_ID " " QUESTION "\n")+  // all q's<br>
        <br>
        // DIMENSIONS helpers<br>
        ROWS ::= Int<br>
        COLS ::= Int<br>
        <br>
        // SQUARE_STATES helpers<br>
        SQUARE ::= "EMPTY" | (CONFIRMED LETTER START*)<br>
        CONFIRMED ::= "+"?  // present if confirmed<br>
        LETTER ::= [A-Z] | "_" // empty boxes are never confirmed<br>
        START ::= (" " MINE WORD_ID " " DIRECTION)+<br>
        MINE ::= ">"? // present if word was entered by current user<br>
        DIRECTION ::= "DOWN" | "ACROSS"<br>
        <br>
        // SCORES helpers<br>
        USER ::= StringNoWhitespace  // note: no quotes around it<br>
        SCORE ::= "-"? Int<br>
        <br>
        // QUESTIONS helpers<br>
        WORD_ID ::= Int<br>
        QUESTION ::= String  // note: has quotes around it<br>
        <br>
        // primitives<br>
        String ::= '"' ([^"\r\n\\] | '\\' [\\nrt] )* '"'<br>
        StringNoWhitespace ::= [^ "\r\n\t\\]*<br>
        Int ::= [0-9]+<br>
     * </code>
     * 
     * @param player player
     * @return a representation of the board
     */
    public synchronized String matchView(String player) {
        StringBuilder result = new StringBuilder();
        List<Integer> boardSize = puzzle.getBoardSize();
        int rows = boardSize.get(0);
        int cols = boardSize.get(1);
        result.append(rows).append("x").append(cols).append("\n");

        result.append("Squares:\n");
        for (BoardPosition pos : BoardPosition.allBoardPositions(rows, cols)) {
            result.append(boardState.get(pos).stringRepForClient(player)).append("\n");
        }

        result.append("Scores:\n");
        for (String p : players) {
            result.append(p).append(" ").append(playerToScore.get(p)).append("\n");
        }

        result.append("Questions:\n");
        result.append(puzzle.questionsString());
        return result.toString();
    }

    /**
     * The given player attempts to guess a word in puzzle.
     * If the guess goes through successfully, the response is "Valid guess"
     * If the guess is unsuccessful, the response is one of: "Invalid guess,
     * wrong word length", "Invalid guess, inconsistent with current board",
     * or "Invalid guess, same as existing guess".
     * 
     * @param player player who is guessing
     * @param wordID ID of the word player is trying to guess (or some other
     *               indicator of specific word)
     * @param guess  guess of player; cannot contain whitespace, capitalization does
     *               not matter
     * @return feedback message indicating if the try was successful
     *         or why it was unsuccessful
     */
    public String tryGuess(String player, int wordID, String guess) {
        assert !matchOver : "match already over";

        boolean boardChanged = false;
        List<Entry> toClear = new ArrayList<>();

        // make sure call is valid
        int entryIndex = wordID - 1;
        if (entryIndex >= puzzle.getWordEntries().size()) {
            throw new IllegalArgumentException("word ID " + wordID + " not in puzzle");
        }
        assert players.contains(player) : "player " + player + " not in game";
        assert players.size() == 2 : "trying to guess with only 1 player in match";
        if (guess.indexOf(' ') != -1) {
            throw new IllegalArgumentException("guess contains whitespace");
        }
        if (guess.indexOf('\t') != -1) {
            throw new IllegalArgumentException("guess contains whitespace (tab)");
        }        
        if (guess.indexOf('\n') != -1) {
            throw new IllegalArgumentException("guess contains whitespace (newline)");
        }
        guess = guess.toUpperCase();
        Entry targetEntry = puzzle.getWordEntries().get(entryIndex);
        if (targetEntry.getWord().length() != guess.length()) {
            return INVALID_GUESS_LENGTH;
        }

        // check for consistency with current state
        boolean allSame = true;
        List<BoardPosition> entryBoardPositions = targetEntry.getPositions();
        for (int i = 0; i < guess.length(); i++) {
            BoardPosition bp = entryBoardPositions.get(i);
            BoardSquare current = boardState.get(bp);
            allSame = allSame && guess.charAt(i) == current.getGuess();
            if (guess.charAt(i) == current.getGuess() || !current.hasGuess()) { // this space is valid; matches current
                                                                                // letter
                continue;
            } else if (current.isConfirmed()) { // invalid, doesn't match confirmed space
                return INVALID_GUESS_INCONSISTENT;
            } else if (guess.charAt(i) != current.getGuess() && !current.consistentGuess(guess.charAt(i), player)) {
                // invalid guess, conflicts with other user
                return INVALID_GUESS_INCONSISTENT;
            } else { // doesn't match, but only conflicts with this user's things
                for (Entry e : puzzle.getWordEntries()) {
                    // find entries for which user's existing (unconfirmed) guess doesn't match new
                    // guess
                    if (!e.equals(targetEntry) && e.getPositions().contains(bp)) {
                        toClear.add(e);
                    }
                }
            }
        }
        
        if (allSame) {  // same as existing guess
            return INVALID_GUESS_ALL_SAME;
        }

        // add guess to board
        for (int i = 0; i < guess.length(); i++) {
            BoardPosition bp = entryBoardPositions.get(i);
            BoardSquare current = boardState.get(bp);
            BoardSquare updated = current.withGuess(guess.charAt(i), player, targetEntry.getDirection());
            if (!updated.equals(current)) { // adding guess changed square
                boardChanged = true;
            }
            boardState.put(bp, updated);
        }

        // clear board if necessary
        boardChanged = clearPositions(toClear) || boardChanged;

        // call all callbacks if the board changed
        if (boardChanged) {
            notifyBoardChange();
        }
        checkRep();
        return VALID_GUESS_RESPONSE;
    }

    /**
     * Private helper; given a list of entries to clear, removes them from the board
     * and returns if the board was
     * changed in the process.
     * 
     * @param toClear list of entries to clear from the board
     * @return whether board was changed
     */
    private synchronized boolean clearPositions(List<Entry> toClear) {
        boolean boardChanged = false;
        for (Entry e : toClear) {
            for (BoardPosition bp : e.getPositions()) {
                BoardSquare current = boardState.get(bp);
                BoardSquare updated = current.removeGuess(e.getDirection());
                if (!updated.equals(current)) {
                    boardChanged = true;
                }
                boardState.put(bp, updated);
            }
        }
        checkRep();
        return boardChanged;
    }

    /**
     * The given player attempts to challenge the other player's unconfirmed word.
     * If the challenger is correct, the challenger gets two bonus points, the word
     * is confirmed, and the returned message is "Successful challenge!".
     * If the challenger is incorrect and the original word was correct, the
     * challenger
     * loses one point, the original word is confirmed, and the returned message is
     * "Failed challenge, target word was already correct".
     * If the challenger and original word are both incorrect, the challenger loses
     * one point,
     * the word is cleared from the board, and the returned message is "Failed
     * challenge,
     * target word and your guess both incorrect".
     * If the challenge is invalid, the returned message is "Invalid challenge".
     * 
     * @param player player who is issuing the challenge
     * @param wordID ID of the word player is trying to challenge
     * @param guess  guess of challenging player
     * @return feedback message indicating if the challenge was successful
     *         or why it was unsuccessful
     */
    public synchronized String challenge(String player, int wordID, String guess) {
        assert !matchOver : "match already over";

        boolean boardChanged = false;

        // check if challenge is valid
        int entryIndex = wordID - 1;
        if (entryIndex >= puzzle.getWordEntries().size()) {
            throw new IllegalArgumentException("word ID " + wordID + " not in puzzle");
        }
        assert players.contains(player) : "player " + player + " not in game";
        assert players.size() == 2 : "trying to guess with only 1 player in match";
        if (guess.indexOf(' ') != -1) {
            throw new IllegalArgumentException("guess contains whitespace");
        }
        if (guess.indexOf('\t') != -1) {
            throw new IllegalArgumentException("guess contains whitespace (tab)");
        }        
        if (guess.indexOf('\n') != -1) {
            throw new IllegalArgumentException("guess contains whitespace (newline)");
        }
        guess = guess.toUpperCase();
        Entry targetEntry = puzzle.getWordEntries().get(entryIndex);
        if (targetEntry.getWord().length() != guess.length()) {
            return INVALID_CHALLENGE_LENGTH;
        }

        // make sure word being challenged was entered by other player, not already
        // confirmed,
        // and different from current word
        boolean allConfirmed = true;
        boolean allSame = true;
        List<BoardPosition> entryPositions = targetEntry.getPositions();
        for (int i = 0; i < entryPositions.size(); i++) {
            BoardPosition bp = entryPositions.get(i);
            BoardSquare current = boardState.get(bp);
            if (!current.hasGuess()) {
                return INVALID_CHALLENGE_NO_GUESS;
            }
            if (current.getPlayer(targetEntry.getDirection()).equals(player)) {
                // note: by RI, if one for this word is controlled by this player in this
                // direction, then all are
                return INVALID_CHALLENGE_YOURS;
            }
            allConfirmed = allConfirmed && current.isConfirmed(); // will be true at end only iff all squares are
                                                                  // confirmed
            allSame = allSame && current.getGuess() == guess.charAt(i); // only true iff all squares have same letters
        }
        if (allConfirmed) {
            return INVALID_CHALLENGE_CONFIRMED;
        } 
        else if (allSame) {
            return INVALID_CHALLENGE_NO_DIFFERENCE;
        }

        // challenge is valid, check correctness
        String response;
        List<Entry> toClear = new ArrayList<>();
        boolean challengeCorrect = guess.equals(targetEntry.getWord());
        boolean currentCorrect = entryHasCorrectGuesses(targetEntry);
        // note: challengeCorrect and currentCorrect are never both true
        assert !challengeCorrect || !currentCorrect : "did not correctly check for identical challenge";

        // branch based on correctness
        if (currentCorrect) { // subtract one from score, confirm all squares
            playerToScore.put(player, playerToScore.get(player) - 1);
            boardChanged = true;
            for (BoardPosition bp : entryPositions) {
                boardState.put(bp, boardState.get(bp).confirmed());
            }
            response = INCORRECT_CHALLENGE_ALREADY_CORRECT;
        } else if (challengeCorrect) { // add 2 to score, confirm squares, clear inconsistent
            playerToScore.put(player, playerToScore.get(player) + CHALLENGE_SUCCESS_SCORE);
            boardChanged = true;
            Direction d = targetEntry.getDirection();
            // update guesses with correct letters, confirmations
            for (int i = 0; i < entryPositions.size(); i++) {
                BoardPosition bp = entryPositions.get(i);
                BoardSquare original = boardState.get(bp);
                BoardSquare updated = original.removeGuess(d).withGuess(guess.charAt(i), player, d).confirmed();
                boardState.put(bp, updated);

                // clear inconsistent guesses
                if (!(original.getGuess() == updated.getGuess())) {
                    // find entries with this bp
                    for (Entry e : puzzle.getWordEntries()) {
                        if (!e.equals(targetEntry) && e.getPositions().contains(bp)) {
                            toClear.add(e);
                        }
                    }
                }
            }
            response = CHALLENGE_SUCCESS_RESPONSE;
        } else {
            playerToScore.put(player, playerToScore.get(player) - 1);
            boardChanged = true;
            toClear.add(targetEntry);
            response = INCORRECT_CHALLENGE_BOTH_INCORRECT;
        }

        // clear positions if necessary
        boardChanged = clearPositions(toClear) || boardChanged;

        // call all callbacks if the board changed
        if (boardChanged) {
            notifyBoardChange();
        }
        checkRep();
        return response;
    }

    /**
     * Private helper; checks if the given entry has all correct guesses currently entered,
     * based on answers in the original Puzzle.
     * 
     * @param targetEntry entry to check for correctness
     * @return true if all guesses are correct, false otherwise
     */
    private boolean entryHasCorrectGuesses(Entry targetEntry) {
        boolean currentCorrect = true;
        List<BoardPosition> entryPositions = targetEntry.getPositions();
        for (int i = 0; i < entryPositions.size(); i++) {
            BoardPosition bp = entryPositions.get(i);
            currentCorrect = currentCorrect && boardState.get(bp).getGuess() == targetEntry.getWord().charAt(i);
        }
        return currentCorrect;
    }

    /**
     * Adds a new listener to call every time the game board changes.
     * The {@link GameChangeListener#boardChanged() boardChanged} method will
     * be called each time the board is changed. A game board change is defined
     * as a change that would be reflected in the UI. This includes:
     * <ul>
     * <li>A player made a guess
     * <li>A player made a challenge
     * <li>A word was confirmed
     * </ul>
     * 
     * @param listener the callback
     */
    public synchronized void addChangeListener(GameChangeListener listener) {
        synchronized (changeListeners) {
            changeListeners.add(listener);
        }
        checkRep();
    }
    
    public synchronized List<String> getPlayers() {
        // makes defensive copy
        return new ArrayList<>(players);
    }

    /**
     * Mutator; checks if the match is over, due to full board; match is over if every letter
     * is correct, or if match was already finalized. If every letter is correct but
     * the match was not already finalized, then this method finalizes the match.
     * 
     * @return true if the match is over
     */
    public synchronized boolean isMatchOver() {
        if (matchOver) {
            return true;
        }
        // check for board being all correct
        boolean allCorrect = true;
        for (Entry e : puzzle.getWordEntries()) {
            allCorrect = allCorrect && entryHasCorrectGuesses(e);
        }
        if (allCorrect) {
            finalizeMatch();
        }
        checkRep();
        return allCorrect;
    }
    
    /**
     * Updates scores so that each user gets 1 point for a correct word
     * that they guessed. If the match was already finalized, this method
     * just returns without mutating the match.
     */
    public synchronized void finalizeMatch() {
        finalizeMatch("");  // empty string guaranteed to not be a current user
    }
    
    /**
     * Overload of {@link Match#finalizeMatch() finalizeMatch}, gives the given user a score of 0
     * for the game and finalizes the match.
     * 
     * @param userForfeiting empty if we are just finalizing match and a user didn't forfeit
     */
    public synchronized void finalizeMatch(String userForfeiting) {
        if (matchOver) {  // match already finalized
            return;
        }
        matchOver = true;
        for (Entry e : puzzle.getWordEntries()) {
            if (entryHasCorrectGuesses(e)) {
                // check if a user actually made these guesses
                BoardSquare first = boardState.get(e.getPositions().get(0));
                String playerWithControl = first.getPlayer(e.getDirection());
                if (!playerWithControl.equals("")) {  // a player actually made this guess
                    // add one to player's score
                    playerToScore.put(playerWithControl, playerToScore.get(playerWithControl) + 1);
                }
            }
            // confirm all if user didn't forfeit
            if (userForfeiting.equals("")) {
                for (BoardPosition bp : e.getPositions()) {
                    boardState.put(bp, boardState.get(bp).confirmed());
                }   
            }
        }
        if (players.contains(userForfeiting) ) {  // give user score 0
            playerToScore.put(userForfeiting, 0);
        }
        
        // don't need to notify listeners about game changing unless a player forfeited,
        // because otherwise the change that led to the game ending will have
        // already triggered a notification
        if (!userForfeiting.equals("")) {
            notifyBoardChange();
        }
        checkRep();
    }
    
    /**
     * Returns a string containing the match ID and description; useful for listing
     * matches available to a player
     */
    @Override public synchronized String toString() {
        return id + " \"" + description + "\"";
    }

}
