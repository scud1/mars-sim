/**
 * Mars Simulation Project
 * DataLogger.java
 * @version 3.2.0 2021-06-20
 * @author Barry Evans
 */

package org.mars_sim.msp.core.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.mars_sim.msp.core.time.MarsClock;


/**
 * Logs data items according to the current Sol. Each sol s a single data item.
 * Only a maximum number of sols is retained.
 * The timestamp of the logger is shifted autumatically via the Simulation.
 * @param <T> Data item being recorded
 */
public abstract class DataLogger<T> implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static int currentSol = 1;
	protected static int currentMsol = 0;
	
	private int maxSols = 7;
	private int latestSol = 0;
	protected T currentData = null;
	protected List<T> dailyData = new LinkedList<T>();
	
	public DataLogger(int maxSols) {
		super();
		this.maxSols = maxSols;
	}
	
	/**
	 * Move time onwards.
	 * @param pulse
	 */
	public static void changeTime(MarsClock time) {
		currentSol = time.getMissionSol();
		currentMsol = time.getMillisolInt();
	}
	
	/**
	 * A new sol should be started in the logger.
	 * @param newSol
	 */
	private void newSol(int newSol) {
		latestSol = newSol;
		currentData = getNewDataItem(); 
		dailyData.add(0, currentData);
		if (dailyData.size() > maxSols) {
			dailyData.remove(maxSols-1);
		}
	}
	
	/**
	 * Create a new data item for a new sol;
	 * @return
	 */
	protected abstract T getNewDataItem();

	/**
	 * The logger is updating
	 */
	protected void updating() {
		if (latestSol != currentSol) {
			newSol(currentSol);
		}
	}

	/**
	 * Returns a Map if data entries per missionSol. Map is keyed on sol.
	 * 
	 * @return Sol to daily data entries.
	 */
	public Map<Integer, T> getHistory() {
		Map<Integer, T> results = new HashMap<>();
		int sol = latestSol;
		for (T t : dailyData) {
			results.put(sol--, t);
		}
		return results;
	}

	/**
	 * Gets the current sol the Data Logger is recording.
	 * 
	 * @return
	 */
	public int getCurrentSol() {
		return currentSol;
	}

	/**
	 * Gets the data held for a single sol.
	 * 
	 * @param sol Sol
	 * @return List of data items
	 */
	public T getSolData(int sol) {
		if (sol < 1) {
			throw new IllegalArgumentException("Mission Sol cannot be less than 1");
		}
		int idx = latestSol - sol;
		if ((idx < 0) || (idx >= dailyData.size())) {
			return null;
		}
		else {
			return dailyData.get(idx);
		}
	}
	
	/**
	 * Gets the latest Sol data being captured.
	 * 
	 * @return
	 */
	public T getTodayData() {
		return currentData;
	}
	
	
	/**
	 * Checks if yestersol's data valid.
	 * 
	 * @return
	 */
	public boolean isYestersolDataValid() {
		return currentSol - 1 > 0;
	}
	
	/**
	 * Gets yestersol's data if it exists.
	 * 
	 * @return
	 */
	public T getYestersolData() {
		return getSolData(currentSol - 1);
	}
}
