/*
 * Mars Simulation Project
 * Medicbot.java
 * @date 2022-09-01
 * @author Manny Kung
 */
package org.mars_sim.msp.core.robot.ai.job;

import java.util.Iterator;
import java.util.List;

import org.mars_sim.msp.core.person.ai.NaturalAttributeManager;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;

/** 
 * The Medicbot class represents a job for an medical treatment expert.
 */
public class Medicbot
extends RobotJob {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public Medicbot() {
		// Use Job constructor
		super();
	}

	/**
	 * Gets a robot's capability to perform this job.
	 * @param robot the robot to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Robot robot) {

		double result = 0D;

		int kkill = robot.getSkillManager().getSkillLevel(SkillType.MEDICINE);
		result = kkill;

		NaturalAttributeManager attributes = robot.getNaturalAttributeManager();
		int experienceAptitude = attributes.getAttribute(NaturalAttributeType.EXPERIENCE_APTITUDE);
		result+= result * ((experienceAptitude - 50D) / 100D);

		return result;
	}

	/**
	 * Gets the base settlement need for this job.
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public double getSettlementNeed(Settlement settlement) {

		double result = 1D;

		// Add total population / 10
		int population = settlement.getNumCitizens();
		result+= population / 15D; // changed from /10D to /15D

		// Add (tech level / 2) for all medical infirmaries.
		List<Building> medicalBuildings = settlement.getBuildingManager().getBuildings(FunctionType.MEDICAL_CARE);
		Iterator<Building> j = medicalBuildings.iterator();
		while (j.hasNext()) {
			Building building = j.next();
			MedicalCare infirmary = (MedicalCare) building.getFunction(FunctionType.MEDICAL_CARE);
			result+= (double) infirmary.getTechLevel() ;
		}			

		return result;	
	}

}
