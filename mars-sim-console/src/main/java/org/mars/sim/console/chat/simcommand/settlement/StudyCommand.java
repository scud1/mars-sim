package org.mars.sim.console.chat.simcommand.settlement;

import java.util.List;
import java.util.Set;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars.sim.console.chat.simcommand.StructuredResponse;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.science.ScientificStudyManager;
import org.mars_sim.msp.core.structure.Settlement;

/**
 * Command to display people in a Settlement
 * This is a singleton.
 */
public class StudyCommand extends AbstractSettlementCommand {

	public static final ChatCommand STUDY = new StudyCommand();

	private StudyCommand() {
		super("st", "study", "Settlement Science studies");
	}

	/** 
	 * Output the 
	 * @return 
	 */
	@Override
	protected boolean execute(Conversation context, String input, Settlement settlement) {
		ScientificStudyManager manager = context.getSim().getScientificStudyManager();
		
		// TODO this should come off the Settlement object
		List<ScientificStudy> studies = manager.getAllStudies(settlement);
		
		StructuredResponse response = new StructuredResponse();
		
		for (ScientificStudy study : studies) {
			outputStudy(response, study);
			response.append(System.lineSeparator());
		}
		
		context.println(response.getOutput());
		
		return true;
	}

	/**
	 * Display the status of a Scientific Study
	 * @param response
	 * @param study
	 */
	private void outputStudy(StructuredResponse response, ScientificStudy study) {
		response.appendHeading(study.getName());
		response.appendLabeledString("Science", study.getScience().getName());
		response.appendLabeledString("Lead", study.getPrimaryResearcher().getName());
		response.appendLabeledString("Phase", study.getPhase());
		
		switch(study.getPhase()) {
		case ScientificStudy.INVITATION_PHASE:
			response.appendLabelledDigit("Max Collaborators", study.getMaxCollaborators());
			response.appendTableHeading("Invitee", PERSON_WIDTH, "Responded", "Accepted");
			
			Set<Person> c = study.getCollaborativeResearchers();
			for (Person person :  study.getInvitedResearchers()) {
				response.appendTableRow(person.getName(),
										(study.hasInvitedResearcherResponded(person) ? "Yes" : "No"),
										(c.contains(person) ? "Yes" : "No"));
			};
			break;
		
		case ScientificStudy.PAPER_PHASE:
		case ScientificStudy.RESEARCH_PHASE:
			response.append("Researchers");
			response.appendTableHeading("Reseacher", PERSON_WIDTH, "Contribution", "Research %", "Paperwork %");
			response.appendTableRow(study.getPrimaryResearcher().getName(), study.getScience().getName(),
									study.getPrimaryResearchWorkTimeCompleted()
										/ study.getTotalPrimaryResearchWorkTimeRequired(),
					 				study.getPrimaryPaperWorkTimeCompleted()
					 					/ study.getTotalPrimaryPaperWorkTimeRequired());

			// Details about collaborators
			Set<Person> researchers = study.getCollaborativeResearchers();
			double researchExpected = study.getTotalCollaborativeResearchWorkTimeRequired();
			double paperExpected = study.getTotalCollaborativeResearchWorkTimeRequired();
			
			for (Person person : researchers) {
				response.appendTableRow(person.getName(),
										study.getContribution(person),
										study.getCollaborativeResearchWorkTimeCompleted(person)
												/ researchExpected,
										study.getCollaborativePaperWorkTimeCompleted(person)
												/ paperExpected);				
			}			
			break;
			
		case ScientificStudy.PEER_REVIEW_PHASE:
			response.appendLabeledString("Review completed", study.getPeerReviewTimeCompleted() + " msol");
			response.appendLabeledString("Review required", study.getTotalPeerReviewTimeRequired() + " msol");
			break;
			
		default:
			break;
		}
	}
}
