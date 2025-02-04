/**
 * Mars Simulation Project
 * EquipmentWindow.java
 * @date 2023-06-07
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window.equipment;

import org.mars_sim.msp.core.equipment.Equipment;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.unit_window.InventoryTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.LocationTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.MaintenanceTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.MalfunctionTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.NotesTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.SalvageTabPanel;
import org.mars_sim.msp.ui.swing.unit_window.UnitWindow;


/**
 * The EquipmentWindow is the window for displaying a piece of equipment.
 */
public class EquipmentUnitWindow extends UnitWindow {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private boolean salvaged;

	private Equipment equipment;
	
    /**
     * Constructor
     *
     * @param desktop the main desktop panel.
     * @param equipment the equipment this window is for.
     */
    public EquipmentUnitWindow(MainDesktopPane desktop, Equipment equipment) {
        // Use UnitWindow constructor
        super(desktop, equipment, equipment.getAssociatedSettlement().getName() + " - " + equipment.getName(), false);
        this.equipment = equipment;

        // Add tab panels
        addTabPanel(new InventoryTabPanel(equipment, desktop));

        addTabPanel(new LocationTabPanel(equipment, desktop));

        if (equipment instanceof Malfunctionable)
        {
            Malfunctionable m = (Malfunctionable) equipment;

        	addTabPanel(new MaintenanceTabPanel(m, desktop));
            addTabPanel(new MalfunctionTabPanel(m, desktop));
        }
		addTabPanel(new NotesTabPanel(equipment, desktop));

        salvaged = equipment.isSalvaged();
        if (salvaged)
        	addTabPanel(new SalvageTabPanel(equipment, desktop));

    	sortTabPanels();

		// Add to tab panels. 
		addTabIconPanels();
    }

    /**
     * Updates this window.
     */
	@Override
    public void update() {
        super.update();
        // Check if equipment has been salvaged.
        if (!salvaged && equipment.isSalvaged()) {
            addTabPanel(new SalvageTabPanel(equipment, desktop));
            salvaged = true;
        }
    }
}
