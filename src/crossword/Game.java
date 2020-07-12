package crossword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable and threadsafe ADT
 * Represents a game system that hosts Crossword Extravaganza competitions
 * (matches) as defined in the project specification.
 * This class is responsible for checking if a match is finished, and finalizing
 * it if so.
 * 
 * <p>
 */
public class Game {

    private final List<Puzzle> puzzles = Collections.synchronizedList(new ArrayList<>());
    private final List<String> users = Collections.synchronizedList(new ArrayList<>());
    private final List<Match> matches = Collections.synchronizedList(new ArrayList<>());
    // listener to listen on new matches
    private final List<GameChangeListener> changeListeners = Collections.synchronizedList(new ArrayList<>());

    /*
     * AF(puzzles, users, matches):
     * represents the Crossword Extravaganza game with the given set
     * of matches, the given set of active users, and the given set of puzzles
     * available
     * RI(puzzles, users, matches):
     * players in matches that aren't yet over are all in users list. Only one match
     * with the same ID is in the system at a time.
     * SRE:
     * no information passed in from client, no rep values directly passed back
     * to client
     * TS:
     * uses the monitor pattern, no rep exposure
     */

    /**
     * Creates a new game system with no puzzles or users yet
     * 
     */
    public Game() {
        checkRep();
    }

    /**
     * Asserts the rep invariant is true.
     */
    private synchronized void checkRep() {
        for (Match m : matches) {
            if (m.isMatchOver()) { // ignore matches that are already done
                continue;
            }
            assert users.containsAll(m.getPlayers()) : "not all players in match (" + m.getPlayers()
                    + ") in this game's" + " set of users (" + users + ")";
            for (Match m2 : matches) {
                if (m != m2) {
                    assert !m.getId().equals(m2.getId()) : "multiple matches with same ID in system";
                }
            }
        }
    }

    /**
     * Adds puzzle to the system's puzzle selection
     * 
     * @param puzzle puzzle added to the system
     * 
     */
    public synchronized void addPuzzle(Puzzle puzzle) {
        // add immutable Puzzle
        puzzles.add(puzzle);
        checkRep();
    }

    /**
     * Add user if userID is not already in user. Return success or userID already
     * in use.
     * 
     * @param userID a user id
     * @return success or userID already in use
     * 
     */
    public synchronized String addUser(String userID) {
        if (users.contains(userID)) {
            return "User ID " + userID + " already in use";
        }
        users.add(userID);
        checkRep();
        return "Success";
    }

    /**
     * Adds a new listener to call every time the available matches changes.
     * The {@link GameChangeListener#boardChanged() boardChanged} method will
     * be called each time the available matches is changed. This includes:
     * <ul>
     * <li>New match which adds match to available matches
     * <li>Play match which removes match from available matches
     * <li>Exit match which removes match from available matches
     * </ul>
     * 
     * @param listener the callback
     */
    public void addMatchChangeListener(GameChangeListener listener) {
        synchronized (changeListeners) {
            changeListeners.add(listener);
        }
        checkRep();
    }

    /**
     * Private helper; notifies listeners in {@link #changeListeners} about
     * changes to matches; protected from modifications to list while iterating.
     */
    private void notifyAvailableMatchesChange() {
        // synchronize on listeners to prevent concurrent modification
        synchronized (changeListeners) {
            System.out.println("Notify available matches listeners, #=" + changeListeners.size());
            for (GameChangeListener listener : changeListeners) {
                listener.boardChanged();
            }
        }
    }

    /**
     * Creates new match in the system with id matchID played on
     * puzzle with player having ID userID
     * 
     * @param matchID     id of new match, must be unique in this game
     * @param description description of the new match
     * @param puzzleID    id of the puzzle that the new match will be played on
     * @param userID      id of the player creating the match
     * 
     */
    public synchronized void newMatch(String matchID, String description, String puzzleID, String userID) {
        // instantiate new match
        for (Match m : matches) {
            if (m.getId().equals(matchID)) {
                throw new IllegalArgumentException("Match ID " + matchID + " already in system");
            }

        }
        Puzzle puzzle = getPuzzle(puzzleID);
        matches.add(new Match(matchID, description, puzzle, userID));
        // available matches changed, notify listener
        notifyAvailableMatchesChange();
        checkRep();
    }

    /**
     * Adds a player with ID userID to an existing system match
     * that currently has one player
     * 
     * @param userID  id of the player trying to join a match
     * @param matchID id of the match player is trying to join
     * 
     */
    public synchronized void playMatch(final String userID, final String matchID) {
        final Match m = getMatch(matchID);
        m.addPlayer(userID);
        m.addChangeListener(new GameChangeListener() {

            public void boardChanged() {
                if (m.isMatchOver()) {
                    exitMatch(matchID);
                }
            }
        });
        // available matches changed, notify listener
        notifyAvailableMatchesChange();
        checkRep();
    }

    /**
     * Add GameChangeListener to the Game to listen current Match changes.
     * Server listen to changes in the match so a response can be sent to client.
     * 
     * @param matchID  id of the current match being listened to
     * @param listener listener
     * 
     */
    public synchronized void addGameChangeListener(String matchID, GameChangeListener listener) {
        Match m = getMatch(matchID);
        m.addChangeListener(listener);
        checkRep();
    }

