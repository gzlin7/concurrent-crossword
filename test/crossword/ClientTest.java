package crossword;

public class ClientTest {

    /*
     * Manual Tests
     * Testing Strategy:
     * partition on state of game,
     * verify each state renders displaying interface requirements,
     * test specific actions for each state, outlined below:
     * 
     * TEST CASES (partitioned by game state):
     * 
     * --START-------------------------------
     * 1. new user ID
     * 2. user ID already in use
     * --CHOOSE-------------------------------
     * 3a. play/join nonexistent match
     * 3b. play/join existing match
     * 4a. create new match with nonexistent puzzle
     * 4b. create valid new match
     * 5. exit game
     * --WAIT---------------------------------
     * 6. exit match
     * --PLAY---------------------------------
     * 7. try/guess a word
     * - guess blank word
     * - overwrite your previous guess
     * 8. invalid try
     * - already guessed by other player
     * - incorrect guess length
     * - same as existing guess
     * - inconsistent with board
     * - word has already been confirmed
     * 9. challenge existing word
     * 10. invalid challenge
     * - challenge confirmed word
     * - challenge your own word
     * - challenge same as existing guess
     * - incorrect length
     * 11. exit match
     * --SHOW SCORE---------------------------
     * 12. new match
     * 13. exit game
     * --ALL STATES---------------------------
     * 14. invalid syntax commands (e.g. invalid command type, whitespace in
     * user/matchID's)
     * 
     * 
     * MANUAL TEST INSTRUCTIONS: to execute all the tests, run the following
     * instructions in order
     * 
     * 1. Start server e.g Run Server on command line:
     * java -cp bin;lib/parserlib.jar crossword.Server puzzles
     * 2. Run 1st client (player1):
     * java -cp bin crossword.Client localhost
     * 
     * -----------------------------------------------------------------------------
     * ---START state---------------------------------------------------------------
     * -----------------------------------------------------------------------------
     *
     * 1-1. Verify that a game window pops up which displays:
     * -sufficient instructions for a user to know what to do
     * -a text box with which the user can enter a user ID
     * -an "enter" button to submit text box response
     * 
     * Test 14: START, invalid syntax commands
     * 1-2. Enter the following invalid commands one at a time. After each command,
     * verify that the window remains in the START state and informs the player of
     * why the command failed but allows the user to enter another command:
     * - "player 1" (problem: space in userID)
     * 
     * Test 1: START, new user ID
     * 1-3. Enter "player1" the text box and hit enter
     * 1-4. Verify transition to the CHOOSE state (details below)
     * 
     * Test 2: START, user ID already in use
     * 1-5. Repeat steps 1,2,1-1 starting with a new, second command line window
     * 1-6. Enter "player1" the text box and hit enter
     * 1-7. Verify that there is a response indicating this user ID is already in
     * use
     * and you must select another user ID, remaining in START state
     * 1-8. Enter "player2" the text box and hit enter
     * 1-9. Verify transition to the CHOOSE state (details below)
     * 
     * -----------------------------------------------------------------------------
     * --CHOOSE and WAIT states-----------------------------------------------------
     * -----------------------------------------------------------------------------
     *
     * 2-1. (CHOOSE) Verify that both players' game windows each display:
     * - new instructions on how to start a new match or enter an existing one
     * - list of matches and the list of available puzzles
     * (at this point should show 0 matches and all puzzles in puzzles folder:
     * - ids, titles listed respectively:
     * - ids: minimal, simple, simpleCopy
     * - titles: "Minimal Puzzle", "Easy", "Easy2")
     * - appropriate description for each puzzle
     * - text box for player to enter commands and enter button to submit
     * 
     * Test 5: CHOOSE, exit game
     * 2-2. (Through player1's window) Enter the following into text box and hit
     * enter: "EXIT"
     * 2-3. Verify player1's session has been terminated but player2 remains in
     * CHOOSE
     * 2-4. Repeat the necessary steps from above to rejoin the game as player1
     * 
     * Test 14: CHOOSE invalid syntax commands
     * 2-5. (Through player1's window) Enter the following invalid commands one at a
     * time. After each command, verify that the window remains in the CHOOSE state
     * with no new matches and displays appropriate error message but allows the
     * user to
     * enter another command:
     * - "NEW match1 simple badDescription" (description not wrapped in quotes)
     * - "NEW cool match simple "Play with me!- player1" (space in matchID/ wrong
     * number of tokens)
     * - "NOTACOMMAND match1 simple "Play with me!- player1"" (invalid command)
     * 
     * Test 4a: create new match with nonexistent puzzle
     * 2-6. (Through player1's window) Enter the following into text box and hit
     * enter: "NEW match1 notApuzzle "Play with me!- player1""
     * 2-7. Verify player1's window remains in the CHOOSE state with no new matches
     * and displays appropriate error message but allows the user to enter another
     * command
     * 
     * Test 4b: CHOOSE, create a valid new match
     * 2-8. (Through player1's window) Enter the following into text box and hit
     * enter: "NEW match1 simple "Play with me!- player1""
     * 2-9. Verify player1's window transitions to the WAIT state, with a display
     * that:
     * - make it clear to the player that they are waiting for another player to
     * join the match and that the only available command is the EXIT command
     * - includes text box and enter button to enter EXIT command
     * 2-10. Verify player2's window live updates to show match1 as available
     * 
     * Test 14: WAIT, invalid syntax commands
     * 2-11. (Through player1's window) Enter the following invalid commands one at
     * a time. After each command, verify that the window remains in the WAIT state
     * and displays an appropriate error message but allows the user to enter
     * another command:
     * - "NOTACOMMAND" (invalid command)
     * 
     * Test 6: WAIT, exit match
     * 2-12. (Through player1's window) Enter the following into text box and hit
     * enter: "EXIT"
     * 2-13. Verify player1's window transitions back to the CHOOSE state (details
     * above) and match1 does not appear in available matches
     * 2-14. Verify player2's window live updates and match1 no longer appears in
     * available matches
     * 
     * Test 3a: CHOOSE, play/join nonexistent match
     * 2-15. Repeat the necessary steps form above to create a new match with match
     * id match2 through player2's window
     * 2-16. (Through player1's window) Enter the following into text box and hit
     * enter: "play notAmatch"
     * 2-17. Verify player1's window remains in the CHOOSE state with no new matches
     * and displays appropriate error message but allows the user to enter another
     * command
     * 
     * Test 3b: CHOOSE, play/join existing match
     * 2-18. (Through player1's window) Enter the following into text box and hit
     * enter: "PLAY match2"
     * 2-19. Verify that both players' windows transition to the PLAY state (below)
     * 
     * -----------------------------------------------------------------------------
     * ---PLAY state----------------------------------------------------------------
     * -----------------------------------------------------------------------------
     *
     * 3-1. Verify that both players' game windows each display:
     * - full crossword puzzle with all the cells and all the words currently filled
     * in (all empty at this point), labeled with word/question numbers
     * - all questions/clues, numbered relative to corresponding word positions
     * - text box and enter button to enter commands
     * - should give the user good awareness of the state of the game, e.g.:
     * ---- (a) which words were entered by which player
     * ---- (b) what the id of each word is
     * ---- (c) how many challenge points each user has (both 0 at this point)
     * ---- (d) which words have been confirmed
     * ---- (e) live updates on any changes to the game
     * 
     * Test 14: PLAY invalid syntax commands
     * 3-2. (Through player1's window) Enter the following invalid commands one at a
     * time. After each command, verify that the window remains in the PLAY state
     * and displays appropriate error message but allows the user to enter another
     * command:
     * - "TRY 1" (missing word)
     * - “TRY star” (missing word id)
     * - "NOTACOMMAND” (invalid command)
     * 
     * Test 7: PLAY, try/guess a word
     * 3-3. (Through player1's window) Enter the following into text box and hit
     * enter: "TRY 1 scar"
     * 3-4. Verify that both players' windows live update to show 'scar' on the
     * crossword for word 1 and indicates that 'scar' was entered by player1 and
     * is unconfirmed
     * 3-5. (Through player1's window) Enter the following into text box and hit
     * enter: "TRY 1 spar"
     * 3-6. Verify that both players' windows live update to show 'scar' on the
     * crossword for word 1 and indicates that 'spar' was entered by player1 and
     * is unconfirmed
     * 
     * Test 8: PLAY, invalid try
     * 3-7. Enter the following into text box one at a time through the given
     * player’s window, and after each command, verify that crossword is unchanged
     * for both players and the displayed response for player makes it clear that
     * try was rejected because of the corresponding reason provided below:
     * -"TRY 1 star" (through player2, word 1 was entered by player1, so player2
     * must challenge it aka board inconsistency)
     * -“TRY 2 stars” (through player1, guess is of incorrect length for the word
     * id)
     * -“TRY 1 spar” (through player1, guess is same as existing guess)
     *
     * Test 9: PLAY, challenge existing word
     * 3-8. (Through player2's window) Enter the following into text box and hit
     * enter: "CHALLENGE 1 star"
     * 3-9. Verify that the both players' windows are changed to display:
     * - crossword now contains 'star' for word 1 and indicates it was entered
     * by player2 and is confirmed
     * - challenge points score is now player1: 0, player2: 2
     * 
     * Test 10: PLAY, invalid challenge
     * 3-10. Enter the following into text box through
     * player1’s window, and verify that crossword is unchanged for both players and
     * the displayed response for player1 makes it clear that challenge was rejected
     * because word has already been confirmed:
     * -"CHALLENGE 1 star”
     *
     * Tests 7,8,9,10 continued:
     * 3-11. Have player 1 guess another word by entering: “TRY 2 mbrket” and verify
     * that this is unsuccessful because guess is inconsistent with the current
     * board
     * 3-12. Have player 1 guess another word by entering: “TRY 2 market” and verify
     * that this is successful and market appears on both players’ boards as
     * player1’s and unconfirmed (score unchanged)
     * 3-13. Have player 1 challenge his own word by entering: “CHALLENGE 2 marcet”
     * and verify that is unsuccessful and boards are unchanged for both players,
     * and that display indicates that challenge failed because you cannot challenge
     * your own word
     * 3-14. Have player 2 enter: “CHALLENGE 2 markets”
     * and verify that is unsuccessful and boards are unchanged for both players,
     * and that display indicates that challenge failed because incorrect length
     * 3-15. Have player 2 enter: “CHALLENGE 2 market”
     * and verify that is unsuccessful and boards are unchanged for both players,
     * and that display indicates that challenge failed because challenge must be
     * unique from guess
     * 
     * Test 11: PLAY, exit match
     * 3-16. (Through player1's window) Enter the following into text box and hit
     * enter: "EXIT"
     * 3-17. Verify that both players' windows transition to the SHOW SCORE state
     * (below)
     *
     * -----------------------------------------------------------------------------
     * ---SHOW SCORE state----------------------------------------------------------
     * -----------------------------------------------------------------------------
     * 
     * 4-1. Verify that both players' windows display the following:
     * - who won the match (player2)
     * - final score (player1: 0, player2: 3)
     * - instructions on what to do next
     * 
     * Test 14: PLAY invalid syntax commands
     * 4-2. (Through player1's window) Enter the following invalid commands one at a
     * time. After each command, verify that the window remains in the SHOW SCORE
     * state and displays some error message but allows the user to enter another
     * command:
     * - "NOTACOMMAND” (invalid command)
     * 
     * Test 12: SHOW SCORE, new match
     * 4-3. (Through player2's window) Enter the following into text box and hit
     * enter: "NEW MATCH"
     * 4-4. Verify that player2's window transitions to the CHOOSE state (above) and
     * player1's window remains in SHOW SCORE
     * 
     * Test 13: SHOW SCORE, exit game
     * 4-5. (Through player1's window) Enter the following into text box and hit
     * enter: "EXIT"
     * 4-6. Verify that player1's session is terminated but player2's window remains
     * in CHOOSE
     * 
     */
}
