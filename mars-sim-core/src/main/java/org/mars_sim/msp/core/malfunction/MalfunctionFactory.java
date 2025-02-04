/*
 * Mars Simulation Project
 * MalfunctionFactory.java
 * @date 2021-10-20
 * @author Scott Davis
 */

package org.mars_sim.msp.core.malfunction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.equipment.EVASuit;
import org.mars_sim.msp.core.equipment.Equipment;
import org.mars_sim.msp.core.equipment.EquipmentOwner;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.ai.task.util.Worker;
import org.mars_sim.msp.core.resource.MaintenanceScope;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.resource.PartConfig;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * This class is a factory for Malfunction objects.
 */
public final class MalfunctionFactory implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(MalfunctionFactory.class.getName());
	
	public static final String METEORITE_IMPACT_DAMAGE = "Meteorite Impact Damage";

	// Data members
	private int newIncidentNum = 0;

	public static SimulationConfig simulationConfig = SimulationConfig.instance();
	public static MalfunctionConfig mc = simulationConfig.getMalfunctionConfiguration();
	public static PartConfig partConfig = simulationConfig.getPartConfiguration();

	/**
	 * Constructs a MalfunctionFactory object.
	 *
	 * @param malfunctionConfig malfunction configuration DOM document.
	 * @throws Exception when malfunction list could not be found.
	 */
	public MalfunctionFactory() {
	}

	/**
	 * Picks a malfunction from a given unit scope.
	 *
	 * @param scopes a collection of scope strings defining the unit.
	 * @return a randomly-picked malfunction or null if there are none available.
	 */
	public MalfunctionMeta pickAMalfunction(Collection<String> scopes) {
		MalfunctionMeta choosenMalfunction = null;

		List<MalfunctionMeta> malfunctions = new ArrayList<>(mc.getMalfunctionList());
		double totalProbability = 0D;
		// Total probability is fixed
		for (MalfunctionMeta m : malfunctions) {
			if (m.isMatched(scopes)) {
				totalProbability += m.getProbability();
			}
		}

		double r = RandomUtil.getRandomDouble(totalProbability);
		// Shuffle the malfunction list
		Collections.shuffle(malfunctions);
		for (MalfunctionMeta m : malfunctions) {
			double probability = m.getProbability();
			
			if (m.isMatched(scopes) && (choosenMalfunction == null)) {
				if (r < probability) {
					// will only pick one malfunction at a time 
					choosenMalfunction = m;
					break;
				} else
					r -= probability;
			}
		}

		// Safety check if probability failed to pick malfunction
		if (choosenMalfunction == null) {
			logger.warning("Failed to pick a malfunction by probability " + totalProbability + ".");
			choosenMalfunction = malfunctions.get(0);
		}

		double failureRate = choosenMalfunction.getProbability();
		// Note : the composite probability of a malfunction is dynamically updated as
		// the field reliability data trickles in
		if (!RandomUtil.lessThanRandPercent(failureRate)) {
			choosenMalfunction = null;
		}

		return choosenMalfunction;
	}

	/**
	 * Gets a collection of malfunctionable entities local to the given person.
	 *
	 * @return collection collection of malfunctionables.
	 */
	private static Collection<Malfunctionable> getLocalMalfunctionables(Worker source) {

		Collection<Malfunctionable> entities = new ArrayList<>();

		if (source.isInSettlement()) {
			entities = getBuildingMalfunctionables(source.getSettlement());
		}

		if (source.isInVehicle()) {
			entities.addAll(getMalfunctionables((Malfunctionable) source.getVehicle()));
		}

		Collection<? extends Unit> inventoryUnits = null;

		if (source instanceof EquipmentOwner) {
			inventoryUnits = ((EquipmentOwner)source).getEquipmentSet();

			if (inventoryUnits != null && !inventoryUnits.isEmpty()) {
				for (Unit unit : inventoryUnits) {
					if ((unit instanceof Malfunctionable) && !entities.contains((Malfunctionable) unit)) {
						entities.add((Malfunctionable) unit);
					}
				}
			}
		}

		return entities;
	}

	/**
	 * Gets a collection of malfunctionable entities local to a given settlement.
	 *
	 * @param settlement the settlement.
	 * @return collection of malfunctionables.
	 */
	private static Collection<Malfunctionable> getBuildingMalfunctionables(Settlement settlement) {
		// Should get a collection of buildings only
		return new ArrayList<>(settlement.getBuildingManager().getBuildingSet());
	}

	/**
	 * Gets a collection of malfunctionable entities local to the given
	 * malfunctionable entity.
	 *
	 * @return collection of malfunctionables.
	 */
	public static Collection<Malfunctionable> getMalfunctionables(Malfunctionable entity) {

		Collection<Malfunctionable> entities = new ArrayList<>();

		entities.add(entity);

		if (entity instanceof EquipmentOwner) {
			for (Equipment e : ((EquipmentOwner)entity).getEquipmentSet()) {
				if (e instanceof Malfunctionable) {
					entities.add((Malfunctionable) e);
				}
			}
		}
		// must filter out drones
		if (entity instanceof Rover || entity instanceof LightUtilityVehicle) {
			Collection<Robot> inventoryUnits1 = ((Crewable)entity).getRobotCrew();
			for (Unit unit : inventoryUnits1) {
				if (unit instanceof Malfunctionable) {
					entities.add((Malfunctionable) unit);
				}
			}
		}

		else if (entity instanceof Settlement) {
			entities.addAll(getBuildingMalfunctionables((Settlement)entity));
		}

		return entities;
	}

	/**
	 * Gets all malfunctionables associated with a settlement.
	 *
	 * @param settlement the settlement.
	 * @return collection of malfunctionables.
	 */
	public static Collection<Malfunctionable> getAssociatedMalfunctionables(Settlement settlement) {

		// Add buildings in settlement
		Collection<Malfunctionable> entities = getBuildingMalfunctionables(settlement);

		// Get all vehicles belong to the Settlement. Vehicles can have a malfunction
		// in the Settlement or outside settlement
		for (Vehicle vehicle : settlement.getParkedVehicles()) {
			entities.addAll(getMalfunctionables(vehicle));
		}

		// Get entities carried by robots
		for (Robot robot : settlement.getAllAssociatedRobots()) {
			entities.addAll(getMalfunctionables(robot));
		}

		// Get entities carried by people on EVA.
		// for (Person person : settlement.getAllAssociatedPeople()) {
		// 	if (person.isOutside())
		// 		entities.addAll(getLocalMalfunctionables(person));
		// }

		// Get entities carried by people on EVA.
		for (Equipment e: settlement.getEquipmentSet()) {
			if (e.getUnitType() == UnitType.EVA_SUIT) {
				EVASuit suit = (EVASuit)e;
				if (suit.getMalfunctionManager().hasMalfunction())
					entities.add(suit);
			}
		}
		
		return entities;
	}

	/**
	 * Gets the repair part probabilities per malfunction for a set of entity scope
	 * strings.
	 *
	 * @param scope a collection of entity scope strings.
	 * @return map of repair parts and probable number of parts needed per
	 *         malfunction.
	 * @throws Exception if error finding repair part probabilities.
	 */
	public static Map<Integer, Double> getRepairPartProbabilities(Collection<String> scope) {
		Map<Integer, Double> repairPartProbabilities = new HashMap<>();

		for (MalfunctionMeta m : mc.getMalfunctionList()) {
			if (m.isMatched(scope)) {
				double malfunctionProbability = m.getProbability() / 100D;

				for (RepairPart p : m.getParts()) {
					double partProbability = p.getRepairProbability() / 100D;
					double averageNumber = RandomUtil.getIntegerAverageValue(p.getNumber());
					double totalNumber = averageNumber * partProbability * malfunctionProbability;

					int id = p.getPartID();
					if (repairPartProbabilities.containsKey(id))
						totalNumber += repairPartProbabilities.get(id);
					repairPartProbabilities.put(id, totalNumber);
				}
			}
		}
		return repairPartProbabilities;
	}

	/**
	 * Gets the probabilities of parts per maintenance for a set of entity scope
	 * strings.
	 *
	 * @param scope a collection of entity scope strings.
	 * @return map of maintenance parts and probable number of parts needed per
	 *         maintenance.
	 * @throws Exception if error finding maintenance part probabilities.
	 */
	static Map<Integer, Double> getMaintenancePartProbabilities(Set<String> scope) {
		Map<Integer, Double> maintenancePartProbabilities = new HashMap<>();
		for (MaintenanceScope maintenance : partConfig.getMaintenance(scope)) {
			double prob = maintenance.getProbability() / 100D;
			int partNumber = maintenance.getMaxNumber();
			double averageNumber = RandomUtil.getIntegerAverageValue(partNumber);
			double totalNumber = averageNumber * prob;

			Integer id = maintenance.getPart().getID();
			if (maintenancePartProbabilities.containsKey(id))
				totalNumber += maintenancePartProbabilities.get(id);
			maintenancePartProbabilities.put(id, totalNumber);
		}

		return maintenancePartProbabilities;
	}

	/**
	 * Obtains the malfunction representing the specified name.
	 *
	 * @param malfunctionName
	 * @return {@link Malfunction}
	 */
	public static MalfunctionMeta getMalfunctionByName(String malfunctionName) {
		MalfunctionMeta result = null;

		for (MalfunctionMeta m : mc.getMalfunctionList()) {
			if (m.getName().equalsIgnoreCase(malfunctionName))
				result = m;
		}

		return result;
	}

	/**
	 * Gets the next incident number for the simulation.
	 *
	 * @return
	 */
	synchronized int getNewIncidentNum() {
		return ++newIncidentNum;
	}

	/**
	 * Computes the reliability of each part.
	 * 
	 * @param missionSol
	 */
	public void computePartReliability(int missionSol) {
		for (Part p : Part.getParts()) {
			p.computeReliability(missionSol);
		}
	}
}
