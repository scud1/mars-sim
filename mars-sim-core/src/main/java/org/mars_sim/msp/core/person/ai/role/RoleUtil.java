/*
 * Mars Simulation Project
 * RoleUtil.java
 * @date 2021-09-27
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.role;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.util.Job;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.job.util.JobUtil;
import org.mars_sim.msp.core.person.ai.training.TrainingType;
import org.mars_sim.msp.core.person.ai.training.TrainingUtils;
import org.mars_sim.msp.core.structure.ChainOfCommand;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * The RoleUtil class determines the roles of the settlers in a simulation.
 */
public class RoleUtil implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static Map<JobType, Map<RoleType, Double>> roleWeights
							= new EnumMap<>(JobType.class);

	private static List<RoleType> specalistsRoles;

	public RoleUtil() {
		// nothing
	}

	/**
	 * Initializes the role prospect array.
	 */
	public static void initialize() {
		if (roleWeights.isEmpty()) {
			for (Job j : JobUtil.getJobs()) {
				JobType id = j.getType();
				roleWeights.put(id, j.getRoleProspects());
			}
		}

		// Cache the specialists
		specalistsRoles = Collections.unmodifiableList(
							Arrays.stream(RoleType.values())
								.filter(RoleType::isSpecialist)
								.collect(Collectors.toList()));
	}

	public static boolean isRoleWeightsInitialized() {
        return !roleWeights.isEmpty();
	}

	/**
	 * Returns a list of specialist roles.
	 * 
	 * @return
	 */
	public static List<RoleType> getSpecialists() {
		return specalistsRoles;
	}

	/**
	 * Finds the best role for a given person in a settlement.
	 *
	 * @param settlement
	 * @param p
	 * @return
	 */
	public static RoleType findBestRole(Person p) {
		RoleType selectedRole = null;
		double highestWeight = 0;

		ChainOfCommand chain = p.getSettlement().getChainOfCommand();
		JobType job = p.getMind().getJob();
		Map<RoleType, Double> weights = roleWeights.get(job);

		// Use a Tree map so entries sorting in increasing order.
		List<RoleType> roles = new ArrayList<>();
		int leastNum = 0;
		RoleType leastFilledRole = null;
		for (RoleType rt: RoleType.values()) {
			if (rt.isSpecialist()) {
				int num = chain.getNumFilled(rt);
				if (leastNum >= num) {
					leastNum = num;
					leastFilledRole = rt;
				}
				roles.add(rt);
			}
		}

		// Move the least polluted role to the front
		if (leastFilledRole != null) {
			roles.remove(leastFilledRole);
			roles.add(0, leastFilledRole);
		}

		for (RoleType rt : roles) {
			boolean isRoleAvailable = chain.isRoleAvailable(rt);

			if (isRoleAvailable) {
				double jobScore = weights.get(rt);
				double trainingScore = getTrainingScore(p, rt);
				double totalScore = jobScore + trainingScore;

				if (highestWeight < totalScore) {
					highestWeight = totalScore;
					// Pick the role based on the highest weight
					selectedRole = rt;
				}
			}
		}

		// No role ????
		if (selectedRole == null) {
			int idx = RandomUtil.getRandomInt(roles.size() - 1);
			selectedRole = roles.get(idx);
		}
		return selectedRole;
	}



	/**
	 * Finds the person who is the best fit for a given role from a pool of candidates.
	 *
	 * @param role
	 * @param candidates
	 * @return
	 */
	public static Person findBestFit(RoleType role, List<Person> candidates) {
		Person bestPerson = null;
		double bestScore = 0;

		for (Person p : candidates) {

			JobType job = p.getMind().getJob();
			Map<RoleType, Double> weights = roleWeights.get(job);

			double score = getRolePropectScore(p, role, weights);

			if ((bestPerson == null) || (score > bestScore)) {
				bestScore = score;
				bestPerson = p;
			}
		}

		return bestPerson;
	}

	/**
	 * Gets the role prospect score of a person on a particular role.
	 *
	 * @param person
	 * @param role
	 * @return the role prospect score
	 */
	public static double getRolePropectScore(Person person, RoleType role, Map<RoleType, Double> weights) {

		double jobScore = Math.round(weights.get(role) * 10.0)/10.0;

		double trainingScore = getTrainingScore(person, role);

		return jobScore + trainingScore;

	}

	/**
	 * Gets the training score of a person on a particular role.
	 *
	 * @param person
	 * @param role
	 * @return the training score
	 */
	public static double getTrainingScore(Person person, RoleType role) {

		List<TrainingType> trainings = person.getTrainings();

		int trainingScore = 0;
		for (TrainingType tt : trainings) {
			trainingScore += TrainingUtils.getModifier(role, tt);
		}

		return trainingScore;

	}

	/**
	 * Takes a Chief role type and return the associated specialty role.
	 * If the input role is not a Chief a null is returned.
	 * 
	 * @param roleType
	 * @return
	 */
	public static RoleType getChiefSpeciality(RoleType roleType) {
		RoleType candidateType = null;
		switch (roleType) {
	        case CHIEF_OF_AGRICULTURE:
	            candidateType = RoleType.AGRICULTURE_SPECIALIST;
	            break;

	        case CHIEF_OF_COMPUTING:
	        	candidateType = RoleType.COMPUTING_SPECIALIST;
	        	break;

	        case CHIEF_OF_ENGINEERING:
	        	candidateType = RoleType.ENGINEERING_SPECIALIST;
	        	break;

	        case CHIEF_OF_LOGISTICS_N_OPERATIONS:
	        	candidateType = RoleType.LOGISTIC_SPECIALIST;
	        	break;

	        case CHIEF_OF_MISSION_PLANNING:
	        	candidateType = RoleType.MISSION_SPECIALIST;
	        	break;

	        case CHIEF_OF_SAFETY_N_HEALTH:
	        	candidateType = RoleType.SAFETY_SPECIALIST;
	        	break;

	        case CHIEF_OF_SCIENCE:
	        	candidateType = RoleType.SCIENCE_SPECIALIST;
	        	break;

	        case CHIEF_OF_SUPPLY_N_RESOURCES:
	        	candidateType = RoleType.RESOURCE_SPECIALIST;
	        	break;

	        default:
	    }
		return candidateType;
	}

	/**
	 * Gets a list of role type name strings.
	 *
	 * @return
	 */
	public static List<String> getRoleNames(int pop) {

		List<String> roleNames = new ArrayList<>();

		if (pop <= ChainOfCommand.POPULATION_WITH_COMMANDER) {
			roleNames.add(RoleType.COMMANDER.getName());
			for (RoleType r : RoleUtil.getSpecialists()) {
				roleNames.add(r.getName());
			}
		}

		else if (pop <= ChainOfCommand.POPULATION_WITH_SUB_COMMANDER) {
			roleNames.add(RoleType.COMMANDER.getName());
			roleNames.add(RoleType.SUB_COMMANDER.getName());
			for (RoleType r : RoleUtil.getSpecialists()) {
				roleNames.add(r.getName());
			}
		}

		else if (pop <= ChainOfCommand.POPULATION_WITH_CHIEFS) {
			for (RoleType r : RoleType.values()) {
				if (r != RoleType.MAYOR || r != RoleType.PRESIDENT)
					roleNames.add(r.getName());
			}
		}

		else if (pop <= ChainOfCommand.POPULATION_WITH_MAYOR) {
			for (RoleType r : RoleType.values()) {
				if (r != RoleType.PRESIDENT)
					roleNames.add(r.getName());
			}
		}

		else {
			for (RoleType r : RoleType.values()) {
				roleNames.add(r.getName());
			}
		}

		Collections.sort(roleNames);
		return roleNames;
	}
	
	public static Map<JobType, Map<RoleType, Double>> getRoleWeights() {
		return roleWeights;
	}

}
