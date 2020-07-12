/*
 * Copyright (c) 2019 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course
 * staff.
 */
package crossword;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * Mutable ADT representing a client of the Crossword Extravaganza game. Upon
 * calling the
 * main method, a Client object is instantiated; the Client will then be
 * responsible for
 * drawing the window, taking input from the user in said window, and moving the
 * user through
 * the states of the game. The states are:
 * <ul>
 * <li><b>START</b>: the user types in a username (ID); if it's invalid they get
 * to keep trying until
 * it's valid, at which point they enter the CHOOSE state.
 * <li><b>CHOOSE</b>: the user is presented with a list of available matches
 * (ready to join) and puzzles (to start a
 * new match) and has three options for commands to type:
 * <ul>
 * <li><b>PLAY match_id</b>: the user joins the given match (which a first
 * player is already in),
 * and transitions right to the PLAY state
 * <li><b>NEW match_id puzzle_id "description"</b>: the user creates a new
 * match, using the given
 * puzzle, with the given description; they then transition to the WAIT state.
 * <li><b>EXIT</b>: this just terminates the session; it should close the window
 * </ul>
 * <li><b>WAIT</b>: the user has one valid command, which is EXIT; otherwise,
 * they just wait for
 * another player to join this match
 * <li><b>PLAY</b>: the current state of the board is shown in the UI, and
 * updates accordingly as this user
 * or the other user do things. The user has three valid commands:
 * <ul>
 * <li><b>TRY id word</b>: the user tries the given word for the given spot
 * <li><b>CHALLENGE id word</b>: the user challenges the word, and their score
 * updates accordingly
 * <li><b>EXIT</b>: this forfeits the match, and transitions immediately to the
 * SHOW SCORE state
 * </ul>
 * <li><b>SHOW SCORE</b>: the scores from the game that just finished are shown,
 * and the user has two
 * possible commands:
 * <ul>
 * <li><b>NEW MATCH</b>: the user returns to the CHOOSE state
 * <li><b>EXIT</b>: again just terminates the session, and closes the window
 * </ul>
 * 
 * The only requirement is that before calling {@link Client#main(String[])
 * main} from the command line,
 * there must be a server at the address in the command line arguments.
 * </ul>
 */
public class Client {

    private final Socket socket;
    private final BufferedReader socketIn;
    private final PrintWriter socketOut;
    private final String username;
    private String matchID;

    /*
     * AF(socket, socketIn, socketOut, username, matchID): A client that connects to the
     * crossword puzzle server and communicates with the server using the socket, sending its
     * responses to the server on socketOut and reading responses from the server on socketIn.
     * This client's name is visible in game as username.
     * 
     * RI: username does not contain " ", "\n", or "\t" characters
     * 
     * SRE: All fields are private and final. All methods are private and take in
     * only immutable types
     * except for main which is public and takes in a String[]; however, only
     * element 0 of this String[] is used, which is an immutable type (String).
     * 
     * TS: All fields are private and final
     * This class is only confined to be within on thread at a time by high-level design.
     * of this project. So it is confined.
     * Only 1 thread reads from the socket, and only 1 thread writes to the socket
     * Blocking Buffered reader is used to collect server responses
     * All reads of server responses in this socket are guarded by the same lock
     * All variables in handleRequest(), receiveRequest(), and sendResponse() are
     * either local (Confinement), or thread-safe final
     * 
     * 
     */

    private static final int CANVAS_WIDTH = 1200;
    private static final int CANVAS_HEIGHT = 900;
    
    private static final int PORT = 4949;
    private static final int TEXTBOX_SIZE = 30;
    private static final int TEXTBOX_FONT_SIZE = 20;
    private static final int ENTER_BUTTON_SIZE = 10;
    
    private static final int WINDOW_WIDTH = 750;
    private static final int START_WINDOW_HEIGHT = 150;
    private static final int CHOOSE_WINDOW_HEIGHT = 500;
    private static final int WAIT_WINDOW_HEIGHT = 200;
    private static final int SHOW_SCORE_WINDOW_HEIGHT = 300;

    /**
     * Start a Crossword Extravaganza client.
     * 
     * @param args The command line arguments should include only the server
     *             address;
     *             must be a valid server.
     * @throws IOException
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException, IOException {

        final Queue<String> arguments = new LinkedList<>(List.of(args));

        final String serverAddress;

        try {
            serverAddress = arguments.remove(); //Server address should be the first and only argument
        } catch (NoSuchElementException nse) {
            throw new IllegalArgumentException("missing server address", nse);
        }

        start(serverAddress);
    }

    /**
     * Creates a new Client object, communicating with the given server; establishes
     * a connection with the server but does not transmit any information yet. Must call
     * {@link Client#start() start}
     * on the new object to actually start the game.
     * 
     * @param serverAddress the address of the server to base the game on
     * @throws IOException
     * @throws UnknownHostException
     */
    private Client(String serverAddress, String username) throws UnknownHostException, IOException {
        this.socket = new Socket(serverAddress, PORT);
        this.socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
        this.socketOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        this.username = username;
        this.matchID = ""; // Initial matchID is empty because this client has not started/joined a match yet
    }

