package org.drools.planner.examples.ras2012.interfaces;

/**
 * Class implementing this interface signifies that the represented entity has a sense of direction, namely eastbound or
 * westbound. The entity can never be simultaneously eastbound and westbound.
 */
public interface Directed {

    /**
     * Whether or not the entity is eastbound.
     * 
     * @return True if eastbound, false otherwise.
     */
    public boolean isEastbound();

    /**
     * Whether or not the entity is westbound.
     * 
     * @return True if westbound, false otherwise.
     */
    public boolean isWestbound();

}
