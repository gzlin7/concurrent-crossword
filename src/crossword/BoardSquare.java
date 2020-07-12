package crossword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import crossword.Entry.Direction;

/**
 * Represents a square on a crossword match board; immutable, threadsafe
 * data type
 * 
 * @author Andrew Churchill
 */
public class BoardSquare {
    // AF(currentGuess, playerWithControlAcross, playerWithControlDown, wordsStartingHere, confirmed, isEmpty):
    //   represents an empty square if isEmpty is true, otherwise represents a square
    //   on a crossword game board with the given current guess, made by the given player,
    //   and with the given confirmation state, with the given words starting here
    //   (represented as an entry in wordsStartingHere with the ID of the word, and then the
    //   direction, such as "6 ACROSS" or "12 DOWN")
    // RI(currentGuess, playerWithControlAcross, playerWithControlDown, wordsStartingHere, confirmed, isEmpty):
    //   true if isEmpty is true; otherwise, currentGuess.length() must be 1, 0 <=
    //   wordsStartingHere.size() <= 2. If confirmed is true then currentGuess must
    //   not just be " " and playerWithControl* must be nonempty. If the current guess is
    //   empty, playerWithControl* must be empty and confirmed must be false.
    // SRE:
    //   fields are private and final; only mutable one is list, which
    //   is never taken directly from client
    // TS:
    //   immutable type, no beneficient mutation
    
    /**
     * Factory method, returns a new BoardSquare with no guess yet, with 
     * the given words starting at it. The words should follow the format 
     * <code>"[number] [direction]"</code>, so for example if word 1 going down
     * starts at this square then wordStartsHere should be <code>"1 DOWN"</code>.
     * There can be a maximum of 2 words starting at one location.
     * 
     * @param wordsStartingHere the word that starts at this location
     * @return non empty BoardSquare, blank so far
     */
    public static BoardSquare nonEmpty(List<String> wordsStartingHere) {
        return new BoardSquare(" ", "", "", new ArrayList<>(wordsStartingHere), false, false);
    }
    
    /**
     * Factory method, returns an empty BoardSquare (a BoardSquare with no word occuring on it).
     * 
     * @return empty BoardSquare
     */
    public static BoardSquare empty() {
        return new BoardSquare(" ", "", "", Collections.emptyList(), false, true);
    }
    
    private final String currentGuess;
    private final String playerWithControlAcross;
    private final String playerWithControlDown;
    private final List<String> wordsStartingHere;
    private final boolean confirmed;
    private final boolean isEmpty;
    
    /**
     * Private constructor, creates a new BoardSquare. Only ever used within this
     * class.
     * 
     * @param currentGuess current guess, either " " or a single character
     * @param playerWithControl the player who made the guess
     * @param wordsStartingHere a list of words starting at this location
     * @param confirmed whether the square's guess was confirmed or not
     * @param isEmpty whether the square is an empty one (not part of a word)
     */
    private BoardSquare(String currentGuess, String playerWithControlAcross, 
            String playerWithControlDown, List<String> wordsStartingHere, boolean confirmed, boolean isEmpty) {
        this.currentGuess = currentGuess;
        this.playerWithControlAcross = playerWithControlAcross;
        this.playerWithControlDown = playerWithControlDown;
        this.wordsStartingHere = new ArrayList<>(wordsStartingHere);
        this.confirmed = confirmed;
        this.isEmpty = isEmpty;
        checkRep();
    }
    
    /**
     * Asserts the rep invariant is true
     */
    private void checkRep() {
        if (isEmpty) {
            return;
        }
        assert currentGuess.length() == 1 : "current guess is not just 1 character";
        assert wordsStartingHere.size() <= 2 : "more than 2 words starting here";
        if (confirmed) {
            assert !currentGuess.equals(" ") : "current guess cannot be empty for confirmed space";
            assert !playerWithControlAcross.equals("") || !playerWithControlDown.equals("") : 
                "player who made guess cannot be empty for confirmed space";
        }
        if (currentGuess.equals(" ")) {
            assert playerWithControlAcross.equals("") : "no guess, still has user with control";
            assert playerWithControlDown.equals("") : "no guess, still has user with control";
            assert !confirmed  : "no guess, still confirmed";
        }
    }
    