    /**
     * Puts the game in the START state; only called once per session. Launches a
     * window with a textbox for the user to enter their user ID in, checks the ID the user submits
     * (and lets them retry while it is not valid), then moves to the user to the CHOOSE state.
     * <p>
     * After the user ID is chosen once here, it cannot change until the window
     * closes and the user starts a whole new session.
     * 
     * @param serverAddress The text string of the serverAddress of the server that
     *                      this client wishes to connect to.
     */
    private static void start(String serverAddress) {

        //Set up each of the components to display in the UI, textbox, enter button, and message
        JTextField textbox = new JTextField(TEXTBOX_SIZE);
        textbox.setFont(new Font("Arial", Font.BOLD, TEXTBOX_FONT_SIZE));

        JButton enterButton = new JButton("Enter");
        enterButton.setSize(ENTER_BUTTON_SIZE, ENTER_BUTTON_SIZE);

        JLabel message = new JLabel("<html>Please Enter Player Name<br/><html>", SwingConstants.CENTER);

        JPanel contentPane = new JPanel();
        contentPane.add(textbox);
        contentPane.add(enterButton);
        contentPane.add(message);

        //Create the window that will be displayed as the UI
        JFrame window = buildWindow("Start", contentPane, WINDOW_WIDTH, START_WINDOW_HEIGHT);

        enterButton.addActionListener((event) -> {
            // This code executes every time the user presses the Enter button.
            String username = textbox.getText();
            startStateTryConnect(username, serverAddress, contentPane, window);
        });
        
        /* 
         * This line of code allows the users to press the enter button
         * on their keyboard to perform the same action as clicking the
         * enter button in the window
         */
        window.getRootPane().setDefaultButton(enterButton);

    }

