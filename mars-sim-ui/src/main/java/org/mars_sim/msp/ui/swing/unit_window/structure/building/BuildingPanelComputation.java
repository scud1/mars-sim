/*
 * Mars Simulation Project
 * BuildingPanelComputation.java
 * @date 2022-07-10
 * @author Manny Kung
 */
package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.structure.building.function.Computation;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.StyleManager;
import org.mars_sim.msp.ui.swing.utils.AttributePanel;

/**
 * The BuildingPanelComputation class is a building function panel representing
 * the computational capability of a building.
 */
@SuppressWarnings("serial")
public class BuildingPanelComputation extends BuildingFunctionPanel {

	private static final String COMPUTING_ICON = "computing";

	private JLabel textField0;
	private JLabel textField1;
	private JLabel textField2;
	/**
	 * Constructor.
	 * @param computation the computation building function.
	 * @param desktop the main desktop.
	 */
	public BuildingPanelComputation(Computation computation, MainDesktopPane desktop) {

		// Use BuildingFunctionPanel constructor
		super(
			Msg.getString("BuildingPanelComputation.title"), 
			ImageLoader.getIconByName(COMPUTING_ICON), 
			computation.getBuilding(), 
			desktop
		);
	}
	
	/**
	 * Build the UI
	 */
	@Override
	protected void buildUI(JPanel center) {

		AttributePanel springPanel = new AttributePanel(3);
		center.add(springPanel, BorderLayout.NORTH);

		// Power Demand
		double powerDemand = building.getComputation().getFullPowerRequired();
		textField0 = springPanel.addTextField(Msg.getString("BuildingPanelComputation.powerDemand"),
				     StyleManager.DECIMAL_KW.format(powerDemand), Msg.getString("BuildingPanelComputation.powerDemand.tooltip"));

		// Usage
		double usage = building.getComputation().getUsagePercent();
		textField1 = springPanel.addTextField(Msg.getString("BuildingPanelComputation.usage"),
					 			StyleManager.DECIMAL_PERC.format(usage), Msg.getString("BuildingPanelComputation.usage.tooltip"));

		// Peak
		double peak = Math.round(building.getComputation().getPeakComputingUnit()* 1_000.0)/1_000.0;
		// Current
		double computingUnit = Math.round(building.getComputation().getComputingUnit()* 1_000.0)/1_000.0;
		String text = computingUnit + " / " + peak + " CUs";
		textField2 = springPanel.addTextField(Msg.getString("BuildingPanelComputation.computingUnit"),
				text, Msg.getString("BuildingPanelComputation.computingUnit.tooltip"));
	}
	
	@Override
	public void update() {

		String power = StyleManager.DECIMAL_KW.format(building.getComputation().getFullPowerRequired());

		if (!textField0.getText().equalsIgnoreCase(power))
			textField0.setText(power);
		
		double util =building.getComputation().getUsagePercent();
		textField1.setText(StyleManager.DECIMAL_PERC.format(util));
		
		double peak = Math.round(building.getComputation().getPeakComputingUnit()* 1_000.0)/1_000.0;

		double computingUnit = Math.round(building.getComputation().getComputingUnit()* 1_000.0)/1_000.0;
		String text = computingUnit + " / " + peak + " CUs";
		if (!textField2.getText().equalsIgnoreCase(text))
			textField2.setText(text);
		
	}
}
