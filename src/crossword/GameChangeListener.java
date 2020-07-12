/*
 * Copyright (c) 2017-2018 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course
 * staff.
 */
package crossword;


/**
 * GameChangeListener to listen on Game change event.
 * 
 * <p>
 */
public interface GameChangeListener {

    /**
     * This method will be called when Game changed.
     */
    public void boardChanged();
}