    /**
     * Return match view based on the given match id and user id.
     * 
     * @param matchID id of the match
     * @param userID  id of the player
     * @return match view
     */
    public synchronized String matchView(String matchID, String userID) {
        Match m = getMatch(matchID);
        return m.matchView(userID);
    }

    /**
     * Return true if the given match is over.
     * 
     * @param matchID id of the match
     * @return true if match is over
     */
    public synchronized boolean isMatchOver(String matchID) {
        Match m = getMatch(matchID);
        return m.isMatchOver();
    }

    /**
     * Returns all matches in the system that are waiting for a second player
     * 
     * @return string list of all matches in the system with one player
     * 
     */
    public synchronized String availableMatches() {
        StringBuilder builder = new StringBuilder();
        for (Match m : matches) {
            if (m.isMatchOver() || m.getPlayers().size() == 2) { // don't include completed matches
                continue;
            }
            builder.append(m.toString());
            builder.append("\n");
        }
        String matchesString = builder.toString();

        if (matchesString.isEmpty()) {
            return matchesString;
        }

        return matchesString.substring(0, builder.length() - 1); // removes last newline character
    }

    /**
     * Returns the match with the given matchID. Returns active matches if possible,
     * otherwise throws IllegalArgumentException.
     * 
     * @param matchID id of match to return
     * @return match object
     */
    private synchronized Match getMatch(String matchID) {
        for (Match m : matches) {
            if (m.getId().equals(matchID)) { // prioritize current matches
                return m;
            }
        }
        throw new IllegalArgumentException("Match id " + matchID + " is not available in game");
    }

    /**
     * Returns the puzzle with the given puzzleID.
     * 
     * @param puzzleID id of puzzle to return
     * @return puzzle object
     */
    private synchronized Puzzle getPuzzle(String puzzleID) {
        for (Puzzle p : this.puzzles) {
            if (p.getId().equals(puzzleID)) {
                return p;
            }
        }
        // didn't find the given puzzle
        throw new IllegalArgumentException("Puzzle id " + puzzleID + " is not available in game");
    }

    /**
     * Returns all puzzles in the system
     * 
     * @return string list of all puzzles in the system
     * 
     */
    public synchronized String allPuzzles() {
        StringBuilder builder = new StringBuilder();
        for (Puzzle puzzle : puzzles) {
            builder.append(puzzle.getId());
            builder.append(" \"").append(puzzle.getName()).append("\"");
            builder.append(" \"").append(puzzle.getDescription()).append("\"");
            builder.append("\n");
        }
        return builder.toString().substring(0, builder.length() - 1); // removes last new line
    }

    /**
     * The given player attempts to try to guess a word
     * in a match with id matchID in the system
     * 
     * @param matchID ID of match in which guess is tried
     * @param player  user ID of the player issuing the guess
     * @param wordID  ID of the word player is trying to guess
     * @param guess   guess of player
     * @return feedback message indicating if the try was successful
     *         or why it was unsuccessful
     */
    public synchronized String tryGuess(String matchID, String player, int wordID, String guess) {
        Match m = getMatch(matchID);
        return m.tryGuess(player, wordID, guess);
    }

    /**
     * The given player attempts to challenge the other player's unconfirmed word
     * in a match with id matchID in the system
     * 
     * @param matchID ID of match in which the challenge is issued
     * @param player  user ID of the player issuing the challenge
     * @param wordID  ID of the word player is trying to challenge
     * @param guess   guess of challenging player
     * @return feedback message indicating if the challenge was successful
     *         or why it was unsuccessful
     */
    public synchronized String challenge(String matchID, String player, int wordID, String guess) {
        Match m = getMatch(matchID);
        return m.challenge(player, wordID, guess);
    }

    /**
     * Ends the match (assuming no player forfeited)
     * 
     * @param matchID match to end
     */
    private synchronized void exitMatch(String matchID) {
        Match m = getMatch(matchID);
        m.finalizeMatch();
        checkRep();
    }

    /**
     * Ends a match in the system (called when any of the match's players exit the
     * match)
     * 
     * @param matchID id of the match being ended
     * @param player  the player who is forfeiting
     */
    public synchronized void exitMatch(String matchID, String player) {
        Match m = getMatch(matchID);
        m.finalizeMatch(player);

        // notify listener only if 2nd player has not joined yet
        if (m.getPlayers().size() == 1)
            notifyAvailableMatchesChange();
        checkRep();
    }

    /**
     * Given player quits the game/terminates the session/logs out
     * 
     * @param userID id of the player quitting the system
     */
    public synchronized void quitGame(String userID) {
        assert users.contains(userID) : "user " + userID + " not valid";

        users.remove(userID);

        // remove match if all players have exited
        List<Match> matchesToRemove = new ArrayList<>();
        for (Match m : matches) {
            Boolean done = true;
            for (String player : m.getPlayers()) {
                if (users.contains(player)) {
                    done = false;
                }
            }
            if (done) {
                matchesToRemove.add(m);
            }
        }
        for (Match r : matchesToRemove) {
            matches.remove(r);
        }
    }
}
