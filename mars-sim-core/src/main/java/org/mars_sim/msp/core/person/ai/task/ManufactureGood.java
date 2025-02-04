/*
 * Mars Simulation Project
 * ManufactureGood.java
 * @date 2021-12-22
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.data.UnitSet;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.manufacture.ManufactureProcess;
import org.mars_sim.msp.core.manufacture.ManufactureProcessInfo;
import org.mars_sim.msp.core.manufacture.ManufactureUtil;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.TaskPhase;
import org.mars_sim.msp.core.person.ai.task.util.Worker;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.RobotType;
import org.mars_sim.msp.core.structure.OverrideType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.Manufacture;
import org.mars_sim.msp.core.tool.Conversion;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * A task for working on a manufacturing process.
 */
public class ManufactureGood extends Task {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(ManufactureGood.class.getName());

	
	/** Task name */
	private static final String NAME = Msg.getString("Task.description.manufactureGood"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase MANUFACTURE = new TaskPhase(Msg.getString("Task.phase.manufacture")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .2D;

	// Data members
	/** The manufacturing workshop the person is using. */
	private Manufacture workshop;

	/**
	 * Constructor.
	 * 
	 * @param person the person to perform the task
	 */
	public ManufactureGood(Person person) {
		super(NAME, person, true, false, STRESS_MODIFIER, SkillType.MATERIALS_SCIENCE, 100D, 25);

		init();
	}

	/**
	 * Constructor.
	 * 
	 * @param robot the robot to perform the task
	 */
	public ManufactureGood(Robot robot) {
		super(NAME, robot, true, false, STRESS_MODIFIER, SkillType.MATERIALS_SCIENCE, 100D,
				10D + RandomUtil.getRandomDouble(50D));

		init();
	}
	
	private void init() {
		// Initialize data members
		if (worker.isInSettlement()) {

			// Get available manufacturing workshop if any.
			Building manufactureBuilding = getAvailableManufacturingBuilding(worker);
			if (manufactureBuilding != null) {
				setDescription(Msg.getString("Task.description.manufactureGood.building",
						 manufactureBuilding.getNickName())); //$NON-NLS-1$
				workshop = manufactureBuilding.getManufacture();

				// Walk to manufacturing building.
				walkToTaskSpecificActivitySpotInBuilding(manufactureBuilding, FunctionType.MANUFACTURE, false);
				
				// Initialize phase
				addPhase(MANUFACTURE);
				setPhase(MANUFACTURE);
			} else {
				endTask();
			}


		} else {
			endTask();
		}
	}

	/**
	 * Cancels any manufacturing processes that's beyond the skill of any people
	 * associated with the settlement.
	 * 
	 * @param settlement
	 */
	public static void cancelDifficultManufacturingProcesses(Settlement settlement) {

		if (settlement != null) {
			int highestSkillLevel = 0;
			SkillManager skillManager = null;
			for (Person tempPerson : settlement.getAllAssociatedPeople()) {
				skillManager = tempPerson.getSkillManager();
				int skill = skillManager.getSkillLevel(SkillType.MATERIALS_SCIENCE);
				if (skill > highestSkillLevel) {
					highestSkillLevel = skill;
				}
			}

			skillManager = null;
			for (Robot tempRobot : settlement.getAllAssociatedRobots()) {
				if (tempRobot.getRobotType() == RobotType.MAKERBOT) {
					// if (skillManager == null)
					skillManager = tempRobot.getSkillManager();
					int skill = skillManager.getSkillLevel(SkillType.MATERIALS_SCIENCE);
					if (skill > highestSkillLevel) {
						highestSkillLevel = skill;
					}
				}
			}

			for (Building building : settlement.getBuildingManager().getBuildingSet(FunctionType.MANUFACTURE)) {
				Manufacture manufacturingFunction = building.getManufacture();
				Set<ManufactureProcess> processes = new HashSet<>(
						manufacturingFunction.getProcesses());
				for (ManufactureProcess process : processes) {
					int processSkillLevel = process.getInfo().getSkillLevelRequired();
					// NOTE: allow a low material science skill person to have access to do the next 2 levels 
					// of skill process or else difficult tasks are not learned.
					if (processSkillLevel - 2 > highestSkillLevel) {
						// Cancel manufacturing process.
						manufacturingFunction.endManufacturingProcess(process, true);
					}
				}
			}
		}
	}

	/**
	 * Gets an available manufacturing building that the worker can use. Returns
	 * null if no manufacturing building is currently available.
	 * 
	 * @param worker the worker
	 * @return available manufacturing building
	 */
	public static Building getAvailableManufacturingBuilding(Worker worker) {

		Building result = null;

		SkillManager skillManager = worker.getSkillManager();
		int skill = skillManager.getEffectiveSkillLevel(SkillType.MATERIALS_SCIENCE);

		if (worker.isInSettlement()) {
			Set<Building> manufacturingBuildings = worker.getSettlement().getBuildingManager().getBuildingSet(FunctionType.MANUFACTURE);
			manufacturingBuildings = BuildingManager.getNonMalfunctioningBuildings(manufacturingBuildings);
			manufacturingBuildings = getManufacturingBuildingsNeedingWork(manufacturingBuildings, skill);
			manufacturingBuildings = getBuildingsWithProcessesRequiringWork(manufacturingBuildings, skill);
			manufacturingBuildings = getHighestManufacturingTechLevelBuildings(manufacturingBuildings);
			manufacturingBuildings = BuildingManager.getLeastCrowded4BotBuildings(manufacturingBuildings);

			if (manufacturingBuildings.size() > 0) {
				result = RandomUtil.getARandSet(manufacturingBuildings);
			}
		}

		return result;
	}

	/**
	 * Gets a list of manufacturing buildings needing work from a list of buildings
	 * with the manufacture function.
	 * 
	 * @param buildingList list of buildings with the manufacture function.
	 * @param skill        the materials science skill level of the person.
	 * @return list of manufacture buildings needing work.
	 */
	private static Set<Building> getManufacturingBuildingsNeedingWork(Set<Building> buildingList, int skill) {

		Set<Building> result = new UnitSet<>();

		Iterator<Building> i = buildingList.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			if (building.getManufacture().requiresWork(skill)) {
				result.add(building);
			}
		}

		return result;
	}

	/**
	 * Gets a subset list of manufacturing buildings with processes requiring work.
	 * 
	 * @param buildingList the original building list.
	 * @param skill        the materials science skill level of the person.
	 * @return subset list of buildings with processes requiring work, or original
	 *         list if none found.
	 */
	private static Set<Building> getBuildingsWithProcessesRequiringWork(Set<Building> buildingList, int skill) {

		Set<Building> result = new UnitSet<>();
		// Add all buildings with processes requiring work.
		Iterator<Building> i = buildingList.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			if (hasProcessRequiringWork(building, skill)) {
				result.add(building);
			}
		}

		// If no building with processes requiring work, return original list.
		if (result.size() == 0) {
			result = buildingList;
		}

		return result;
	}

