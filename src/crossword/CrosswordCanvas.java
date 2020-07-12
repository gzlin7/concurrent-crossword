/*
 * Copyright (c) 2019 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course
 * staff.
 */
package crossword;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.JComponent;

/**
 * This component allows you to draw a crossword puzzle. Right now it just has
 * some helper methods to draw cells and add text in them, and some demo code
 * to show you how they are used. You can use this code as a starting point when
 * you develop your own UI.
 * 
 * @author asolar
 */
class CrosswordCanvas extends JComponent {

    private String board;

    /*
     * AF(board): A canvas to draw the crossword puzzle of a given match on. The
     * crossword puzzle board of the given match is board.
     * 
     * RI: true
     * 
     * SRE: All fields are private and immutable. All public methods take in
     * immutable types except for paint which takes in a Graphics. The method paint does nothing to
     * mutate the rep (i.e. does not modify the string board). All methods are void
     * 
     * TS: All public methods are synchronized on this. All mutations of this rep
     * are guarded by the same lock (the lock on this) so that there are no
     * race conditions on mutating this rep and then attempted to access
     * it while another thread is mutating it.
     * 
     */

    /**
     * Horizontal offset from corner for first cell.
     */
    private final int originX = 100;
    /**
     * Vertical offset from corner for first cell.
     */
    private final int originY = 150;
    /**
     * Size of each cell in crossword. Use this to rescale your crossword to have
     * larger or smaller cells.
     */
    private final int delta = 30;

    /**
     * Font for letters in the crossword.
     */
    private final Font mainFont = new Font("Arial", Font.PLAIN, delta * 4 / 5);

    /**
     * Font for small indices used to indicate an ID in the crossword.
     */
    private final Font indexFont = new Font("Arial", Font.PLAIN, delta / 3);

    /**
     * Font for small indices used to indicate an ID in the crossword.
     */
    private final Font textFont = new Font("Arial", Font.PLAIN, 12);

    /**
     * Draw a cell at position (row, col) in a crossword.
     * 
     * @param row Row where the cell is to be placed.
     * @param col Column where the cell is to be placed.
     * @param g   Graphics environment used to draw the cell.
     */
    private void drawCell(int row, int col, Graphics g) {
        Color oldColor = g.getColor(); //Get the old color of g to reset it back to this at the end of this method
        Graphics2D g2 = (Graphics2D) g; //Get a 2D graphics object from g to increases the thickness of lines drawn
        Stroke oldStroke = g2.getStroke();
        final int strokeThickness = 2;
        g2.setStroke(new BasicStroke(strokeThickness));

        g2.drawRect(originX + col * delta, originY + row * delta, delta, delta);
        g2.setStroke(oldStroke);
        final Color white = new Color(255, 255, 255);
        g.setColor(white); //Drawn the cell with a white background
        g.fillRect(originX + col * delta+1, originY + row * delta+1, delta-2, delta-2);
        g.setColor(oldColor);
    }
    
    /**
     * Draw a cell at position (row, col) in a crossword. Draw the cell as confirmed correct
     * 
     * @param row Row where the cell is to be placed.
     * @param col Column where the cell is to be placed.
     * @param g   Graphics environment used to draw the cell.
     */
    private void drawCellConfirmed(int row, int col, Graphics g) {
        Color oldColor = g.getColor(); //Get the old color of g to reset it back to this at the end of this method
        Graphics2D g2 = (Graphics2D) g; //Get a 2D graphics object from g to increases the thickness of lines drawn
        Stroke oldStroke = g2.getStroke();
        final int strokeThickness = 3; //Confirmed cell thickness is thicker than non-confirmed cell
        g2.setStroke(new BasicStroke(strokeThickness));
        
        g2.drawRect(originX + col * delta, originY + row * delta, delta, delta);
        g2.setStroke(oldStroke);
        final Color white = new Color(255, 255, 255);
        g.setColor(white); //Drawn the cell with a white background
        g.fillRect(originX + col * delta+2, originY + row * delta+2, delta-strokeThickness, delta-strokeThickness);
        g.setColor(oldColor);
    }

    /**
     * Place a letter inside the cell at position (row, col) in a crossword.
     * 
     * @param letter Letter to add to the cell.
     * @param row    Row position of the cell.
     * @param col    Column position of the cell.
     * @param g      Graphics environment to use.
     */
    private void letterInCell(String letter, int row, int col, Graphics g) {
        g.setFont(mainFont);
        FontMetrics fm = g.getFontMetrics();
        final int smallOffset = delta / 5;
        final int tinyOffset = delta / 15; //Tiny offset is used to fine tune the placement of the letter for visual appeal
        g.drawString(letter, originX + col * delta + smallOffset, originY + row * delta + fm.getAscent() + tinyOffset);
    }
    
