/*
 * Mars Simulation Project
 * TabPanelEVA.java
 * @date 2022-09-05
 * @author Manny Kung
 */

package org.mars_sim.msp.ui.swing.unit_window.vehicle;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Collection;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.building.function.EVA;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.core.vehicle.VehicleAirlock;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;
import org.mars_sim.msp.ui.swing.unit_window.UnitListPanel;
import org.mars_sim.msp.ui.swing.unit_window.UnitWindow;

import com.alee.laf.panel.WebPanel;


/**
 * The TabPanelEVA class represents the EVA airlock function of a vehicle.
 */
@SuppressWarnings("serial")
public class TabPanelEVA extends TabPanel {

	private static final String SUIT_ICON = Msg.getString("icon.suit"); //$NON-NLS-1$
	
	private static final String UNLOCKED = "Unlocked";
	private static final String LOCKED = "Locked";
	
	private int innerDoorCache;
	private int outerDoorCache;
	private int occupiedCache;
	private int emptyCache;
	private double cycleTimeCache;
	private boolean activationCache;
	private boolean transitionCache;
	
	private String operatorCache = "";
	private String airlockStateCache = "";
	private String innerDoorStateCache = "";
	private String outerDoorStateCache = "";

	private JTextField innerDoorLabel;
	private JTextField outerDoorLabel;
	private JTextField occupiedLabel;
	private JTextField emptyLabel;
	private JTextField operatorLabel;
	private JTextField airlockStateLabel;
	private JTextField activationLabel;
	private JTextField transitionLabel;
	private JTextField cycleTimeLabel;
	private JTextField innerDoorStateLabel;
	private JTextField outerDoorStateLabel;
	
	private	UnitListPanel<Person> occupants;

	private VehicleAirlock vehicleAirlock;


    /**
     * Constructor.
     * @param vehicle the vehicle.
     * @param desktop The main desktop.
     */
    public TabPanelEVA(Vehicle vehicle, MainDesktopPane desktop) {
        // Use the TabPanel constructor
        super(
            Msg.getString("TabPanelEVA.title"), //$NON-NLS-1$
			ImageLoader.getNewIcon(SUIT_ICON),        	
        	Msg.getString("TabPanelEVA.title"), //$NON-NLS-1$
        	vehicle, 
        	desktop
        );

        if (vehicle instanceof Rover)
        	vehicleAirlock = (VehicleAirlock)((Rover) vehicle).getAirlock();
    }
    
	/**
	 * Build the UI
	 * 
	 * @param content
	 */
    @Override
    protected void buildUI(JPanel content) {
    	if (vehicleAirlock == null) {
    		return;
    	}

        // Create top panel
        WebPanel topPanel = new WebPanel(new GridLayout(6, 1, 0, 0));
        topPanel.setMaximumSize(UnitWindow.WIDTH, UnitWindow.HEIGHT);
        topPanel.setSize(new Dimension(UnitWindow.WIDTH, UnitWindow.HEIGHT));
        content.add(topPanel, BorderLayout.NORTH);

		// Create innerDoorLabel
		innerDoorLabel = addTextField(topPanel, Msg.getString("TabPanelEVA.innerDoor.number"),
				vehicleAirlock.getNumAwaitingInnerDoor(), 4, null);

		if (vehicleAirlock.isInnerDoorLocked())
			innerDoorStateCache = LOCKED;
		else {
			innerDoorStateCache = UNLOCKED;
		}
		// Create innerDoorStateLabel
		innerDoorStateLabel = addTextField(topPanel, Msg.getString("TabPanelEVA.innerDoor.state"),
										   innerDoorStateCache, 10, null);

		// Create outerDoorLabel
		outerDoorLabel = addTextField(topPanel, Msg.getString("TabPanelEVA.outerDoor.number"),
				vehicleAirlock.getNumAwaitingOuterDoor(), 4, null);

		if (vehicleAirlock.isOuterDoorLocked())
			outerDoorStateCache = LOCKED;
		else {
			outerDoorStateCache = UNLOCKED;
		}
		// Create outerDoorStateLabel
		outerDoorStateLabel = addTextField(topPanel, Msg.getString("TabPanelEVA.outerDoor.state"),
										   outerDoorStateCache, 10, null);

		// Create occupiedLabel
		occupiedLabel = addTextField(topPanel, Msg.getString("TabPanelEVA.occupied"),
				vehicleAirlock.getNumInChamber(), 4, null);

		// Create activationLabel
		activationLabel = addTextField(topPanel, Msg.getString("TabPanelEVA.airlock.activation"),
										 vehicleAirlock.isActivated() + "", 10, null);
		// Create emptyLabel
		emptyLabel = addTextField(topPanel, Msg.getString("TabPanelEVA.empty"),
				vehicleAirlock.getNumEmptied(), 4, null);

		// Create airlockStateLabel
		airlockStateLabel = addTextField(topPanel, Msg.getString("TabPanelEVA.airlock.state"),
										 vehicleAirlock.getState().toString(), 10, null);

		// Create cycleTimeLabel
		cycleTimeLabel = addTextField(topPanel, Msg.getString("TabPanelEVA.airlock.cycleTime"),
									  DECIMAL_PLACES1.format(vehicleAirlock.getRemainingCycleTime()), 4, null);
		
		transitionLabel = addTextField(topPanel, Msg.getString("TabPanelEVA.airlock.transition"),
				 vehicleAirlock.isTransitioning() + "", 10, null);

		WebPanel operatorPanel = new WebPanel(new FlowLayout());
		topPanel.add(operatorPanel, BorderLayout.CENTER);
		
		// Create OperatorLabel
		operatorLabel = addTextField(operatorPanel, Msg.getString("TabPanelEVA.operator"),
				vehicleAirlock.getOperatorName(), 10, null);
				
		// Create occupant panel
		WebPanel occupantPanel = new WebPanel(new FlowLayout(FlowLayout.CENTER));
		addBorder(occupantPanel, Msg.getString("TabPanelEVA.titledB.occupants"));
		content.add(occupantPanel, BorderLayout.CENTER);
		
        // Create occupant list
        occupants = new UnitListPanel<>(getDesktop(), new Dimension(150, 100)) {
			@Override
			protected Collection<Person> getData() {
				return getUnitsFromIds(vehicleAirlock.getAllInsideOccupants());
			}
        };
        
        occupantPanel.add(occupants);
    }

