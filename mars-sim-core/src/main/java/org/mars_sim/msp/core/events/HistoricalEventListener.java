/**
 * Mars Simulation Project
 * HistoricalEventListener.java
 * @version 3.2.0 2021-06-20
 * @author Barry Evans
 */

package org.mars_sim.msp.core.events;

/**
 * This interface is implemented by any object that is to receive notification
 * of the registration or removal of an HistoricalEvent.
 */
public interface HistoricalEventListener {

	/**
	 * A new event has been added at the specified manager.
	 *
	 * @param event The new {@link HistoricalEvent} added.
	 */
	public void eventAdded(HistoricalEvent he);
	
	/**
	 * A consecutive sequence of events have been removed from the manager.
	 *
	 * @param startIndex First exclusive index of the event to be removed.
	 * @param endIndex Last exclusive index of the event to be removed..
	 */
	public void eventsRemoved(int startIndex, int endIndex);
}
