/*
 * Mars Simulation Project
 * Connection.java
 * @date 2022-07-13
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person;

public enum Connection {

	CHECKING_MESSAGES 		("Checking Personal Messages"),
	WATCHING_EARTH_NEWS		("Watching Earth News"),
	BROWSING_MARSLINK		("Browsing MarsLink"),
	WATCHING_EARTH_TV		("Watching Earth TV"),
	WATCHING_A_MOVIE		("Watching a Movie"),
	BROWSING_EARTHLINK 		("Browsing EarthLink"),
	;
		
	private String name;

	/** hidden constructor. */
	private Connection(String name) {
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}

	@Override
	public final String toString() {
		return getName();
	}
}