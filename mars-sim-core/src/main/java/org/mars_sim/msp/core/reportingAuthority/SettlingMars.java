/**
 * Mars Simulation Project
 * SettlingMars.java
 * @version 3.08 2015-12-12
 * @author Manny Kung
 */
package org.mars_sim.msp.core.reportingAuthority;

public class SettlingMars implements MissionAgenda {
	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private final String name = "Settling Mars";

	@Override
	public String getObjectiveName() {
		return name;
	}

	@Override
	public void reportFindings() {
		System.out.println("I'm analyzing geological features in this region.");
	}

	@Override
	public void gatherSamples() {
		System.out.println("I'm putting together a report of the local in-situ resources "
				+ "that we can collect and process for our immediate uses.");
	}




}