    /**
     * Place a letter inside the cell at position (row, col) in a crossword. 
     * Draw the letter as confirmed correct on the board
     * 
     * @param letter Letter to add to the cell.
     * @param row    Row position of the cell.
     * @param col    Column position of the cell.
     * @param g      Graphics environment to use.
     */
    private void letterInCellConfirmed(String letter, int row, int col, Graphics g) {
        Color oldColor = g.getColor();
        final Color darkRed = new Color(180, 0, 0);
        g.setColor(darkRed);
        g.setFont(mainFont);
        FontMetrics fm = g.getFontMetrics();
        final int smallOffset = delta / 5;
        final int tinyOffset = delta / 15; //Tiny offset is used to fine tune the placement of the letter for visual appeal
        g.drawString(letter, originX + col * delta + smallOffset, originY + row * delta + fm.getAscent() + tinyOffset);
        g.setColor(oldColor);
    }

    /**
     * Add a vertical ID for the cell at position (row, col).
     * 
     * @param id  ID to add to the position.
     * @param row Row position of the cell.
     * @param col Column position of the cell.
     * @param g   Graphics environment to use.
     */
    private void verticalId(String id, int row, int col, Graphics g) {
        g.setFont(indexFont);
        final int smallOffset = delta / 8; //Small offset is used to fine tune the placement of the ID for visual appeal
        g.drawString(id, originX + col * delta + smallOffset, originY + row * delta - smallOffset);
    }

