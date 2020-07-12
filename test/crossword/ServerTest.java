/*
 * Copyright (c) 2017-2018 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course
 * staff.
 */
package crossword;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import crossword.Server.MessageType;

/**
 * Server Test
 */
public class ServerTest {

    /*
     * Testing strategy:
     * Partitions:
     *  request:
     *      invalid,                                                x
     *      valid                                                   x
     *  WARMUP:
     *      not tested (irrelevant to final product)                x
     *  ADD_USER:
     *      user id already in system,                              x
     *      user id not in system                                   x
     *  GET_PUZZLES:
     *      directory for server contains valid puzzles,            x
     *      directory for server contains invalid puzzles           x
     *  GET_MATCHES:
     *      system contains unavailable matches (2 players),        x
     *      system contains unavailable matches (completed),        x
     *      system contains no available matches (none started),    x
     *      system contains available matches (1 player waiting)    x
     *  NEW_MATCH: (note: should also result in BOARD_CHANGED, GAME_OVER responses)
     *      non-existent puzzle id,                                 x
     *      match id already exists,                                x
     *      valid match parameters                                  x
     *  PLAY_MATCH: (note: should also result in BOARD_CHANGED, GAME_OVER responses)
     *      non-existent match id,                                  x
     *      given user already in match,                            x
     *      match already has two players,                          x
     *      valid match to join                                     x
     *  EXIT_MATCH:
     *      GAME_OVER response                                      o
     *  TRY:
     *      valid guess (should respond to other player as well)    x
     *      invalid guess,                                          x
     *      invalid word ID,                                        x
     *      valid guess, ends game                                  x
     *  CHALLENGE:
     *      successful challenge,                                   x
     *      unsuccessful challenge,                                 x
     *      rejected challenge,                                     x
     *      successful challenge, ends game                         x
     *  QUIT:
     *      other player gets GAME_OVER                             x
     * 
     * Note 1: certain partitions (e.g. incorrect match ID passed to EXIT_MATCH)
     * are not included, because they are a. trivial and b. handled by the client,
     * and therefore would not happen when the system is used. 
     * 
     * Note 2: These tests do not test game logic thoroughly; those tests exist in 
     * MatchTest. This test suite is designed to test the Server and Game classes.
     * 
     * Note 3: the server is started with the same directory every time; it contains valid
     * and invalid puzzles.
     */
    

    // private Server server;
    private Socket socket;
    // private Thread thread;
    private BufferedReader in;
    private PrintWriter out;

    private Server server;
    private Thread thread;

    // setup() is called before each test to start server
    @BeforeEach public void setup() throws Exception {
        // setup Game server, port number is automatically allocated
        server = new Server(new File("puzzles"), 0);
        thread = startServer(server);
        socket = connectToServer(thread, server);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
    }

    // tearDown() is called after each test to stop server
    @AfterEach public void tearDown() throws Exception {
        socket.close();
    }

    private static final String LOCALHOST = "127.0.0.1";
    private static final int MAX_CONNECTION_ATTEMPTS = 5;

    /* Start server on its own thread. */
    private static Thread startServer(final Server server) {
        Thread thread = new Thread(() -> {
            try {
                server.serve();
            } catch (IOException ioe) {
                throw new RuntimeException("serve() threw IOException", ioe);
            }
        });
        thread.start();
        return thread;
    }

