/*
 * Mars Simulation Project
 * TabPanelManufacture.java
 * @date 2022-07-09
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.manufacture.ManufactureProcess;
import org.mars_sim.msp.core.manufacture.ManufactureProcessInfo;
import org.mars_sim.msp.core.manufacture.ManufactureUtil;
import org.mars_sim.msp.core.manufacture.SalvageProcess;
import org.mars_sim.msp.core.manufacture.SalvageProcessInfo;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.OverrideType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.Manufacture;
import org.mars_sim.msp.core.tool.Conversion;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.JComboBoxMW;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

/**
 * A tab panel displaying settlement manufacturing information.
 */
@SuppressWarnings("serial")
public class TabPanelManufacture extends TabPanel {

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(TabPanelManufacture.class.getName());
	
	private static final int WORD_WIDTH = 70;
	private static final String MANU_ICON ="manufacture";
	
	/** The Settlement instance. */
	private Settlement settlement;
	
	private JPanel manufactureListPane;
	private JScrollPane manufactureScrollPane;
	private List<ManufactureProcess> processCache;
	private List<SalvageProcess> salvageCache;
	
	/** building selector. */
	private JComboBoxMW<Building> buildingComboBox;
	/** List of available manufacture buildings. */
	private Vector<Building> buildingComboBoxCache;
	/** Process selector. */
	private JComboBoxMW<ManufactureProcessInfo> processSelection;
	/** List of available processes. */
	private Vector<ManufactureProcessInfo> processSelectionCache;
	/** List of available salvage processes. */
	private Vector<SalvageProcessInfo> salvageSelectionCache;
	
	/** Process selection button. */
	private JButton newProcessButton;
	/** Checkbox for overriding manufacturing. */
	private JCheckBox overrideManuCheckbox;
	/** Checkbox for overriding salvaging. */	
	private JCheckBox overrideSalvageCheckbox;

	/**
	 * Constructor.
	 * 
	 * @param unit    {@link Unit} the unit to display.
	 * @param desktop {@link MainDesktopPane} the main desktop.
	 */
	public TabPanelManufacture(Unit unit, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelManufacture.title"), //$NON-NLS-1$
			ImageLoader.getIconByName(MANU_ICON),
			Msg.getString("TabPanelManufacture.title"), //$NON-NLS-1$
			unit, desktop
		);