    /**
     * Add a vertical ID for the cell at position (row, col).
     * Draws it such that the client controls the word with this ID
     * 
     * @param id  ID to add to the position.
     * @param row Row position of the cell.
     * @param col Column position of the cell.
     * @param g   Graphics environment to use.
     */
    private void verticalIdFancy(String id, int row, int col, Graphics g) {
        g.setFont(indexFont);
        Color oldColor = g.getColor();
        final Color darkBlue = new Color(0, 0, 153); //Draw a dark blue box around the word number
        g.setColor(darkBlue);
        final int smallOffset = delta / 8;
        g.drawString(id, originX + col * delta + smallOffset, originY + row * delta - smallOffset);
        
        Graphics2D g2 = (Graphics2D) g;
        Stroke oldStroke = g2.getStroke();
        final int strokeThickness = 2;
        g2.setStroke(new BasicStroke(strokeThickness)); //Increase the thickness of the drawing stroke
        
        final int tinyOffset = delta / 15; //Tiny offset is used to fine tune the placement of the ID for visual appeal
        final int halfBox = delta / 2; 
        g2.drawRect(originX + col * delta, originY + row * delta - halfBox, halfBox - tinyOffset, halfBox);
        g2.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    /**
     * Add a horizontal ID for the cell at position (row, col).
     * 
     * @param id  ID to add to the position.
     * @param row Row position of the cell.
     * @param col Column position of the cell.
     * @param g   Graphics environment to use.
     */
    private void horizontalId(String id, int row, int col, Graphics g) {
        g.setFont(indexFont);
        FontMetrics fm = g.getFontMetrics();
        int maxwidth = fm.charWidth('0') * id.length();
        final int smallOffset = delta / 8;
        final int tinyOffset = delta / 15; //Tiny offset is used to fine tune the placement of the ID for visual appeal
        g.drawString(id, originX + col * delta - maxwidth - smallOffset,
                originY + row * delta + fm.getAscent() + tinyOffset);
    }

    /**
     * Add a horizontal ID for the cell at position (row, col).
     * Draws it such that the client controls the word with this ID
     * 
     * @param id  ID to add to the position.
     * @param row Row position of the cell.
     * @param col Column position of the cell.
     * @param g   Graphics environment to use.
     */
    private void horizontalIdFancy(String id, int row, int col, Graphics g) {
        g.setFont(indexFont);
        Color oldColor = g.getColor();
        final Color darkBlue = new Color(0, 0, 153); //Draw a dark blue box around the word number
        g.setColor(darkBlue);
        FontMetrics fm = g.getFontMetrics();
        int maxwidth = fm.charWidth('0') * id.length();
        final int smallOffset = delta / 8;
        final int tinyOffset = delta / 15; //Tiny offset is used to fine tune the placement of the ID for visual appeal
        g.drawString(id, originX + col * delta - maxwidth - smallOffset,
                originY + row * delta + fm.getAscent() + tinyOffset);
        
        Graphics2D g2 = (Graphics2D) g;
        Stroke oldStroke = g2.getStroke();
        final int strokeThickness = 2;
        g2.setStroke(new BasicStroke(strokeThickness)); //Increase the thickness of the drawing stroke
        
        final int halfBox = delta / 2;
        g2.drawRect(originX + col * delta - maxwidth * 2, originY + row * delta, halfBox - smallOffset, halfBox);
        g2.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    // The three methods that follow are meant to show you one approach to writing
    // in your canvas. They are meant to give you a good idea of how text output and
    // formatting work, but you are encouraged to develop your own approach to using
    // style and placement to convey information about the state of the game.

    private int line = 0;

    // The Graphics interface allows you to place text anywhere in the component,
    // but it is useful to have a line-based abstraction to be able to just print
    // consecutive lines of text.
    // We use a line counter to compute the position where the next line of code is
    // written, but the line needs to be reset every time you paint, otherwise the
    // text will keep moving down.
    private void resetLine() {
        line = 0;
    }

    /**
     * Prints a line of text on the canvas for the user to see
     * @param s the String being the line of text to print to the canvas
     * @param g Graphics environment to use.
     */
    private void println(String s, Graphics g) {
        g.setFont(textFont);
        FontMetrics fm = g.getFontMetrics();
        // Before changing the color it is a good idea to record what the old color
        // was.
        Color oldColor = g.getColor();
        final Color darkRed = new Color(100, 0, 0);
        g.setColor(darkRed);
        final int xOffset = 800;  // for bigger puzzles
        final double lineHeight = 6 / 5;
        g.drawString(s, originX + xOffset, (int)(originY + line * fm.getAscent() * lineHeight));
        // After writing the text you can return to the previous color.
        g.setColor(oldColor);
        ++line;
    }

    /**
     * Paints this.board to the crossword canvas for the user to see
     * @param g Graphics environment to use.
     * 
     * 
     * <p>
     * The board description must match the following grammar:
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
     * 
     */
    @Override
    public synchronized void paint(Graphics g) {

        //Collect the dimensions of the board first
        String[] dimsAndRestOfBoard = this.board.split("\nSquares:\n");
        String dimensions = dimsAndRestOfBoard[0];
        String rest = dimsAndRestOfBoard[1];

        //Now collect the Letter Squares of the board
        String[] squaresAndRestOfBoard = rest.split("\nScores:\n");
        String squares = squaresAndRestOfBoard[0];
        rest = squaresAndRestOfBoard[1];

        //Now collect the Scores and Questions of the Board
        String[] scoresAndQuestions = rest.split("\nQuestions:\n");
        String scores = scoresAndQuestions[0];
        String questions = scoresAndQuestions[1];

        String[] dims = dimensions.split("x");
        int rows = Integer.parseInt(dims[0]);
        int cols = Integer.parseInt(dims[1]);

        String[] squareArray = squares.split("\n");

        //Iterate through the dimensions of the board and drawn each cell as needed
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                String square = squareArray[(cols * j) + i];

                if (square.equalsIgnoreCase("EMPTY"))
                    continue;

                /*
                 * Split up the information of the square by its individual components
                 * ie components like letter, id_number, across or down, confirmed, etc.
                 */
                String[] squareTokens = square.split(" "); 
                
                boolean confirmed = false;
                if (squareTokens[0].contains("+"))
                    confirmed = true;

                if (squareTokens[0].equals("_")) { //Drawn an empty square if there is no letter in the cell
                    drawCell(j, i, g);
                }

                else if (confirmed) {
                    drawCellConfirmed(j, i, g);
                    letterInCellConfirmed(squareTokens[0].substring(1), j, i, g);
                } else {
                    drawCell(j, i, g); //Draw the cell and letter normally if it is not confirmed
                    letterInCell(squareTokens[0], j, i, g);
                }

                final int noAcrossNorDown = 1; //This normal letter on the board is not the start of an across or down word
                final int acrossOrDown = 3;  //This letter on the board is the start of an across or down word
                final int acrossAndDown = 5; //This letter on the board is the start of both and across word and a down word
                
                if (squareTokens.length == noAcrossNorDown) {
                    continue; //If it is not the start of an across or down word, we are done drawing this cell
                }
                
                else if (squareTokens.length == acrossOrDown) {  
                    handleAcrossOrDown(squareTokens, i, j, g); //Draw the cell with the across or down info as needed
                }
                
                else if (squareTokens.length == acrossAndDown) {
                    handleAcrossAndDown(squareTokens, i, j, g); //Draw the cell with the across and down info as needed
                }
            }
        }

        
        
        resetLine(); //Reset the lines to print the player scores and word clues to the canvas

        println("Scores:", g); //Print all of the player scores to the canvas
        String[] scoreArray = scores.split("\n"); 
        for (int i = 0; i < scoreArray.length; i++) {
            println(scoreArray[i], g);
        }

        println("", g); //Add one line of separation

        println("Clues:", g); //Print all of the word hints to the canvas
        String[] questionArray = questions.split("\n");
        for (int i = 0; i < questionArray.length; i++) {
            println(questionArray[i], g);
        }

    }
    