    /* Connect to server with retries on failure. */
    private static Socket connectToServer(final Thread serverThread, final Server server) throws IOException {
        final int port = server.port();
        assertTrue(port > 0, "server.port() " + port);
        for (int attempt = 0; attempt < MAX_CONNECTION_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(attempt * 10);
            } catch (InterruptedException ie) {
            }
            if (!serverThread.isAlive()) {
                throw new IOException("server thread no longer running");
            }
            try {
                final Socket socket = new Socket(LOCALHOST, port);
                // set socket to never timeout
                socket.setSoTimeout(0);
                return socket;
            } catch (ConnectException ce) {
                // may try again
            }
        }
        throw new IOException("unable to connect after " + MAX_CONNECTION_ATTEMPTS + " attempts");
    }

    /**
     * Helper method to read message from BufferedReader, assert MessageType, return message content
     * @param in reader to read from
     * @param type expected return type
     * @return server's returned message
     * @throws IOException
     */ 
    private static String readMessageAssert(final BufferedReader in, Server.MessageType type) throws IOException {
        String line = in.readLine();

        if (type == null) {
            assertEquals(null, line, "expected socket stream terminated");
            return line;
        }

        // get message header
        String[] tokens = line.split(" ");
        String command = tokens[0];

        System.out.println("Got response message " + command);
        // assert message type
        assertEquals(type.toString(), command, "incorrect server message type");

        // find out number of lines to follow
        int numLines = Integer.parseInt(tokens[1]);
        if (numLines == 0) {
            // no more lines to follow, return empty content
            return "";
        }

        // get message content
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numLines; i++) {
            builder.append(in.readLine()).append("\n");
        }

        return builder.toString().substring(0, builder.length() - 1); // remove last newline
    }

    // helper method to get number of lines
    /**
     * Returns number of lines of given response
     * 
     * @param response string to process
     * @return number of lines in given string
     */
    private static int numLines(String response) {
        return response.split("\n").length;
    }

    @Test @Tag("no-didit") public void testAssertionsEnabled() {
        assertThrows(AssertionError.class, () -> {
            assert false;
        }, "make sure assertions are enabled with VM argument '-ea'");

        try {
            // sleep to avoid tearDown() running too early
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
    }

    // cover get_puzzles request:
    @Test @Tag("no-didit") public void testGetPuzzles() throws IOException {
        // get all puzzles
        out.println(Server.MessageType.GET_PUZZLES);
        String response = readMessageAssert(in, Server.MessageType.GET_PUZZLES);
        String expectedOne = "simple \"Easy\" \"An easy puzzle to get started\"";
        assertTrue(numLines(response) > 0, "expected response");
        assertTrue(response.contains(expectedOne), "incorrect server response");
    }

    // cover add_user, quit requests:
    @Test @Tag("no-didit") public void testAddUser() throws IOException {
        out.println(Server.MessageType.ADD_USER + " player1");
        String response = readMessageAssert(in, Server.MessageType.ADD_USER);
        assertEquals("Success", response, "expected Success");
        out.println(Server.MessageType.ADD_USER + " player1");
        response = readMessageAssert(in, Server.MessageType.ADD_USER);
        assertTrue(response.contains("already in use"), "expected in use");

        // quit
        out.println(Server.MessageType.QUIT + " player1");
        // expect in stream terminated (return null)
        response = readMessageAssert(in, null);
    }

    // cover invalid new_match, play_match, exti_match, and invalid operation
    // requests:
    @Test @Tag("no-didit") public void testInvalidMatch() throws IOException {
        UUID uuid = UUID.randomUUID();
        String matchID = uuid.toString();
        String matchDesc = "\"Test Invalid\"";
        String player1 = "player1";

        // add player1
        out.println(Server.MessageType.ADD_USER + " " + player1);
        String response = readMessageAssert(in, Server.MessageType.ADD_USER);
        assertEquals("Success", response, "expected Success");

        // new invalid new_match request
        out.println(Server.MessageType.NEW_MATCH + " " + player1 + " " + matchID + " invalidPuzzle " + matchDesc);
        response = readMessageAssert(in, Server.MessageType.NEW_MATCH);
        assertTrue(response.startsWith(Server.FAIL), "expected Fail");

        // new invalid play_match request
        out.println(Server.MessageType.PLAY_MATCH + " " + player1 + " invalidMatch");
        response = readMessageAssert(in, Server.MessageType.PLAY_MATCH);
        assertTrue(response.startsWith(Server.FAIL), "expected Fail");

        // invalid exit_match
        out.println(Server.MessageType.EXIT_MATCH + " invalid_match");
        response = readMessageAssert(in, Server.MessageType.INVALID_REQUEST);

        // invalid request
        out.println("aaa");
        response = readMessageAssert(in, Server.MessageType.INVALID_REQUEST);
    }

    // cover 2 players add_user, new_match, play_match, ... requests:
    // tests basic operations
    @Test @Tag("no-didit") public void testNewMatch() throws IOException {
        String puzzleID = "simple";
        UUID uuid = UUID.randomUUID();
        String matchID = uuid.toString();
        String matchDesc = "\"Test Match 1\"";
        String player1 = "player1";
        String player2 = "player2";

        // add player1
        out.println(Server.MessageType.ADD_USER + " " + player1);
        String response = readMessageAssert(in, Server.MessageType.ADD_USER);
        assertEquals("Success", response, "expected Success");

        // new match request, wait for player2 to join
        out.println(Server.MessageType.NEW_MATCH + " " + player1 + " " + matchID + " " + puzzleID + " " + matchDesc);
        response = readMessageAssert(in, Server.MessageType.NEW_MATCH);
        assertTrue(response.startsWith(Server.SUCCESS), "expected Success");

        // new thread for player2
        new Thread() {

            @Override public void run() {
                Socket socket2;
                try {
                    // connect to server as 2nd player
                    socket2 = connectToServer(thread, server);
                    BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream(), UTF_8));
                    PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);

                    // add player2
                    out2.println(Server.MessageType.ADD_USER + " " + player2);
                    String response2 = readMessageAssert(in2, Server.MessageType.ADD_USER);
                    assertEquals("Success", response2, "expected Success");
                    
                    // test duplicate matchId
                    out2.println(Server.MessageType.NEW_MATCH + " " + player2 + " " + matchID + " " + puzzleID + " " + matchDesc);
                    response2 = readMessageAssert(in2, Server.MessageType.NEW_MATCH);
                    System.out.println(response2);

                    // test get_matches request
                    out2.println(Server.MessageType.GET_MATCHES);
                    response2 = readMessageAssert(in2, Server.MessageType.GET_MATCHES);
                    assertEquals(matchID + " " + matchDesc, response2, "expected available matches");
                    System.out.println(response2);

                    // player2 join match
                    out2.println(Server.MessageType.PLAY_MATCH + " " + player2 + " " + matchID);
                    response2 = readMessageAssert(in2, Server.MessageType.BOARD_CHANGED);

                    // player2 got BOARD_CHANGED message (because player1 tried a word)
                    response2 = readMessageAssert(in2, Server.MessageType.BOARD_CHANGED);

                    // player 2 challenge word 1
                    out2.println(Server.MessageType.CHALLENGE + " " + player2 + " " + matchID + " 1 STAR");
                    response2 = readMessageAssert(in2, Server.MessageType.CHALLENGE);
                    assertEquals("Successful challenge!", response2, "expected response");
                    response2 = readMessageAssert(in2, Server.MessageType.BOARD_CHANGED);
                    assertTrue(response2.contains("+S >1 ACROSS"), "expected challenge word confirmed");
                    assertTrue(response2.contains(player2 + " 2"), "expected challenger get 2 bonuspoints");

                    // player1 exit match, player2 should receive GAME_OVER message with scores
                    response2 = readMessageAssert(in2, Server.MessageType.GAME_OVER);
                    assertTrue(response2.contains("Scores"), "expected scores");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();

        // player1 read BOARD_CHANGE message (player2 joined)
        response = readMessageAssert(in, Server.MessageType.BOARD_CHANGED);
        assertTrue(numLines(response) > 10, "expected response");
        readMessageAssert(in, Server.MessageType.AVAILABLE_MATCHES);

        // player1 invalid try word
        out.println(Server.MessageType.TRY + " " + player1 + " " + matchID + " 1 BRIGHT");
        response = readMessageAssert(in, Server.MessageType.TRY);
        assertEquals("Invalid guess, wrong word length", response, "expected response");

        // player1 try word
        out.println(Server.MessageType.TRY + " " + player1 + " " + matchID + " 1 DOOR");
        response = readMessageAssert(in, Server.MessageType.TRY);
        assertEquals("Valid guess", response, "expected response");
        response = readMessageAssert(in, Server.MessageType.BOARD_CHANGED);
        // board update with letter
        assertTrue(response.contains("D >1 ACROSS"), "expected board flipped");

        // player2 challenged with correct word, expected board changed
        response = readMessageAssert(in, Server.MessageType.BOARD_CHANGED);
        assertTrue(response.contains("+S 1 ACROSS"), "expected challenger word confirmed");
        assertTrue(response.contains(player1 + " 0"), "expected player1 word incorrect score 0");

        // test exit_match
        out.println(Server.MessageType.EXIT_MATCH + " " + player1 + " " + matchID);
        // expect GAME_OVER response with scores
        response = readMessageAssert(in, Server.MessageType.GAME_OVER);
        assertTrue(response.contains("Scores"), "expected scores");
    }
    
    
    // cover add_user, new_match requests:
    // tests listening on avail_matches
    @Test @Tag("no-didit") public void testAvailableMatches() throws IOException {
        String puzzleID = "simple";
        UUID uuid = UUID.randomUUID();
        String matchID = uuid.toString();
        String matchDesc = "\"Test Match 1\"";
        String player1 = "player1";
        String player2 = "player2";

        // add player1
        out.println(Server.MessageType.ADD_USER + " " + player1);
        String response = readMessageAssert(in, Server.MessageType.ADD_USER);
        assertEquals("Success", response, "expected Success");

        // new thread for player2
        new Thread() {

            @Override public void run() {
                Socket socket2;
                try {
                    // connect to server as 2nd player
                    socket2 = connectToServer(thread, server);
                    BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream(), UTF_8));
                    PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);

                    // add player2
                    out2.println(Server.MessageType.ADD_USER + " " + player2);
                    String response2 = readMessageAssert(in2, Server.MessageType.ADD_USER);
                    assertEquals("Success", response2, "expected Success");

                    // player2 got AVAIL_MATCHES message (because player1 started new match)
                    response2 = readMessageAssert(in2, Server.MessageType.AVAILABLE_MATCHES);
                     
                    // give enough time for player1 thread to start a new match
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    // player2 join match
                    out2.println(Server.MessageType.PLAY_MATCH + " " + player2 + " " + matchID);
                    response2 = readMessageAssert(in2, Server.MessageType.BOARD_CHANGED);
                    // player1 exit match from 2 players match, will not trigger AVAILABLE_MATCHES
                    response2 = readMessageAssert(in2, Server.MessageType.GAME_OVER);                   
                    
                    // player1 new match #2, expect AVAILABLE_MATCHES
                    response2 = readMessageAssert(in2, Server.MessageType.AVAILABLE_MATCHES);
                    // player1 exit match from 1 player match, expect AVAILABLE_MATCHES
                    response2 = readMessageAssert(in2, Server.MessageType.AVAILABLE_MATCHES);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
        
        // give enough time for player2 thread to add player2
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // new match request, wait for player2 to join
        out.println(Server.MessageType.NEW_MATCH + " " + player1 + " " + matchID + " " + puzzleID + " " + matchDesc);
        response = readMessageAssert(in, Server.MessageType.NEW_MATCH);
        assertTrue(response.startsWith(Server.SUCCESS), "expected Success");

        // player1 read BOARD_CHANGE message (player2 joined)
        response = readMessageAssert(in, Server.MessageType.BOARD_CHANGED);
        response = readMessageAssert(in, Server.MessageType.AVAILABLE_MATCHES);
        
        // player1 exit match, should trigger GAME_OVER message
        out.println(Server.MessageType.EXIT_MATCH + " " + player1 + " " + matchID);
        response = readMessageAssert(in, Server.MessageType.GAME_OVER);
        
        // player1 new match #2
        out.println(Server.MessageType.NEW_MATCH + " " + player1 + " match2 " + puzzleID + " " + matchDesc);
        
        // sleep to give time before exiting match
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // player1 exit match #2
        out.println(Server.MessageType.EXIT_MATCH + " " + player1 + " match2 ");
    }
    
    // send invalid request; add user, try adding same user; add second user, add third
    // user; first user starts match then tries joining it, second user tries invalid puzzle 
    // (inconsistent), then same match ID, then non-existent match id, then just gets and joins
    // first user; second user gives invalid guess, then invalid word ID, then valid word;
    // first user finishes board and game is over; third user gets matches and tries
    // to join the completed one
    @Test @Tag("no-didit")
    public void testLotsOfInvalidRequests() throws IOException {
        final String puzzleID = "minimal";
        final UUID uuid = UUID.randomUUID();
        final String matchID = uuid.toString();
        final String matchDesc = "\"Test Match\"";
        final String player1 = "player1";
        final String player2 = "player2";
        final String player3 = "player3";
        
        // invalid request
        out.println("BIG_REQUEST " + player1 + " " + matchID);
        String response = readMessageAssert(in, Server.MessageType.INVALID_REQUEST);

        // add player1
        out.println(Server.MessageType.ADD_USER + " " + player1);
        response = readMessageAssert(in, Server.MessageType.ADD_USER);
        assertEquals("Success", response, "expected Success");

        // first user starts match, invalidly tries to join it
        out.println(MessageType.NEW_MATCH + " " + player1 + " " + matchID + " " + puzzleID + " " + matchDesc + " ");
        response = readMessageAssert(in, MessageType.NEW_MATCH);
        assertEquals(Server.SUCCESS, response, "expected Success");
        out.println(MessageType.PLAY_MATCH + " " + player1 + " " + matchID);
        response = readMessageAssert(in, MessageType.PLAY_MATCH);
        assertTrue(response.startsWith(Server.FAIL), "result is not failure");
        
        // new thread for player2
        Thread player2Thread = new Thread() {

            @Override public void run() {
                Socket socket2;
                try {
                    // connect to server as 2nd player
                    socket2 = connectToServer(thread, server);
                    BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream(), UTF_8));
                    PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);

                    // add second user
                    out2.println(Server.MessageType.ADD_USER + " " + player1);
                    String response = readMessageAssert(in2, MessageType.ADD_USER);
                    assertEquals("User ID " + player1 + " already in use", response, "expected invalid addition of repeat user");
                    out2.println(Server.MessageType.ADD_USER + " " + player2);
                    response = readMessageAssert(in2, MessageType.ADD_USER);
                    assertEquals("Success", response, "expected success");
                    
                    // second user tries a bunch of invalid matches
                    out2.println(MessageType.NEW_MATCH + " " + player2 + " " + matchID + " " + "inconsistent" + " " + matchDesc + " ");
                    response = readMessageAssert(in2, MessageType.NEW_MATCH);
                    assertTrue(response.startsWith(Server.FAIL), "expected failure");
                    out2.println(MessageType.NEW_MATCH + " " + player2 + " " + matchID + " " + puzzleID + " " + matchDesc + " ");
                    response = readMessageAssert(in2, MessageType.NEW_MATCH);
                    assertTrue(response.startsWith(Server.FAIL), "expected failure");
                    out2.println(MessageType.NEW_MATCH + " " + player2 + " " + puzzleID + " " + matchDesc + " "); // no matchID
                    response = readMessageAssert(in2, MessageType.NEW_MATCH);
                    assertTrue(response.startsWith(Server.FAIL), "expected failure");
                    // just join match
                    out2.println(MessageType.GET_MATCHES);
                    response = readMessageAssert(in2, MessageType.GET_MATCHES);
                    assertEquals(matchID + " " + matchDesc, response, "incorrect match list");
                    out2.println(MessageType.PLAY_MATCH + " " + player2 + " " + matchID);
                    response = readMessageAssert(in2, MessageType.BOARD_CHANGED);
                    
                    // second user gives invalid guesses
                    out2.println(MessageType.TRY + " " + player2 + " " + matchID + " 1 CART");
                    response = readMessageAssert(in2, MessageType.TRY);
                    assertEquals("Invalid guess, wrong word length", response, "expected invalid guess");
                    out2.println(MessageType.TRY + " " + player2 + " " + matchID + " 8 CAT");
                    response = readMessageAssert(in2, MessageType.INVALID_REQUEST);
                    
                    // finally valid guess
                    out2.println(MessageType.TRY + " " + player2 + " " + matchID + " 1 CAT");
                    response = readMessageAssert(in2, MessageType.TRY);
                    assertEquals("Valid guess", response, "Expected valid guess");

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
        player2Thread.start();  
        try {
            player2Thread.join();
        } catch (InterruptedException e) {}
        
        // board changed twice: user joined, guessed valid word
        response = readMessageAssert(in, Server.MessageType.BOARD_CHANGED);
        assertTrue(numLines(response) > 10, "expected response");
        System.out.println("RESPONSE HERE 1: \n" + response);
        response = readMessageAssert(in, Server.MessageType.BOARD_CHANGED);
        assertTrue(numLines(response) > 10, "expected response");
        System.out.println("RESPONSE HERE 2: \n" + response);



        // player1 finishes board
        out.println(MessageType.TRY + " " + player1 + " " + matchID + " 2 MAT");
        response = readMessageAssert(in, MessageType.TRY);
        assertEquals("Valid guess", response, "Expected valid guess");
        response = readMessageAssert(in, Server.MessageType.BOARD_CHANGED);
        assertTrue(numLines(response) > 10, "expected response");


        out.println(MessageType.TRY + " " + player1 + " " + matchID + " 3 CAR");
        response = readMessageAssert(in, MessageType.TRY);
        assertEquals("Valid guess", response, "Expected valid guess");
        response = readMessageAssert(in, Server.MessageType.BOARD_CHANGED);
        assertTrue(numLines(response) > 10, "expected response");


        out.println(MessageType.TRY + " " + player1 + " " + matchID + " 4 TAX");
        response = readMessageAssert(in, Server.MessageType.GAME_OVER);
        assertTrue(numLines(response) > 10, "expected response");
        response = readMessageAssert(in, MessageType.TRY);
        assertEquals("Valid guess", response, "Expected valid guess");


        
        // new thread for player3
        new Thread() {

            @Override public void run() {
                Socket socket3;
                try {
                    // connect to server as 3rd player
                    socket3 = connectToServer(thread, server);
                    BufferedReader in3 = new BufferedReader(new InputStreamReader(socket3.getInputStream(), UTF_8));
                    PrintWriter out3 = new PrintWriter(new OutputStreamWriter(socket3.getOutputStream(), UTF_8), true);

                    // add player3
                    out3.println(Server.MessageType.ADD_USER + " " + player3);
                    String response = readMessageAssert(in3, MessageType.ADD_USER);
                    assertEquals("Success", response, "expected success");
                    
                    out3.println(MessageType.PLAY_MATCH + " " + player3 + " " + matchID);
                    response = readMessageAssert(in3, MessageType.PLAY_MATCH);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.run();

    }
    
    // two players join a match, each player guesses a word, first player guesses invalid word
    // then leaves (via request), second player tries to play another word
    @Test @Tag("no-didit")
    public void testLeave() throws IOException {
        final String puzzleID = "minimal";
        final UUID uuid = UUID.randomUUID();
        final String matchID = uuid.toString();
        final String matchDesc = "\"Test Match\"";
        final String player1 = "player1";
        final String player2 = "player2";

        // add player1
        out.println(Server.MessageType.ADD_USER + " " + player1);
        String response = readMessageAssert(in, Server.MessageType.ADD_USER);
        assertEquals("Success", response, "expected Success");

        // first user starts match
        out.println(MessageType.NEW_MATCH + " " + player1 + " " + matchID + " " + puzzleID + " " + matchDesc + " ");
        response = readMessageAssert(in, MessageType.NEW_MATCH);
        assertEquals(Server.SUCCESS, response, "expected Success");
        
        // new thread for player2
        Thread player2Thread = new Thread() {

            @Override public void run() {
                Socket socket2;
                try {
                    // connect to server as 2nd player
                    socket2 = connectToServer(thread, server);
                    BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream(), UTF_8));
                    PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
                    
                    // add user, join match
                    out2.println(Server.MessageType.ADD_USER + " " + player2);
                    String response = readMessageAssert(in2, MessageType.ADD_USER);
                    assertEquals("Success", response, "expected success");
                    out2.println(Server.MessageType.PLAY_MATCH + " " + player2 + " " + matchID);
                    response = readMessageAssert(in2, MessageType.BOARD_CHANGED);
                    assertTrue(numLines(response) > 10);
                    
                    // wait for board changed response from user 1 leaving
                    response = readMessageAssert(in2, MessageType.GAME_OVER);
                    
                    // try invalid request, should failure
                    out2.println(MessageType.TRY + " " + player2 + " " + matchID + " 1 CRI");
                    response = readMessageAssert(in2, MessageType.INVALID_REQUEST);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        player2Thread.start();
        
        // wait for second user to join
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        readMessageAssert(in, Server.MessageType.AVAILABLE_MATCHES);
        
        // player 1 guesses invalid word, then rage quits
        out.println(MessageType.TRY + " " + player1 + " " + matchID + " 1 CRITL");
        readMessageAssert(in, MessageType.TRY);
        out.println(MessageType.EXIT_MATCH + " " + player1 + " " + matchID);
        try {
            player2Thread.join();
        } catch (InterruptedException e) {}
    }
    
    // two players join a match, first player fills entire board, second player challenges one
    // word with invalid word (rejected), then incorrect word (both rejected), first player reguesses
    // a wrong word, second player challenges with the correct word to end the game
    @Test @Tag("no-didit")
    public void testChallengeToWin() throws IOException {
        final String puzzleID = "minimal";
        final UUID uuid = UUID.randomUUID();
        final String matchID = uuid.toString();
        final String matchDesc = "\"Test Match\"";
        final String player1 = "player1";
        final String player2 = "player2";

        // add player1
        out.println(Server.MessageType.ADD_USER + " " + player1);
        String response = readMessageAssert(in, Server.MessageType.ADD_USER);
        assertEquals("Success", response, "expected Success");

        // first user starts match
        out.println(MessageType.NEW_MATCH + " " + player1 + " " + matchID + " " + puzzleID + " " + matchDesc + " ");
        response = readMessageAssert(in, MessageType.NEW_MATCH);
        assertEquals(Server.SUCCESS, response, "expected Success");
        
        // new thread for player2
        Thread player2Thread = new Thread() {

            @Override public void run() {
                Socket socket2;
                try {
                    // connect to server as 2nd player
                    socket2 = connectToServer(thread, server);
                    BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream(), UTF_8));
                    PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
                    
                    // add user, join match
                    out2.println(Server.MessageType.ADD_USER + " " + player2);
                    String response = readMessageAssert(in2, MessageType.ADD_USER);
                    assertEquals("Success", response, "expected success");
                    out2.println(Server.MessageType.PLAY_MATCH + " " + player2 + " " + matchID);
                    response = readMessageAssert(in2, MessageType.BOARD_CHANGED);
                    assertTrue(numLines(response) > 10);
                    
                    // wait for 4 board changed responses from user 1 guessing 4 words
                    for (int i = 0; i < 4; i++) {
                        response = readMessageAssert(in2, MessageType.BOARD_CHANGED);
                    }
                    
                    // try invalid challenge, then failed challenge
                    out2.println(MessageType.CHALLENGE + " " + player2 + " " + matchID + " 4 TAPPED");
                    response = readMessageAssert(in2, MessageType.CHALLENGE);
                    out2.println(MessageType.CHALLENGE + " " + player2 + " " + matchID + " 4 TAN");
                    response = readMessageAssert(in2, MessageType.CHALLENGE);
                    assertEquals("Failed challenge, target word and your guess both incorrect", response, "expected failed challenge");
                    readMessageAssert(in2, MessageType.BOARD_CHANGED);
                    
                    // wait for user 1 to make another wrong guess
                    readMessageAssert(in2, MessageType.BOARD_CHANGED);

                    // challenge to end game
                    out2.println(MessageType.CHALLENGE + " " + player2 + " " + matchID + " 4 TAX");
                    readMessageAssert(in2, MessageType.GAME_OVER);
                    response = readMessageAssert(in2, MessageType.CHALLENGE);
                    assertEquals("Successful challenge!", response, "expected successful challenge");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        player2Thread.start();
        
        // wait for second user to join
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        readMessageAssert(in, Server.MessageType.AVAILABLE_MATCHES);
        
        // player 1 guesses entire board, just gets TAX wrong
        // word 1: cat
        out.println(MessageType.TRY + " " + player1 + " " + matchID + " 1 cat");
        response = readMessageAssert(in, MessageType.TRY);
        assertEquals("Valid guess", response, "expected valid guess");
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        // word 2: mat
        out.println(MessageType.TRY + " " + player1 + " " + matchID + " 2 mat");
        response = readMessageAssert(in, MessageType.TRY);
        assertEquals("Valid guess", response, "expected valid guess");
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        // word 3: car
        out.println(MessageType.TRY + " " + player1 + " " + matchID + " 3 car");
        response = readMessageAssert(in, MessageType.TRY);
        assertEquals("Valid guess", response, "expected valid guess");
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        // word 4: tax
        out.println(MessageType.TRY + " " + player1 + " " + matchID + " 4 tar");
        response = readMessageAssert(in, MessageType.TRY);
        assertEquals("Valid guess", response, "expected valid guess");
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        
        // wait for second user to give failed challenge
        readMessageAssert(in, MessageType.BOARD_CHANGED);

        // guess incorrectly on TAX again
        out.println(MessageType.TRY + " " + player1 + " " + matchID + " 4 tal");
        response = readMessageAssert(in, MessageType.TRY);
        assertEquals("Valid guess", response, "expected valid guess");
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        
        // game should be over now
        readMessageAssert(in, MessageType.GAME_OVER);
        try {
            player2Thread.join();
        } catch (InterruptedException e) {}
    }
    
    // two players join a match, user 1 plays word and user 2 challenges correctly, user 2 plays
    // overlapping word and user 1 challenges correctly, user 1 tries to challenge first word,
    // user 1 exits match (via EXIT command, not by completely logging out), user 2 creates new match
    // (using same puzzle), user 1 joins it, user 2 exits match gracefully
    @Test @Tag("no-didit")
    public void testExits() throws IOException {
        final String puzzleID = "minimal";
        final UUID uuid = UUID.randomUUID();
        final String matchID = uuid.toString();
        final String matchDesc = "\"Test Match\"";
        final String player1 = "player1";
        final String player2 = "player2";

        // add player1
        out.println(Server.MessageType.ADD_USER + " " + player1);
        String response = readMessageAssert(in, Server.MessageType.ADD_USER);
        assertEquals("Success", response, "expected Success");

        // first user starts match
        out.println(MessageType.NEW_MATCH + " " + player1 + " " + matchID + " " + puzzleID + " " + matchDesc + " ");
        response = readMessageAssert(in, MessageType.NEW_MATCH);
        assertEquals(Server.SUCCESS, response, "expected Success");
        
        // new thread for player2
        Thread player2Thread = new Thread() {

            @Override public void run() {
                Socket socket2;
                try {
                    // connect to server as 2nd player
                    socket2 = connectToServer(thread, server);
                    BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream(), UTF_8));
                    PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
                    
                    // add user, join match
                    out2.println(Server.MessageType.ADD_USER + " " + player2);
                    String response = readMessageAssert(in2, MessageType.ADD_USER);
                    assertEquals("Success", response, "expected success");
                    out2.println(Server.MessageType.PLAY_MATCH + " " + player2 + " " + matchID);
                    response = readMessageAssert(in2, MessageType.BOARD_CHANGED);
                    assertTrue(numLines(response) > 10);
                    
                    // wait for board changed responses from user 1 guessing word
                    readMessageAssert(in2, MessageType.BOARD_CHANGED);
                                        
                    // successful challenge
                    out2.println(MessageType.CHALLENGE + " " + player2 + " " + matchID + " 1 CAT");
                    response = readMessageAssert(in2, MessageType.CHALLENGE);
                    assertEquals("Successful challenge!", response, "expected failed challenge");
                    readMessageAssert(in2, MessageType.BOARD_CHANGED);
                    
                    // guess second word
                    out2.println(MessageType.TRY + " " + player2 + " " + matchID + " 2 MAR");
                    response = readMessageAssert(in2, MessageType.TRY);
                    assertEquals("Valid guess", response, "expected valid guess");
                    readMessageAssert(in2, MessageType.BOARD_CHANGED);
                    
                    // wait for user 1 to challenge, then game to end
                    readMessageAssert(in2, MessageType.BOARD_CHANGED);
                    readMessageAssert(in2, MessageType.GAME_OVER);
                    
                    // start new match
                    out2.println(MessageType.NEW_MATCH + " " + player2 + " match2 " + puzzleID + " " + matchDesc);
                    response = readMessageAssert(in2, MessageType.NEW_MATCH);
                    
                    // wait for player 1
                    readMessageAssert(in2, MessageType.BOARD_CHANGED);
                    readMessageAssert(in2, Server.MessageType.AVAILABLE_MATCHES);
                    
                    // exit
                    out2.println(MessageType.EXIT_MATCH + " " + player2 + " match2");
                    readMessageAssert(in2, MessageType.GAME_OVER);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        player2Thread.start();
        
        // wait for second user to join
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        readMessageAssert(in, Server.MessageType.AVAILABLE_MATCHES);
        
        // player 1 guesses first word wrong
        out.println(MessageType.TRY + " " + player1 + " " + matchID + " 1 cap");
        response = readMessageAssert(in, MessageType.TRY);
        assertEquals("Valid guess", response, "expected valid guess");
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        
        // wait for user 2 to challenge, and put new word
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        
        // challenge second word, challenge first word
        // second word
        out.println(MessageType.CHALLENGE + " " + player1 + " " + matchID + " 2 mat");
        response = readMessageAssert(in, MessageType.CHALLENGE);
        assertEquals("Successful challenge!", response, "expected valid guess");
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        // first word
        out.println(MessageType.CHALLENGE + " " + player1 + " " + matchID + " 1 mat");
        response = readMessageAssert(in, MessageType.CHALLENGE);
        assertEquals("Invalid challenge, all spaces already confirmed", response, "expected invalid challenge");

        // exit game
        out.println(MessageType.EXIT_MATCH + " " + player1 + " " + matchID);
        readMessageAssert(in, MessageType.GAME_OVER);
        
        // join player 2's match
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {}
        out.println(MessageType.PLAY_MATCH + " " + player1 + " match2");
        readMessageAssert(in, MessageType.BOARD_CHANGED);
        
        // game should end
        readMessageAssert(in, MessageType.GAME_OVER);
        
        try {
            player2Thread.join();
        } catch (InterruptedException e) {}
    }

}
