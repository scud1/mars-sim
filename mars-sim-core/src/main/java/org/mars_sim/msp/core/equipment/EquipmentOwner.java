/*
 * Mars Simulation Project
 * EquipmentOwner.java
 * @date 2021-10-17
 * @author Manny Kung
 */
package org.mars_sim.msp.core.equipment;

import java.util.List;
import java.util.Set;

import org.mars_sim.msp.core.data.ResourceHolder;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResource;

public interface EquipmentOwner extends ResourceHolder {
	
	/**
	 * Gets the total mass held in this entity.
	 * 
	 * @return
	 */
	public double getStoredMass();
	
	/**
	 * Get the equipment list
	 * 
	 * @return
	 */
	List<Equipment> getEquipmentList();
	

	/**
	 * Finds all equipment with a particular equipment type
	 * 
	 * @param type EquipmentType
	 * @return collection of equipment or empty collection if none.
	 */
	List<Equipment> getEquipment(EquipmentType type);
	
	/**
	 * Does this person possess an equipment of this equipment type
	 * 
	 * @param typeID
	 * @return
	 */
	public boolean containsEquipment(EquipmentType type);
	
	/**
	 * Adds an equipment to this person
	 * 
	 * @param equipment
	 * @return true if this person can carry it
	 */
	public boolean addEquipment(Equipment equipment);
	
	/**
	 * Remove an equipment 
	 * 
	 * @param equipment
	 */
	public boolean removeEquipment(Equipment equipment);
	
	/**
	 * Stores the item resource
	 * 
	 * @param resource the item resource
	 * @param quantity
	 * @return excess quantity that cannot be stored
	 */
	public double storeItemResource(int resource, int quantity);
	
	/**
	 * Retrieves the item resource 
	 * 
	 * @param resource
	 * @param quantity
	 * @return quantity that cannot be retrieved
	 */
	public double retrieveItemResource(int resource, int quantity);
	
	/**
	 * Gets the item resource stored
	 * 
	 * @param resource
	 * @return quantity
	 */
	public int getItemResourceStored(int resource);
    
	/**
	 * Gets all stored amount resources
	 * 
	 * @return all stored amount resources.
	 */
	public Set<AmountResource> getAllAmountResourcesStored();
	
	/**
	 * Gets all stored item resources
	 * 
	 * @return all stored item resources.
	 */
	public Set<ItemResource> getAllItemResourcesStored();
	
	/**
	 * Finds the number of empty containers of a class that are contained in storage and have
	 * an empty inventory.
	 * 
	 * @param containerClass  the unit class.
	 * @param brandNew  does it include brand new bag only
	 * @return number of empty containers.
	 */
	public int findNumEmptyContainersOfType(EquipmentType containerType, boolean brandNew);
	
	/**
	 * Finds a container in storage.
	 * 
	 * @param containerType
	 * @param empty does it need to be empty ?
	 * @param resource 
	 * @return instance of container or null if none.
	 */
	public Container findContainer(EquipmentType containerType, boolean empty, int resource);
}
