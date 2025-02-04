/*
 * Mars Simulation Project
 * Transportable.java
 * @date 2023-05-01
 * @author Scott Davis
 */
package org.mars_sim.msp.core.interplanetary.transport;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Entity;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.events.ScheduledEventHandler;
import org.mars_sim.msp.core.events.ScheduledEventManager;
import org.mars_sim.msp.core.interplanetary.transport.resupply.ResupplyUtil;
import org.mars_sim.msp.core.person.EventType;
import org.mars_sim.msp.core.time.MarsTime;
import org.mars_sim.msp.core.time.MasterClock;

/**
 * An class for an item that is transported between planets/moons/etc.
 */
public abstract class Transportable
	implements Comparable<Transportable>, Entity, ScheduledEventHandler {
	
	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
	private MarsTime arrivalDate;
	private MarsTime launchDate;
	private TransitState state;
	private Coordinates landingSite;
	private String name;

	protected static TransportManager tm;
	protected static MasterClock master;

	public Transportable(String name, Coordinates landingSite) {
		this.name = name;
		
		this.landingSite = landingSite;
		this.state = TransitState.PLANNED;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String newName) {
		name = newName;
	}

	public Coordinates getLandingLocation() {
		return landingSite;
	}

	protected void updateLandingLocation(Coordinates newLocation) {
		landingSite = newLocation;
	}

	public TransitState getTransitState() {
		return state;
	}

	public MarsTime getLaunchDate() {
		return launchDate;
	}

	/**
	 * Gets the settlementName.
	 * 
	 * @return the settlement's name.
	 */
	public abstract String getSettlementName();

	/**
	 * Gets the arrival date at the destination.
	 * 
	 * @return arrival date as a MarsTime instance.
	 */
	public MarsTime getArrivalDate() {
		return arrivalDate;
	}

	/**
	 * Sets the arrival date of the resupply mission.
	 * 
	 * @param arrivalDate the arrival date.
	 */
	public void setArrivalDate(MarsTime arrivalDate) {
		this.arrivalDate = arrivalDate;

		// Determine launch date.
		launchDate = arrivalDate.addTime(-1D * ResupplyUtil.getAverageTransitTime() * 1000D);

		// Set resupply state based on launch and arrival time.
		MarsTime now = master.getMarsTime();
		MarsTime nextScheduledEvent = launchDate;
		state = TransitState.PLANNED;
		if (now.getTimeDiff(launchDate) > 0D) {
			state = TransitState.IN_TRANSIT;
			nextScheduledEvent = arrivalDate;
			if (now.getTimeDiff(arrivalDate) > 0D) {
				// Should have already arrived
				nextScheduledEvent = now;
			}
		}

		// Set the event handle for the next future change
		ScheduledEventManager trigger = getOwningManager();
		trigger.removeEvent(this); // Remove any old scheduled event
		trigger.addEvent(nextScheduledEvent, this);
	}

	/**
	 * Which managers is controlling the future events
	 * @return
	 */
	protected abstract ScheduledEventManager getOwningManager();

	/**
	 * Performs the arrival of the transportable.
	 */
	protected abstract void performArrival(SimulationConfig sc, Simulation sim);

	/**
	 * Reinitializes a loading item to the active UnitManager.
	 * 
	 * @param um
	 */
	public abstract void reinit(UnitManager um);

	/**
	 * Description for the future event
	 * @return
	 */
	public String getEventDescription() {
		return (state == TransitState.PLANNED ? "Launch of " : "Arrival of ") + name;
	}

	/**
	 * Cancels a transport item.
	 * 
	 */
	public void cancel() {
		state = TransitState.CANCELED;
		tm.fireEvent(this, EventType.TRANSPORT_ITEM_CANCELLED);

		getOwningManager().removeEvent(this);
	}

	/**
	 * The Resupply has moved to a different phase.
	 * @param now Current time
	 * @return return the milliosols to the next phase
	 */
	public int execute(MarsTime now) {
		int nextEvent = 0;
		switch(state) {
			case PLANNED:
				// Launch has arrived
				state = TransitState.IN_TRANSIT;
				tm.fireEvent(this, EventType.TRANSPORT_ITEM_LAUNCHED);
				nextEvent = (int) arrivalDate.getTimeDiff(now);
				break;
			case IN_TRANSIT:
				// Arrvived
				tm.fireEvent(this, EventType.TRANSPORT_ITEM_ARRIVED);
				state = TransitState.ARRIVED;
				performArrival(SimulationConfig.instance(), Simulation.instance());
				break;
			default:
				// Unexpected event			
		}
		return nextEvent;
	}

	@Override
	public int compareTo(Transportable o) {
		int result = 0;

		double arrivalTimeDiff = getArrivalDate().getTimeDiff(o.getArrivalDate());
		if (arrivalTimeDiff < 0D) {
			result = -1;
		} else if (arrivalTimeDiff > 0D) {
			result = 1;
		} else {
			// If arrival time is the same, compare by name alphabetically.
			result = getName().compareTo(o.getName());
		}

		return result;
	}

	static void initalizeInstances(MasterClock mc, TransportManager transportManager) {
		master = mc;
		tm = transportManager;
	}
}
