/*
 * Mars Simulation Project
 * ModifyTransportItemDialog.java
 * @date 2022-07-20
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.resupply;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.mars_sim.msp.core.interplanetary.transport.Transportable;
import org.mars_sim.msp.core.interplanetary.transport.resupply.Resupply;
import org.mars_sim.msp.core.interplanetary.transport.settlement.ArrivingSettlement;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.ModalInternalFrame;

/**
 * A dialog for modifying transport items.
 * TODO externalize strings
 */
@SuppressWarnings("serial")
public class ModifyTransportItemDialog extends ModalInternalFrame {

	// Data members.
	private Transportable transportItem;
	private TransportItemEditingPanel editingPanel;
	private ResupplyWindow resupplyWindow;

	private JButton commitButton;
	
	/**
	 * Constructor.
	 * 
	 * @param owner the owner of this dialog.
	 * @param title title of dialog.
	 * @param transportItem the transport item to modify.
	 */
	public ModifyTransportItemDialog(MainDesktopPane desktop, ResupplyWindow resupplyWindow, String title, Transportable transportItem) {// , boolean isFX) {

		// Use ModalInternalFrame constructor
        super("Modify Mission");

		// Initialize data members.
		this.transportItem = transportItem;
		this.resupplyWindow = resupplyWindow;

		this.setSize(560, 500);

		 // Create main panel
        JPanel mainPane = new JPanel(new BorderLayout());
        setContentPane(mainPane);

        initEditingPanel();

		mainPane.add(editingPanel, BorderLayout.CENTER);

		// Create the button pane.
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

		mainPane.add(buttonPane, BorderLayout.SOUTH);

		// Create commit button.
		commitButton = new JButton("Commit Changes");
		commitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// Modify transport item and close dialog.
				modifyTransportItem();
			}
		});
		buttonPane.add(commitButton);

		// Create cancel button.
		// Change button text from "Cancel"  to "Discard Changes"
		JButton cancelButton = new JButton("Discard Changes");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// Close dialog.
				dispose();
			}

		});
		buttonPane.add(cancelButton);

        // Add to its own tab pane
		desktop.add(this);    

		Dimension desktopSize = desktop.getParent().getSize();
	    Dimension jInternalFrameSize = this.getSize();
	    int width = (desktopSize.width - jInternalFrameSize.width) / 2;
	    int height = (desktopSize.height - jInternalFrameSize.height) / 2;

	    setLocation(width, height);
	    setModal(true);
	    setVisible(true);
	}

	public void initEditingPanel() {

		// Create editing panel.
		editingPanel = null;
		if (transportItem instanceof ArrivingSettlement) {
			editingPanel = new ArrivingSettlementEditingPanel((ArrivingSettlement) transportItem, resupplyWindow, this, null);
		}
		else if (transportItem instanceof Resupply) {
			editingPanel = new ResupplyMissionEditingPanel((Resupply) transportItem, resupplyWindow, this, null);
		}
		else {
			throw new IllegalStateException("Transport item: " + transportItem + " is not valid.");
		}
	}


	/**
	 * Modify the transport item and close the dialog.
	 */
	private void modifyTransportItem() {
		if ((editingPanel != null) && editingPanel.modifyTransportItem()) {
			dispose();
			resupplyWindow.refreshMission();
		}
	}
	
	public void setCommitButton(boolean value) {
		SwingUtilities.invokeLater(() -> commitButton.setEnabled(value));
	}
	
}
