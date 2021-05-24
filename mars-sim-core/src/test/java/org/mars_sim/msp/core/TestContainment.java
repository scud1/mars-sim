/**
 * Mars Simulation Project
 * LoadVehicleTest.java
 * @version 3.1.0 2017-01-21
 * @author Scott Davis
 */

package org.mars_sim.msp.core;

import org.mars_sim.msp.core.equipment.Bag;
import org.mars_sim.msp.core.equipment.EVASuit;
import org.mars_sim.msp.core.location.LocationStateType;
import org.mars_sim.msp.core.mars.MarsSurface;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.MockSettlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.Function;
import org.mars_sim.msp.core.vehicle.MockVehicle;
import org.mars_sim.msp.core.vehicle.Vehicle;

import junit.framework.TestCase;

public class TestContainment
extends TestCase {

	
	private Building garage;
	private MockSettlement settlement;
	private MarsSurface surface;

	@Override
    public void setUp() throws Exception {
        SimulationConfig config = SimulationConfig.instance();
        config.loadConfig();
        Simulation sim = Simulation.instance();
        sim.testRun();
        
        Function.initializeInstances(config.getBuildingConfiguration(), null, null, null, null, sim.getUnitManager());
        
        surface = MarsSurface.marsSurface;
        
		settlement = new MockSettlement();
		garage = new Building(1, "Garage", "Garage", 0D, 0D, 0D, 0D, 0D, settlement.getBuildingManager());
        
//        UnitManager unitManager = Simulation.instance().getUnitManager();
//		Iterator<Settlement> i = unitManager.getSettlements().iterator();
//		while (i.hasNext()) {
//			unitManager.removeUnit(i.next());
//		}
//				
//		// Create test settlement.
//		settlement = new MockSettlement();
//		
//        BuildingManager buildingManager = settlement.getBuildingManager();
//        
//		// Removes all mock buildings and building functions in the settlement.
//		buildingManager.removeAllMockBuildings();
//		
//		unitManager.addUnit(settlement);
    }

	private void testContainment(Unit source, Unit container, Unit topContainer, LocationStateType lon) {
		assertEquals("Location state type", lon, source.getLocationStateType());
		assertEquals("Parent container", container, source.getContainerUnit());
		assertEquals("Top container", topContainer, source.getTopContainerUnit());
	}

	
	/*
	 * Test method for 'org.mars_sim.msp.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testPersonInGarage() throws Exception {
		Person person = new Person("Worker One", settlement);

		person.setContainerUnit(garage);

		// TODO Should top container be settlement ??
		testContainment(person, garage, garage, LocationStateType.INSIDE_SETTLEMENT);
	}
	
	/*
	 * Test method for 'org.mars_sim.msp.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testVehicleInGarage() throws Exception {
		Vehicle vehicle = new MockVehicle(settlement);

		vehicle.setContainerUnit(garage);

		// TODO Should top container be settlement ??
		testContainment(vehicle, garage, garage, LocationStateType.INSIDE_SETTLEMENT);
	}

	/*
	 * Test method for 'org.mars_sim.msp.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testPassenagerInGarage() throws Exception {
		Vehicle vehicle = new MockVehicle(settlement);

		vehicle.setContainerUnit(garage);

		Person person = new Person("Passanger1 Name", settlement);
		person.setContainerUnit(vehicle);
		
		assertTrue("InVehicle", person.isInVehicle());
		
		// TODO If Person is in a Vehicle parked in Garage then they are in a Settlement
		//assertTrue("InSettlement", person.isInSettlement());

		// TODO Should this be Settlement top container?
		testContainment(person, vehicle, garage, LocationStateType.INSIDE_VEHICLE);
	}
	
	/*
	 * Test method for 'org.mars_sim.msp.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testVehicleNearSettlement() throws Exception {
		Vehicle vehicle = new MockVehicle(settlement);

		vehicle.setContainerUnit(settlement);

		testContainment(vehicle, settlement, settlement, LocationStateType.WITHIN_SETTLEMENT_VICINITY);
	}
	
	/*
	 * Test method for 'org.mars_sim.msp.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testVehicleOnSurface() throws Exception {
		Vehicle vehicle = new MockVehicle(settlement);

		vehicle.setContainerUnit(surface);

		// If on the surface then should container be null
		testContainment(vehicle, surface, surface, LocationStateType.MARS_SURFACE);
	}

	/*
	 * Test method for 'org.mars_sim.msp.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testBagInGarage() throws Exception {

		Bag bag = new Bag(settlement.getCoordinates());
		bag.setContainerUnit(garage);
		
		testContainment(bag, garage, garage, LocationStateType.INSIDE_SETTLEMENT);
	}
	
	/*
	 * Test method for 'org.mars_sim.msp.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testBagOnSurface() throws Exception {

		Bag bag = new Bag(settlement.getCoordinates());
		bag.setContainerUnit(MarsSurface.marsSurface);
		
		testContainment(bag, MarsSurface.marsSurface, MarsSurface.marsSurface, LocationStateType.MARS_SURFACE);
	}
	
	/*
	 * Test method for 'org.mars_sim.msp.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testBagInVehicleNearSettlement() throws Exception {
		Vehicle vehicle = new MockVehicle(settlement);

		vehicle.setContainerUnit(settlement);

		Bag bag = new Bag(vehicle.getCoordinates());
		bag.setContainerUnit(vehicle);
		
		testContainment(bag, vehicle, settlement, LocationStateType.INSIDE_VEHICLE);
	}
	
	/*
	 * Test method for 'org.mars_sim.msp.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testEVAOnPerson() throws Exception {
		Person person = new Person("Worker Two", settlement);

		person.setContainerUnit(MarsSurface.marsSurface);

		EVASuit suit = new EVASuit(settlement.getCoordinates());
		suit.setContainerUnit(person);
		
		// TODO Shou;dn't the top container be the Settlement ?
		testContainment(suit, person, person, LocationStateType.ON_PERSON_OR_ROBOT);
	}
}