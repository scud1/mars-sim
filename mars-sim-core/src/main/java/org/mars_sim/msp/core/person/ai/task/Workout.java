/*
 * Mars Simulation Project
 * Workout.java
 * @date 2022-08-04
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.TaskPhase;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.Exercise;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * The Workout class is a task for working out in an exercise facility.
 */
public class Workout extends Task {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.workout"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase EXERCISING = new TaskPhase(Msg.getString("Task.phase.exercising")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = -.75D;

	// Data members
	/** The exercise building the person is using. */
	private Exercise gym;

	/**
	 * Constructor. This is an effort-driven task.
	 *
	 * @param person the person performing the task.
	 */
	public Workout(Person person) {
		// Use Task constructor.
		super(NAME, person, true, false, STRESS_MODIFIER,
				10.0 + RandomUtil.getRandomInt(-7, 7));

		if (person.isInSettlement()) {

			// If person is in a settlement, try to find a gym.
			Building gymBuilding = BuildingManager.getAvailableGymBuilding(person);
			if (gymBuilding != null) {
				// Walk to gym building.
				walkToTaskSpecificActivitySpotInBuilding(gymBuilding, FunctionType.EXERCISE, true);
				gym = gymBuilding.getExercise();

				// Initialize phase
				addPhase(EXERCISING);
				setPhase(EXERCISING);
			}
			else {
				// Go back to his quarters
				Building quarters = person.getQuarters();
				if (quarters != null) {
					walkToBed(quarters, person, true);
				}

				// Initialize phase
				addPhase(EXERCISING);
				setPhase(EXERCISING);
			}

		} else {
			endTask();
		}
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (EXERCISING.equals(getPhase())) {
			return exercisingPhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Performs the exercising phase.
	 *
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double exercisingPhase(double time) {
		// Regulates hormones
		// Improves musculoskeletal systems
		// Record the exercise time [in millisols]
		person.getPhysicalCondition().workout(time);
		
		return 0;
	}

	/**
	 * Removes the person from the associated gym.
	 */
	@Override
	protected void clearDown() {
		// Remove person from exercise function so others can use it.
		if (gym != null && gym.getNumExercisers() > 0) {
			gym.removeExerciser();
		}
	}
}
