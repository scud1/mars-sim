/*
 * Mars Simulation Project
 * MedicalCare.java
 * @date 2021-12-22
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.RequestMedicalTreatment;
import org.mars_sim.msp.core.person.ai.task.TreatMedicalPatient;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.health.HealthProblem;
import org.mars_sim.msp.core.person.health.MedicalAid;
import org.mars_sim.msp.core.person.health.MedicalStation;
import org.mars_sim.msp.core.person.health.Treatment;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.FunctionSpec;

/**
 * The MedicalCare class represents a building function for providing medical
 * care.
 */
public class MedicalCare extends Function implements MedicalAid {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private MedicalStation medicalStation;

	/**
	 * Constructor.
	 * 
	 * @param building the building this function is for.
	 * @param spec Specification of Function
	 * @throws BuildingException if function could not be constructed.
	 */
	public MedicalCare(Building building, FunctionSpec spec) {
		// Use Function constructor.
		super(FunctionType.MEDICAL_CARE, spec, building);

		int techLevel = spec.getTechLevel();
		int beds = spec.getCapacity();
		medicalStation = new MedicalStation(building.getName(), techLevel, beds);

		// NOTE: will need to distinguish between activity spots and bed locations
		// Load bed locations by loadBedLocations(buildingConfig.getMedicalCareBedLocations(building.getBuildingType()))
	}

	/**
	 * Gets the value of the function for a named building type.
	 * 
	 * @param type the building type.
	 * @param newBuilding  true if adding a new building.
	 * @param settlement   the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String type, boolean newBuilding, Settlement settlement) {

		// Demand is 5 medical points per inhabitant.
		double demand = settlement.getNumCitizens() * 5D;

		double supply = 0D;
		boolean removedBuilding = false;
		for(Building building : settlement.getBuildingManager().getBuildings(FunctionType.MEDICAL_CARE)) {
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(type) && !removedBuilding) {
				removedBuilding = true;
			} else {
				MedicalCare medFunction = building.getMedical();
				double tech = medFunction.getTechLevel();
				double beds = medFunction.getSickBedNum();
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				supply += (tech * tech) * beds * wearModifier;
			}
		}

		double medicalPointValue = demand / (supply + 1D) / 10D;

		FunctionSpec fSpec = buildingConfig.getFunctionSpec(type, FunctionType.MEDICAL_CARE);
		double tech = fSpec.getTechLevel();
		double beds = fSpec.getCapacity();
		double medicalPoints = (tech * tech) * beds;

		return medicalPoints * medicalPointValue;
	}

	/**
	 * Gets the number of sick beds.
	 * 
	 * @return Sick bed count.
	 */
	public int getSickBedNum() {
		return medicalStation.getSickBedNum();
	}

	/**
	 * Gets the current number of people being treated here.
	 * 
	 * @return Patient count.
	 */
	public int getPatientNum() {
		return medicalStation.getPatientNum();
	}
	
	/**
	 * Checks if there are any empty beds for new patients
	 * 
	 * @return true or false
	 */
	public boolean hasEmptyBeds() {
        return getPatientNum() < getSickBedNum();
	}
	
	/**
	 * Gets the patients at this medical station.
	 * 
	 * @return Collection of People.
	 */
	public Collection<Person> getPatients() {
		return medicalStation.getPatients();
	}

	/**
	 * Gets the number of people using this medical aid to treat sick people.
	 * 
	 * @return number of people
	 */
	public int getPhysicianNum() {
		int result = 0;

		if (getBuilding().hasFunction(FunctionType.LIFE_SUPPORT)) {
			LifeSupport lifeSupport = getBuilding().getLifeSupport();
			Iterator<Person> i = lifeSupport.getOccupants().iterator();
			while (i.hasNext()) {
				Task task = i.next().getMind().getTaskManager().getTask();
				if (task instanceof TreatMedicalPatient) {
					MedicalAid aid = ((TreatMedicalPatient) task).getMedicalAid();						
					if ((aid != null) && (aid == this))
						result++;
				}
				else if (task instanceof RequestMedicalTreatment) {
					MedicalAid aid = ((RequestMedicalTreatment) task).getMedicalAid();						
					if ((aid != null) && (aid == this))
						result++;
				}	
			}
		}

		return result;
	}

	@Override
	public List<HealthProblem> getProblemsAwaitingTreatment() {
		return medicalStation.getProblemsAwaitingTreatment();
	}

	@Override
	public List<HealthProblem> getProblemsBeingTreated() {
		return medicalStation.getProblemsBeingTreated();
	}

	@Override
	public List<Treatment> getSupportedTreatments() {
		return medicalStation.getSupportedTreatments();
	}

	@Override
	public boolean canTreatProblem(HealthProblem problem) {
		return medicalStation.canTreatProblem(problem);
	}

	@Override
	public void requestTreatment(HealthProblem problem) {
		medicalStation.requestTreatment(problem);
	}

	@Override
	public void cancelRequestTreatment(HealthProblem problem) {
		medicalStation.cancelRequestTreatment(problem);
	}

	@Override
	public void startTreatment(HealthProblem problem, double treatmentDuration) {
		medicalStation.startTreatment(problem, treatmentDuration);
	}

	@Override
	public void stopTreatment(HealthProblem problem) {
		medicalStation.stopTreatment(problem);
	}

	@Override
	public List<Person> getRestingRecoveryPeople() {
		return medicalStation.getRestingRecoveryPeople();
	}

	@Override
	public void startRestingRecovery(Person person) {
		medicalStation.startRestingRecovery(person);
	}

	@Override
	public void stopRestingRecovery(Person person) {
		medicalStation.stopRestingRecovery(person);
	}

	/**
	 * Gets the treatment level.
	 * 
	 * @return treatment level
	 */
	public int getTechLevel() {
		return medicalStation.getTreatmentLevel();
	}

	@Override
	public double getMaintenanceTime() {
		double result = 0D;
		// Add maintenance for treatment level.
		result += medicalStation.getTreatmentLevel() * 10D;
		// Add maintenance for number of sick beds.
		result += medicalStation.getSickBedNum() * 10D;

		return result;
	}

	@Override
	public void destroy() {
		super.destroy();

		medicalStation = null;
	}
}
