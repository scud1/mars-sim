/*
 * Mars Simulation Project
 * ExploredLocation.java
 * @date 2021-12-09
 * @author Scott Davis
 */

package org.mars_sim.msp.core.environment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * A class representing an explored location. It contains information on
 * estimated mineral concentrations and if it has been mined or not. Perhaps
 * later we can add more information related to exploration, such as evidence
 * for life.
 */
public class ExploredLocation implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(ExploredLocation.class.getName());
	
	private static final int AVERAGE_RESERVE_MASS = 10_000;
	
	// Private members.
	private boolean mined;
	private boolean explored;
	private boolean reserved;
	private int numEstimationImprovement;
	private double totalMass;
	private double remainingMass;

	private Settlement settlement;
	private Coordinates location;

	private Map<String, Double> estimatedMineralConcentrations;
	/**
	 * A map for the degree of certainty in estimating mineral concentration
	 */
	private Map<String, Double> degreeCertainty = new HashMap<>();

	/**
	 * Constructor.
	 *
	 * @param location                       the location coordinates.
	 * @param estimationImprovement			 The number times the estimates have been improved
	 * @param estimatedMineralConcentrations a map of all mineral types and their
	 *                                       estimated concentrations (0% -100%)
	 * @param the                            settlement the exploring mission is
	 *                                       from.
	 */
	ExploredLocation(Coordinates location, int estimationImprovement, Map<String, Double> estimatedMineralConcentrations, Settlement settlement) {
		this.location = location;
		this.estimatedMineralConcentrations = estimatedMineralConcentrations;
		this.settlement = settlement;
		mined = false;
		explored = false;
		reserved = false;
		this.numEstimationImprovement = estimationImprovement;
		
		// Future: Need to find better algorithm to estimate the reserve amount of each mineral 
		double reserve = 0;
		for (String s: estimatedMineralConcentrations.keySet()) {
			double concentration = estimatedMineralConcentrations.get(s);
			reserve += AVERAGE_RESERVE_MASS * concentration * RandomUtil.getRandomDouble(.5, 5);
		}

		totalMass = reserve; //RandomUtil.computeGaussianWithLimit(reserve, .5, reserve * .1);
		remainingMass = totalMass;
		
		logger.info(settlement + " - " + location.getFormattedString() 
			+ "  Estimated reserve: " + (int)totalMass + " kg. % Concentration: "
			+  estimatedMineralConcentrations);
	}

	public boolean isEmpty() {
		if (remainingMass == 0.0)
			return true;
		
		return false;
	}
	
	public double excavateMass(double amount) {
		if (remainingMass < amount) {
			remainingMass = 0;
			return amount - remainingMass;
		}
		remainingMass -= amount;
		return 0;
	}
	
	public double getRemainingMass() {
		return remainingMass;
	}
	
	/**
	 * Gets the location coordinates.
	 *
	 * @return coordinates.
	 */
	public Coordinates getLocation() {
		return location;
	}

	/**
	 * Gets a map of estimated mineral concentrations at the location.
	 *
	 * @return a map of all mineral types and their estimated concentrations (0%
	 *         -100%)
	 */
	public Map<String, Double> getEstimatedMineralConcentrations() {
		return estimatedMineralConcentrations;
	}

	/**
	 * Gets the number of times the mineral concentration estimation has been
	 * improved.
	 *
	 * @return number of times.
	 */
	public int getNumEstimationImprovement() {
		return numEstimationImprovement;
	}

	/**
	 * Improves the certainty of mineral concentration estimation.
	 */
	public void improveCertainty() {
		
		List<String> minerals = new ArrayList<>(degreeCertainty.keySet());
		
		int rand = RandomUtil.getRandomInt(minerals.size() - 1);
		
		String aMineral = minerals.get(rand);
		
		
		
	}
	
	/**
	 * Adds an mineral concentration estimation improvement.
	 */
	public void addEstimationImprovement() {
		numEstimationImprovement++;
		improveCertainty();
	}

	/**
	 * Sets if the location has been mined or not.
	 *
	 * @param mined true if mined.
	 */
	public void setMined(boolean mined) {
		this.mined = mined;
	}

	/**
	 * Checks if the location has been mined or not.
	 *
	 * @return true if mined.
	 */
	public boolean isMined() {
		return mined;
	}

	/**
	 * Sets if the location has been explored or not.
	 *
	 * @param explored true if explored.
	 */
	public void setExplored(boolean explored) {
		this.explored = explored;
	}

	/**
	 * Checks if the location has been explored of not.
	 *
	 * @return true if explored.
	 */
	public boolean isExplored() {
		return explored;
	}

	/**
	 * Sets if the location has been reserved for mining.
	 *
	 * @param reserved true if reserved.
	 */
	public void setReserved(boolean reserved) {
		this.reserved = reserved;
	}

	/**
	 * Checks if the location has been reserved for mining.
	 *
	 * @return true if reserved.
	 */
	public boolean isReserved() {
		return reserved;
	}

	/**
	 * The settlement that explored this site. This may be null if it is 
	 * an unclaimed location.
	 *
	 * @return settlement
	 */
	public Settlement getSettlement() {
		return settlement;
	}


	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		location = null;
		estimatedMineralConcentrations.clear();
		estimatedMineralConcentrations = null;
		settlement = null;
	}
}
