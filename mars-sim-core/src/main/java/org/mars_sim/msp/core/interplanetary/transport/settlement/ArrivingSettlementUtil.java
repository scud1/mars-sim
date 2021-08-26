/**
 * Mars Simulation Project
 * ArrivingSettlementUtil.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.interplanetary.transport.settlement;

import java.util.ArrayList;
import java.util.List;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.interplanetary.transport.TransitState;
import org.mars_sim.msp.core.reportingAuthority.ReportingAuthority;
import org.mars_sim.msp.core.reportingAuthority.ReportingAuthorityFactory;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.SettlementConfig;
import org.mars_sim.msp.core.time.MarsClock;

/**
 * Utility class for arriving settlements.
 */
public class ArrivingSettlementUtil {

	/** Average transit time for arriving settlements from Earth to Mars (sols). */
	public static int AVG_TRANSIT_TIME = 250;

	private static MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
	
	/**
	 * Private constructor for utility class.
	 */
	private ArrivingSettlementUtil() {
		// Do nothing
	}

	/**
	 * Create the initial arriving settlements from the settlement configuration.
	 */
	public static List<ArrivingSettlement> createInitialArrivingSettlements(SettlementConfig settlementConfig,
			ReportingAuthorityFactory raFactory) {

		List<ArrivingSettlement> arrivingSettlements = new ArrayList<ArrivingSettlement>();

		int arrivingSettlementNum = settlementConfig.getNumberOfNewArrivingSettlements();
		for (int x = 0; x < arrivingSettlementNum; x++) {

			String sponsor = settlementConfig.getNewArrivingSettlementSponsor(x); 
			String template = settlementConfig.getNewArrivingSettlementTemplate(x);
			String name = settlementConfig.getNewArrivingSettlementName(x);
			if (name.equals(SettlementConfig.RANDOM)) {
				// Seems wrong as the Sponsor should be defined
				ReportingAuthority ra = raFactory.getAuthority(sponsor);
				name = Settlement.generateName(ra);
			}

			
			int population = settlementConfig.getNewArrivingSettlementPopulationNumber(x);
			int numOfRobots = settlementConfig.getNewArrivingSettlementNumOfRobots(x);
			// Determine arrival time.
			double arrivalTime = settlementConfig.getNewArrivingSettlementArrivalTime(x);
			MarsClock arrivalDate = (MarsClock) currentTime.clone();
			arrivalDate.addTime(arrivalTime * 1000D);

			// Get arriving settlement longitude
			double longitude = 0D;
			String longitudeStr = settlementConfig.getNewArrivingSettlementLongitude(x);
			if (longitudeStr.equals(SettlementConfig.RANDOM)) {
				longitude = Coordinates.getRandomLongitude();
			} else {
				longitude = Coordinates.parseLongitude2Theta(longitudeStr);
			}

			// Get arriving settlement latitude
			double latitude = 0D;
			String latitudeStr = settlementConfig.getNewArrivingSettlementLatitude(x);
			if (latitudeStr.equals(SettlementConfig.RANDOM)) {
				latitude = Coordinates.getRandomLatitude();
			} else {
				latitude = Coordinates.parseLatitude2Phi(latitudeStr);
			}
			Coordinates location = new Coordinates(latitude, longitude);

			
			// Create arriving settlement.
			ArrivingSettlement arrivingSettlement = new ArrivingSettlement(name, template, sponsor,
					arrivalDate, location, population, numOfRobots);
			// Add scenarioID 
			int scenarioID = settlementConfig.getNewArrivingSettlementTemplateID(x);
			arrivingSettlement.setTemplateID(scenarioID);
			
			// Determine launch date.
			MarsClock launchDate = (MarsClock) arrivalDate.clone();
			launchDate.addTime(-1D * AVG_TRANSIT_TIME * 1000D);
			arrivingSettlement.setLaunchDate(launchDate);

			// Set transit state based on launch and arrival time.
			TransitState transitState = TransitState.PLANNED;
			if (MarsClock.getTimeDiff(currentTime, launchDate) >= 0D) {
				transitState = TransitState.IN_TRANSIT;
				if (MarsClock.getTimeDiff(currentTime, arrivalDate) >= 0D) {
					transitState = TransitState.ARRIVED;
				}
			}
			arrivingSettlement.setTransitState(transitState);

			// Add arriving settlement to list.
			arrivingSettlements.add(arrivingSettlement);
		}

		return arrivingSettlements;
	}
}