	/**
	 * Checks if manufacturing building has any processes requiring work.
	 * 
	 * @param manufacturingBuilding the manufacturing building.
	 * @param skill                 the materials science skill level of the person.
	 * @return true if processes requiring work.
	 */
	public static boolean hasProcessRequiringWork(Building manufacturingBuilding, int skill) {

		boolean result = false;

		Iterator<ManufactureProcess> i = manufacturingBuilding.getManufacture().getProcesses().iterator();
		while (i.hasNext()) {
			ManufactureProcess process = i.next();
			boolean workRequired = (process.getWorkTimeRemaining() > 0D);
			boolean skillRequired = (process.getInfo().getSkillLevelRequired() <= skill);
			if (workRequired && skillRequired)
				result = true;
		}

		return result;
	}

	/**
	 * Gets a subset list of manufacturing buildings with the highest tech level
	 * from a list of buildings with the manufacture function.
	 * 
	 * @param buildingList list of buildings with the manufacture function.
	 * @return subset list of highest tech level buildings.
	 */
	private static Set<Building> getHighestManufacturingTechLevelBuildings(Set<Building> buildingList) {

		Set<Building> result = new UnitSet<>();

		int highestTechLevel = 0;
		Iterator<Building> i = buildingList.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			Manufacture manufacturingFunction = building.getManufacture();
			if (building.getManufacture().getTechLevel() > highestTechLevel) {
				highestTechLevel = manufacturingFunction.getTechLevel();
			}
		}

		Iterator<Building> j = buildingList.iterator();
		while (j.hasNext()) {
			Building building = j.next();
			if (building.getManufacture().getTechLevel() == highestTechLevel) {
				result.add(building);
			}
		}

