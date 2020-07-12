/*
 * Copyright (c) 2019 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course
 * staff.
 */
package crossword;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Crossword puzzle game Server.
 */
public class Server {

    /** Server supported request or message type */
    public static enum MessageType {
        WARMUP, ADD_USER, GET_PUZZLES, GET_MATCHES, NEW_MATCH, PLAY_MATCH, EXIT_MATCH, TRY, CHALLENGE, QUIT, BOARD_CHANGED, GAME_OVER, AVAILABLE_MATCHES, INVALID_REQUEST, HOLD, DISPOSE
    }

    public static final String SUCCESS = "Success";
    public static final String FAIL = "Fail";

    /** Server port number */
    public static final int PORT = 4949;

    private static final int TOKEN_THREE = 3;
    private static final int TOKEN_FOUR = 4;
    private static final int TOKEN_MAX = 5;

    /**
     * Start a Crossword Extravaganza server using the given arguments.
     * 
     * <p>
     * Command-line usage:
     * 
     * <pre>
     * java -cp path crossword.Server puzzle_folder
     * </pre>
     * 
     * <p>
     * When the server boots up, it should read all the *.puzzle files in the
     * puzzle_folder given in the command line; it should then wait for client
     * connections on port 4949.
     * 
     * <p>
     * To start a server with the puzzles inside the puzzles folder:
     * 
     * <pre>
     * java -cp bin;lib/parserlib.jar crossword.Server puzzles
     * </pre>
     * 
     * @param args The command line arguments should include only the folder where
     *             the puzzles are located.
     * @throws IOException if an error occurs reading the file in the given folder
     */
    public static void main(String[] args) throws IOException {
        // check if folder argument is provided
        if (args.length < 1) {
            throw new IllegalArgumentException("missing folder");
        }

        // check if folder exists
        File folder = new File(args[0]);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("folder invalid or does not exist");
        }