    /**
     * Creates a new BoardSquare, with a guess made by the current player. Must not be empty or confirmed.
     * 
     * @param guess guess by user; must be length 1
     * @param player user who made guess
     * @param direction direction guess was made in
     * @return new BoardSquare updated with guess
     */
    public BoardSquare withGuess(String guess, String player, Direction direction) {
        assert !isEmpty : "can't guess empty square";
        assert !confirmed || guess.equals(currentGuess) : "can't make new guess on confirmed square";
        assert guess.length() == 1 : "guess must be of length 1";
        assert player.length() > 0 : "user cannot be empty";
        if (direction == Direction.ACROSS) {
            return new BoardSquare(guess, player, this.playerWithControlDown, new ArrayList<>(wordsStartingHere),
                    confirmed, false);

        }
        else {  // direction is down
            return new BoardSquare(guess, this.playerWithControlAcross, player, new ArrayList<>(wordsStartingHere),
                    confirmed, false);

        }
    }
    
    /**
     * Overload of {@link BoardSquare#withGuess(String, String, Direction)} for convenience.
     * 
     * @param guess guess by user; must be length 1
     * @param player user who made guess
     * @param direction direction guess was made in
     * @return new BoardSquare updated with guess
     */
    public BoardSquare withGuess(char guess, String player, Direction direction) {
        return withGuess(String.valueOf(guess), player, direction);
    }

    
    /**
     * Creates a new BoardSquare, with the current guess confirmed. The
     * BoardSquare this is called on must already have a guess made by a player.
     * 
     * @return updated BoardSquare
     */
    public BoardSquare confirmed() {
        assert !isEmpty : "can't confirm empty square";
        assert this.currentGuess != " " : "can't confirm empty guess";
        assert !this.playerWithControlAcross.equals("") || !this.playerWithControlDown.equals("") : 
            "can't confirm square without player associated";
        return new BoardSquare(currentGuess, playerWithControlAcross, playerWithControlDown, new ArrayList<>(wordsStartingHere),
                true, false);
    }
    
    /**
     * Creates a new BoardSquare, where any current guess in the given direction is cleared. Cannot be
     * called on an empty square; removes control in given and returns for a confirmed square, so
     * be careful not to call this in the same direction as a confirmed word, or incorrect results will ensue. 
     * If there is no guess in the other direction, the letter is also cleared; if there is a guess 
     * in the other direction, it is not cleared.
     * 
     * @param direction direction to remove the guess in
     * @return updated BoardSquare
     */
    public BoardSquare removeGuess(Direction direction) {
        assert !isEmpty : "can't remove guess on empty square";
        if (direction == Direction.ACROSS) {
            return new BoardSquare(playerWithControlDown.equals("") ? " " : currentGuess, 
                    "", playerWithControlDown, new ArrayList<>(wordsStartingHere),
                    confirmed, false);
        }
        else { // direction is down
            return new BoardSquare(playerWithControlAcross.equals("") ? " " : currentGuess, 
                    playerWithControlAcross, "", new ArrayList<>(wordsStartingHere), 
                    confirmed, false);
        }
    }
    
    /**
     * Checks if the current square has a guess; false if this is empty
     * 
     * @return true if a guess was made
     */
    public boolean hasGuess() {
        return !isEmpty && !this.currentGuess.equals(" ");
    }
    
    /**
     * Checks if the current square's guess has been confirmed; false if this is empty
     * 
     * @return true if the guess was confirmed
     */
    public boolean isConfirmed() {
        return !isEmpty && this.confirmed;
    }
    
    /**
     * Gets the name of the player with control of this square. Cannot be called on a square
     * that is empty.
     * 
     * @return name of the player with control; empty string if no player has control
     */
    public String getPlayer(Direction direction) {
        assert !isEmpty : "current square is empty";
        if (direction == Direction.ACROSS) {
            return this.playerWithControlAcross;
        }
        else { // direction is down
            return this.playerWithControlDown;
        }
    }
    