		settlement = (Settlement) unit;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected void buildUI(JPanel content) {

		// Create scroll panel for manufacture list pane.
		manufactureScrollPane = new JScrollPane();
		// increase vertical mousewheel scrolling speed for this one
		manufactureScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		manufactureScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		manufactureScrollPane.setPreferredSize(new Dimension(220, 215));
		content.add(manufactureScrollPane, BorderLayout.CENTER);

		// Prepare manufacture outer list pane.
		JPanel manufactureOuterListPane = new JPanel(new BorderLayout(0, 0));
//		manufactureOuterListPane.setBorder(new MarsPanelBorder());
		manufactureScrollPane.setViewportView(manufactureOuterListPane);

		// Prepare manufacture list pane.
		manufactureListPane = new JPanel();
		manufactureListPane.setLayout(new BoxLayout(manufactureListPane, BoxLayout.Y_AXIS));
		manufactureOuterListPane.add(manufactureListPane, BorderLayout.NORTH);

		// Create the process panels.
		processCache = getManufactureProcesses();
		Iterator<ManufactureProcess> i = processCache.iterator();
		while (i.hasNext())
			manufactureListPane.add(new ManufacturePanel(i.next(), true, WORD_WIDTH));

		// Create salvage panels.
		salvageCache = new ArrayList<>(); //getSalvageProcesses(); //getSalvageProcesses());
		Iterator<SalvageProcess> j = salvageCache.iterator();
		while (j.hasNext())
			manufactureListPane.add(new SalvagePanel(j.next(), true, WORD_WIDTH));

		// Create interaction panel.
		JPanel interactionPanel = new JPanel(new GridLayout(5, 1, 0, 0));
		content.add(interactionPanel, BorderLayout.NORTH);

		// Create new building selection.
		buildingComboBoxCache = getManufacturingBuildings();
		buildingComboBox = new JComboBoxMW<Building>(buildingComboBoxCache);
		buildingComboBox.setRenderer(new PromptComboBoxRenderer(" (1). Select a Building"));
		buildingComboBox.setSelectedIndex(-1);
		buildingComboBox.setToolTipText(Msg.getString("TabPanelManufacture.tooltip.selectBuilding")); //$NON-NLS-1$
		buildingComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				update();
			}
		});
		interactionPanel.add(buildingComboBox);

		// Create new manufacture process selection.
		Building workshopBuilding = (Building) buildingComboBox.getSelectedItem();
		processSelectionCache = getAvailableProcesses(workshopBuilding);
		processSelection = new JComboBoxMW<>(processSelectionCache);
		processSelection.setSelectedIndex(-1);
		processSelection.setRenderer(new ManufactureSelectionListCellRenderer(" (2). Select a Process"));
		processSelection.setToolTipText(Msg.getString("TabPanelManufacture.tooltip.selectAvailableProcess")); //$NON-NLS-1$
		interactionPanel.add(processSelection);

		// Add available salvage processes.
		salvageSelectionCache = getAvailableSalvageProcesses(workshopBuilding);
		Iterator<SalvageProcessInfo> k = salvageSelectionCache.iterator();
		while (k.hasNext())
			processSelection.addItem(k.next());

		// Create new process button.
		newProcessButton = new JButton(Msg.getString("TabPanelManufacture.button.createNewProcess")); //$NON-NLS-1$
		newProcessButton.setEnabled(processSelection.getItemCount() > 0);
		newProcessButton.setToolTipText(Msg.getString("TabPanelManufacture.tooltip.createNewProcess")); //$NON-NLS-1$
		newProcessButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				try {
					Building workshopBuilding = (Building) buildingComboBox.getSelectedItem();
					if (workshopBuilding != null) {
						Manufacture workshop = workshopBuilding.getManufacture();
						Object selectedItem = processSelection.getSelectedItem();
						if (selectedItem != null) {
							if (selectedItem instanceof ManufactureProcessInfo) {
								ManufactureProcessInfo selectedProcess = (ManufactureProcessInfo) selectedItem;
								if (ManufactureUtil.canProcessBeStarted(selectedProcess, workshop)) {
									workshop.addProcess(new ManufactureProcess(selectedProcess, workshop));
									update();

									logger.log(workshopBuilding, Level.CONFIG, 0L, "Player selected the manufacturing process '" 
											+ selectedProcess.getName() + "'.");
									
									buildingComboBox.setRenderer(new PromptComboBoxRenderer(" (1). Select a Building"));
									buildingComboBox.setSelectedIndex(-1);
									processSelection.setRenderer(
											new ManufactureSelectionListCellRenderer(" (2). Select a Process"));
									processSelection.setSelectedIndex(-1);
								}
							} else if (selectedItem instanceof SalvageProcessInfo) {
								SalvageProcessInfo selectedSalvage = (SalvageProcessInfo) selectedItem;
								if (ManufactureUtil.canSalvageProcessBeStarted(selectedSalvage, workshop)) {
									Unit salvagedUnit = ManufactureUtil.findUnitForSalvage(selectedSalvage, settlement);
									workshop.addSalvageProcess(
											new SalvageProcess(selectedSalvage, workshop, salvagedUnit));
									update();

									logger.log(workshopBuilding, Level.CONFIG, 0L, "Player selected the salvaging process '" 
											+ salvagedUnit.getName() + "'.");
								}
							}
						}
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, Msg.getString("TabPanelManufacture.log.newProcessButton"), e); //$NON-NLS-1$
				}
			}
		});
		interactionPanel.add(newProcessButton);

		// Create manufacturing override check box.
		overrideManuCheckbox = new JCheckBox(Msg.getString("TabPanelManufacture.checkbox.overrideManufacturing")); //$NON-NLS-1$
		overrideManuCheckbox.setToolTipText(Msg.getString("TabPanelManufacture.tooltip.overrideManufacturing")); //$NON-NLS-1$
		overrideManuCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setOverride(OverrideType.MANUFACTURE, overrideManuCheckbox.isSelected());
			}
		});
		overrideManuCheckbox.setSelected(settlement.getProcessOverride(OverrideType.MANUFACTURE));
		interactionPanel.add(overrideManuCheckbox);
		
		// Create salvaging override check box.
		overrideSalvageCheckbox = new JCheckBox(Msg.getString("TabPanelManufacture.checkbox.overrideSalvaging")); //$NON-NLS-1$
		overrideSalvageCheckbox.setToolTipText(Msg.getString("TabPanelManufacture.tooltip.overrideSalvaging")); //$NON-NLS-1$
		overrideSalvageCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setOverride(OverrideType.SALVAGE, overrideSalvageCheckbox.isSelected());
			}
		});
		overrideSalvageCheckbox.setSelected(settlement.getProcessOverride(OverrideType.SALVAGE));
		interactionPanel.add(overrideSalvageCheckbox);
		
	}

	class PromptComboBoxRenderer extends DefaultListCellRenderer {

		private String prompt;

		/*
		 * Set the text to display when no item has been selected.
		 */
		public PromptComboBoxRenderer(String prompt) {
			this.prompt = prompt;
		}

		/*
		 * Custom rendering to display the prompt text when no item is selected
		 */
		// Add color rendering
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			
			if (value == null) {
				setText(prompt);
				return this;
			}
//			if (c instanceof JLabel) {
//	            if (isSelected) {
//	                c.setBackground(Color.orange);
//	            } else {
//	                c.setBackground(Color.white);
//	            }
//	        } else {
//	            c.setBackground(Color.white);
//	            c = super.getListCellRendererComponent(
//	                    list, value, index, isSelected, cellHasFocus);
//	        }
			return c;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void update() {

		// Update processes if necessary.
		List<ManufactureProcess> processes = getManufactureProcesses();
		List<SalvageProcess> salvages = getSalvageProcesses();
		if (!processCache.equals(processes) || !salvageCache.equals(salvages)) {

			// Add manufacture panels for new processes.
			Iterator<ManufactureProcess> i = processes.iterator();
			while (i.hasNext()) {
				ManufactureProcess process = i.next();
				if (!processCache.contains(process))
					manufactureListPane.add(new ManufacturePanel(process, true, WORD_WIDTH));
			}

			// Add salvage panels for new salvage processes.
			Iterator<SalvageProcess> k = salvages.iterator();
			while (k.hasNext()) {
				SalvageProcess salvage = k.next();
				if (!salvageCache.contains(salvage))
					manufactureListPane.add(new SalvagePanel(salvage, true, WORD_WIDTH));
			}

			// Remove manufacture panels for old processes.
			Iterator<ManufactureProcess> j = processCache.iterator();
			while (j.hasNext()) {
				ManufactureProcess process = j.next();
				if (!processes.contains(process)) {
					ManufacturePanel panel = getManufacturePanel(process);
					if (panel != null)
						manufactureListPane.remove(panel);
				}
			}

			// Remove salvage panels for old salvages.
			Iterator<SalvageProcess> l = salvageCache.iterator();
			while (l.hasNext()) {
				SalvageProcess salvage = l.next();
				if (!salvages.contains(salvage)) {
					SalvagePanel panel = getSalvagePanel(salvage);
					if (panel != null)
						manufactureListPane.remove(panel);
				}
			}

			manufactureScrollPane.validate();

			// Update processCache
			processCache.clear();
			processCache.addAll(processes);

			// Update salvageCache
			salvageCache.clear();
			salvageCache.addAll(salvages);
		}

		// Update all process panels.
		Iterator<ManufactureProcess> i = processes.iterator();
		while (i.hasNext()) {
			ManufacturePanel panel = getManufacturePanel(i.next());
			if (panel != null)
				panel.update();
		}

		// Update all salvage panels.
		Iterator<SalvageProcess> j = salvages.iterator();
		while (j.hasNext()) {
			SalvagePanel panel = getSalvagePanel(j.next());
			if (panel != null)
				panel.update();
		}

		// Update building selection list.
		Vector<Building> newBuildings = getManufacturingBuildings();
		if (!newBuildings.equals(buildingComboBoxCache)) {
			buildingComboBoxCache = newBuildings;
			Building currentSelection = (Building) buildingComboBox.getSelectedItem();
			buildingComboBox.removeAllItems();
			Iterator<Building> k = buildingComboBoxCache.iterator();
			while (k.hasNext())
				buildingComboBox.addItem(k.next());

			if (currentSelection != null) {
				if (buildingComboBoxCache.contains(currentSelection))
					buildingComboBox.setSelectedItem(currentSelection);
			}
		}

		// Update process selection list.
		Building selectedBuilding = (Building) buildingComboBox.getSelectedItem();
		Vector<ManufactureProcessInfo> newProcesses = getAvailableProcesses(selectedBuilding);
		Vector<SalvageProcessInfo> newSalvages = getAvailableSalvageProcesses(selectedBuilding);
		
		if (!newProcesses.equals(processSelectionCache) || !newSalvages.equals(salvageSelectionCache)) {
			
			processSelectionCache = newProcesses;
			salvageSelectionCache = newSalvages;
			Object currentSelection = processSelection.getSelectedItem();
			processSelection.removeAllItems();

			Iterator<ManufactureProcessInfo> l = processSelectionCache.iterator();
			while (l.hasNext())
				processSelection.addItem(l.next());

			Iterator<SalvageProcessInfo> m = salvageSelectionCache.iterator();
			while (m.hasNext())
				processSelection.addItem(m.next());

			if (currentSelection != null) {
				if (processSelectionCache.contains(currentSelection))
					processSelection.setSelectedItem(currentSelection);
			}
		}

		// Update new process button.
		newProcessButton.setEnabled(processSelection.getItemCount() > 0);

		// Update ooverride check box.
		if (settlement.getProcessOverride(OverrideType.MANUFACTURE) != overrideManuCheckbox.isSelected())
			overrideManuCheckbox.setSelected(settlement.getProcessOverride(OverrideType.MANUFACTURE));
	}

	/**
	 * Gets all the manufacture processes at the settlement.
	 * 
	 * @return list of manufacture processes.
	 */
	private List<ManufactureProcess> getManufactureProcesses() {
		List<ManufactureProcess> result = new ArrayList<>();

		Iterator<Building> i = settlement.getBuildingManager().getBuildings(FunctionType.MANUFACTURE).iterator();
		while (i.hasNext()) {
			result.addAll(i.next().getManufacture().getProcesses());
		}

		return result;
	}

	/**
	 * Gets all the salvage processes at the settlement.
	 * 
	 * @return list of salvage processes.
	 */
	private List<SalvageProcess> getSalvageProcesses() {
		List<SalvageProcess> result = new ArrayList<>();

		Iterator<Building> i = settlement.getBuildingManager().getBuildings(FunctionType.MANUFACTURE).iterator();
		while (i.hasNext()) {
			result.addAll(i.next().getManufacture().getSalvageProcesses());
		}

		return result;
	}

	/**
	 * Gets the panel for a manufacture process.
	 * 
	 * @param process the manufacture process.
	 * @return manufacture panel or null if none.
	 */
	private ManufacturePanel getManufacturePanel(ManufactureProcess process) {
		ManufacturePanel result = null;
		for (int x = 0; x < manufactureListPane.getComponentCount(); x++) {
			Component component = manufactureListPane.getComponent(x);
			if (component instanceof ManufacturePanel) {
				ManufacturePanel panel = (ManufacturePanel) component;
				if (panel.getManufactureProcess().equals(process))
					result = panel;
			}
		}
		return result;
	}

	/**
	 * Gets the panel for a salvage process.
	 * 
	 * @param process the salvage process.
	 * @return the salvage panel or null if none.
	 */
	private SalvagePanel getSalvagePanel(SalvageProcess process) {
		SalvagePanel result = null;
		for (int x = 0; x < manufactureListPane.getComponentCount(); x++) {
			Component component = manufactureListPane.getComponent(x);
			if (component instanceof SalvagePanel) {
				SalvagePanel panel = (SalvagePanel) component;
				if (panel.getSalvageProcess().equals(process))
					result = panel;
			}
		}
		return result;
	}

	/**
	 * Gets all manufacturing buildings at a settlement.
	 * 
	 * @return vector of buildings.
	 */
	private Vector<Building> getManufacturingBuildings() {
		return new Vector<>(settlement.getBuildingManager().getBuildings(FunctionType.MANUFACTURE));
	}

	/**
	 * Gets all manufacturing processes available at the workshop.
	 * 
	 * @param manufactureBuilding the manufacturing building.
	 * @return vector of processes.
	 */
	private Vector<ManufactureProcessInfo> getAvailableProcesses(Building manufactureBuilding) {
		Vector<ManufactureProcessInfo> result = new Vector<>();

		try {
			if (manufactureBuilding != null) {

				// Determine highest materials science skill level at settlement.
				Settlement settlement = manufactureBuilding.getSettlement();
				int highestSkillLevel = 0;
				Iterator<Person> i = settlement.getAllAssociatedPeople().iterator();
				while (i.hasNext()) {
					Person tempPerson = i.next();
					SkillManager skillManager = tempPerson.getSkillManager();
					int skill = skillManager.getSkillLevel(SkillType.MATERIALS_SCIENCE);
					if (skill > highestSkillLevel) {
						highestSkillLevel = skill;
					}
				}
				
				// Note: Allow a low material science skill person to have access to 
				// do the next 2 levels of skill process or else difficult 
				// tasks are not learned.
				highestSkillLevel = highestSkillLevel + 2;
				
				Iterator<Robot> k = settlement.getAllAssociatedRobots().iterator();
				while (k.hasNext()) {
					Robot r = k.next();
					SkillManager skillManager = r.getSkillManager();
					int skill = skillManager.getSkillLevel(SkillType.MATERIALS_SCIENCE);
					if (skill > highestSkillLevel) {
						highestSkillLevel = skill;
					}
				}
						
				Manufacture workshop = manufactureBuilding.getManufacture();
				if (workshop.getCurrentTotalProcesses() < workshop.getNumPrintersInUse()) {
					Iterator<ManufactureProcessInfo> j = ManufactureUtil
							.getManufactureProcessesForTechSkillLevel(workshop.getTechLevel(), highestSkillLevel)
							.iterator();
					while (j.hasNext()) {
						ManufactureProcessInfo process = j.next();
						if (ManufactureUtil.canProcessBeStarted(process, workshop))
							result.add(process);
					}
				}
			}
		} catch (Exception e) {
		}
		// Enable Collections.sorts by implementing Comparable<>
		Collections.sort(result);
		return result;
	}

	/**
	 * Gets all salvage processes available at the workshop.
	 * 
	 * @param manufactureBuilding the manufacturing building.
	 * @return vector of processes.
	 */
	private Vector<SalvageProcessInfo> getAvailableSalvageProcesses(Building manufactureBuilding) {
		Vector<SalvageProcessInfo> result = new Vector<SalvageProcessInfo>();
		try {
			if (manufactureBuilding != null) {
				Manufacture workshop = manufactureBuilding.getManufacture();
				Iterator<SalvageProcessInfo> i = Collections
						.unmodifiableList(ManufactureUtil.getSalvageProcessesForTechLevel(workshop.getTechLevel()))
						.iterator();
				while (i.hasNext()) {
					SalvageProcessInfo process = i.next();
					if (ManufactureUtil.canSalvageProcessBeStarted(process, workshop))
						result.add(process);
				}
			}
		} catch (Exception e) {
		}
		// Enable Collections.sorts by implementing Comparable<>
		Collections.sort(result);
		return result;
	}

	/**
	 * Sets the settlement override flag.
	 * 
	 * @param override the override flag.
	 */
	private void setOverride(OverrideType type, boolean override) {
		settlement.setProcessOverride(type, override);
	}

	/**
	 * Inner class for the manufacture selection list cell renderer.
	 */
	private static class ManufactureSelectionListCellRenderer extends DefaultListCellRenderer {

		private static final int PROCESS_NAME_LENGTH = 70;
		private String prompt;

		/*
		 * Set the text to display when no item has been selected.
		 */
		public ManufactureSelectionListCellRenderer(String prompt) {
			this.prompt = prompt;
		}

		// TODO check actual combobox size before cutting off too much of the processes'
		// names
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof ManufactureProcessInfo) {
				ManufactureProcessInfo info = (ManufactureProcessInfo) value;
				if (info != null) {
					String processName = Conversion.capitalize(info.getName());
					if (processName.length() > PROCESS_NAME_LENGTH)
						processName = processName.substring(0, PROCESS_NAME_LENGTH)
								+ Msg.getString("TabPanelManufacture.cutOff"); //$NON-NLS-1$

					((JLabel) result).setText(processName);
					((JComponent) result).setToolTipText(ManufacturePanel.getToolTipString(info, null));
				}
			} else if (value instanceof SalvageProcessInfo) {
				SalvageProcessInfo info = (SalvageProcessInfo) value;
				if (info != null) {
					String processName = Conversion.capitalize(info.getName());
					if (processName.length() > PROCESS_NAME_LENGTH)
						processName = processName.substring(0, PROCESS_NAME_LENGTH)
								+ Msg.getString("TabPanelManufacture.cutOff"); //$NON-NLS-1$
					((JLabel) result).setText(processName);
					((JComponent) result).setToolTipText(SalvagePanel.getToolTipString(null, info, null));
				}
			}

			if (value == null)
				setText(prompt);

			return result;
		}
	}

	/**
	 * Prepares object for garbage collection.
	 */
	@Override
	public void destroy() {
		super.destroy();
		
		settlement = null;
		manufactureListPane = null;
		manufactureScrollPane = null;
		processCache = null;
		salvageCache = null;
		
		buildingComboBox = null;
		buildingComboBoxCache = null;
		processSelection = null;
		processSelectionCache = null;
		salvageSelectionCache = null;
		newProcessButton = null;
		overrideManuCheckbox = null;
	}
}