    /**
     * Handles drawing a cell on the board for the cell that is the start of an
     * across or a down word on the game board 
     * @param squareTokens the collection of each item of information about this
     *        square/cell to draw. ie. the letter, across or down, word number
     * @param i integer being the column on the canvas to draw the cell
     * @param j integer being the row on the canvas to draw the cell
     * @param g Graphics environment to use.
     */
    private void handleAcrossOrDown(String[] squareTokens, int i, int j, Graphics g) {
        final int letterIndex = 1;
        final int directionIndex = 2;

        boolean mine = false;
        if (squareTokens[letterIndex].contains(">")) //Check to see if this client has control of this word
            mine = true;

        //Add the fancy ID to the cell if this client controls the word, otherwise draw the normal ID
        if (squareTokens[directionIndex].equalsIgnoreCase("ACROSS")) {
            if (mine) {
                horizontalIdFancy(squareTokens[letterIndex].substring(1), j, i, g);
            } else {
                horizontalId(squareTokens[letterIndex], j, i, g);
            }
        }

        else if (squareTokens[directionIndex].equalsIgnoreCase("DOWN")) {
            if (mine) {
                verticalIdFancy(squareTokens[letterIndex].substring(1), j, i, g);
            } else {
                verticalId(squareTokens[letterIndex], j, i, g);
            }
        }
    }
    
    
    /**
     * Handles drawing a cell on the board for the cell that is the start of an
     * across and the the start of a down word on the game board
     * @param squareTokens the collection of each item of information about this
     *        square/cell to draw. ie. the letter, across or down, word number
     * @param i integer being the column on the canvas to draw the cell
     * @param j integer being the row on the canvas to draw the cell
     * @param g Graphics environment to use.
     */
    private void handleAcrossAndDown(String[] squareTokens, int i, int j, Graphics g) {
        
        final int directionIndex = 2;
        
        if (squareTokens[directionIndex].equalsIgnoreCase("ACROSS")) { //If the board response has the across word before the down word
            
            final int acrossLetterIndex = 1;
            
            boolean mineAcross = false; //Check to see if this client has control of this across word
            if (squareTokens[acrossLetterIndex].contains(">"))
                mineAcross = true;

            //Draw the ID fancy if this player controls the across word, or else draw the normal ID
            if (mineAcross) {
                horizontalIdFancy(squareTokens[acrossLetterIndex].substring(1), j, i, g);
            } else {
                horizontalId(squareTokens[acrossLetterIndex], j, i, g);
            }

            final int downLetterIndex = 3;
            
            boolean mineDown = false; //Check to see if this client has control of this down word
            if (squareTokens[downLetterIndex].contains(">"))
                mineDown = true;
            
            //Draw the ID fancy if this player controls the down word, or else draw the normal ID
            if (mineDown) {
                verticalIdFancy(squareTokens[downLetterIndex].substring(1), j, i, g);
            } else {
                verticalId(squareTokens[downLetterIndex], j, i, g);
            }

        }

        else { //If the board response has the down word before the across word
            
            final int acrossLetterIndex = 3;
            
            boolean mineAcross = false; //Check to see if this client has control of this across word
            if (squareTokens[acrossLetterIndex].contains(">"))
                mineAcross = true;

            //Draw the ID fancy if this player controls the across word, or else draw the normal ID
            if (mineAcross) {
                horizontalIdFancy(squareTokens[acrossLetterIndex].substring(1), j, i, g);
            } else {
                horizontalId(squareTokens[acrossLetterIndex], j, i, g);
            }

            final int downLetterIndex = 1;
            
            boolean mineDown = false; //Check to see if this client has control of this down word
            if (squareTokens[downLetterIndex].contains(">"))
                mineDown = true;
            
            //Draw the ID fancy if this player controls the down word, or else draw the normal ID
            if (mineDown) {
                verticalIdFancy(squareTokens[downLetterIndex].substring(1), j, i, g);
            } else {
                verticalId(squareTokens[downLetterIndex], j, i, g);
            }
        }
    }

    /**
     * Assigns the given board in the UI (including all its squares, the current
     * user scores, and all the hints). The string describing the board must follow
     * this grammar:
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
     * Updates the UI on returning.
     * 
     * @param newBoard a board following the grammar above
     */
    public synchronized void drawBoard(String newBoard) {
        this.board = newBoard;
    }

}