    /**
     * Gets a list of current matches and boards from the server, and displays them
     * for the user in the window; the user has three options for commands to type:
     * <ul>
     * <li><b>PLAY match_id</b>: the user joins the given match (which a first
     * player is already in), and transitions right to the PLAY state
     * <li><b>NEW match_id puzzle_id "description"</b>: the user creates a new
     * match, using the given puzzle, with the given description; they then transition to the WAIT state.
     * <li><b>EXIT</b>: this just terminates the session; it should close the window
     * </ul>
     * 
     * @throws IOException
     * 
     */
    private void choose() throws IOException {
        /* 
         * There should be no responses from the server to read at this point
         * Empty out the socketIn just to be sure this client is ready to recieve
         * server responses
         */
        this.emptyOutSocketIn();

        //Collect all of the puzzles and matches to display in the UI window
        this.socketOut.println("get_puzzles");
        String puzzles = this.handleServerResponses()[1];
        String puzzlesHTML = buildHtmlList("Puzzles", puzzles);

        this.socketOut.println("get_matches");
        String matches = this.handleServerResponses()[1];
        String matchesHTML = buildHtmlList("Matches", matches);

        //These are the commands the the user is allowed to enter in this state
        String validCommandsHTML = "<html>Valid Commands are:<br/>" +
                                   "play match_ID<br/>" +
                                   "new match_ID puzzle_ID \"description\"<br/>" + 
                                   "exit<html>";
        //Initial feeback from the server is empty
        String feedback = "";
        /*
         * This is the display containing each component of text to display in the UI in the choose state
         * The purpose of this is to have a mutable collection of strings that can be modified by the enter
         * button action listener and the thread listening for choose state server responses. Because these
         * threads need this collection to be final, the strings inside the collection can be modified as needed
         */
        final String[] display = new String[] { puzzlesHTML, matchesHTML, validCommandsHTML, feedback };
        String puzzlesMatchesValidCommands = buildChooseDisplay(display);

        JTextField textbox = new JTextField(TEXTBOX_SIZE);
        textbox.setFont(new Font("Arial", Font.BOLD, TEXTBOX_FONT_SIZE));

        JButton enterButton = new JButton("Enter");
        enterButton.setSize(ENTER_BUTTON_SIZE, ENTER_BUTTON_SIZE);

        //This label contains all of the text to display in the UI
        JLabel puzzlesMatchesValidCommandsLabel = new JLabel(puzzlesMatchesValidCommands, SwingConstants.CENTER);

        JPanel contentPane = new JPanel();
        contentPane.add(textbox);
        contentPane.add(enterButton);
        contentPane.add(puzzlesMatchesValidCommandsLabel);

        //Create the window that will be displayed as the UI
        JFrame window = buildWindow("Choose " + this.username, contentPane, WINDOW_WIDTH, CHOOSE_WINDOW_HEIGHT);

        enterButton.addActionListener((event) -> {
            // This code executes every time the user presses the Enter button.
            //This code should only send requests to the server, never read responses
            String input = textbox.getText();
            try {
                String[] tokens = input.split(" ");
                String command = tokens[0].toUpperCase();

                if (input.equalsIgnoreCase("EXIT")) {
                    this.socketOut.println("QUIT " + this.username);
                    window.dispose();
                    this.exit();

                } else if (command.equalsIgnoreCase("PLAY")) {
                    this.chooseStateHandlePlay(tokens, display, contentPane, window);

                } else if (command.equalsIgnoreCase("NEW")) {
                    this.chooseStateHandleNew(tokens, display, contentPane, window);

                } else {
                    String response = "Must enter one of the valid commands";
                    printResponseToChooseStateUI(response, display, contentPane, window);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        
        /* 
         * This line of code allows the users to press the enter button
         * on their keyboard to perform the same action as clicking the
         * enter button in the window
         */
        window.getRootPane().setDefaultButton(enterButton);
        
        /*
         * This thread listens for all responses from the server and handles them
         * Including responses for when new matches are added to the game
         */
        Thread thread = this.getChooseStateThread(contentPane, window, display);
        thread.start();
    }

    /**
     * Asks the server to notify when a second user joins the match; when they do,
     * transitions to the PLAY state. While waiting, the user has one valid command,
     * <b>EXIT</>, which terminates the match and returns to the choose state
     * 
     * @throws IOException
     */
    private void waitForPlayer() throws IOException {

        /* 
         * There should be no responses from the server to read at this point
         * Empty out the socketIn just to be sure this client is ready to recieve
         * server responses
         */
        this.emptyOutSocketIn();

        String validCommandsHTML = "<html>Waiting for Second Player To Join<br/>" +
                                   "Valid Commands are:<br/>" +
                                   "exit<html>";

        //set up each of the components contained in the UI, textbox, enter button, server feedback
        JTextField textbox = new JTextField(TEXTBOX_SIZE);
        textbox.setFont(new Font("Arial", Font.BOLD, TEXTBOX_FONT_SIZE));

        JButton enterButton = new JButton("Enter");
        enterButton.setSize(ENTER_BUTTON_SIZE, ENTER_BUTTON_SIZE);

        JLabel validCommandsLabel = new JLabel(validCommandsHTML, SwingConstants.CENTER);

        JPanel contentPane = new JPanel();
        contentPane.add(textbox);
        contentPane.add(enterButton);
        contentPane.add(validCommandsLabel);

        //Create the window that will be displayed as the UI
        JFrame window = buildWindow("Wait For Player " + this.username, contentPane, WINDOW_WIDTH, WAIT_WINDOW_HEIGHT);

        enterButton.addActionListener((event) -> {
            // This code executes every time the user presses the Enter Button
            String input = textbox.getText();

            if (input.equalsIgnoreCase("EXIT")) {
                this.socketOut.println("EXIT_MATCH " + this.username + " " + this.matchID);
            } else {
                String response = "Must enter one of the valid commands";
                System.out.println(response);
                printResponseToUI(validCommandsHTML, response, contentPane, window);
            }

        });
        
        /* 
         * This line of code allows the users to press the enter button
         * on their keyboard to perform the same action as clicking the
         * enter button in the window
         */
        window.getRootPane().setDefaultButton(enterButton);

        /*
         * This thread listens for all responses from the server and handles them
         * Including responses for when a new player joins this match so it is ready to play
         */
        Thread thread = this.getWaitForPlayerThread(window);
        thread.start();
    }

    /**
     * Displays the current board to the user, and updates it accordingly as the
     * match progresses; if the other user modifies the board, it still updates in this user's UI.
     * <p>
     * The user has three valid commands:
     * <ul>
     * <li><b>TRY id word</b>: the user tries the given word for the given spot
     * <li><b>CHALLENGE id word</b>: the user challenges the word, and their score
     * updates accordingly
     * <li><b>EXIT</b>: this forfeits the match, and transitions immediately to the
     * SHOW SCORE state
     * </ul>
     * When the game finishes (either by forfeit or by the puzzle being completed),
     * transitions to the SHOW SCORE state.
     * 
     * @throws IOException
     */
    private void play(String board) throws IOException {

        /* 
         * There should be no responses from the server to read at this point
         * Empty out the socketIn just to be sure this client is ready to recieve
         * server responses
         */
        this.emptyOutSocketIn();

        System.out.println("play");
        
        //Create the crossword puzzle displayed in the UI
        CrosswordCanvas canvas = new CrosswordCanvas();
        canvas.setSize(CANVAS_WIDTH, CANVAS_HEIGHT);
        canvas.drawBoard(board);

        //set up each of the components contained in the UI, enter button, textbox, server feedback
        JButton enterButton = new JButton("Enter");
        enterButton.setSize(ENTER_BUTTON_SIZE, ENTER_BUTTON_SIZE);

        JTextField textbox = new JTextField(TEXTBOX_SIZE);
        textbox.setFont(new Font("Arial", Font.BOLD, TEXTBOX_FONT_SIZE));

        String validCommandsHTML = "<html>Please Enter Your Command.<br/>" +
                                   "Valid commands are:<br/>" +
                                   "try (word_number) (word_guess)<br/>" + 
                                   "challenge (word_number) (word_guess)<br/>" + 
                                   "exit<html>";

        JLabel feedbackLabel = new JLabel(validCommandsHTML, SwingConstants.LEFT);

        JPanel contentPane = new JPanel();
        contentPane.add(textbox);
        contentPane.add(enterButton);
        contentPane.add(feedbackLabel);
        
        final int extraWindowSpace = 50;
        //Create the window that will be displayed as the UI
        JFrame window = buildWindowWithCanvas("Play " + this.username, canvas, contentPane, CANVAS_WIDTH + extraWindowSpace, CANVAS_HEIGHT + extraWindowSpace);

        enterButton.addActionListener((event) -> {
            // This code executes every time the user presses the Enter Button
            // All gameplay-related requests are sent to the server in here
            
            String input = textbox.getText();
            String[] tokens = input.split(" ");
            String command = tokens[0].toUpperCase();
            if (input.equalsIgnoreCase("EXIT")) {
                this.socketOut.println("EXIT_MATCH " + this.username + " " + matchID);

            } else if (command.equalsIgnoreCase("TRY")) {
                this.playStateHandleRequest("try", tokens, contentPane, window);
                

            } else if (command.equalsIgnoreCase("CHALLENGE")) {
                this.playStateHandleRequest("challenge", tokens, contentPane, window);

            } else {
                String response = "Must enter<br/>one of the valid commands";
                System.out.println(response);
                printResponseToUI(validCommandsHTML, response, contentPane, window);
            }
        });
        
        /* 
         * This line of code allows the users to press the enter button
         * on their keyboard to perform the same action as clicking the
         * enter button in the window
         */
        window.getRootPane().setDefaultButton(enterButton);

        /*
         * This thread listens for all responses from the server and handles them
         * Including responses for game board updates, players quiting, and for when the 
         * game has finished
         */
        Thread thread = this.getPlayStateThread(canvas, contentPane, window);
        thread.start();
    }


    /**
     * Displays the scores of the game that just finished to the users indefinitely.
     * <p>
     * The user has two possible commands:
     * <ul>
     * <li><b>NEW MATCH</b>: the user returns to the CHOOSE state
     * <li><b>EXIT</b>: again just terminates the session, and closes the window
     * </ul>
     */
    private void showScore(String playerScores) {
        /* 
         * There should be no responses from the server to read at this point
         * Empty out the socketIn just to be sure this client is ready to recieve
         * server responses
         */
        this.emptyOutSocketIn();

        String scoresHTML = buildHtmlList("Final Scores", playerScores);

        String displayHTML = "<html>Game Over<br/>" + 
                                    scoresHTML + 
                                    "Please Enter Your Command<br/>" +
                                    "Valid Commands are:<br/>" + 
                                    "new match<br/>" + 
                                    "exit<html>";

      //set up each of the components contained in the UI, textbox, enterbutton, server feedback about invalid commands
        JTextField textbox = new JTextField(TEXTBOX_SIZE);
        textbox.setFont(new Font("Arial", Font.BOLD, TEXTBOX_FONT_SIZE));

        JButton enterButton = new JButton("Enter");
        enterButton.setSize(ENTER_BUTTON_SIZE, ENTER_BUTTON_SIZE);

        JLabel validCommandsLabel = new JLabel(displayHTML, SwingConstants.CENTER);

        JPanel contentPane = new JPanel();
        contentPane.add(textbox);
        contentPane.add(enterButton);
        contentPane.add(validCommandsLabel);

        //Create the window that will be displayed as the UI
        JFrame window = buildWindow("Show Score " + username, contentPane, WINDOW_WIDTH, SHOW_SCORE_WINDOW_HEIGHT);

        enterButton.addActionListener((event) -> {
            // This code executes every time the user presses the Enter Button
            //All requests to the server in this state are sent from here

            String input = textbox.getText();
            if (input.equalsIgnoreCase("NEW MATCH")) {
                try {
                    window.dispose();
                    this.choose();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (input.equalsIgnoreCase("EXIT")) {
                this.socketOut.println("QUIT " + this.username);
                window.dispose();
                this.exit();

            } else {
                String response = "Must enter one of the valid commands";
                System.out.println(response);
                printResponseToUI(displayHTML, response, contentPane, window);
            }
        });
        
        /* 
         * This line of code allows the users to press the enter button
         * on their keyboard to perform the same action as clicking the
         * enter button in the window
         */
        window.getRootPane().setDefaultButton(enterButton);
    }

    /**
     * Exits the current state, terminates the connection to the server for this
     * user, cleans up all resources used, and closes the window.
     */
    private void exit() {
        try {
            this.socketIn.close();
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.socketOut.close();
        System.exit(0);
        return;
    }

    /**
     * Helper function for the start method. Takes in an input username from the user and attempts to
     * connect to the server using the given username. Also prints out responses to the UI for invalid entries
     * @param username Username String entered by the user
     * @param serverAddress address of the server to connect to
     * @param contentPane the JPanel containing all of the content in the window displaying the UI
     * @param window the JFrame window that displays the UI
     */
    private static void startStateTryConnect(String username, String serverAddress, JPanel contentPane, JFrame window) {
        boolean validUsername = checkValidUsername(username);
        if (validUsername) {
            try {
                Client client = new Client(serverAddress, username);
                client.socketOut.println("ADD_USER " + client.username);
                String response = client.handleServerResponses()[1];
                System.out.println(response);
                if (response.equalsIgnoreCase("Success\n")) {
                    client.choose();
                    window.setVisible(false);

                } else {
                    System.out.println("Entered player name is already in use");
                    printResponseToUI("<html>Please Enter Player Name<html>", "Entered player name is already in use",
                            contentPane, window);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else { //Username was not valid
            System.out.println("Username cannot contain spaces");
            printResponseToUI("<html>Please Enter Player Name<html>", "Username cannot contain spaces", contentPane,
                    window);
        }
    }

    /**
     * Handles the functionality for when a play request is sent from the choose state
     * attempts to joining the match this.matchID and will print the appropriate responses
     * to the UI for failed attempts
     * @param tokens the String tokens of the user input divided up by spaces
     * @param display The array of all of the HTML Strings that are displayed in the UI
     * @param contentPane the JPanel containing all of the content in the window displaying the UI
     * @param window the JFrame window that displays the UI
     * @throws IOException
     */
    private void chooseStateHandlePlay(String[] tokens, String[] display, JPanel contentPane, JFrame window) throws IOException {
        final int requiredTokens = 2;
        if (tokens.length == requiredTokens) {
            String newMatchID = tokens[1];
            this.setMatchID(newMatchID);
            this.socketOut.println("PLAY_MATCH " + this.username + " " + matchID);
        } else { //If the play request did not have the correct amount of tokens
            String response = "Play command requires 2 words: play match_id";
            printResponseToChooseStateUI(response, display, contentPane, window);
        }
    }

    /**
     * Handles the functionality for when a new request is sent from the choose state
     * attempts to create the match with the matchID entered by the user and will 
     * print the appropriate responses to the UI for failed attempts
     * @param tokens the String tokens of the user input divided up by spaces
     * @param display The array of all of the HTML Strings that are displayed in the UI
     * @param contentPane the JPanel containing all of the content in the window displaying the UI
     * @param window the JFrame window that displays the UI
     * @throws IOException
     */
    private void chooseStateHandleNew(String[] tokens, String[] display, JPanel contentPane, JFrame window) throws IOException {
        final int minimumTokens = 4;
        if (tokens.length >= minimumTokens) {
            String newMatchID = tokens[1];
            String puzzleID = tokens[2];
            String description = tokens[3];
            this.setMatchID(newMatchID);
            for (int i = minimumTokens; i < tokens.length; i++) {
                description += " " + tokens[i];
            }
            //Check for all of the possible invalid descriptions
            if (!(description.substring(0, 1).equals("\"")) ||
                !(description.substring(description.length() - 1).equals("\"")) ||
                 (description.equals("\""))) {
                String response = "New command requires following format: new match_id puzzle_name \"Description (in quotes)\"";
                printResponseToChooseStateUI(response, display, contentPane, window);
            } else if (description.equals("\"\"")) {
                String response = "Description cannot be empty";
                printResponseToChooseStateUI(response, display, contentPane, window);
            } else { //If the description is valid, send the request to the server
                this.socketOut.println("NEW_MATCH " + this.username + " " + matchID + " " + puzzleID + " " + description);
            }

        } else { //If the new request did not have the correct amount of tokens
            String response = "New command requires following format: new (match_id) (puzzle_name) (\"Description (in quotes)\")";
            printResponseToChooseStateUI(response, display, contentPane, window);
        }
    }
    
    /**
     * The UI in the choose state is different from the UIs of the other states
     * and so requires this specialized method to print out server responses
     * into its UI. Takes in the response from the server and prints it to the
     * choose state UI
     * @param response the response from the server to print to the UI
     * @param display the array containing each HTML string that is displayed
     *        in the choose state
     * @param contentPane the JPanel containing all of the content in the window displaying the UI
     * @param window the JFrame window that displays the UI
     */
    private static void printResponseToChooseStateUI(String response, String[] display, JPanel contentPane, JFrame window) {
        setChooseDisplayResponse(display, response);
        String newPuzzlesAndMatches = buildChooseDisplay(display);
        System.out.println(response);
        printResponseToUI(newPuzzlesAndMatches, response, contentPane, window);
    }
    

    /**
     * The thread that listens for server responses while in the choose state.
     * Takes in responses from the server and determines how to handle each response.
     * @param contentPane the JPanel containing all of the content in the window displaying the UI
     * @param window the JFrame window that displays the UI
     * @param display the array containing each HTML string that is displayed
     *        in the choose state
     * @return The thread that listens for server responses while in the choose state.
     */
    private Thread getChooseStateThread(JPanel contentPane, JFrame window, String[] display) {
        Client client = this;
        return new Thread() {

            private volatile boolean keepGoing = true;
            public void stopTheThread() { //Function to terminate this thread when transitioning to the next game state
                keepGoing = false;
            }

            @Override
            public void run() {
                keepGoing = true;
                while (keepGoing) {
                    try {
                        String[] responseArray = client.handleServerResponses();
                        String type = responseArray[0];
                        String response = responseArray[1];
                        
                        if (type.equalsIgnoreCase("NEW_MATCH") && response.equalsIgnoreCase("Success\n")) { //This client created a new match
                            window.dispose();
                            stopTheThread();
                            client.waitForPlayer();
                        }
                        
                        else if (type.equalsIgnoreCase("BOARD_CHANGED")) { //This client sent a play request and will now join the match desired
                            String board = response;
                            window.dispose();
                            stopTheThread();
                            client.play(board);
                        }
                        
                        else if (type.equalsIgnoreCase("AVAILABLE_MATCHES")) { //Update to the matches availible in the game, update the chooseState UI as needed              
                            // update puzzles and matches info
                            String matchesHTML = buildHtmlList("Matches", response);
                            String oldResponse = getChooseDisplayResponse(display);
                            setChooseDisplayMatches(display, matchesHTML);
                            printResponseToChooseStateUI(oldResponse, display, contentPane, window);
                        }
                        
                        else {
                            final int responseStringStart = 5;
                            response = response.substring(responseStringStart);
                            printResponseToChooseStateUI(response, display, contentPane, window);
                        }
                        
  
                    } catch (IOException e) {
                        e.printStackTrace();
                    } 
                    
                }  
            } 
        };

        
    }

    
    /**
     * The thread that listens for server responses while in the wait state.
     * Takes in responses from the server and determines how to handle each response.
     * @param window the JFrame window that displays the UI
     * @return The thread that listens for server responses while in the wait state.
     */
    private Thread getWaitForPlayerThread(JFrame window) {
        Client client = this;
        return new Thread() {

            private volatile boolean keepGoing = true;
            public void stopTheThread() { // Function to stop this thread when transitioning to the next game state
                keepGoing = false;
            }

            @Override
            public void run() {
                keepGoing = true;
                while (keepGoing) {
                    try {
                        String[] responseArray = client.handleServerResponses();
                        String type = responseArray[0];
                        String response = responseArray[1];
                        if (type.equalsIgnoreCase("BOARD_CHANGED")) { //A second player has joined this match and it is now ready to play
                            String board = response;
                            window.dispose();
                            stopTheThread();
                            client.play(board);
                        }
    
                        else if (type.equalsIgnoreCase("GAME_OVER")) { //The client decided to exit out of this match
                            client.setMatchID("");
                            window.dispose();
                            stopTheThread();
                            client.choose();  
                        }
                        
                        else if (type.equalsIgnoreCase("AVAILABLE_MATCHES")) { //Update to the matches in the game, ignore because it is only relevant for choose state
                           // ignore
                        }
                        
    
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    
                }
            }
        };
        
                
    }
    
    /**
     * Helper method to handle try and challenge requests while in the play state.
     * Also handles invalid responses and prints out appropriate feedback to the UI
     * @param requestType Must be either try or challenge (ignore case) for the 
     *        try or challenge requests
     * @param tokens the String tokens of the user input divided up by spaces
     * @param contentPane the JPanel containing all of the content in the window displaying the UI
     * @param window the JFrame window that displays the UI
     */
    private void playStateHandleRequest(String requestType, String[] tokens, JPanel contentPane, JFrame window) {
        final int requiredTokensLength = 3;
        if (tokens.length == requiredTokensLength) {
            String wordID = tokens[1];
            String wordGuess = tokens[2];
            this.socketOut.println(requestType +" " + this.username + " " + this.matchID + " " + wordID + " " + wordGuess); // Send a try or challenge request to the server
            
        } else { //User sent an invalid request
            String validCommandsHTML = "<html>Please Enter Your Command. Valid commands are:<br/>" +
                                       "try (word_number) (word_guess)<br/>" + 
                                       "challenge (word_number) (word_guess)<br/>" + 
                                       "exit<html>";
            String response = requestType+" request requires 3 total words:<br/>"+
                              requestType+" [word_number] [word_guess]";
            System.out.println(response);
            printResponseToUI(validCommandsHTML, response, contentPane, window);
        }
    }
    
    /**
     * The thread that listens for server responses while in the play state.
     * Takes in responses from the server and determines how to handle each response.
     * @param canvas the crossword canvas that is drawn to and that the players are
     *        playing on.
     * @param contentPane the JPanel containing all of the content in the window displaying the UI
     * @param window the JFrame window that displays the UI
     * @return The thread that listens for server responses while in the play state.
     */
    private Thread getPlayStateThread(CrosswordCanvas canvas, JPanel contentPane, JFrame window) {
        String validCommandsHTML = "<html>Please Enter Your Command.<br/>" +
                                   "Valid commands are:<br/>" +
                                   "try (word_number) (word_guess)<br/>" + 
                                   "challenge (word_number) (word_guess)<br/>" + 
                                   "exit<html>";
        
        Client client = this;
        return new Thread() {

            private volatile boolean keepGoing = true;
            public void stopTheThread() { //Function to to stop this thread when transitioning to the next game state
                keepGoing = false;
            }

            @Override
            public void run() {
                keepGoing = true;
                while (keepGoing) {
                    try {
                        String[] responseArray = client.handleServerResponses();
                        String type = responseArray[0];
                        String response = responseArray[1];

                        if (type.equalsIgnoreCase("BOARD_CHANGED")) { //Update to the game board, need to redraw it
                            String newBoard = response;
                            canvas.drawBoard(newBoard);
                            canvas.repaint();

                        } else if (type.equalsIgnoreCase("GAME_OVER")) { //The game is now finished. Either the board became completed or a player exited the match
                            stopTheThread();
                            String newBoard = response;
                            canvas.drawBoard(newBoard);
                            canvas.repaint();
                            System.out.println("game over");
                            window.dispose();

                            client.setMatchID("");
                            String playerScores = getPlayerScores(newBoard);
                            client.showScore(playerScores);

                        } else if (type.equalsIgnoreCase("AVAILABLE_MATCHES")) { //Update to the matches availible in the game. Ignore it because it is only relevant for the choose state
                            // ignore

                        } else { // Previous try or challenge request was successful or a failure
                            System.out.println(response);
                            printResponseToUI(validCommandsHTML, response, contentPane, window);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    
                }
            }
        };
        
        
    }
    
    
    /**
     * Reads out all of the lines of a single server response inside
     * this client's socketIn. builds the type of the response as
     * well as the content of the response together as a two-element
     * array and returns it
     * @return the type of the response along with the content String
     *         of the response as a two-element array
     * @throws IOException
     */
    private synchronized String[] handleServerResponses() throws IOException {
        //Get the type of the response and number of lines containing this response, and then read it
        String firstLine = this.socketIn.readLine();
        String type = firstLine.split(" ")[0];
        int numLines = Integer.parseInt(firstLine.split(" ")[1]);
        String response = "";
        for (int i = 0; i < numLines; i++) {
            response += this.socketIn.readLine() + "\n";
        }
        //Return the type of the response along with the content of the response just read
        return new String[] { type, response };
    }

    /**
     * Prints out the new string to the UI two lines below where the old string was. Useful
     * for printing the server responses to the UI so that the client can see the feedback
     * from the server about the commands they are entering. Both the old string and new string
     * will be visible in the UI with two lines of separation between them after this function call
     * @param oldString the String that used to be displayed in the UI. Need this to ensure
     *        the new string gets added to it (but two lines below it)
     * @param newString the new string to be printed to the UI two line's below where the old
     *        String was.
     * @param contentPane the JPanel containing all of the content in the window displaying the UI
     * @param window the JFrame window that displays the UI
     */
    private static void printResponseToUI(String oldString, String newString, JPanel contentPane, JFrame window) {
        /*
         * Print the response to the UI by removing the old text currently on the UI and adding the new text
         * combined with the old text to the UI
         */
        contentPane.remove(contentPane.getComponentCount() - 1);
        JLabel newLabel = new JLabel(oldString + "<html><br/><br/>" + newString + "<html>", SwingConstants.CENTER);
        contentPane.add(newLabel);
        int width = window.getWidth();
        int height = window.getHeight();
        //Need to resize the window. The newly updated text doesn't show up in the window until it resizes
        window.setSize(width + 1, height + 1);
        window.setSize(width, height); //Set the window back to its original size now that the new text shows up in it
    }

    /**
     * Takes in a single String of made up of multiple lines
     * and formats it to appear as a list in HTML to display
     * as a list with each String on it's own line
     * @param title the title of the String list, for example: puzzles, matches, etc.
     * @param inputList the String with each list item on it's own line.
     * @return
     */
    private static String buildHtmlList(String title, String inputList) {
        String responseHtmlList = "<html>" + title + ":<br/>"; //Start the list with the title of it
        String[] inputArray = inputList.split("\n");
        for (int i = 0; i < inputArray.length; i++) { //Add each item to the list with a newline
            responseHtmlList += inputArray[i];
            responseHtmlList += "<br/>"; //newline character for HTML
        }
        responseHtmlList += "<br/><html>";
        return responseHtmlList;
    }
    
    /**
     * Builds a window to display as the UI for the user to view and interact with and returns it
     * @param title the title of the window displayed on the top bar of the window
     * @param contentPane the JPanel containing all of the content in the window displaying the UI
     * @param width the desired width of the window
     * @param height the desired height of the window
     * @return the window to display as the UI for the user to view and interact
     */
    private static JFrame buildWindow(String title, JPanel contentPane, int width, int height) {
        JFrame window = new JFrame(title);
        window.setLayout(new BorderLayout());
        window.add(contentPane, BorderLayout.SOUTH);
        window.setSize(width, height);
        window.getContentPane().add(contentPane);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setVisible(true);
        return window;
    }

    /**
     * Same function as build window, except that it builds this window with the crossword canvas the players
     * are playing on included in the window.
     *    
     * Builds a window to display as the UI for the user to view and interact with and returns it
     * @param title the title of the window displayed on the top bar of the window
     * @param contentPane the JPanel containing all of the content in the window displaying the UI
     * @param width the desired width of the window
     * @param height the desired height of the window
     * @return the window to display as the UI for the user to view and interact, with the crossword canvas included
     */
    private static JFrame buildWindowWithCanvas(String title, CrosswordCanvas canvas, JPanel contentPane, int width, int height) {
        JFrame window = new JFrame(title);
        window.setLayout(new BorderLayout());
        window.add(canvas, BorderLayout.CENTER);
        window.add(contentPane, BorderLayout.SOUTH);
        window.setSize(width, height);
        window.getContentPane().add(contentPane);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setVisible(true);
        return window;
    }

    /**
     * Reads every line of this client's socketIn so that
     * it is empty and ready for new responses. Used so that
     * old responses don't get in the way of desired new responses
     */
    private void emptyOutSocketIn() {
        try {
            while (this.socketIn.ready()) { //Keep reading the lines until there are no more lines left to read
                this.socketIn.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Retrieves the player scores from a given board and 
     * formats them as a single string with each player
     * and their score each on their own lines
     * @param board the String representing the game board
     *        including the players' scores for the players
     *        participating in the game
     * @return the player scores from a given board and 
     *         formated them as a single string with each player
     *         and their score each on their own lines
     */
    private static String getPlayerScores(String board) {
        List<String> boardList = Arrays.asList(board.split("\n"));
        int scoresIndex = boardList.indexOf("Scores:");  //Find the index for where the player scores are located in the board
        int questionsIndex = boardList.indexOf("Questions:"); //This is the index where the player scores end

        String playerScores = "";
        for (int i = 1; i < questionsIndex - scoresIndex; i++) { //Collect each player score and add them to the final string
            playerScores += boardList.get(scoresIndex + i) + "\n";
        }

        return playerScores;
    }

    /**
     * Takes in the array containing each HTML string to
     * display in the choose state and concatenates them together into
     * a single HTML string.
     * @param display the array containing each HTML string to
     *        display in the choose state
     * @return a single HTML string made up of each HTML string
     *         to display in the choose state
     */
    private static String buildChooseDisplay(String[] display) {
        String puzzlesHTML = display[0]; //Collect each HTML string from the display array and return them as a single HTML string
        String matchesHTML = display[1];
        String validCommandsHTML = display[2];
        return puzzlesHTML + matchesHTML + validCommandsHTML;
    }

    /**
     * Checks to see if a username entered by the user is valid/
     * Valid means does not contain spaces, newlines, or tabs
     * @param username the username entered by the user
     * @return true if the username does not contain spaces,
     *         newlines, or tabs, false otherwise
     */
    private static boolean checkValidUsername(String username) {
        return !(username.contains(" ") || username.contains("\n") || username.contains("\t"));
    }
    
    private static final int DISPLAY_RESPONSE_INDEX = 3;
    /**
     * Mutates the choose display to contain a feedback response from the server
     * @param display the array of HTML strings displayed in the choose state
     * @param response the server feedback response to add to the display
     */
    private static void setChooseDisplayResponse(String[] display, String response) {
        display[DISPLAY_RESPONSE_INDEX] = response;
    }
    
    /**
     * Gets the server feedback response that is currently contained in the choose state
     * and returns it
     * @param display the array of HTML strings displayed in the choose state
     * @return the server feeback response currently displayed in the choose state
     */
    private static String getChooseDisplayResponse(String[] display) {
        return display[DISPLAY_RESPONSE_INDEX];
    }

    /**
     * Mutates the choose display to contain the new list of matches received from the server
     * @param display the array of HTML strings displayed in the choose state
     * @param newMatches list of matches received from the server to display in the UI
     */
    private static void setChooseDisplayMatches(String[] display, String newMatches) {
        final int displayMatchesIndex = 1;
        display[displayMatchesIndex] = newMatches;
    }
    
    /**
     * Sets the matchID associated with this client
     * @param matchID the new matchID to associate with this client
     */
    private void setMatchID(String matchID) {
        this.matchID = matchID;
    }

}