    @Override
    public void update() {

		// Update innerDoorLabel
		int inner = vehicleAirlock.getNumAwaitingInnerDoor();
		if (innerDoorCache != inner) {
			innerDoorCache = inner;
			innerDoorLabel.setText(Integer.toString(inner));
		}

		// Update outerDoorLabel
		int outer = vehicleAirlock.getNumAwaitingOuterDoor();
		if (outerDoorCache != outer) {
			outerDoorCache = outer;
			outerDoorLabel.setText(Integer.toString(outer));
		}

		// Update occupiedLabel
		int numChamber = vehicleAirlock.getNumInChamber();
		if (occupiedCache != numChamber) {
			occupiedCache = numChamber;
			occupiedLabel.setText(Integer.toString(numChamber));
		}

		// Update emptyLabel
		int emptyNumChamber = vehicleAirlock.getNumEmptied();
		if (emptyCache != emptyNumChamber) {
			emptyCache = emptyNumChamber;
			emptyLabel.setText(Integer.toString(emptyNumChamber));
		}

		// Update operatorLabel
		String name = vehicleAirlock.getOperatorName();
		if (!operatorCache.equalsIgnoreCase(name)) {
			operatorCache = name;
			operatorLabel.setText(name);
		}

		// Update airlockStateLabel
		String state = vehicleAirlock.getState().toString();
		if (!airlockStateCache.equalsIgnoreCase(state)) {
			airlockStateCache = state;
			airlockStateLabel.setText(state);
		}
		
		// Update activationLabel
		boolean activated = vehicleAirlock.isActivated();
		if (activationCache != activated) {
			activationCache = activated;
			activationLabel.setText(Boolean.toString(activated));
		}

		// Update activationLabel
		boolean transition = vehicleAirlock.isTransitioning();
		if (transitionCache != transition) {
			transitionCache = transition;
			transitionLabel.setText(Boolean.toString(transition));
		}
		
		// Update cycleTimeLabel
		double time = vehicleAirlock.getRemainingCycleTime();
		if (cycleTimeCache != time) {
			cycleTimeCache = time;
			cycleTimeLabel.setText(DECIMAL_PLACES1.format(cycleTimeCache));
		}

		String innerDoorState = "";
		if (vehicleAirlock.isInnerDoorLocked())
			innerDoorState = LOCKED;
		else {
			innerDoorState = UNLOCKED;
		}

		// Update innerDoorStateLabel
		if (!innerDoorStateCache.equalsIgnoreCase(innerDoorState)) {
			innerDoorStateCache = innerDoorState;
			innerDoorStateLabel.setText(innerDoorState);
		}

		String outerDoorState = "";
		if (vehicleAirlock.isOuterDoorLocked())
			outerDoorState = LOCKED;
		else {
			outerDoorState = UNLOCKED;
		}

		// Update outerDoorStateLabel
		if (!outerDoorStateCache.equalsIgnoreCase(outerDoorState)) {
			outerDoorStateCache = outerDoorState;
			outerDoorStateLabel.setText(outerDoorState);
		}

        // Update occupant list
        occupants.update();
    }

    @Override
    public void destroy() {
        super.destroy();
        
        occupiedLabel = null;
        emptyLabel = null;
        operatorLabel = null;
        airlockStateLabel = null;
        cycleTimeLabel = null;

        occupants = null;

        vehicleAirlock = null;
    }
}