        // start server
        new Server(folder, PORT).serve();
    }

    private final ServerSocket serverSocket;
    private final Game game;

    // Abstraction function:
    // AF(game, port) = server for game that listens for connections on port
    // on port
    //
    // Representation invariant:
    // none
    //
    // Safety from rep exposure:
    // all fields are private, final
    // all public methods return immutable types
    //
    // Thread safety argument:
    // All fields are private, final
    // Game is thread safe
    // Only 1 thread uses ServerSocket to accept new connection.
    // Only 1 thread reads from each socket, and only 1 thread writes to each socket
    // BlockingQueue from Concurrent package is used to collect server responses
    // from multiple threads (receiveRequest and GameChangedListener) before they
    // are written to the socket by the sendResponse thread
    // All variables in handleRequest(), receiveRequest(), and sendResponse() are
    // either local (Confinement), or thread-safe final

    /**
     * Make a new server that listens for connections on port. Server should read
     * all the *.puzzle files in the
     * folder; it should then wait for client connections on port.
     * 
     * @param folder puzzle folder
     * @param port   server port number
     * @throws IOException if an error occurs opening the server socket
     */
    public Server(File folder, int port) throws IOException {
        this.serverSocket = new ServerSocket(port);

        // create Game and load puzzles from the folder directory
        game = new Game();
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".puzzle")) {
                // System.out.println(file.getPath());
                try {
                    // load and add puzzle to Game
                    game.addPuzzle(Puzzle.parseFromFile(file.getPath()));
                } catch (Exception e) {
                    System.out.println("Invalid puzzle: " + file.getPath());
                    e.printStackTrace();
                    // ignore invalid puzzle file
                }
            }
        }

        checkRep();
    }

    // Check that the rep invariant is true
    private void checkRep() {
    }

    /**
     * Get the port on which this server is listening.
     * 
     * @return the port on which this server is listening for connections
     */
    public int port() {
        return serverSocket.getLocalPort();
    }

    /**
     * Run the server, listening for and handling client connections.
     * Never returns normally.
     * 
     * <p>
     * Valid request commands include: WARMUP, ADD_USER, GET_PUZZLES, GET_MATCHES,
     * NEW_MATCH, PLAY_MATCH, EXIT_MATCH, TRY, CHALLENGE, QUIT
     * Request commands are case-insensitive. If the request is invalid (e.g.
     * command not supported, not enough parameters), INVALID_REQUEST response
     * message will be sent.
     * <br>
     * Valid response message types: WARMUP, ADD_USER, GET_PUZZLES, GET_MATCHES,
     * NEW_MATCH, PLAY_MATCH, TRY, CHALLENGE, BOARD_CHANGED, GAME_OVER,
     * INVALID_REQUEST
     * <br>
     * All responses are in the following format:
     * 
     * <pre>
     * [Message Type] [number of lines to follow (in response content)]\n
     * [Response Content]
     * </pre>
     * 
     * All parameters denoted by square brackets [ ] in the following Request
     * formats must be nonempty
     * 
     * <p>
     * <b>WARMUP</b> request for warmup to return a board (match view) for client to
     * render
     * <ul>
     * <li>Request format: WARMUP
     * <li>Response Message Type: WARMUP
     * <li>Response Content: match view, see match view grammar section
     * </ul>
     * 
     * <p>
     * <b>ADD_USER</b> Add user if userID is not already in use. Returns success or
     * userID already in use.
     * <ul>
     * <li>Request format: ADD_USER [userID] (userID must not contain any tab or
     * whitespace)
     * <li>Response Message Type: ADD_USER
     * <li>Response Content: success or userID already in use
     * </ul>
     * 
     * <p>
     * <b>GET_PUZZLES</b> Get all puzzles in the system
     * <ul>
     * <li>Request format: GET_PUZZLES
     * <li>Response Message Type: GET_PUZZLES
     * <li>Response Content: (PuzzleID Name Description '\n')*
     * </ul>
     * 
     * <p>
     * <b>GET_MATCHES</b> Get all available (waiting) matches in the system
     * <ul>
     * <li>Request format: GET_MATCHES
     * <li>Response Message Type: GET_MATCHES
     * <li>Response Content: (MatchID Description '\n')*
     * </ul>
     * 
     * <p>
     * <b>NEW_MATCH</b> Creates new match in the system with matchID played on
     * puzzle puzzleID with player userID. Return success or fail. After a
     * successful NEW_MATCH request, server automatically watches for game changes.
     * BOARD_CHANGED or GAME_OVER response messages will be sent when a second
     * player joins the match, the board is changed, or the game is over.
     * <ul>
     * <li>Request format: NEW_MATCH [userID] [matchID] [puzzleID] "[Description]"
     * (matchID must not contain any tab or whitespace)
     * <li>Response Message Type: NEW_MATCH
     * <li>Response Content: Success or Fail
     * </ul>
     * 
     * <p>
     * <b>PLAY_MATCH</b> Adds a player with ID userID to an existing system match
     * that currently has one player. After successful PLAY_MATCH request, server
     * automatically watches for game changes. BOARD_CHANGED or GAME_OVER response
     * messages will be sent when the board is changed or the game is over.
     * <ul>
     * <li>Request format: PLAY_MATCH [userID] [matchID]
     * <li>Response Message Type: BOARD_CHANGED if success or PLAY_MATCH if fail
     * <li>Response Content: error for PLAY_MATCH or match view for BOARD_CHANGED
     * </ul>
     * 
     * <p>
     * <b>TRY</b> The given player attempts to try to guess a word in a match with
     * id matchID in the system. wordID is an Integer, other parameters are String.
     * Return feedback (i.e. success or reason for failure).
     * <ul>
     * <li>Request format: TRY [userID] [matchID] [wordID] [word]
     * <li>Response Message Type: TRY
     * <li>Response Content: feedback
     * </ul>
     * 
     * <p>
     * <b>CHALLENGE</b> The given player attempts to challenge the other player's
     * unconfirmed word in a match with id matchID in the system. wordID is an
     * Integer, other parameters are String. Return feedback (i.e. success or reason
     * for failure).
     * <ul>
     * <li>Request format: CHALLENGE [userID] [matchID] [wordID] [word]
     * <li>Response Message Type: CHALLENGE
     * <li>Response Content: feedback
     * </ul>
     * 
     * <p>
     * <b>EXIT_MATCH</b> Ends a match in the system
     * <ul>
     * <li>Request format: EXIT_MATCH [userID] [matchID]
     * <li>Response Message Type: GAME_OVER
     * <li>Response Content: match view, see match view grammar section
     * </ul>
     * 
     * <p>
     * <b>QUIT</b> Given player quits the game/terminates the session/logs out.
     * Close connection
     * <ul>
     * <li>Request format: QUIT [userID]
     * <li>Response Message Type: none
     * <li>Response Content: N/A
     * </ul>
     * 
     * Response (Match View) for BOARD_CHANGED and GAME_OVER
     * <br>
     * 
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
     * @throws IOException if an error occurs waiting for a connection
     */
    public void serve() throws IOException {
        System.err.println("Server listening on " + serverSocket.getLocalSocketAddress());
        while (true) {
            // block until a client connects
            final Socket socket = serverSocket.accept();
            // BlockingQueue for sending response back to client
            final BlockingQueue<String> responseQueue = new ArrayBlockingQueue<String>(200);

            // create a new socket-reading thread for each client connection
            new Thread() {

                @Override public void run() {
                    // handle the client
                    try {
                        receiveRequest(socket, responseQueue);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(); // but do not stop serving
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();

            // create a new socket-writing thread for each client connection
            new Thread() {

                @Override public void run() {
                    // handle the client
                    try {
                        sendResponse(socket, responseQueue);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(); // but do not stop serving
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }
    }

    /**
     * Handle a single client connection. Take response from responseQueue to send
     * back to client.
     * 
     * @param socket        socket connected to client
     * @param responseQueue response blocking queue
     * @throws IOException if the connection encounters an error or closes
     *                     unexpectedly
     */
    private static void sendResponse(final Socket socket, final BlockingQueue<String> responseQueue)
            throws IOException {
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        try {
            boolean hold = false;
            boolean dispose = false;
            String msgHeld = "";
            for (;;) {
                String response;
                try {
                    response = responseQueue.take();
                    if ("QUIT".equals(response)) {
                        // close socket
                        break;
                    }
                    // get response message type
                    int index = response.indexOf(" ");
                    MessageType type = MessageType.valueOf(response.substring(0, index));
                    // turn on hold flag upon receiving HOLD message
                    if (type == MessageType.HOLD) {
                        hold = true;
                        // nothing to send to client
                        continue;
                    }
                    if (type == MessageType.BOARD_CHANGED && hold) {
                        // holding BOARD_CHANGED
                        msgHeld = response;
                        continue;
                    }
                    if (type == MessageType.DISPOSE) {
                        dispose = true;
                        // nothing to send to client
                        continue;
                    }
                    if (type == MessageType.AVAILABLE_MATCHES && dispose) {
                        dispose = false;
                        // do not send to client, user initiated matches changes
                        continue;
                    }
                    // otherwise, send response to client
                    // System.out.println("Response:" + response);
                    out.println(response);

                    // TRY or CHALLENGE message has been sent to client
                    // release hold and reset message held
                    if (type == MessageType.TRY || type == MessageType.CHALLENGE) {
                        hold = false;
                        if (!msgHeld.isEmpty()) {
                            out.println(msgHeld);
                            msgHeld = "";
                        }
                    }
                    // game back to Choose state, reset dispose flag
                    if (type == MessageType.GET_MATCHES || type == MessageType.GET_PUZZLES) {
                        dispose = false;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            responseQueue.clear();
            out.close();
        }
    }

    /**
     * Handle a single client connection. Returns when the client disconnects.
     * 
     * @param socket             socket connected to client
     * @param responseQueue
     * @param gameChangeListener
     * @throws IOException
     *                     if the connection encounters an error or closes
     *                     unexpectedly
     */
    private void receiveRequest(final Socket socket, final BlockingQueue<String> responseQueue) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));

        try {
            for (String input = in.readLine(); input != null; input = in.readLine()) {
                try {
                    System.out.println("Request: " + input);
                    String responseMessage = handleRequest(input, responseQueue);
                    // System.out.println("Response Message: " + responseMessage);
                    if (!"".equals(responseMessage)) {
                        // put responseMessage on queue for sending back to client
                        try {
                            responseQueue.put(responseMessage);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if ("QUIT".equals(responseMessage)) {
                            // break and finally will close the socket
                            break;
                        }
                    }
                } catch (Exception e) {
                    // put Invalid_Request responseMessage on queue for sending back to client
                    try {
                        e.printStackTrace();
                        responseQueue.put(buildMessage(MessageType.INVALID_REQUEST, input));
                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        } finally {
            in.close();
        }
    }

    /**
     * Handle a single client request and return response.
     * 
     * @param input              message from client
     * @param gameChangeListener
     * @return output message to client
     * @throws IOException
     */
    private String handleRequest(final String input, final BlockingQueue<String> responseQueue) throws IOException {
        String[] tokens = input.split("\\s+");
        MessageType command;
        String userID = "";
        String matchID = "";

        try {
            command = MessageType.valueOf(tokens[0].toUpperCase());
        } catch (Exception e) {
            throw new UnsupportedOperationException(input);
        }

        if (tokens.length > 1) {
            // get userID
            userID = tokens[1];
        }
        if (tokens.length > 2) {
            // get matchID
            matchID = tokens[2];
        }

        switch (command) {
        case ADD_USER:
            if (tokens.length != 2)
                throw new UnsupportedOperationException(input);

            String feedback = game.addUser(userID);
            if ("Success".equals(feedback)) {
                // added user successfully, start listen on available matches changes
                game.addMatchChangeListener(new GameChangeListener() {

                    public void boardChanged() {
                        try {
                            String response = buildMessage(MessageType.AVAILABLE_MATCHES, game.availableMatches());
                            responseQueue.put(response);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            return buildMessage(command, feedback);

        case GET_PUZZLES:
            return buildMessage(command, game.allPuzzles());

        case GET_MATCHES:
            return buildMessage(command, game.availableMatches());

        case NEW_MATCH:
            if (tokens.length < TOKEN_MAX)
                throw new UnsupportedOperationException(input);

            String puzzleID = tokens[TOKEN_THREE];
            try {
                // add internal DISPOSE message to responseQueue to dispose AVAILABLE_MATCHES
                // message as current user initiated the change
                responseQueue.put(MessageType.DISPOSE + " 0");
                
                // input wrapped by double quotes is description
                int begin = input.indexOf("\"");
                int end = input.lastIndexOf("\"");
                String description = input.substring(begin + 1, end);
                game.newMatch(matchID, description, puzzleID, userID);
                game.addGameChangeListener(matchID, new MatchChangeListener(matchID, userID, responseQueue, game));
                return buildMessage(command, SUCCESS);
            } catch (Exception e) {
                return buildMessage(command, FAIL + " " + e.getMessage());
            }

        case PLAY_MATCH:
            if (tokens.length <= 2)
                throw new UnsupportedOperationException(input);

            try {
                // add internal DISPOSE message to responseQueue to dispose AVAILABLE_MATCHES
                // message as current user initiate the change
                responseQueue.put(MessageType.DISPOSE + " 0");
                
                game.playMatch(userID, matchID);
                game.addGameChangeListener(matchID, new MatchChangeListener(matchID, userID, responseQueue, game));
                return buildMessage(MessageType.BOARD_CHANGED, game.matchView(matchID, userID));
            } catch (Exception e) {
                return buildMessage(command, FAIL + " " + e.getMessage());
            }

        case EXIT_MATCH:
            if (tokens.length <= 2)
                throw new UnsupportedOperationException(input);

            userID = tokens[1];
            matchID = tokens[2];
            try {
                game.exitMatch(matchID, userID);
                // add internal DISPOSE message to responseQueue to dispose AVAILABLE_MATCHES
                // message as current user initiate the change
                responseQueue.put(MessageType.DISPOSE + " 0");
            } catch (Exception e) {
                // ignore
            }

            return "";

        case TRY:
            if (tokens.length != TOKEN_MAX)
                throw new UnsupportedOperationException(input);

            try {
                // add internal HOLD message to responseQueue to hold BOARD_CHANGED message
                // to make sure TRY message will be sent before BOARD_CHANGED message
                responseQueue.put(MessageType.HOLD + " 0");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int tryWordID = Integer.parseInt(tokens[TOKEN_THREE]);
            String guess = tokens[TOKEN_FOUR];
            String msg = game.tryGuess(matchID, userID, tryWordID, guess);
            return buildMessage(command, msg);

        case CHALLENGE:
            if (tokens.length != TOKEN_MAX)
                throw new UnsupportedOperationException(input);

            try {
                // add internal HOLD message to responseQueue to hold BOARD_CHANGED message
                // to make sure CHALLENGE message will be sent before BOARD_CHANGED message
                responseQueue.put(MessageType.HOLD + " 0");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int wordID = Integer.parseInt(tokens[TOKEN_THREE]);
            String word = tokens[TOKEN_FOUR];
            return buildMessage(command, game.challenge(matchID, userID, wordID, word));

        case QUIT:
            if (tokens.length < 2)
                throw new UnsupportedOperationException(input);

            game.quitGame(userID);
            return command.toString();

        default:
            // if we reach here, the client message did not follow the protocol
            throw new UnsupportedOperationException(input);
        }
    }

    // Builder response message with messageType, number of lines to follow, and
    // content.
    private static String buildMessage(MessageType messageType, String content) {
        StringBuilder builder = new StringBuilder();
        builder.append(messageType).append(" ").append(content.split("\n").length);
        builder.append("\n").append(content);
        return builder.toString();
    }

    // Inner class for GameChangeListener
    static class MatchChangeListener implements GameChangeListener {

        private final String matchID;
        private final String userID;
        private final BlockingQueue<String> responseQueue;
        private final Game game;

        /**
         * Create a MatchChangeListener to listen on match board change.
         * 
         * @param matchID match id
         * @param userID  user id
         * @param queue   response blockingQueue
         * @param game    game
         */
        MatchChangeListener(final String matchID, final String userID, final BlockingQueue<String> queue,
                final Game game) {
            this.userID = userID;
            responseQueue = queue;
            this.matchID = matchID;
            this.game = game;
        }

        /**
         * This method will be called when Game changed.
         */
        public void boardChanged() {
            String response;

            System.out.println("In boardChanged! player=" + userID);

            // check if game over
            if (game.isMatchOver(matchID)) {
                response = buildMessage(MessageType.GAME_OVER, game.matchView(matchID, userID));
            } else {
                response = buildMessage(MessageType.BOARD_CHANGED, game.matchView(matchID, userID));
            }

            // board changed, put response on blockingQueue to send back to client
            try {
                responseQueue.put(response);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