    /**
     * Gets the character of the guess the player has made.
     * 
     * @return current guess; <code>' '</code> if there is no current guess
     */
    public char getGuess() {
        return currentGuess.charAt(0);
    }
    
    /**
     * Checks if the current square is an empty square (not part of a word)
     * 
     * @return true if the square is empty
     */
    public boolean isEmpty() {
        return isEmpty;
    }
    
    /**
     * Checks if the given guess is consistent with the current state if made by
     * the given player. A guess is consistent if it matches the character already
     * guessed, or if it conflicts but the current guess was not made by the other
     * user (in either the across or down directions)
     * 
     * @param guess the guess by the player to check
     * @param player the player making the guess
     * @return true if the guess is consistent
     */
    public boolean consistentGuess(char guess, String player) {
        if (guess == currentGuess.charAt(0) || currentGuess.equals(" ")) {
            return true;
        }
        // only consistent if player with control across and down is either none or current player
        return (player.equals(playerWithControlAcross) || playerWithControlAcross.equals("")) &&
                (player.equals(playerWithControlDown) || playerWithControlDown.equals(""));
    }
    
    /**
     * Gives the string representation of this space. To be used by the server
     * when communicating with the client about the game state. For empty spaces, this
     * is just <code>"_"</code>. Follows this grammar:
     * <p>
     * <code>
        SQUARE ::= "EMPTY" | (CONFIRMED LETTER START*)<br>
        CONFIRMED ::= "+"?  // present if confirmed<br>
        LETTER ::= [A-Z] | "_" // empty boxes are never confirmed<br>
        START ::= (" " MINE WORD_ID " " DIRECTION)+<br>
        MINE ::= ">"? // present if word was entered by current user<br>
        DIRECTION ::= "DOWN" | "ACROSS"<br>
        </code>
     * @param userAsking the user to specify control for (will have <code>">"</code>
     * in front of spaces they guessed)
     * @return string representing the current square
     */
    public String stringRepForClient(String userAsking) {
        if (isEmpty) {
            return "EMPTY";
        }
        String startingHere = "";
        for (String s : wordsStartingHere) {
            if (s.contains("ACROSS") && playerWithControlAcross.equals(userAsking)) {
                startingHere += " >" + s;
            }
            else if (s.contains("DOWN") && playerWithControlDown.equals(userAsking)) {
                startingHere += " >" + s;
            }
            else {
                startingHere += " " + s;
            }
        }
        return (confirmed ? "+" : "") +
               currentGuess.replace(' ', '_') + startingHere;
    }
    
    /**
     * Specifies which user made the guess in each direction
     */
    @Override 
    public String toString() {
        return stringRepForClient("not a user") + 
                (playerWithControlAcross.equals("") ? "" : " (across: " + playerWithControlAcross + ")") +
                (playerWithControlDown.equals("") ? "" : " (down: " + playerWithControlDown + ")");
    }
    
    @Override 
    public boolean equals(Object that) {
        return that instanceof BoardSquare && this.sameValue((BoardSquare) that);
    }
    
    /**
     * Private helper, asserts structural equality.
     * 
     * @param that other board square to check for structural equality with
     * @return true if this and that structurally equal
     */
    private boolean sameValue(BoardSquare that) {
        return (isEmpty && that.isEmpty) || // two empty squares are the same
                (currentGuess.equals(that.currentGuess) &&
                playerWithControlAcross.equals(that.playerWithControlAcross) &&
                playerWithControlDown.equals(that.playerWithControlDown) &&
                wordsStartingHere.equals(that.wordsStartingHere) &&
                confirmed == that.confirmed);
    }
    
    @Override 
    public int hashCode() {
        if (isEmpty) {
            return 1;
        }
        return Objects.hash(currentGuess, playerWithControlAcross, playerWithControlDown, wordsStartingHere, confirmed);
    }
    
    
}
