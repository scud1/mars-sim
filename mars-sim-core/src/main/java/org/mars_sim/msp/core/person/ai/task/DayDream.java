/*
 * Mars Simulation Project
 * DayDream.java
 * @date 2022-07-24
 * @author Barry Evans
 */

package org.mars_sim.msp.core.person.ai.task;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.TaskPhase;

/**
 * A stress free task of doing nothing.
 */
public class DayDream extends Task {

	private static final long serialVersionUID = 1L;
	
	public static final String NAME = "Day Dreaming";

	private static final TaskPhase NOTHING = new TaskPhase("Doing Nothing");

	/**
	 * A stress free task that just consume time
	 * @param person
	 */
	public DayDream(Person person) {
		super(NAME, person, false, false, 0D, 2D);
		
		// Initialize phase
		addPhase(NOTHING);
		setPhase(NOTHING);
	}
	
	/**
	 * Consumes the time and do nothing.
	 */
	@Override
	protected double performMappedPhase(double time) {
		return 0;
	}
}