		return result;
	}

	/**
	 * Gets the highest manufacturing process goods value for the worker and the
	 * manufacturing building.
	 * 
	 * @param worker the worker to perform manufacturing.
	 * @param manufacturingBuilding the manufacturing building.
	 * @return highest process good value.
	 */
	public static double getHighestManufacturingProcessValue(Worker worker, Building manufacturingBuilding) {

		double highestProcessValue = 0D;

		int skillLevel = worker.getSkillManager().getEffectiveSkillLevel(SkillType.MATERIALS_SCIENCE);

		Manufacture manufacturingFunction = manufacturingBuilding.getManufacture();
		int techLevel = manufacturingFunction.getTechLevel();

		Iterator<ManufactureProcessInfo> i = ManufactureUtil
				.getManufactureProcessesForTechSkillLevel(techLevel, skillLevel).iterator();
		while (i.hasNext()) {
			ManufactureProcessInfo process = i.next();
			if (ManufactureUtil.canProcessBeStarted(process, manufacturingFunction)
					|| isProcessRunning(process, manufacturingFunction)) {
				Settlement settlement = manufacturingBuilding.getSettlement();
				double processValue = ManufactureUtil.getManufactureProcessValue(process, settlement);
				if (processValue > highestProcessValue) {
					highestProcessValue = processValue;
				}
			}
		}

		return highestProcessValue;
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (MANUFACTURE.equals(getPhase())) {
			return manufacturePhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Perform the manufacturing phase.
	 * 
	 * @param time the time to perform (millisols)
	 * @return remaining time after performing (millisols)
	 */
	private double manufacturePhase(double time) {

		if (worker.isOutside()) {
			endTask();
			return 0;
		}
			
		// Check if workshop has malfunction.
		Building entity = workshop.getBuilding();
		if (entity.getMalfunctionManager().hasMalfunction()) {
			logger.info(worker, 30_000L, "Manufacturing halted due to malfunction.");
			endTask();
			return time * .75;
		}

        // Cancel any manufacturing processes that's beyond the skill of any people
        // associated with the settlement.
		ManufactureGood.cancelDifficultManufacturingProcesses(entity.getSettlement());
        
		// Determine amount of effective work time based on "Materials Science"
		// skill.
		double workTime = time;
		int skill = getEffectiveSkillLevel();
		if (skill == 0) {
			workTime /= 2;
		} else {
			workTime += workTime * (.2D * (double) skill);
		}

		// Apply work time to manufacturing processes.
		while ((workTime > 0D) && !isDone()) {
			ManufactureProcess process = getRunningManufactureProcess();
			if (process != null) {
				double remainingWorkTime = process.getWorkTimeRemaining();
				double providedWorkTime = workTime;
				if (providedWorkTime > remainingWorkTime) {
					providedWorkTime = remainingWorkTime;
				}
				process.addWorkTime(providedWorkTime);
				workTime -= providedWorkTime;

				if ((process.getWorkTimeRemaining() <= 0D) && (process.getProcessTimeRemaining() <= 0D)) {
					workshop.endManufacturingProcess(process, false);
				}
			}
			
			else {
				if (!worker.getSettlement().getProcessOverride(OverrideType.MANUFACTURE))
					process = createNewManufactureProcess();
				
				if (process == null) {
					endTask();
				}
			}

			if (process != null)
				// Prints description
				setDescription(Conversion.capitalize(process.toString())); // $NON-NLS-1$
			else
				setDescription(Msg.getString("Task.description.manufactureGood.inspecting")); //$NON-NLS-1$

		}

		// Add experience
		addExperience(time);

		// Check for accident in workshop.
		checkForAccident(entity, 0.005D, time);

		
		return 0D;
	}

	/**
	 * Gets an available running manufacturing process.
	 * 
	 * @return process or null if none.
	 */
	private ManufactureProcess getRunningManufactureProcess() {
		ManufactureProcess result = null;

		int skillLevel = getEffectiveSkillLevel();
		Iterator<ManufactureProcess> i = workshop.getProcesses().iterator();
		while (i.hasNext() && (result == null)) {
			ManufactureProcess process = i.next();
			if ((process.getInfo().getSkillLevelRequired() <= skillLevel) && (process.getWorkTimeRemaining() > 0D)) {
				result = process;
			}
		}

		return result;
	}

	/**
	 * Checks if a process type is currently running at a manufacturing building.
	 * 
	 * @param processInfo         the process type.
	 * @param manufactureBuilding the manufacturing building.
	 * @return true if process is running.
	 */
	private static boolean isProcessRunning(ManufactureProcessInfo processInfo, Manufacture manufactureBuilding) {
		boolean result = false;

		Iterator<ManufactureProcess> i = manufactureBuilding.getProcesses().iterator();
		while (i.hasNext()) {
			ManufactureProcess process = i.next();
			if (process.getInfo().getName().equals(processInfo.getName())) {
				result = true;
			}
		}

		return result;
	}

	/**
	 * Creates a new manufacturing process if possible.
	 * 
	 * @return the new manufacturing process or null if none.
	 */
	private ManufactureProcess createNewManufactureProcess() {
		ManufactureProcess result = null;

		if (workshop.getCurrentTotalProcesses() < workshop.getNumPrintersInUse()) {

			int skillLevel = getEffectiveSkillLevel();
			int techLevel = workshop.getTechLevel();

			// Determine all manufacturing processes that are possible and profitable.
			Map<ManufactureProcessInfo, Double> processProbMap = new HashMap<>();
			Iterator<ManufactureProcessInfo> i = ManufactureUtil
					.getManufactureProcessesForTechSkillLevel(techLevel, skillLevel).iterator();
			while (i.hasNext()) {
				ManufactureProcessInfo processInfo = i.next();
				if (ManufactureUtil.canProcessBeStarted(processInfo, workshop)) {
					double processValue = ManufactureUtil.getManufactureProcessValue(processInfo, worker.getSettlement());

					if (processValue > 0D) {
						processProbMap.put(processInfo, processValue);
					}
				}
			}

			// Randomly choose among possible manufacturing processes based on their
			// relative profitability.
			ManufactureProcessInfo chosenProcess = null;
			if (!processProbMap.isEmpty()) {
				chosenProcess = RandomUtil.getWeightedRandomObject(processProbMap);
			}

			// Create chosen manufacturing process.
			if (chosenProcess != null) {
				result = new ManufactureProcess(chosenProcess, workshop);
				workshop.addProcess(result);
			}
		}

		return result;
	}
}
