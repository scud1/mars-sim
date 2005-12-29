/**
 * Mars Simulation Project
 * CollectIce.java
 * @version 2.78 05-08-18
 * @author Scott Davis
 */
package org.mars_sim.msp.simulation.person.ai.mission;

import org.mars_sim.msp.simulation.person.*;
import org.mars_sim.msp.simulation.person.ai.job.Job;
import org.mars_sim.msp.simulation.resource.AmountResource;
import org.mars_sim.msp.simulation.structure.Settlement;
import org.mars_sim.msp.simulation.vehicle.Rover;
import org.mars_sim.msp.simulation.vehicle.Vehicle;
import org.mars_sim.msp.simulation.vehicle.VehicleIterator;

/** 
 * The Exploration class is a mission to travel in a rover to several
 * random locations around a settlement and collect ice.
 */
public class CollectIce extends CollectResourcesMission {

	//	Amount of ice to be gathered at a given site (kg). 
	private static final double SITE_GOAL = 2000D;
	
	// Collection rate of ice during EVA (kg/millisol).
	private static final double COLLECTION_RATE = 1D;
	
	// Number of collection sites.
	private static final int NUM_SITES = 1;
	
	// Minimum number of people to do mission.
	private final static int MIN_PEOPLE = 2;

	/**
	 * Constructor
	 * @param startingPerson the person starting the mission.
	 * @throws MissionException if problem constructing mission.
	 */
	public CollectIce(Person startingPerson) throws MissionException {
		
		// Use CollectResourcesMission constructor.
		super("Ice Prospecting", startingPerson, AmountResource.ICE, SITE_GOAL, COLLECTION_RATE, 
				NUM_SITES, MIN_PEOPLE);
	}
	
	/** 
	 * Gets the weighted probability that a given person would start this mission.
	 * @param person the given person
	 * @return the weighted probability
	 */
	public static double getNewMissionProbability(Person person) {

		double result = 0D;

		if (person.getLocationSituation().equals(Person.INSETTLEMENT)) {
			Settlement settlement = person.getSettlement();
	    
			boolean reservableRover = areVehiclesAvailable(settlement);

			double water = settlement.getInventory().getAmountResourceStored(AmountResource.WATER);
			boolean enoughWater = (water >= 5000D);

			// At least one person left to hold down the fort.
			boolean remainingInhabitant = atLeastOnePersonRemainingAtSettlement(settlement);
	    
			if (reservableRover && remainingInhabitant) {
				if (enoughWater) result = 1D;
				else result = 100D;
			} 
			
			// Crowding modifier
			int crowding = settlement.getCurrentPopulationNum() - settlement.getPopulationCapacity();
			if (crowding > 0) result *= (crowding + 1);	
			
			// Job modifier.
			Job job = person.getMind().getJob();
			if (job != null) result *= job.getStartMissionProbabilityModifier(CollectIce.class);			
		}
        
		return result;
	}
	
	/**
	 * Checks to see if any vehicles are available at a settlement.
	 * @param settlement the settlement to check.
	 * @return true if vehicles are available.
	 */
	private static boolean areVehiclesAvailable(Settlement settlement) {
		
		boolean result = false;
		
		VehicleIterator i = settlement.getParkedVehicles().iterator();
		while (i.hasNext()) {
			Vehicle vehicle = i.next();
			
			boolean usable = true;
			if (vehicle.isReserved()) usable = false;
			if (!vehicle.getStatus().equals(Vehicle.PARKED)) usable = false;
			if (!(vehicle instanceof Rover)) usable = false;
			if (vehicle.getInventory().getAmountResourceCapacity(AmountResource.ICE) <= 0D) usable = false;
			
			if (usable) result = true;    
		}
		
		return result;
	}
	
	/**
	 * Checks if vehicle is usable for this mission.
	 * (This method should be overridden by children)
	 * @param newVehicle the vehicle to check
	 * @return true if vehicle is usable.
	 */
	protected boolean isUsableVehicle(Vehicle newVehicle) {
		boolean usable = super.isUsableVehicle(newVehicle);
		
		// Make sure rover can carry ice.
		if (newVehicle.getInventory().getAmountResourceCapacity(AmountResource.ICE) <= 0D) usable = false;
		
		return usable;
	}
	
	/**
	 * Compares the quality of two vehicles for use in this mission.
	 * (This method should be added to by children)
	 * @param firstVehicle the first vehicle to compare
	 * @param secondVehicle the second vehicle to compare
	 * @return -1 if the second vehicle is better than the first vehicle, 
	 * 0 if vehicle are equal in quality,
	 * and 1 if the first vehicle is better than the second vehicle.
	 * @throws IllegalArgumentException if firstVehicle or secondVehicle is null.
	 */
	protected int compareVehicles(Vehicle firstVehicle, Vehicle secondVehicle) {
		int result = super.compareVehicles(firstVehicle, secondVehicle);
		
		// Check if one can hold more ice than the other.
		if ((result == 0) && (isUsableVehicle(firstVehicle)) && (isUsableVehicle(secondVehicle))) {
			double firstIceCapacity = firstVehicle.getInventory().getAmountResourceCapacity(AmountResource.ICE);
			double secondIceCapacity = secondVehicle.getInventory().getAmountResourceCapacity(AmountResource.ICE);
			if (firstIceCapacity > secondIceCapacity) result = 1;
			else if (firstIceCapacity < secondIceCapacity) result = -1;
		}
		
		return result;
	}
}