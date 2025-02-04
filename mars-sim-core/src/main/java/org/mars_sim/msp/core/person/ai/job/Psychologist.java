/*
 * Mars Simulation Project
 * Psychologist.java
 * @date 2021-09-27
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.job;

import java.util.Iterator;
import java.util.List;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.NaturalAttributeManager;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.util.Job;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;

/**
 * The Psychologist class represents a job for evaluating a person's mind and behavior.
 */
public class Psychologist extends Job {
	
	/** Constructor. */
	public Psychologist() {
		// Use Job constructor
		super(JobType.PSYCHOLOGIST, Job.buildRoleMap(5.0, 0.0, 5.0, 25.0, 20.0, 10.0, 15.0, 20.0));
	}

	/**
	 * Gets a person's capability to perform this job.
	 * 
	 * @param person the person to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Person person) {

		double result = person.getSkillManager().getSkillLevel(SkillType.PSYCHOLOGY);

		NaturalAttributeManager attributes = person.getNaturalAttributeManager();
		int academicAptitude = attributes.getAttribute(NaturalAttributeType.ACADEMIC_APTITUDE);
		int experienceAptitude = attributes.getAttribute(NaturalAttributeType.EXPERIENCE_APTITUDE);
		double averageAptitude = (academicAptitude + experienceAptitude) / 2D;
		result += result * ((averageAptitude - 100D) / 100D);

		if (person.getPhysicalCondition().hasSeriousMedicalProblems())
			result = 0D;

		return result;
	}

	/**
	 * Gets the base settlement need for this job.
	 * 
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public double getSettlementNeed(Settlement settlement) {

		double result = .1;

		// Add total population / 10
		int population = settlement.getNumCitizens();

		// Add (labspace * tech level) / 2 for all labs with medical specialties.
		result += getBuildingScienceDemand(settlement, ScienceType.PSYCHOLOGY, 6D);

		// Add (tech level / 2) for all medical infirmaries.
		List<Building> medicalBuildings = settlement.getBuildingManager().getBuildings(FunctionType.MEDICAL_CARE);
		Iterator<Building> j = medicalBuildings.iterator();
		while (j.hasNext()) {
			Building building = j.next();
			MedicalCare infirmary = building.getMedical();
			result += (double) infirmary.getTechLevel() / 7D;
		}

		result = (result + population / 12D) / 5.0;
				
		return result;
	}
}
