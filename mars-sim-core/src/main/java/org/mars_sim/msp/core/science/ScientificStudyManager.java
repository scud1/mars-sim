/**
 * Mars Simulation Project
 * ScientificStudyManager.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.science;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.Temporal;

/**
 * A class that keeps track of all scientific studies in the simulation.
 */
public class ScientificStudyManager // extends Thread
		implements Serializable, Temporal {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final Logger logger = Logger.getLogger(ScientificStudyManager.class.getName());
	
	// Data members
	private List<ScientificStudy> studies;

	/**
	 * Constructor.
	 */
	public ScientificStudyManager() {
		studies = new CopyOnWriteArrayList<ScientificStudy>();
	}

	/**
	 * Creates a new scientific study.
	 * 
	 * @param researcher      the primary researcher.
	 * @param science         the primary field of science.
	 * @param difficultyLevel the difficulty level of the study.
	 * @return the created study.
	 */
	public synchronized ScientificStudy createScientificStudy(Person researcher, ScienceType science, int difficultyLevel) {
		if (researcher == null)
			throw new IllegalArgumentException("Researcher cannot be null");
		if (science == null)
			throw new IllegalArgumentException("Science cannot be null");
		if (difficultyLevel < 0)
			throw new IllegalArgumentException("difficultyLevel must be positive value");

		String name = science.getName() + " #" + (studies.size() + 1);
		ScientificStudy study = new ScientificStudy(name, researcher, science, difficultyLevel);
		studies.add(study);

		logger.fine(researcher.getName() + " began writing proposal for new " + study.toString());

		return study;
	}

	/**
	 * Gets all ongoing scientific studies.
	 * 
	 * @return list of studies.
	 */
	public List<ScientificStudy> getOngoingStudies() {
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (!study.isCompleted())
				result.add(study);
		}
		return result;
	}

	/**
	 * Gets all completed scientific studies, regardless of completion state.
	 * 
	 * @return list of studies.
	 */
	public List<ScientificStudy> getCompletedStudies() {
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (study.isCompleted())
				result.add(study);
		}
		return result;
	}

	/**
	 * Gets all successfully completed scientific studies.
	 * 
	 * @return list of studies.
	 */
	public List<ScientificStudy> getSuccessfulStudies() {
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (study.isCompleted() && study.getCompletionState().equals(ScientificStudy.SUCCESSFUL_COMPLETION))
				result.add(study);
		}
		return result;
	}

	/**
	 * Gets all failed completed scientific studies.
	 * 
	 * @return list of studies.
	 */
	public List<ScientificStudy> getFailedStudies() {
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (study.isCompleted() && study.getCompletionState().equals(ScientificStudy.FAILED_COMPLETION))
				result.add(study);
		}
		return result;
	}

	/**
	 * Gets all canceled scientific studies.
	 * 
	 * @return list of studies.
	 */
	public List<ScientificStudy> getCanceledStudies() {
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (study.isCompleted() && study.getCompletionState().equals(ScientificStudy.CANCELED))
				result.add(study);
		}
		return result;
	}

	/**
	 * Gets the researcher's ongoing primary research scientific study, if any.
	 * 
	 * @param researcher the primary researcher.
	 * @return primary research scientific study or null if none.
	 */
	public ScientificStudy getOngoingPrimaryStudy(Person researcher) {
		ScientificStudy result = null;
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (!study.isCompleted() && (study.getPrimaryResearcher().equals(researcher)))
				result = study;
		}
		return result;
	}

	/**
	 * Gets the number of all completed scientific studies where researcher was the primary
	 * researcher.
	 * 
	 * @param researcher the primary researcher.
	 * @return the number of studies.
	 */
	public int getNumCompletedPrimaryStudies(Person researcher) {
		int result = 0;
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (study.isCompleted() && (study.getPrimaryResearcher().equals(researcher)))
				result++;
		}
		return result;
	}
	
	/**
	 * Gets all completed scientific studies where researcher was the primary
	 * researcher.
	 * 
	 * @param researcher the primary researcher.
	 * @return list of studies.
	 */
	public List<ScientificStudy> getCompletedPrimaryStudies(Person researcher) {
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (study.isCompleted() && (study.getPrimaryResearcher().equals(researcher)))
				result.add(study);
		}
		return result;
	}

	/**
	 * Gets all ongoing scientific studies where researcher is a collaborative
	 * researcher.
	 * 
	 * @param researcher the collaborative researcher.
	 * @return list of studies.
	 */
	public List<ScientificStudy> getOngoingCollaborativeStudies(Person researcher) {
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (!study.isCompleted() && (study.getCollaborativeResearchers().contains(researcher)))
				result.add(study);
		}
		return result;
	}

	/**
	 * Gets all ongoing scientific studies where researcher was a collaborative
	 * researcher in a particular settlement.
	 * 
	 * @param settlement
	 * @return list of studies.
	 */
	public List<ScientificStudy> getOngoingCollaborativeStudies(Settlement settlement, ScienceType type) {
		boolean allSubject = false;
		if (type == null)
			allSubject = true;
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();

		List<Person> pList = new CopyOnWriteArrayList<>(settlement.getAllAssociatedPeople());

		for (Person p : pList) {
			Iterator<ScientificStudy> i = studies.iterator();
			while (i.hasNext()) {
				ScientificStudy study = i.next();
				if (allSubject || type == study.getScience()) {
					if (!study.isCompleted() && (study.getCollaborativeResearchers().contains(p)))
						result.add(study);
				}
			}
		}
		return result;
	}

	/**
	 * Gets the number of all completed scientific studies where researcher was a collaborative
	 * researcher.
	 * 
	 * @param researcher the collaborative researcher.
	 * @return a number
	 */
	public int getNumCompletedCollaborativeStudies(Person researcher) {
		int result = 0;
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (study.isCompleted() && (study.getCollaborativeResearchers().contains(researcher)))
				result++;
		}
		return result;
	}
	
	/**
	 * Gets all completed scientific studies where researcher was a collaborative
	 * researcher.
	 * 
	 * @param researcher the collaborative researcher.
	 * @return list of studies.
	 */
	public List<ScientificStudy> getCompletedCollaborativeStudies(Person researcher) {
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (study.isCompleted() && (study.getCollaborativeResearchers().contains(researcher)))
				result.add(study);
		}
		return result;
	}

	/**
	 * Gets all completed scientific studies where researcher was a collaborative
	 * researcher in a particular settlement.
	 * 
	 * @param settlement
	 * @return list of studies.
	 */
	public List<ScientificStudy> getCompletedCollaborativeStudies(Settlement settlement, ScienceType type) {
		boolean allSubject = false;
		if (type == null)
			allSubject = true;
		
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();

		List<Person> pList = new CopyOnWriteArrayList<>(settlement.getAllAssociatedPeople());

		for (Person p : pList) {
			Iterator<ScientificStudy> i = studies.iterator();
			while (i.hasNext()) {
				ScientificStudy study = i.next();
				if (allSubject || type == study.getScience()) {
					if (study.isCompleted() && (study.getCollaborativeResearchers().contains(p)))
						result.add(study);
				}
			}
		}
		return result;
	}

	/**
	 * Gets all ongoing scientific studies at a primary research settlement.
	 * 
	 * @param settlement the primary research settlement.
	 * @return list of studies.
	 */
	public List<ScientificStudy> getOngoingPrimaryStudies(Settlement settlement, ScienceType type) {
		boolean allSubject = false;
		if (type == null)
			allSubject = true;
		
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (allSubject || type == study.getScience()) {
				if (!study.isCompleted() && settlement.equals(study.getPrimarySettlement()))
					result.add(study);
			}
		}
		return result;
	}

	/**
	 * Gets all completed scientific studies at a primary research settlement.
	 * 
	 * @param settlement the primary research settlement.
	 * @return list of studies.
	 */
	public List<ScientificStudy> getCompletedPrimaryStudies(Settlement settlement, ScienceType type) {
		boolean allSubject = false;
		if (type == null)
			allSubject = true;
		
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (allSubject || type == study.getScience()) {
				if (study.isCompleted() && settlement.equals(study.getPrimarySettlement()))
					result.add(study);
			}
		}
		return result;
	}

	/**
	 * Gets all failed scientific studies at a primary research settlement.
	 * 
	 * @param settlement the primary research settlement.
	 * @return list of studies.
	 */
	public List<ScientificStudy> getAllFailedStudies(Settlement settlement, ScienceType type) {
		boolean allSubject = false;
		if (type == null)
			allSubject = true;
		
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (allSubject || type == study.getScience()) {
				if (study.isCompleted() && study.getCompletionState().equals(ScientificStudy.FAILED_COMPLETION)
						&& settlement.equals(study.getPrimarySettlement()))
					result.add(study);
			}
		}
		return result;
	}

	/**
	 * Gets all successful scientific studies at a primary research settlement.
	 * 
	 * @param settlement the primary research settlement.
	 * @return list of studies.
	 */
	public List<ScientificStudy> getAllSuccessfulStudies(Settlement settlement, ScienceType type) {
		boolean allSubject = false;
		if (type == null)
			allSubject = true;
		
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (allSubject || type == study.getScience()) {
				if (study.isCompleted() && study.getCompletionState().equals(ScientificStudy.SUCCESSFUL_COMPLETION)
						&& settlement.equals(study.getPrimarySettlement()))
					result.add(study);
			}
		}
		return result;
	}

	/**
	 * Gets all canceled scientific studies at a primary research settlement.
	 * 
	 * @param settlement the primary research settlement.
	 * @return list of studies.
	 */
	public List<ScientificStudy> getAllCanceledStudies(Settlement settlement, ScienceType type) {
		boolean allSubject = false;
		if (type == null)
			allSubject = true;
		
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (allSubject || type == study.getScience()) {
				if (study.isCompleted() && study.getCompletionState().equals(ScientificStudy.CANCELED)
						&& settlement.equals(study.getPrimarySettlement()))
					result.add(study);
			}
		}
		return result;
	}
	
	/**
	 * Gets all studies that have open invitations for collaboration for a
	 * researcher.
	 * 
	 * @param collaborativeResearcher the collaborative researcher.
	 * @return list of studies.
	 */
	public List<ScientificStudy> getOpenInvitationStudies(Person collaborativeResearcher) {
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (!study.isCompleted() && study.getPhase().equals(ScientificStudy.INVITATION_PHASE)) {
				if (study.getInvitedResearchers().contains(collaborativeResearcher)) {
					if (!study.hasInvitedResearcherResponded(collaborativeResearcher))
						result.add(study);
				}
			}
		}
		return result;
	}

	/**
	 * Gets a list of all studies a researcher is involved with.
	 * 
	 * @param researcher the researcher.
	 * @return list of scientific studies.
	 */
	public List<ScientificStudy> getAllStudies(Person researcher) {
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();

		// Add ongoing primary study.
		ScientificStudy primaryStudy = getOngoingPrimaryStudy(researcher);
		if (primaryStudy != null)
			result.add(primaryStudy);

		// Add any ongoing collaborative studies.
		List<ScientificStudy> collaborativeStudies = getOngoingCollaborativeStudies(researcher);
		result.addAll(collaborativeStudies);

		// Add completed primary studies.
		List<ScientificStudy> completedPrimaryStudies = getCompletedPrimaryStudies(researcher);
		result.addAll(completedPrimaryStudies);

		// Add completed collaborative studies.
		List<ScientificStudy> completedCollaborativeStudies = getCompletedCollaborativeStudies(researcher);
		result.addAll(completedCollaborativeStudies);

		return result;
	}

	/**
	 * Gets a list of all studies a settlement is primary for.
	 * 
	 * @param settlement the settlement.
	 * @return list of scientific studies.
	 */
	public List<ScientificStudy> getAllStudies(Settlement settlement) {
		List<ScientificStudy> result = new CopyOnWriteArrayList<ScientificStudy>();

		// Add any ongoing primary studies.
		List<ScientificStudy> primaryStudies = getOngoingPrimaryStudies(settlement, null);
		result.addAll(primaryStudies);

		// Add any completed primary studies.
		List<ScientificStudy> completedPrimaryStudies = getCompletedPrimaryStudies(settlement, null);
		result.addAll(completedPrimaryStudies);

		return result;
	}

	/**
	 * Update all of the studies.
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			ScientificStudy study = i.next();
			if (!study.isCompleted()) {

				study.timePassing(pulse);
			}
		}
		
		return true;
	}

	public double getPhaseScore(ScientificStudy ss) {
		if (ss.getPhase().equals(ScientificStudy.PROPOSAL_PHASE)) {
			return .5;
		}
		
		else if (ss.getPhase().equals(ScientificStudy.INVITATION_PHASE)) {
			return 1.0;
		}
		
		if (ss.getPhase().equals(ScientificStudy.RESEARCH_PHASE)) {
			return 1.5;
		}
		
		else if (ss.getPhase().equals(ScientificStudy.PAPER_PHASE)) {
			return 2.0;
		}
		
		else if (ss.getPhase().equals(ScientificStudy.PEER_REVIEW_PHASE)) {
			return 2.5;
		}
		
		return 0;
	}
	
	public int getPhaseType(ScientificStudy ss) {
		if (ss.getPhase().equals(ScientificStudy.PROPOSAL_PHASE)) {
			return 0;
		}
		
		else if (ss.getPhase().equals(ScientificStudy.INVITATION_PHASE)) {
			return 1;
		}
		
		if (ss.getPhase().equals(ScientificStudy.RESEARCH_PHASE)) {
			return 2;
		}
		
		else if (ss.getPhase().equals(ScientificStudy.PAPER_PHASE)) {
			return 3;
		}
		
		else if (ss.getPhase().equals(ScientificStudy.PEER_REVIEW_PHASE)) {
			return 4;
		}
		
		return 5;
	}
	
	/**
	 * Computes the overall relationship score of a settlement
	 * 
	 * @param s Settlement
	 * @param type {@link ScienceType} if null, query all science types
	 * @return the score
	 */
	public double getScienceScore(Settlement s, ScienceType type) {
		boolean allSubject = false;
		if (type == null)
			allSubject = true;

		double score = 0;
		
		double succeed = 3;	
		double failed = 1;
		double canceled = 0.5;
		double oPri ;
		double oCol ;
		
//		List<ScientificStudy> list0 = getCompletedPrimaryStudies(s);
//		if (!list0.isEmpty()) {
//			for (ScientificStudy ss : list0) {
//				if (allSubject || type == ss.getScience()) {				
//					score += priCompleted;
//				}
//			}
//		}

		List<ScientificStudy> list00 = getOngoingCollaborativeStudies(s, type);
		if (!list00.isEmpty()) {
			for (ScientificStudy ss : list00) {
				if (allSubject || type == ss.getScience()) {
					score += getPhaseScore(ss);
				}
			}
		}
		
		List<ScientificStudy> list01 = getOngoingPrimaryStudies(s, type);
		if (!list01.isEmpty()) {
			for (ScientificStudy ss : list01) {
				if (allSubject || type == ss.getScience()) {
					score += getPhaseScore(ss);
				}
			}
		}

		List<ScientificStudy> list02 = getAllFailedStudies(s, type);
		if (!list02.isEmpty()) {
			for (ScientificStudy ss : list02) {
				if (allSubject || type == ss.getScience()) {
					score += failed;
				}
			}
		}

		List<ScientificStudy> list03 = getAllCanceledStudies(s, type);
		if (!list03.isEmpty()) {
			for (ScientificStudy ss : list03) {
				if (allSubject || type == ss.getScience()) {
					score += canceled;
				}
			}
		}
		
		List<ScientificStudy> list04 = this.getAllSuccessfulStudies(s, type);
		if (!list04.isEmpty()) {
			for (ScientificStudy ss : list04) {
				if (allSubject || type == ss.getScience()) {				
					score += succeed;
				}
			}
		}
		
		
//		List<ScientificStudy> list05 = getCompletedCollaborativeStudies(s);
//		if (!list05.isEmpty()) {
//			for (ScientificStudy ss : list05) {
//				if (allSubject || type == ss.getScience()) {
//					score += colCompleted;
//				}
//			}
//		}

		score = Math.round(score * 100.0) / 100.0;

		return score;
	}

	/**
	 * Computes the overall relationship score of a settlement
	 * 
	 * @param s Settlement
	 * @param type {@link ScienceType} if null, query all science types
	 * @return the score
	 */
	public double[] getNumScienceStudy(Settlement s, ScienceType type) {
		double[] array = new double[5];
		
		// 0 = succeed 	
		// 1 = failed
		// 2 = canceled
		// 3 = oPri
		// 4 = oCol

		boolean allSubject = false;
		if (type == null)
			allSubject = true;
		
//		List<ScientificStudy> list0 = getCompletedPrimaryStudies(s);
//		if (!list0.isEmpty()) {
//			for (ScientificStudy ss : list0) {
//				if (allSubject || type == ss.getScience()) {				
//					score += priCompleted;
//				}
//			}
//		}

		List<ScientificStudy> list00 = getOngoingCollaborativeStudies(s, type);
		if (!list00.isEmpty()) {
			for (ScientificStudy ss : list00) {
				if (allSubject || type == ss.getScience()) {
					int phase = getPhaseType(ss);
					if (phase != 5)		
						array[4]++; // getPhaseScore(ss);
				}
			}
		}
		
		List<ScientificStudy> list01 = getOngoingPrimaryStudies(s, type);
		if (!list01.isEmpty()) {
			for (ScientificStudy ss : list01) {
				if (allSubject || type == ss.getScience()) {
					int phase = getPhaseType(ss);
					if (phase != 5)		
						array[3]++; // getPhaseScore(ss);
				}
			}
		}

		List<ScientificStudy> list02 = getAllFailedStudies(s, type);
		if (!list02.isEmpty()) {
			for (ScientificStudy ss : list02) {
				if (allSubject || type == ss.getScience()) {
					array[1]++; //score += failed;
				}
			}
		}

		List<ScientificStudy> list03 = getAllCanceledStudies(s, type);
		if (!list03.isEmpty()) {
			for (ScientificStudy ss : list03) {
				if (allSubject || type == ss.getScience()) {
					array[2]++; //score += canceled;
				}
			}
		}
		
		List<ScientificStudy> list04 = this.getAllSuccessfulStudies(s, type);
		if (!list04.isEmpty()) {
			for (ScientificStudy ss : list04) {
				if (allSubject || type == ss.getScience()) {				
					array[0]++;//score += succeed;
				}
			}
		}

		return array;
	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		Iterator<ScientificStudy> i = studies.iterator();
		while (i.hasNext()) {
			i.next().destroy();
		}
		studies.clear();
		studies = null;
	}
}
