/**
 * Mars Simulation Project
 * ConstructionSite.java
 * @date 2023-06-07
 * @author Scott Davis
 */

package org.mars_sim.msp.core.structure.construction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.mars_sim.msp.core.BoundedObject;
import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.LocalBoundedObject;
import org.mars_sim.msp.core.LocalPosition;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.ai.mission.BuildingConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.MissionPhase;
import org.mars_sim.msp.core.person.ai.task.util.Worker;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.Structure;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.vehicle.GroundVehicle;

/**
 * A building construction site.
 */
public class ConstructionSite
extends Structure
implements  LocalBoundedObject {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

	// default logger.
	private static final SimLogger logger = SimLogger.getLogger(ConstructionSite.class.getName());
	
    // Construction site events.
    public static final String START_UNDERGOING_CONSTRUCTION_EVENT = "start undergoing construction";
    public static final String END_UNDERGOING_CONSTRUCTION_EVENT = "end undergoing construction";
    public static final String START_UNDERGOING_SALVAGE_EVENT = "start undergoing salvage";
    public static final String END_UNDERGOING_SALVAGE_EVENT = "end undergoing salvage";
    public static final String ADD_CONSTRUCTION_STAGE_EVENT = "adding construction stage";
    public static final String REMOVE_CONSTRUCTION_STAGE_EVENT = "removing construction stage";
    public static final String CREATE_BUILDING_EVENT = "creating new building";
    public static final String REMOVE_BUILDING_EVENT = "removing old building";

    // Data members
    private boolean undergoingConstruction;
    private boolean undergoingSalvage;
    private boolean manual;
    private boolean isSitePicked;
    private boolean isMousePickedUp;
    
	/** construction skill for this site. */
    private int constructionSkill;

    private double width;
    private double length;
    private LocalPosition position;
    private double facing;

    private transient List<ConstructionListener> listeners;

    private Collection<Worker> members;
    private List<GroundVehicle> vehicles;

    private ConstructionStage foundationStage;
    private ConstructionStage frameStage;
    private ConstructionStage buildingStage;
    private ConstructionManager constructionManager;
    private Settlement settlement;
    private ConstructionStageInfo stageInfo;

    private MissionPhase phase;
    
    /**
     * Constructor.
     */
    public ConstructionSite(Settlement settlement) {
    	super("A Construction Site", settlement.getCoordinates());
    	
    	this.constructionManager = settlement.getConstructionManager();
    	this.settlement = settlement;

    	width = 0D;
        length = 0D;
        position = LocalPosition.DEFAULT_POSITION;
        facing = 0D;
        foundationStage = null;
        frameStage = null;
        buildingStage = null;
        undergoingConstruction = false;
        undergoingSalvage = false;
        listeners = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public double getWidth() {
        return width;
    }

    /**
     * Sets the width of the construction site.
     * @param width the width (meters).
     */
    public void setWidth(double width) {
        this.width = width;
    }

    @Override
    public double getLength() {
        return length;
    }

    /**
     * Sets the length of the construction site.
     * @param length the length (meters).
     */
    public void setLength(double length) {
        this.length = length;
    }

    @Override
    public LocalPosition getPosition() {
    	return position;
    }
    
	public void setPosition(LocalPosition position2) {
		this.position = position2;
	}
	
    @Override
    public double getFacing() {
        return facing;
    }

    /**
     * Sets the facing of the construction site.
     * @param facing
     */
    public void setFacing(double facing) {
        this.facing = facing;
    }

    /**
     * Checks if all construction is complete at the site.
     * 
     * @return true if construction is complete.
     */
    public boolean isAllConstructionComplete() {
        if ((buildingStage != null) && !undergoingSalvage) return buildingStage.isComplete();
        else return false;
    }

    /**
     * Checks if all salvage is complete at the site.
     * 
     * @return true if salvage is complete.
     */
    public boolean isAllSalvageComplete() {
        if (undergoingSalvage) {
            if (foundationStage == null) return true;
            else return foundationStage.isComplete();
        }
        else return false;
    }

    /**
     * Checks if site is currently undergoing construction.
     * 
     * @return true if undergoing construction.
     */
    public boolean isUndergoingConstruction() {
        return undergoingConstruction;
    }

    /**
     * Checks if site is currently undergoing salvage.
     * 
     * @return true if undergoing salvage.
     */
    public boolean isUndergoingSalvage() {
        return undergoingSalvage;
    }

    /**
     * Sets if site is currently undergoing construction.
     * 
     * @param undergoingConstruction true if undergoing construction.
     */
    public void setUndergoingConstruction(boolean undergoingConstruction) {
        this.undergoingConstruction = undergoingConstruction;
        if (undergoingConstruction) fireConstructionUpdate(START_UNDERGOING_CONSTRUCTION_EVENT);
        else fireConstructionUpdate(END_UNDERGOING_CONSTRUCTION_EVENT);
    }

    /**
     * Sets if site is currently undergoing salvage.
     * 
     * @param undergoingSalvage true if undergoing salvage.
     */
    public void setUndergoingSalvage(boolean undergoingSalvage) {
        this.undergoingSalvage = undergoingSalvage;
        if (undergoingSalvage) fireConstructionUpdate(START_UNDERGOING_SALVAGE_EVENT);
        else fireConstructionUpdate(END_UNDERGOING_SALVAGE_EVENT);
    }

    /**
     * Gets the current construction stage at the site.
     * 
     * @return construction stage.
     */
    public ConstructionStage getCurrentConstructionStage() {
        ConstructionStage result = null;

        if (buildingStage != null) result = buildingStage;
        else if (frameStage != null) result = frameStage;
        else if (foundationStage != null) result = foundationStage;

        return result;
    }

    /**
     * Gets the next construction stage type.
     * 
     * @return next construction stage type or null if none.
     */
    public String getNextStageType() {
        String result = null;

        if (buildingStage != null) result = null;
        else if (frameStage != null) result = ConstructionStageInfo.BUILDING;
        else if (foundationStage != null) result = ConstructionStageInfo.FRAME;
        else result = ConstructionStageInfo.FOUNDATION;

        return result;
    }

    /**
     * Adds a new construction stage to the site.
     * 
     * @param stage the new construction stage.
     * @throws Exception if error adding construction stage.
     */
    public void addNewStage(ConstructionStage stage) {
        if (ConstructionStageInfo.FOUNDATION.equals(stage.getInfo().getType())) {
            if (foundationStage != null) throw new IllegalStateException("Foundation stage already exists.");
            foundationStage = stage;
        }
        else if (ConstructionStageInfo.FRAME.equals(stage.getInfo().getType())) {
            if (frameStage != null) throw new IllegalStateException("Frame stage already exists");
            if (foundationStage == null) throw new IllegalStateException("Foundation stage hasn't been added yet.");
            frameStage = stage;
        }
        else if (ConstructionStageInfo.BUILDING.equals(stage.getInfo().getType())) {
            if (buildingStage != null) throw new IllegalStateException("Building stage already exists");
            if (frameStage == null) throw new IllegalStateException("Frame stage hasn't been added yet.");
            buildingStage = stage;
        }
        else throw new IllegalStateException("Stage type: " + stage.getInfo().getType() + " not valid");

        // Update construction site dimensions.
        updateDimensions(stage);

        // Fire construction event.
        fireConstructionUpdate(ADD_CONSTRUCTION_STAGE_EVENT, stage);
    }

    /**
     * Updates the width and length dimensions to a construction stage.
     * 
     * @param stage the construction stage.
     */
    private void updateDimensions(ConstructionStage stage) {

        double stageWidth = stage.getInfo().getWidth();
        double stageLength = stage.getInfo().getLength();

        if (!stage.getInfo().isUnsetDimensions()) {
            if (stageWidth != width) {
                width = stageWidth;
            }
            if (stageLength != length) {
                length = stageLength;
            }
        }
        else {
            if ((stageWidth > 0D) && (stageWidth != width)) {
                width = stageWidth;
            }
            else if (width <= 0D) {
                // Use default width (may be modified later).
                width = 10D;
            }
            if ((stageLength > 0D) && (stageLength != length)) {
                length = stageLength;
            }
            else if (length <= 0D) {
                // Use default length (may be modified later).
                length = 10D;
            }
        }
    }

    /**
     * Removes a salvaged stage from the construction site.
     * 
     * @param stage the salvaged construction stage.
     * @throws Exception if error removing the stage.
     */
    public void removeSalvagedStage(ConstructionStage stage) {
        if (ConstructionStageInfo.BUILDING.equals(stage.getInfo().getType())) {
            buildingStage = null;
        }
        else if (ConstructionStageInfo.FRAME.equals(stage.getInfo().getType())) {
            frameStage = null;
        }
        else if (ConstructionStageInfo.FOUNDATION.equals(stage.getInfo().getType())) {
            foundationStage = null;
        }
        else throw new IllegalStateException("Stage type: " + stage.getInfo().getType() + " not valid");

        // Fire construction event.
        fireConstructionUpdate(REMOVE_CONSTRUCTION_STAGE_EVENT, stage);
    }

    /**
     * Removes the current salvaged construction stage.
     * 
     * @throws Exception if error removing salvaged construction stage.
     */
    public void removeSalvagedStage() {
        if (undergoingSalvage) {
            if (buildingStage != null) buildingStage = null;
            else if (frameStage != null) frameStage = null;
            else if (foundationStage != null) foundationStage = null;
            else throw new IllegalStateException("Construction site has no stage to remove");
        }
        else throw new IllegalStateException("Construction site is not undergoing salvage");
    }

    /**
     * Creates a new building from the construction site.
     * 
     * @param manager the settlement's building manager.
     * @return newly constructed building.
     * @throws Exception if error constructing building.
     */
    public Building createBuilding(int settlementID) {
        if (buildingStage == null) throw new IllegalStateException("Building stage doesn't exist");

        Settlement settlement = unitManager.getSettlementByID(settlementID);
        BuildingManager manager = settlement.getBuildingManager();
        int id = manager.getNextTemplateID();
        String buildingType = buildingStage.getInfo().getName();
        String uniqueName = manager.getBuildingNickName(buildingType);
        
        int zone = 0;
        
        Building newBuilding = new Building(id, zone, buildingType, uniqueName,
        		new BoundedObject(position, width, length, facing),
                settlement.getBuildingManager());
        
        manager.addBuilding(newBuilding, true);

        // Record completed building name.
        constructionManager = settlement.getConstructionManager();
        MarsClock timeStamp = new MarsClock(Simulation.instance().getMasterClock().getMarsClock());
        constructionManager.addConstructedBuildingLogEntry(buildingStage.getInfo().getName(), timeStamp);

        // Clear construction value cache.
        constructionManager.getConstructionValues().clearCache();

        // Fire construction event.
        fireConstructionUpdate(CREATE_BUILDING_EVENT, newBuilding);

        return newBuilding;
    }

    /**
     * Gets the building name the site will construct.
     * 
     * @return building name or null if undetermined.
     */
    public String getBuildingName() {
        if (buildingStage != null) return buildingStage.getInfo().getName();
        else return null;
    }

    /**
     * Checks if the site's current stage is unfinished.
     * 
     * @return true if stage unfinished.
     */
    public boolean hasUnfinishedStage() {
        ConstructionStage currentStage = getCurrentConstructionStage();
        return (currentStage != null) && !currentStage.isComplete();
    }

    /**
     * Checks if this site contains a given stage.
     * 
     * @param stage the stage info.
     * @return true if contains stage.
     */
    public boolean hasStage(ConstructionStageInfo stage) {
        if (stage == null) throw new IllegalArgumentException("stage cannot be null");

        boolean result = false;
        if ((foundationStage != null) && foundationStage.getInfo().equals(stage)) result = true;
        else if ((frameStage != null) && frameStage.getInfo().equals(stage)) result = true;
        else if ((buildingStage != null) && buildingStage.getInfo().equals(stage)) result = true;

        return result;
    }

    /**
     * Adds a listener.
     * 
     * @param newListener the listener to add.
     */
    public final void addConstructionListener(ConstructionListener newListener) {
        if (listeners == null)
            listeners = Collections.synchronizedList(new ArrayList<>());
        if (!listeners.contains(newListener)) listeners.add(newListener);
    }

    /**
     * Removes a listener.
     * 
     * @param oldListener the listener to remove.
     */
    public final void removeConstructionListener(ConstructionListener oldListener) {
        if (listeners == null)
            listeners = Collections.synchronizedList(new ArrayList<>());
        if (listeners.contains(oldListener)) listeners.remove(oldListener);
    }

    /**
     * Fires a construction update event.
     * 
     * @param updateType the update type.
     */
    final void fireConstructionUpdate(String updateType) {
        fireConstructionUpdate(updateType, null);
    }

    /**
     * Fires a construction update event.
     * 
     * @param updateType the update type.
     * @param target the event target or null if none.
     */
    final void fireConstructionUpdate(String updateType, Object target) {
        if (listeners == null)
            listeners = Collections.synchronizedList(new ArrayList<>());
        synchronized(listeners) {
            Iterator<ConstructionListener> i = listeners.iterator();
            while (i.hasNext()) i.next().constructionUpdate(
                    new ConstructionEvent(this, updateType, target));
        }
    }

    /**
     * Relocates the construction site by changing its coordinates.
     */
	public void relocateSite() {
		Coordinates coord = getCoordinates();
		BuildingConstructionMission.positionNewSite(this);
		logger.info(this, "Manually relocated by player from " 
				+ coord.getFormattedString() + " to "
				+ getCoordinates().getFormattedString());
	}

    public ConstructionManager getConstructionManager() {
    	return constructionManager;
    }

	/**
	 * Gets the associated settlement this unit is with
	 *
	 * @return the associated settlement
	 */
	public Settlement getAssociatedSettlement() {
		return settlement;
	}

	/**
	 * Gets the settlement this unit is with.
	 *
	 * @return the settlement
	 */
    public Settlement getSettlement() {
    	return settlement;
    }

    public void setSkill(int constructionSkill) {
    	this.constructionSkill = constructionSkill;
    }

    public int getSkill() {
    	return constructionSkill;
    }

	public void setMembers(Collection<Worker> members) {
		this.members = members;
	}

	public void setVehicles(List<GroundVehicle> vehicles) {
		this.vehicles = vehicles;
	}

	public Collection<Worker> getMembers() {
		return members;
	}

	public List<GroundVehicle> getVehicles() {
		return vehicles;
	}

	public ConstructionStageInfo getStageInfo() {
		return stageInfo;
	}

	public void setStageInfo(ConstructionStageInfo stageInfo) {
		this.stageInfo = stageInfo;
	}

	public boolean getManual() {
		return manual;
	}

	public void setManual(boolean manual) {
		this.manual = manual;
	}

	// for triggering the alertDialog()
	public boolean isSitePicked() {
		return isSitePicked;
	}

	public void setSitePicked(boolean value) {
		this.isSitePicked = value;
	}

	public boolean isMousePicked() {
		return isMousePickedUp;
	}

	public void setMousePicked(boolean value) {
		isMousePickedUp = value;
	}

	@Override
	public UnitType getUnitType() {
		return UnitType.CONSTRUCTION;
	}

	/**
	 * Is this unit inside a settlement ?
	 *
	 * @return true if the unit is inside a settlement
	 */
	@Override
	public boolean isInSettlement() {
		return false;
	}

	@Override
    public String toString() {
		StringBuilder result = new StringBuilder("Site");

		ConstructionStage stage = getCurrentConstructionStage();
		if (stage != null) {
			result.append(": ").append(stage.getInfo().getName());
			if (undergoingConstruction) result.append(" - Under Construction");
			else if (undergoingSalvage) result.append(" - Under Salvage");
			else if (hasUnfinishedStage()) {
				if (stage.isSalvaging()) result.append(" - Salvage Unfinished");
				else result.append(" - Construction Unfinished");
			}
		}

		return result.toString();
	}
	
	public void setPhase(MissionPhase phase) {
		this.phase = phase;
	}
	
	public MissionPhase getPhase() {
		return phase;
	}
	
	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		position = null;
	    members = null;
	    vehicles = null;
	    foundationStage = null;
	    frameStage = null;
	    buildingStage = null;
	    constructionManager = null;
	    settlement = null;
	    stageInfo = null;
	}
}
