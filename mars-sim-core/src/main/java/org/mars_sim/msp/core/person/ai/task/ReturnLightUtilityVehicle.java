/**
 * Mars Simulation Project
 * ReturnLightUtilityVehicle.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.RoverMission;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.resource.ItemResourceUtil;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * A task for returning a light utility vehicle (LUV) to a rover or settlement
 * when a person finds themselves operating one.
 */
public class ReturnLightUtilityVehicle extends Task implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(ReturnLightUtilityVehicle.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.returnLightUtilityVehicle"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase RETURN_LUV = new TaskPhase(Msg.getString("Task.phase.returnLUV")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .5D;

	// Data members.
	LightUtilityVehicle luv = null;
	Unit returnContainer = null;

	/**
	 * Constructor.
	 * 
	 * @param person the person starting the task.
	 */
	public ReturnLightUtilityVehicle(Person person) {
		super(NAME, person, false, false, STRESS_MODIFIER, null, 0D);

		Vehicle personVehicle = person.getVehicle();
		if ((personVehicle != null) && (personVehicle instanceof LightUtilityVehicle)) {
			luv = (LightUtilityVehicle) personVehicle;
		} else {
			endTask();
			logger.severe(person.getName() + " is not in a light utility vehicle.");
		}

		// Return container may be settlement or rover.
		returnContainer = null;

		// Attempt to determine return container based on mission.
		Mission mission = person.getMind().getMission();
		if (mission != null) {
			if (mission instanceof RoverMission) {
				RoverMission roverMission = (RoverMission) mission;
				returnContainer = roverMission.getRover();
			} else {
				returnContainer = mission.getAssociatedSettlement();
			}
		}

		// If returnContainer hasn't been found, look for local settlement.
		if (returnContainer == null) {
			Iterator<Settlement> i = unitManager.getSettlements().iterator();
			while (i.hasNext()) {
				Settlement settlement = i.next();
				if (person.getCoordinates().equals(settlement.getCoordinates())) {
					returnContainer = settlement;
					break;
				}
			}
		}

		// If returnContainer hasn't been found, look for local rover.
		if (returnContainer == null) {
			Iterator<Vehicle> i = unitManager.getVehicles().iterator();
			while (i.hasNext()) {
				Vehicle vehicle = i.next();
				if (vehicle instanceof Rover) {
					returnContainer = vehicle;
					break;
				}
			}
		}

		// Initialize task phase
		addPhase(RETURN_LUV);
		setPhase(RETURN_LUV);

		// If returnContainer still hasn't been found, end task.
		if (returnContainer == null) {
			endTask();
			logger.severe(person.getName() + " cannot find a settlement or rover to return light utility vehicle.");
		} else {
			if (luv != null) {
				setDescription(Msg.getString("Task.description.returnLightUtilityVehicle.detail", luv.getName(),
					returnContainer.getName())); // $NON-NLS-1$
				logger.fine(person.getName() + " is starting to return light utility vehicle: " + luv.getName() + " to "
					+ returnContainer.getName());
			}
		}
	}

//	public ReturnLightUtilityVehicle(Robot robot) {
//		super(NAME, robot, false, false, STRESS_MODIFIER, false, 0D);
//
//		Vehicle robotVehicle = robot.getVehicle();
//		if ((robotVehicle != null) && (robotVehicle instanceof LightUtilityVehicle)) {
//			luv = (LightUtilityVehicle) robotVehicle;
//		} else {
//			endTask();
//			logger.severe(robot.getName() + " is not in a light utility vehicle.");
//		}
//
//		// Return container may be settlement or rover.
//		returnContainer = null;
//
//		// Attempt to determine return container based on mission.
//		Mission mission = robot.getBotMind().getMission();
//		if (mission != null) {
//			if (mission instanceof RoverMission) {
//				RoverMission roverMission = (RoverMission) mission;
//				returnContainer = roverMission.getRover();
//			} else {
//				returnContainer = mission.getAssociatedSettlement();
//			}
//		}
//
//		// If returnContainer hasn't been found, look for local settlement.
//		if (returnContainer == null) {
//			Iterator<Settlement> i = unitManager.getSettlements().iterator();
//			while (i.hasNext()) {
//				Settlement settlement = i.next();
//				if (robot.getCoordinates().equals(settlement.getCoordinates())) {
//					returnContainer = settlement;
//					break;
//				}
//			}
//		}
//
//		// If returnContainer hasn't been found, look for local rover.
//		if (returnContainer == null) {
//			Iterator<Vehicle> i = unitManager.getVehicles().iterator();
//			while (i.hasNext()) {
//				Vehicle vehicle = i.next();
//				if (vehicle instanceof Rover) {
//					returnContainer = vehicle;
//					break;
//				}
//			}
//		}
//
//		// Initialize task phase
//		addPhase(RETURN_LUV);
//		setPhase(RETURN_LUV);
//
//		// If returnContainer still hasn't been found, end task.
//		if (returnContainer == null) {
//			endTask();
//			logger.severe(robot.getName() + " cannot find a settlement or rover to return light utility vehicle.");
//		} else {
//			setDescription(Msg.getString("Task.description.returnLightUtilityVehicle.detail", luv.getName(),
//					returnContainer.getName())); // $NON-NLS-1$
//			logger.fine(robot.getName() + " is starting to return light utility vehicle: " + luv.getName() + " to "
//					+ returnContainer.getName());
//		}
//	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (RETURN_LUV.equals(getPhase())) {
			return returnLUVPhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Perform the return LUV phase.
	 * 
	 * @param time the time to perform the task phase.
	 * @return remaining time after performing the phase.
	 */
	private double returnLUVPhase(double time) {

		Mission mission = null;

		if (person != null)
			mission = person.getMind().getMission();
		else if (robot != null)
			// If not in a mission, return vehicle and unload attachment parts.
			mission = robot.getBotMind().getMission();
		
		if (mission == null) {
			// Put light utility vehicle in return container.
			if (returnContainer.getInventory().canStoreUnit(luv, false)) {
				
				if (person != null)
					// Remove person from light utility vehicle.
					luv.getInventory().retrieveUnit(person);
				else if (robot != null)
					// Remove robot from light utility vehicle.
					luv.getInventory().retrieveUnit(robot);

				luv.setOperator(null);
				
				returnContainer.getInventory().storeUnit(luv);		
				
				if (returnContainer instanceof Settlement) {
					luv.findNewParkingLoc();
				}
			} else {
				logger.severe("Light utility vehicle: " + luv.getName() + " could not be stored in "
						+ returnContainer.getName());
			}

			// Unload any attachment parts or inventory from light utility vehicle.
			unloadLUVInventory();
		}

		endTask();

		return time;
	}

	/**
	 * Unload all attachment parts and inventory from light utility vehicle.
	 */
	private void unloadLUVInventory() {

		Inventory luvInv = luv.getInventory();
		Inventory rcInv = returnContainer.getInventory();

		// Unload all units.
		Iterator<Unit> j = luvInv.getContainedUnits().iterator();
		while (j.hasNext()) {
			Unit unit = j.next();
			if (rcInv.canStoreUnit(unit, false)) {
				unit.transfer(luvInv, rcInv);
//				luvInv.retrieveUnit(unit);
//				rcInv.storeUnit(unit);
			} else {
				logger.severe(unit.getName() + " cannot be stored in " + returnContainer.getName());
			}
		}

		// Unload all parts.
		Iterator<Integer> i = luvInv.getAllItemResourcesStored().iterator();
		while (i.hasNext()) {
			Integer item = i.next();
			int num = luvInv.getItemResourceNum(item);
			Part part= (Part)(ItemResourceUtil.findItemResource(item));
			double mass = part.getMassPerItem() * num;
			if (rcInv.getRemainingGeneralCapacity(false) >= mass) {
				luvInv.retrieveItemResources(item, num);
				rcInv.storeItemResources(item, num);
			} else {
				logger.severe(part.getName() + " numbered " + num + " cannot be stored in " + returnContainer.getName()
						+ " due to insufficient remaining general capacity.");
			}
		}

		// Unload all amount resources.
		Iterator<Integer> k = luvInv.getAllARStored(false).iterator();
		while (k.hasNext()) {
			Integer resource = k.next();
			double amount = luvInv.getAmountResourceStored(resource, false);
			if (rcInv.hasAmountResourceCapacity(resource, amount, false)) {
				luvInv.retrieveAmountResource(resource, amount);
				rcInv.storeAmountResource(resource, amount, true);
			} else {
				logger.severe(ResourceUtil.findAmountResourceName(resource) + " of amount " + amount + " kg. cannot be stored in "
						+ returnContainer.getName());
			}
		}
	}
}
